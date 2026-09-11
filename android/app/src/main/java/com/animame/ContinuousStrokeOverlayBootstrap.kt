package com.animame

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import com.animame.editor.AnimationDocument
import com.animame.editor.StrokeData
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Draws connecting segments over procedural stamps so normal brushes read as continuous lines. */
object ContinuousStrokeOverlayBootstrap {
    private const val TAG = "anima-me-continuous-strokes"

    fun install(context: android.content.Context) {
        val activity = context as? Activity ?: return
        if (activity.javaClass.simpleName != "MainActivity") return
        val root = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (root.findViewWithTag<View>(TAG) != null) return
        val editor = readField(activity, "editor") ?: return
        val overlay = Overlay(activity, editor).apply { tag = TAG; isClickable = false }
        root.addView(overlay, FrameLayout.LayoutParams(-1, -1).apply {
            topMargin = dp(activity, 66)
            bottomMargin = dp(activity, 58)
        })
        overlay.bringToFront()
        root.postDelayed(object : Runnable {
            override fun run() { overlay.invalidate(); root.postDelayed(this, 120L) }
        }, 120L)
    }

    private class Overlay(activity: Activity, private val editor: Any) : View(activity) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        override fun onTouchEvent(event: MotionEvent): Boolean = false

        override fun onDraw(canvas: Canvas) {
            val document = readField(editor, "document") as? AnimationDocument ?: return
            if (readBoolean(editor, "playing", false)) return
            val activeId = readString(editor, "activeLayerId", document.activeLayer.id)
            val layer = document.layers.firstOrNull { it.id == activeId } ?: document.activeLayer
            val frame = layer.frameAt(document.currentFrame) ?: return
            val zoom = readFloat(editor, "zoom", 1f).coerceAtLeast(.01f)
            val panX = readFloat(editor, "panX", 0f)
            val panY = readFloat(editor, "panY", 0f)
            val rotation = Math.toRadians(readFloat(editor, "viewportRotation", 0f).toDouble())
            val w = readFloat(editor, "width", width.toFloat())
            val h = readFloat(editor, "height", height.toFloat())
            val left = w * .07f; val right = w * .93f; val top = 66f; val bottom = h - 58f
            val cx = (left + right) * .5f; val cy = (top + bottom) * .5f
            canvas.save()
            canvas.clipRect(left, top + 14f, right, bottom - 14f)
            frame.strokes.forEach { drawStroke(canvas, it, cx, cy, zoom, panX, panY, rotation) }
            canvas.restore()
        }

        private fun drawStroke(c: Canvas, stroke: StrokeData, cx: Float, cy: Float, zoom: Float, panX: Float, panY: Float, rotation: Double) {
            val samples = stroke.samples
            if (samples.size < 2) return
            paint.color = Color.argb(
                (stroke.opacity.coerceIn(0f, 1f) * 255f).toInt().coerceIn(1, 255),
                Color.red(stroke.color), Color.green(stroke.color), Color.blue(stroke.color)
            )
            for (i in 1 until samples.size) {
                val a = samples[i - 1]; val p = samples[i]
                val sa = transform(a.x, a.y, cx, cy, zoom, panX, panY, rotation)
                val sb = transform(p.x, p.y, cx, cy, zoom, panX, panY, rotation)
                val pressure = ((a.pressure + p.pressure) * .5f).coerceIn(.05f, 1f)
                paint.strokeWidth = (stroke.size * pressure * zoom).coerceAtLeast(.5f)
                if (hypot(sb.first - sa.first, sb.second - sa.second) <= paint.strokeWidth * 8f) {
                    c.drawLine(sa.first, sa.second, sb.first, sb.second, paint)
                }
            }
        }

        private fun transform(x: Float, y: Float, cx: Float, cy: Float, zoom: Float, panX: Float, panY: Float, rotation: Double): Pair<Float, Float> {
            val px = x - cx; val py = y - cy
            val co = cos(rotation); val si = sin(rotation)
            return cx + panX + (px * co - py * si) * zoom to cy + panY + (px * si + py * co) * zoom
        }
    }

    private fun readField(target: Any, name: String): Any? = runCatching {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            try {
                val field = type.getDeclaredField(name); field.isAccessible = true; return@runCatching field.get(target)
            } catch (_: NoSuchFieldException) { type = type.superclass }
        }
        null
    }.getOrNull()

    private fun readFloat(target: Any, name: String, fallback: Float): Float = (readField(target, name) as? Number)?.toFloat() ?: fallback
    private fun readBoolean(target: Any, name: String, fallback: Boolean): Boolean = (readField(target, name) as? Boolean) ?: fallback
    private fun readString(target: Any, name: String, fallback: String): String = readField(target, name) as? String ?: fallback
    private fun dp(activity: Activity, value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()
}
