package com.animame.editor

import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Drawing pipeline kept deliberately separate from the renderer:
 * - smoothing makes the sampled trajectory continuous/clean;
 * - stabilization corrects hand wobble and can be tuned independently;
 * - anti-aliasing remains a renderer concern.
 */
object StrokeProcessingEngine {
    fun process(
        input: List<Stabilizer.Sample>,
        smoothing: Boolean,
        stabilizerPercent: Float,
        realTime: Boolean = true
    ): List<Stabilizer.Sample> {
        if (input.isEmpty()) return emptyList()
        val stabilized = if (stabilizerPercent > 0f) {
            stabilize(input, stabilizerPercent, realTime)
        } else input.map { it.copy() }
        return if (smoothing) smooth(stabilized) else stabilized
    }

    /** Causal stabilization: stronger correction for higher settings and faster motion. */
    fun stabilize(input: List<Stabilizer.Sample>, percent: Float, realTime: Boolean): List<Stabilizer.Sample> {
        if (input.size < 2 || percent <= 0f) return input.map { it.copy() }
        val strength = (percent / 100f).coerceIn(0f, 1f)
        val out = ArrayList<Stabilizer.Sample>(input.size)
        var x = input.first().x
        var y = input.first().y
        out += input.first().copy()
        for (i in 1 until input.size) {
            val raw = input[i]
            val prev = input[i - 1]
            val speed = hypot(raw.x - prev.x, raw.y - prev.y)
            val fastBoost = if (realTime) (speed / 24f).coerceIn(0f, 1f) * 0.35f else 0f
            val correction = (strength * 0.72f + fastBoost * strength).coerceIn(0f, 0.94f)
            val follow = 1f - correction
            x += (raw.x - x) * follow
            y += (raw.y - y) * follow
            out += raw.copy(x = x, y = y)
        }
        if (out.size > 2) {
            val last = input.last()
            out[out.lastIndex] = last.copy()
        }
        return out
    }

    /**
     * Geometry smoothing only. This does not turn anti-aliasing on and does not
     * replace stabilization. It removes sample-to-sample jitter while retaining
     * endpoints, so disabling it can intentionally expose the raw/pixel-like path.
     */
    fun smooth(input: List<Stabilizer.Sample>): List<Stabilizer.Sample> {
        if (input.size < 3) return input.map { it.copy() }
        val out = ArrayList<Stabilizer.Sample>(input.size)
        out += input.first().copy()
        for (i in 1 until input.lastIndex) {
            val a = input[i - 1]
            val b = input[i]
            val c = input[i + 1]
            val ab = hypot(b.x - a.x, b.y - a.y)
            val bc = hypot(c.x - b.x, c.y - b.y)
            val total = (ab + bc).coerceAtLeast(0.001f)
            val wa = 0.25f + 0.25f * (bc / total)
            val wb = 0.50f
            val wc = 0.25f + 0.25f * (ab / total)
            out += b.copy(
                x = a.x * wa + b.x * wb + c.x * wc,
                y = a.y * wa + b.y * wb + c.y * wc
            )
        }
        out += input.last().copy()
        return out
    }

    fun resample(input: List<Stabilizer.Sample>, spacingPx: Float): List<Stabilizer.Sample> {
        if (input.size < 2 || spacingPx <= 0f) return input.map { it.copy() }
        val out = ArrayList<Stabilizer.Sample>()
        out += input.first().copy()
        var carry = 0f
        for (i in 1 until input.size) {
            val a = input[i - 1]
            val b = input[i]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val d = sqrt(dx * dx + dy * dy)
            if (d <= 0f) continue
            var traveled = spacingPx - carry
            while (traveled < d) {
                val t = traveled / d
                out += a.copy(
                    x = a.x + dx * t,
                    y = a.y + dy * t,
                    pressure = a.pressure + (b.pressure - a.pressure) * t,
                    timeMs = a.timeMs + ((b.timeMs - a.timeMs) * t).toLong()
                )
                traveled += spacingPx
            }
            carry = (d - (traveled - spacingPx)).coerceIn(0f, spacingPx)
        }
        out += input.last().copy()
        return out
    }
}
