package com.animame.editor

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import kotlin.math.hypot

/**
 * Continuous bitmap stroke renderer. Smoothing here is geometric/rendering smoothing,
 * deliberately independent from the hand-trace stabilizer.
 */
object ContinuousStrokeRenderer {
    fun draw(canvas: Canvas, samples: List<StrokeSample>, paint: Paint, smooth: Boolean) {
        if (samples.isEmpty()) return
        if (samples.size == 1) {
            val p = samples[0]
            canvas.drawCircle(p.x, p.y, paint.strokeWidth * 0.5f, paint)
            return
        }
        if (!smooth) {
            var previous = samples[0]
            for (i in 1 until samples.size) {
                val current = samples[i]
                canvas.drawLine(previous.x, previous.y, current.x, current.y, paint)
                previous = current
            }
            return
        }

        // Quadratic midpoint interpolation produces one continuous path instead of
        // a visible chain of independent line segments.
        val path = Path()
        val first = samples[0]
        path.moveTo(first.x, first.y)
        for (i in 1 until samples.lastIndex) {
            val current = samples[i]
            val next = samples[i + 1]
            val midX = (current.x + next.x) * 0.5f
            val midY = (current.y + next.y) * 0.5f
            path.quadTo(current.x, current.y, midX, midY)
        }
        val last = samples.last()
        path.lineTo(last.x, last.y)
        canvas.drawPath(path, paint)
    }

    fun resample(samples: List<StrokeSample>, spacingPx: Float): List<StrokeSample> {
        if (samples.size < 2 || spacingPx <= 0f) return samples
        val out = ArrayList<StrokeSample>()
        out += samples.first()
        var carry = 0f
        var a = samples.first()
        for (i in 1 until samples.size) {
            val b = samples[i]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val distance = hypot(dx, dy)
            if (distance <= 0f) continue
            var travelled = spacingPx - carry
            while (travelled <= distance) {
                val t = travelled / distance
                out += StrokeSample(
                    x = a.x + dx * t,
                    y = a.y + dy * t,
                    pressure = a.pressure + (b.pressure - a.pressure) * t,
                    timeMs = (a.timeMs + ((b.timeMs - a.timeMs) * t)).toLong(),
                    tilt = a.tilt + (b.tilt - a.tilt) * t,
                    orientation = a.orientation + (b.orientation - a.orientation) * t
                )
                travelled += spacingPx
            }
            carry = (carry + distance) % spacingPx
            a = b
        }
        if (out.lastOrNull() != samples.last()) out += samples.last()
        return out
    }
}
