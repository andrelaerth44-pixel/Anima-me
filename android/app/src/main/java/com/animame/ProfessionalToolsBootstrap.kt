package com.animame

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.animame.editor.AnimationDocument
import com.animame.editor.DrawingFrame
import com.animame.editor.EditorToolEngine
import com.animame.editor.LiquifyEngine
import com.animame.editor.StrokeData
import com.animame.editor.StrokeSample
import java.lang.reflect.Field
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Interactive tool bridge installed over MainActivity's existing canvas.
 * Brush/eraser remain owned by MainActivity; the overlay owns the tools that
 * previously only changed the selected button: bucket, lasso, transform,
 * eyedropper and liquify. It deliberately never touches camera keyframes.
 */
object ProfessionalToolsBootstrap {
    private const val TAG = "anima-me-professional-tools"

    fun install(context: android.content.Context) {
        val activity = context as? Activity ?: return
        if (activity.javaClass.simpleName != "MainActivity") return
        val content = activity.findViewById<android.widget.FrameLayout>(android.R.id.content) ?: return
        if (content.findViewWithTag<View>(TAG) != null) return
        val editor = readField(activity, "editor") ?: return
        val overlay = ToolOverlay(activity, editor).apply { tag = TAG }
        content.addView(overlay, android.widget.FrameLayout.LayoutParams(-1, -1).apply {
            topMargin = dp(activity, 66)
            bottomMargin = dp(activity, 58)
        })
        overlay.bringToFront()
    }

    private class ToolOverlay(private val activity: Activity, private val editor: Any) : View(activity) {
        private val engine = EditorToolEngine()
        private var polygon = mutableListOf<PointF>()
        private var selection = EditorToolEngine.Selection()
        private var active = false
        private var startX = 0f
        private var startY = 0f
        private var lastX = 0f
        private var lastY = 0f
        private var changed = false
        private var undoFrame: DrawingFrame? = null
        private var liquifyPath = mutableListOf<StrokeSample>()
        private var cursorX = -1f
        private var cursorY = -1f
        private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.CYAN
        }

        override fun onDraw(canvas: Canvas) {
            val tool = currentTool()
            if (tool == EditorToolEngine.ToolType.LASSO && polygon.size > 1) {
                for (i in 1 until polygon.size) canvas.drawLine(polygon[i - 1].x, polygon[i - 1].y, polygon[i].x, polygon[i].y, overlayPaint)
                if (!active && polygon.size > 2) canvas.drawLine(polygon.last().x, polygon.last().y, polygon.first().x, polygon.first().y, overlayPaint)
            }
            if (selection.strokeIds.isNotEmpty() && tool == EditorToolEngine.ToolType.TRANSFORM) {
                val frame = currentFrame()
                val points = frame?.strokes?.filter { selection.contains(it) }?.flatMap { it.samples } ?: emptyList()
                if (points.isNotEmpty()) {
                    val minX = points.minOf { it.x }; val maxX = points.maxOf { it.x }
                    val minY = points.minOf { it.y }; val maxY = points.maxOf { it.y }
                    val a = screenPoint(minX, minY); val b = screenPoint(maxX, maxY)
                    canvas.drawRect(a.first - 8f, a.second - 8f, b.first + 8f, b.second + 8f, overlayPaint)
                }
            }
            if (tool == EditorToolEngine.ToolType.LIQUIFY && cursorX >= 0f) {
                val radius = readFloat(editor, "liquifyRadius", 80f)
                canvas.drawCircle(cursorX, cursorY, radius.coerceAtMost(300f), overlayPaint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val tool = currentTool()
            if (tool == EditorToolEngine.ToolType.BRUSH || tool == EditorToolEngine.ToolType.ERASER) return false
            if (tool == EditorToolEngine.ToolType.LIQUIFY) return handleLiquify(event)
            if (tool == EditorToolEngine.ToolType.LASSO) return handleLasso(event)
            if (tool == EditorToolEngine.ToolType.TRANSFORM) return handleTransform(event)
            if (tool == EditorToolEngine.ToolType.EYEDROPPER) return handleEyedropper(event)
            if (tool == EditorToolEngine.ToolType.BUCKET) return handleBucket(event)
            return false
        }

        private fun handleLasso(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    polygon.clear(); polygon += PointF(e.x, e.y); active = true; invalidate(); return true
                }
                MotionEvent.ACTION_MOVE -> {
                    polygon += PointF(e.x, e.y); invalidate(); return true
                }
                MotionEvent.ACTION_UP -> {
                    polygon += PointF(e.x, e.y)
                    val frame = currentFrame() ?: return true
                    val canvasPolygon = polygon.map { screenToCanvas(it.x, it.y) }
                    selection = engine.lassoSelect(frame, canvasPolygon)
                    active = false
                    invalidate(); refresh()
                    Toast.makeText(activity, "Laço: ${selection.strokeIds.size} traço(s) selecionado(s)", Toast.LENGTH_SHORT).show()
                    return true
                }
            }
            return true
        }

