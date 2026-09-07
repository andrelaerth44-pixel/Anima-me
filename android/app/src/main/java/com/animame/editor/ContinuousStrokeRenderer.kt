package com.animame.editor

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Continuous stroke renderer.
 * Smooth is geometric/rendering continuity only; trajectory correction belongs to
 * StrokeProcessingEngine's Stabilizer stage.
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
        val path = Path()
        path.moveTo(samples.first().x, samples.first().y)
        for (i in 1 until samples.lastIndex) {
            val current = samples[i]
            val next = samples[i + 1]
            path.quadTo(current.x, current.y, (current.x + next.x) * 0.5f, (current.y + next.y) * 0.5f)
        }
        val last = samples.last()
        path.lineTo(last.x, last.y)
        canvas.drawPath(path, paint)
    }

    /** Pressure/tilt-aware renderer that preserves dynamics along the entire stroke. */
    fun drawPressureAware(
        canvas: Canvas,
        samples: List<StrokeSample>,
        baseSize: Float,
        baseOpacity: Float,
        settings: BrushSettings,
        color: Int,
        smooth: Boolean,
        antiAlias: Boolean
    ) {
        if (samples.isEmpty()) return
        val bs = settings.copy(size = baseSize, opacity = baseOpacity).normalized()
        val spacing = max(0.75f, baseSize * if (smooth) 0.16f else 0.22f)
        val points = resample(samples, spacing)
        if (points.size == 1) {
            val p = points[0]
            val paint = makePaint(bs, color, antiAlias, p.pressure, p.tilt, 0f, 1f, true)
            if (bs.eraser) paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
            canvas.drawCircle(p.x, p.y, max(0.25f, bs.radiusFor(p.pressure, p.tilt)), paint)
            paint.xfermode = null
            return
        }

        var distance = 0f
        val total = max(0.001f, pathLength(points))
        for (i in 1 until points.size) {
            val a = points[i - 1]
            val b = points[i]
            val segment = hypot(b.x - a.x, b.y - a.y)
            val pressure = (a.pressure + b.pressure) * 0.5f
            val tilt = (a.tilt + b.tilt) * 0.5f
            val midDistance = distance + segment * 0.5f
            val paint = makePaint(bs, color, antiAlias, pressure, tilt, midDistance, total, false)
            if (bs.eraser) paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)

            if (smooth) {
                val before = if (i > 1) points[i - 2] else a
                val after = if (i + 1 < points.size) points[i + 1] else b
                val path = Path()
                path.moveTo((before.x + a.x) * 0.5f, (before.y + a.y) * 0.5f)
                path.quadTo(a.x, a.y, (a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f)
                path.quadTo(b.x, b.y, (b.x + after.x) * 0.5f, (b.y + after.y) * 0.5f)
                canvas.drawPath(path, paint)
            } else {
                canvas.drawLine(a.x, a.y, b.x, b.y, paint)
            }
            paint.xfermode = null
            distance += segment
        }
    }

    private fun makePaint(
        bs: BrushSettings,
        color: Int,
        antiAlias: Boolean,
        pressure: Float,
        tilt: Float,
        distance: Float,
        total: Float,
        fill: Boolean
    ): Paint = Paint(if (antiAlias && bs.antialias) Paint.ANTI_ALIAS_FLAG else 0).apply {
        style = if (fill) Paint.Style.FILL else Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = max(0.5f, bs.radiusFor(pressure, tilt) * 2f)
        alpha = (bs.opacityFor(pressure, tilt, distance, total) * 255f).roundToInt().coerceIn(0, 255)
        this.color = color
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

    private fun pathLength(samples: List<StrokeSample>): Float {
        var d = 0f
        for (i in 1 until samples.size) d += hypot(samples[i].x - samples[i - 1].x, samples[i].y - samples[i - 1].y)
        return d
    }
}
