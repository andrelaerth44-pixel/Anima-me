package com.animame

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.animame.editor.StrokeData
import com.animame.editor.StrokeSample
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Lightweight overlay used by the editor while a stroke is being previewed. */
object ContinuousStrokeOverlayBootstrap {
    private const val TAG = "anima-me-continuous-stroke"

    fun install(context: android.content.Context) {
        val activity = context as? Activity ?: return
        if (activity.javaClass.simpleName != "MainActivity") return
        val root = activity.findViewById<View>(android.R.id.content) ?: return
        if (root.findViewWithTag<View>(TAG) != null) return
        val overlay = View(activity).apply { tag = TAG; visibility = View.GONE }
        (root as? android.view.ViewGroup)?.addView(overlay, android.view.ViewGroup.LayoutParams(1, 1))
    }

    fun drawStroke(c: Canvas, stroke: StrokeData, samples: List<StrokeSample>, cx: Float, cy: Float, zoom: Float, panX: Float, panY: Float, rotation: Double) {
        if (samples.size < 2) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb((stroke.opacity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(1, 255), Color.red(stroke.color), Color.green(stroke.color), Color.blue(stroke.color))
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        for (i in 1 until samples.size) {
            val a = samples[i - 1]
            val p = samples[i]
            val sa = transform(a.x, a.y, cx, cy, zoom, panX, panY, rotation)
            val sb = transform(p.x, p.y, cx, cy, zoom, panX, panY, rotation)
            val pressure = ((a.pressure + p.pressure) * .5f).coerceIn(.05f, 1f)
            paint.strokeWidth = (stroke.size * pressure * zoom).coerceAtLeast(.5f)
            if (hypot(sb.first - sa.first, sb.second - sa.second) <= paint.strokeWidth * 8f) c.drawLine(sa.first, sa.second, sb.first, sb.second, paint)
        }
    }

    private fun transform(x: Float, y: Float, cx: Float, cy: Float, zoom: Float, panX: Float, panY: Float, rotation: Double): Pair<Float, Float> {
        val px = x - cx
        val py = y - cy
        val co = cos(rotation).toFloat()
        val si = sin(rotation).toFloat()
        return cx + panX + (px * co - py * si) * zoom to cy + panY + (px * si + py * co) * zoom
    }
}
