package com.animame.editor

import android.graphics.PointF
import kotlin.math.hypot

/** Independent live drawing processing: Smooth != Stabilizer != Anti-alias. */
object StrokeProcessingEngine {
    private fun clone(s: Stabilizer.Sample, x: Float = s.point.x, y: Float = s.point.y): Stabilizer.Sample =
        s.copy(point = PointF(x, y))

    fun process(input: List<Stabilizer.Sample>, smoothing: Boolean, stabilizerPercent: Float, realTime: Boolean = true): List<Stabilizer.Sample> {
        if (input.isEmpty()) return emptyList()
        val stabilized = if (stabilizerPercent > 0f) stabilize(input, stabilizerPercent, realTime) else input.map { clone(it) }
        return if (smoothing) smooth(stabilized) else stabilized
    }

    fun stabilize(input: List<Stabilizer.Sample>, percent: Float, realTime: Boolean): List<Stabilizer.Sample> {
        if (input.size < 2 || percent <= 0f) return input.map { clone(it) }
        val strength = (percent / 100f).coerceIn(0f, 1f)
        val out = ArrayList<Stabilizer.Sample>(input.size)
        var x = input.first().point.x
        var y = input.first().point.y
        out += clone(input.first())
        for (i in 1 until input.size) {
            val raw = input[i]
            val prev = input[i - 1]
            val speed = hypot((raw.point.x - prev.point.x).toDouble(), (raw.point.y - prev.point.y).toDouble()).toFloat()
            val fastBoost = if (realTime) (speed / 24f).coerceIn(0f, 1f) * 0.35f else 0f
            val correction = (strength * 0.72f + fastBoost * strength).coerceIn(0f, 0.94f)
            val follow = 1f - correction
            x += (raw.point.x - x) * follow
            y += (raw.point.y - y) * follow
            out += clone(raw, x, y)
        }
        out[out.lastIndex] = clone(input.last())
        return out
    }

    fun smooth(input: List<Stabilizer.Sample>): List<Stabilizer.Sample> {
        if (input.size < 3) return input.map { clone(it) }
        val out = ArrayList<Stabilizer.Sample>(input.size)
        out += clone(input.first())
        for (i in 1 until input.lastIndex) {
            val a = input[i - 1]
            val b = input[i]
            val c = input[i + 1]
            val ab = hypot((b.point.x - a.point.x).toDouble(), (b.point.y - a.point.y).toDouble()).toFloat()
            val bc = hypot((c.point.x - b.point.x).toDouble(), (c.point.y - b.point.y).toDouble()).toFloat()
            val total = (ab + bc).coerceAtLeast(0.001f)
            val wa = 0.25f + 0.25f * (bc / total)
            val wb = 0.5f
            val wc = 0.25f + 0.25f * (ab / total)
            out += clone(b, a.point.x * wa + b.point.x * wb + c.point.x * wc, a.point.y * wa + b.point.y * wb + c.point.y * wc)
        }
        out += clone(input.last())
        return out
    }

    fun resample(input: List<Stabilizer.Sample>, spacingPx: Float): List<Stabilizer.Sample> {
        if (input.size < 2 || spacingPx <= 0f) return input.map { clone(it) }
        val out = ArrayList<Stabilizer.Sample>()
        out += clone(input.first())
        var carry = 0f
        for (i in 1 until input.size) {
            val a = input[i - 1]
            val b = input[i]
            val dx = b.point.x - a.point.x
            val dy = b.point.y - a.point.y
            val d = hypot(dx.toDouble(), dy.toDouble()).toFloat()
            if (d <= 0f) continue
            var traveled = spacingPx - carry
            while (traveled < d) {
                val t = traveled / d
                out += clone(a, a.point.x + dx * t, a.point.y + dy * t)
                traveled += spacingPx
            }
            carry = (d - (traveled - spacingPx)).coerceIn(0f, spacingPx)
        }
        out += clone(input.last())
        return out
    }
}
