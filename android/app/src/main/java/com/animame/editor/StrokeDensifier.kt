package com.animame.editor

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.hypot

/**
 * Produces evenly spaced samples between input points while preserving
 * pressure, tilt and stylus orientation. This keeps fast stylus strokes
 * visually continuous when input events arrive farther apart than the brush
 * spacing.
 */
object StrokeDensifier {
    fun densify(samples: List<StrokeSample>, maxStep: Float = 8f): List<StrokeSample> {
        if (samples.size < 2) return samples
        val step = maxStep.coerceAtLeast(.5f)
        val result = ArrayList<StrokeSample>(samples.size * 2)
        result += samples.first()
        for (i in 1 until samples.size) {
            val a = samples[i - 1]
            val b = samples[i]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val segments = ceil(hypot(dx, dy) / step).toInt().coerceIn(1, 64)
            for (segment in 1..segments) {
                val t = segment.toFloat() / segments.toFloat()
                result += StrokeSample(
                    x = a.x + (b.x - a.x) * t,
                    y = a.y + (b.y - a.y) * t,
                    pressure = a.pressure + (b.pressure - a.pressure) * t,
                    timeMs = (a.timeMs + (b.timeMs - a.timeMs) * t).toLong(),
                    tilt = a.tilt + (b.tilt - a.tilt) * t,
                    orientation = lerpAngle(a.orientation, b.orientation, t)
                )
            }
        }
        return result
    }

    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var delta = b - a
        while (delta > PI) delta -= (PI * 2f).toFloat()
        while (delta < -PI) delta += (PI * 2f).toFloat()
        return a + delta * t
    }
}