        private fun handleTransform(e: MotionEvent): Boolean {
            val frame = currentFrame() ?: return true
            if (selection.strokeIds.isEmpty()) {
                if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                    val p = screenToCanvas(e.x, e.y)
                    val hit = frame.strokes.lastOrNull { stroke -> stroke.samples.any { hypot(it.x - p.first, it.y - p.second) <= max(12f, stroke.size * .75f) } }
                    if (hit != null) selection = EditorToolEngine.Selection(setOf(hit.id))
                    else Toast.makeText(activity, "Use Laço para selecionar vários traços", Toast.LENGTH_SHORT).show()
                }
                invalidate(); return true
            }
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = e.x; startY = e.y; lastX = e.x; lastY = e.y; active = true
                    undoFrame = cloneFrame(frame); changed = false; return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val a = screenToCanvas(lastX, lastY); val b = screenToCanvas(e.x, e.y)
                    val dx = b.first - a.first; val dy = b.second - a.second
                    if (dx != 0f || dy != 0f) {
                        engine.transform(frame, selection, EditorToolEngine.Transform(translateX = dx, translateY = dy))
                        changed = true
                    }
                    lastX = e.x; lastY = e.y; refresh(); invalidate(); return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    active = false
                    if (changed) Toast.makeText(activity, "Transformação aplicada", Toast.LENGTH_SHORT).show()
                    invalidate(); refresh(); return true
                }
            }
            return true
        }

        private fun handleEyedropper(e: MotionEvent): Boolean {
            if (e.actionMasked != MotionEvent.ACTION_DOWN) return true
            val frame = currentFrame() ?: return true
            val p = screenToCanvas(e.x, e.y)
            val hit = frame.strokes.flatMap { s -> s.samples.map { s to it } }
                .minByOrNull { (_, sample) -> hypot(sample.x - p.first, sample.y - p.second) }
            if (hit != null && hypot(hit.second.x - p.first, hit.second.y - p.second) < 100f) {
                writeField(editor, "accent", hit.first.color)
                Toast.makeText(activity, "Cor capturada", Toast.LENGTH_SHORT).show()
                refresh()
            } else Toast.makeText(activity, "Nenhuma cor próxima", Toast.LENGTH_SHORT).show()
            return true
        }

        private fun handleBucket(e: MotionEvent): Boolean {
            if (e.actionMasked != MotionEvent.ACTION_DOWN) return true
            val frame = currentFrame() ?: return true
            val p = screenToCanvas(e.x, e.y)
            val boundary = frame.strokes.asSequence()
                .filter { it.samples.size >= 3 && hypot(it.samples.first().x - it.samples.last().x, it.samples.first().y - it.samples.last().y) <= max(8f, it.size * 1.5f) }
                .firstOrNull { stroke -> pointInPolygon(p, stroke.samples.map { PointF(it.x, it.y) }) }
            if (boundary == null) {
                Toast.makeText(activity, "O Balde precisa de um contorno fechado", Toast.LENGTH_SHORT).show()
                return true
            }
            undoFrame = cloneFrame(frame)
            val poly = boundary.samples.map { PointF(it.x, it.y) }
            val minX = poly.minOf { it.x }; val maxX = poly.maxOf { it.x }
            val minY = poly.minOf { it.y }; val maxY = poly.maxOf { it.y }
            val size = readFloat(editor, "brushSize", 12f).coerceAtLeast(1f)
            val color = readInt(editor, "accent", ThemeColorStore.DEFAULT)
            val brushId = readString(editor, "brushId", "canvas_1")
            val settings = com.animame.editor.BrushDefaults.forPreset(brushId).copy(size = size).normalized()
            var y = minY + size * .5f
            var lines = 0
            while (y <= maxY && lines < 2048) {
                val xs = mutableListOf<Float>()
                for (i in poly.indices) {
                    val a = poly[i]; val b = poly[(i + 1) % poly.size]
                    if ((a.y > y) != (b.y > y)) {
                        val x = a.x + (y - a.y) * (b.x - a.x) / (b.y - a.y)
                        xs += x
                    }
                }
                xs.sort()
                var i = 0
                while (i + 1 < xs.size) {
                    val x1 = xs[i]; val x2 = xs[i + 1]
                    if (x2 - x1 >= size * .15f) {
                        val samples = mutableListOf<StrokeSample>()
                        var x = x1
                        val step = max(.75f, size * .55f)
                        while (x <= x2) { samples += StrokeSample(x, y, 1f, System.currentTimeMillis()); x += step }
                        if (samples.size >= 2) frame.strokes += StrokeData(brushId = brushId, color = color, size = size, opacity = 1f, settings = settings, samples = samples)
                    }
                    i += 2
                }
                y += max(1f, size * .65f); lines++
            }
            refresh(); invalidate(); Toast.makeText(activity, "Balde preenchido", Toast.LENGTH_SHORT).show(); return true
        }

        private fun handleLiquify(e: MotionEvent): Boolean {
            val frame = currentFrame() ?: return true
            val radius = readFloat(editor, "liquifyRadius", 80f).coerceIn(4f, 1000f)
            val strength = readFloat(editor, "liquifyStrength", .45f).coerceIn(0f, 1f)
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    liquifyPath.clear(); val p = screenToCanvas(e.x, e.y); liquifyPath += StrokeSample(p.first, p.second, 1f, e.eventTime)
                    cursorX = e.x; cursorY = e.y; undoFrame = cloneFrame(frame); invalidate(); return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val p = screenToCanvas(e.x, e.y); val previous = liquifyPath.last(); liquifyPath += StrokeSample(p.first, p.second, 1f, e.eventTime)
                    val modeName = readString(editor, "liquifyMode", LiquifyEngine.Mode.PUSH.name)
                    val mode = runCatching { LiquifyEngine.Mode.valueOf(modeName) }.getOrDefault(LiquifyEngine.Mode.PUSH)
                    val settings = LiquifyEngine.Settings(radius, strength, mode)
                    frame.strokes.forEach { stroke ->
                        val changedSamples = LiquifyEngine.apply(stroke.samples, p.first, p.second, p.first - previous.x, p.second - previous.y, settings)
                        stroke.samples.clear(); stroke.samples.addAll(changedSamples)
                    }
                    cursorX = e.x; cursorY = e.y; refresh(); invalidate(); return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    liquifyPath.clear(); cursorX = -1f; cursorY = -1f; refresh(); invalidate(); return true
                }
            }
            return true
        }

        private fun currentTool(): EditorToolEngine.ToolType = runCatching {
            @Suppress("UNCHECKED_CAST")
            readField(editor, "tool") as EditorToolEngine.ToolType
        }.getOrDefault(EditorToolEngine.ToolType.BRUSH)

        private fun currentFrame(): DrawingFrame? {
            val document = readField(editor, "document") as? AnimationDocument ?: return null
            return document.activeLayer.ensureFrame(document.currentFrame)
        }

        private fun refresh() {
            runCatching {
                editor.javaClass.getMethod("invalidate").invoke(editor)
                editor.javaClass.getMethod("refreshTimeline").invoke(editor)
            }
        }

        private fun screenToCanvas(x: Float, y: Float): Pair<Float, Float> {
            val w = (readField(editor, "width") as? Int)?.toFloat() ?: width.toFloat()
            val h = (readField(editor, "height") as? Int)?.toFloat() ?: height.toFloat()
            val left = w * .07f; val right = w * .93f; val top = 66f; val bottom = h - 58f
            val cx = (left + right) * .5f; val cy = (top + bottom) * .5f
            val zoom = readFloat(editor, "zoom", 1f).coerceAtLeast(.01f)
            val panX = readFloat(editor, "panX", 0f); val panY = readFloat(editor, "panY", 0f)
            val rotation = Math.toRadians(readFloat(editor, "viewportRotation", 0f).toDouble())
            val px = x - cx - panX; val py = y - cy - panY
            val cosA = cos(-rotation); val sinA = sin(-rotation)
            val rx = px * cosA - py * sinA; val ry = px * sinA + py * cosA
            return cx + rx / zoom to cy + ry / zoom
        }

        private fun screenPoint(x: Float, y: Float): Pair<Float, Float> {
            val w = (readField(editor, "width") as? Int)?.toFloat() ?: width.toFloat()
            val h = (readField(editor, "height") as? Int)?.toFloat() ?: height.toFloat()
            val left = w * .07f; val right = w * .93f; val top = 66f; val bottom = h - 58f
            val cx = (left + right) * .5f; val cy = (top + bottom) * .5f
            val zoom = readFloat(editor, "zoom", 1f); val panX = readFloat(editor, "panX", 0f); val panY = readFloat(editor, "panY", 0f)
            val a = Math.toRadians(readFloat(editor, "viewportRotation", 0f).toDouble())
            val px = (x - cx) * zoom; val py = (y - cy) * zoom
            val rx = px * cos(a) - py * sin(a); val ry = px * sin(a) + py * cos(a)
            return cx + rx + panX to cy + ry + panY
        }

        private fun cloneFrame(frame: DrawingFrame): DrawingFrame {
            val copy = DrawingFrame(exposure = frame.exposure)
            frame.strokes.forEach { s -> copy.strokes += s.copy(samples = s.samples.map { it.copy() }.toMutableList()) }
            return copy
        }

        private fun pointInPolygon(point: Pair<Float, Float>, polygon: List<PointF>): Boolean {
            if (polygon.size < 3) return false
            var inside = false; var j = polygon.lastIndex
            for (i in polygon.indices) {
                val a = polygon[i]; val b = polygon[j]
                if ((a.y > point.second) != (b.y > point.second) && point.first < (b.x - a.x) * (point.second - a.y) / ((b.y - a.y).takeIf { it != 0f } ?: Float.MIN_VALUE) + a.x) inside = !inside
                j = i
            }
            return inside
        }
    }

    private fun readField(target: Any, name: String): Any? {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            val result = runCatching {
                val field: Field = type!!.getDeclaredField(name); field.isAccessible = true; field.get(target)
            }.getOrNull()
            if (result != null) return result
            type = type.superclass
        }
        return null
    }
    private fun writeField(target: Any, name: String, value: Any?) {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            runCatching { val field = type!!.getDeclaredField(name); field.isAccessible = true; field.set(target, value); return }
            type = type.superclass
        }
    }
    private fun readFloat(target: Any, name: String, fallback: Float): Float = runCatching { (readField(target, name) as Number).toFloat() }.getOrDefault(fallback)
    private fun readInt(target: Any, name: String, fallback: Int): Int = runCatching { (readField(target, name) as Number).toInt() }.getOrDefault(fallback)
    private fun readString(target: Any, name: String, fallback: String): String = readField(target, name) as? String ?: fallback
    private fun dp(activity: Activity, value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()
    private fun max(a: Float, b: Float) = if (a > b) a else b
}
