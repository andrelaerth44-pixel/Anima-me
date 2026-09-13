package com.animame.editor

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Stroke correction inspired by the interaction model of ibisPaint and RoughAnimator.
 * It is deliberately implemented as an independent algorithm, not a copy of either app.
 */
object StrokeStabilizer {
    enum class Mode { REAL_TIME, AFTER }

    data class Settings(
        val constant: Float = 0f,
        val fastStrokes: Float = 0f,
        val smoothing: Float = 0f,
        val prediction: Float = 0f,
        val mode: Mode = Mode.REAL_TIME,
        val legacy: Boolean = false,
        val forceFade: Boolean = false,
        val fadeStart: Float = 0f,
        val fadeEnd: Float = 1f
    ) {
        fun normalized() = copy(
            constant = constant.coerceIn(0f, 100f),
            fastStrokes = fastStrokes.coerceIn(0f, 100f),
            smoothing = smoothing.coerceIn(0f, 100f),
            prediction = prediction.coerceIn(0f, 100f),
            fadeStart = fadeStart.coerceIn(0f, 1f),
            fadeEnd = fadeEnd.coerceIn(0f, 1f)
        )
    }

    fun realTime(samples: List<StrokeSample>, settings: Settings): List<StrokeSample> {
        val s = settings.normalized()
        if (samples.size < 2) return samples
        val out = ArrayList<StrokeSample>(samples.size)
        var previous = samples.first()
        out += previous
        for (i in 1 until samples.size) {
            val raw = samples[i]
            val dx = raw.x - samples[i - 1].x
            val dy = raw.y - samples[i - 1].y
            val speed = hypot(dx, dy) / max(1L, raw.timeMs - samples[i - 1].timeMs).toFloat()
            val speed01 = (speed / 2.5f).coerceIn(0f, 1f)
            val correction = ((s.constant / 100f) + (s.fastStrokes / 100f) * speed01).coerceIn(0f, .96f)
            val predictedX = raw.x + dx * (s.prediction / 100f) * .35f
            val predictedY = raw.y + dy * (s.prediction / 100f) * .35f
            val targetX = if (s.prediction > 0f) predictedX else raw.x
            val targetY = if (s.prediction > 0f) predictedY else raw.y
            val x = previous.x + (targetX - previous.x) * (1f - correction)
            val y = previous.y + (targetY - previous.y) * (1f - correction)
            previous = StrokeSample(x, y, raw.pressure, raw.timeMs)
            out += previous
        }
        return out
    }

    fun after(samples: List<StrokeSample>, settings: Settings): List<StrokeSample> {
        val s = settings.normalized()
        if (samples.size < 3) return samples
        val passes = max(1, (s.smoothing / 18f).toInt())
        var out = samples
        repeat(passes) {
            out = out.mapIndexed { i, p ->
                if (i == 0 || i == out.lastIndex) p
                else {
                    val a = out[i - 1]
                    val b = out[i + 1]
                    val amount = (s.smoothing / 100f).coerceIn(0f, .92f)
                    StrokeSample(
                        p.x + ((a.x + b.x) * .5f - p.x) * amount,
                        p.y + ((a.y + b.y) * .5f - p.y) * amount,
                        p.pressure,
                        p.timeMs
                    )
                }
            }
        }
        return out
    }

    fun apply(samples: List<StrokeSample>, settings: Settings): List<StrokeSample> {
        val s = settings.normalized()
        if (s.legacy) return after(samples, s)
        return when (s.mode) {
            Mode.REAL_TIME -> realTime(samples, s)
            Mode.AFTER -> after(samples, s)
        }
    }
}
