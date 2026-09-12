package com.animame

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import com.animame.editor.AnimationDocument
import com.animame.editor.BrushDefaults
import com.animame.editor.DrawingFrame
import com.animame.editor.EditorToolEngine
import com.animame.editor.LiquifyEngine
import com.animame.editor.StrokeCorrectionStore
import com.animame.editor.StrokeData
import com.animame.editor.StrokeSample
import java.lang.reflect.Field
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/** Interactive routing for tools that are not owned by MainActivity's brush gesture. */
object ProfessionalToolsBootstrap {
    private const val TAG = "anima-me-professional-tools"
    private const val EXT_TAG = "anima-me-drawing-tool-extension"

    fun install(context: android.content.Context) {
        val activity = context as? Activity ?: return
        if (activity.javaClass.simpleName != "MainActivity") return
        val content = activity.findViewById<android.widget.FrameLayout>(android.R.id.content) ?: return
        val editor = readField(activity, "editor") ?: return

        if (content.findViewWithTag<View>(TAG) == null) {
            val overlay = ToolOverlay(activity, editor).apply { tag = TAG }
            content.addView(overlay, android.widget.FrameLayout.LayoutParams(-1, -1).apply {
                topMargin = dp(activity, 66)
                bottomMargin = dp(activity, 58)
            })
            overlay.bringToFront()
        }
        installToolbarExtensions(activity, content, editor)
    }

    private fun installToolbarExtensions(activity: Activity, content: ViewGroup, editor: Any) {
        val root = content.getChildAt(0) as? ViewGroup ?: return
        val toolbar = root.getChildAt(1) as? LinearLayout ?: return
        if (toolbar.findViewWithTag<View>(EXT_TAG) != null) return

        toolbar.addView(toolButton(activity, "Linha", EXT_TAG) {
            writeField(editor, "tool", EditorToolEngine.ToolType.LINE)
            Toast.makeText(activity, "Linha", Toast.LENGTH_SHORT).show()
        })
        toolbar.addView(toolButton(activity, "Forma", EXT_TAG) {
            writeField(editor, "tool", EditorToolEngine.ToolType.SHAPE)
            val mode = DrawingToolState.nextShapeMode()
            Toast.makeText(activity, "Forma: ${mode.name.lowercase()}", Toast.LENGTH_SHORT).show()
        })
        toolbar.addView(toolButton(activity, "Laço Op.", EXT_TAG) {
            writeField(editor, "tool", EditorToolEngine.ToolType.LASSO)
            val mode = DrawingToolState.nextLassoMode()
            Toast.makeText(activity, "Laço: ${mode.name.lowercase()}", Toast.LENGTH_SHORT).show()
        })
        toolbar.addView(toolButton(activity, "Estabil.", EXT_TAG) {
            cycleStabilizer(activity)
        })
        toolbar.addView(toolButton(activity, "Pressão", EXT_TAG) {
            BrushToolState.load(activity)
            BrushToolState.pressure = !BrushToolState.pressure
            BrushToolState.save(activity)
            Toast.makeText(activity, "Pressão: ${if (BrushToolState.pressure) "ativa" else "desativada"}", Toast.LENGTH_SHORT).show()
        })
    }

    private fun toolButton(activity: Activity, label: String, tag: String, action: () -> Unit): Button =
        Button(activity).apply {
            this.tag = tag
            text = label
            textSize = 9f
            setTextColor(ThemeColorStore.TEXT)
            backgroundTintList = android.content.res.ColorStateList.valueOf(ThemeColorStore.NAVY_800)
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(70, 54)
        }

    private fun cycleStabilizer(activity: Activity) {
        val next = when {
            StrokeCorrectionStore.constant <= 0f && StrokeCorrectionStore.smoothing <= 5f -> 1
            StrokeCorrectionStore.constant < 50f -> 2
            StrokeCorrectionStore.constant < 80f -> 3
            else -> 0
        }
        when (next) {
            0 -> {
                StrokeCorrectionStore.constant = 0f
                StrokeCorrectionStore.fastStrokes = 0f
                StrokeCorrectionStore.smoothing = 0f
                StrokeCorrectionStore.prediction = 0f
            }
            1 -> {
                StrokeCorrectionStore.constant = 30f
                StrokeCorrectionStore.fastStrokes = 20f
                StrokeCorrectionStore.smoothing = 25f
                StrokeCorrectionStore.prediction = 0f
            }
            2 -> {
                StrokeCorrectionStore.constant = 55f
                StrokeCorrectionStore.fastStrokes = 35f
                StrokeCorrectionStore.smoothing = 45f
                StrokeCorrectionStore.prediction = 10f
            }
            else -> {
                StrokeCorrectionStore.constant = 80f
                StrokeCorrectionStore.fastStrokes = 55f
                StrokeCorrectionStore.smoothing = 70f
                StrokeCorrectionStore.prediction = 20f
            }
        }
        Toast.makeText(activity, "Estabilização: ${StrokeCorrectionStore.constant.toInt()}%", Toast.LENGTH_SHORT).show()
    }

    private class ToolOverlay(private val activity: Activity, private val editor: Any) : View(activity) {
        private val engine = EditorToolEngine()
        private var polygon = mutableListOf<PointF>()
        private var selection = EditorToolEngine.Selection()
        private var active = false
        private var lastX = 0f
        private var lastY = 0f
        private var shapeStartX = 0f
        private var shapeStartY = 0f
        private var shapeEndX = 0f
        private var shapeEndY = 0f
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
            if ((tool == EditorToolEngine.ToolType.LINE || tool == EditorToolEngine.ToolType.SHAPE) && active) {
                drawGeometryPreview(canvas)
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
                canvas.drawCircle(cursorX, cursorY, readFloat(editor, "liquifyRadius", 80f).coerceAtMost(300f), overlayPaint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            return when (currentTool()) {
                EditorToolEngine.ToolType.BRUSH, EditorToolEngine.ToolType.ERASER -> false
                EditorToolEngine.ToolType.LIQUIFY -> handleLiquify(event)
                EditorToolEngine.ToolType.LASSO -> handleLasso(event)
                EditorToolEngine.ToolType.TRANSFORM -> handleTransform(event)
                EditorToolEngine.ToolType.EYEDROPPER -> handleEyedropper(event)
                EditorToolEngine.ToolType.BUCKET -> handleBucket(event)
                EditorToolEngine.ToolType.LINE -> handleLine(event)
                EditorToolEngine.ToolType.SHAPE -> handleShape(event)
            }
        }

        private fun handleLasso(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { polygon.clear(); polygon += PointF(e.x, e.y); active = true; invalidate() }
                MotionEvent.ACTION_MOVE -> { polygon += PointF(e.x, e.y); invalidate() }
                MotionEvent.ACTION_UP -> {
                    polygon += PointF(e.x, e.y)
                    val frame = currentFrame() ?: return true
                    val candidate = engine.lassoSelect(frame, polygon.map { screenToCanvas(it.x, it.y) })
                    selection = when (DrawingToolState.lassoMode) {
                        DrawingToolState.LassoMode.NEW -> candidate
                        DrawingToolState.LassoMode.ADD -> EditorToolEngine.Selection(selection.strokeIds + candidate.strokeIds)
                        DrawingToolState.LassoMode.SUBTRACT -> EditorToolEngine.Selection(selection.strokeIds - candidate.strokeIds)
                        DrawingToolState.LassoMode.INTERSECT -> EditorToolEngine.Selection(selection.strokeIds.intersect(candidate.strokeIds))
                    }
                    active = false; refresh(); invalidate()
                    Toast.makeText(activity, "Laço: ${selection.strokeIds.size} traço(s)", Toast.LENGTH_SHORT).show()
                }
            }
            return true
        }

        private fun handleLine(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { shapeStartX = e.x; shapeStartY = e.y; shapeEndX = e.x; shapeEndY = e.y; active = true; invalidate() }
                MotionEvent.ACTION_MOVE -> { shapeEndX = e.x; shapeEndY = e.y; invalidate() }
                MotionEvent.ACTION_UP -> {
                    shapeEndX = e.x; shapeEndY = e.y
                    createGeometryStroke(lineSamples(shapeStartX, shapeStartY, shapeEndX, shapeEndY))
                    active = false; refresh(); invalidate()
                }
            }
            return true
        }

        private fun handleShape(e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { shapeStartX = e.x; shapeStartY = e.y; shapeEndX = e.x; shapeEndY = e.y; active = true; invalidate() }
                MotionEvent.ACTION_MOVE -> { shapeEndX = e.x; shapeEndY = e.y; invalidate() }
                MotionEvent.ACTION_UP -> {
                    shapeEndX = e.x; shapeEndY = e.y
                    val samples = when (DrawingToolState.shapeMode) {
                        DrawingToolState.ShapeMode.RECTANGLE -> rectangleSamples()
                        DrawingToolState.ShapeMode.ELLIPSE -> ellipseSamples()
                        DrawingToolState.ShapeMode.ARROW -> arrowSamples()
                    }
                    createGeometryStroke(samples)
                    active = false; refresh(); invalidate()
                }
            }
            return true
        }

        private fun createGeometryStroke(screenSamples: List<PointF>) {
            val frame = currentFrame() ?: return
            if (screenSamples.size < 2) return
            val samples = screenSamples.map { p ->
                val c = screenToCanvas(p.x, p.y)
                StrokeSample(c.first, c.second, 1f, System.currentTimeMillis())
            }.toMutableList()
            val size = readFloat(editor, "brushSize", 12f).coerceAtLeast(1f)
            val opacity = readFloat(editor, "brushOpacity", 1f).coerceIn(0f, 1f)
            val color = readInt(editor, "accent", ThemeColorStore.DEFAULT)
            val brushId = readString(editor, "brushId", "canvas_1")
            val settings = BrushDefaults.forPreset(brushId).copy(size = size, opacity = opacity).normalized()
            frame.strokes += StrokeData(brushId = brushId, color = color, size = size, opacity = opacity, settings = settings, samples = samples)
        }

        private fun lineSamples(x1: Float, y1: Float, x2: Float, y2: Float): List<PointF> = listOf(PointF(x1, y1), PointF(x2, y2))

        private fun rectangleSamples(): List<PointF> {
            val l = minOf(shapeStartX, shapeEndX); val r = maxOf(shapeStartX, shapeEndX)
            val t = minOf(shapeStartY, shapeEndY); val b = maxOf(shapeStartY, shapeEndY)
            return listOf(PointF(l,t), PointF(r,t), PointF(r,b), PointF(l,b), PointF(l,t))
        }

        private fun ellipseSamples(): List<PointF> {
            val cx = (shapeStartX + shapeEndX) * .5f; val cy = (shapeStartY + shapeEndY) * .5f
            val rx = kotlin.math.abs(shapeEndX - shapeStartX) * .5f; val ry = kotlin.math.abs(shapeEndY - shapeStartY) * .5f
            return (0..40).map { i ->
                val a = (Math.PI * 2.0 * i / 40.0)
                PointF(cx + (rx * kotlin.math.cos(a)).toFloat(), cy + (ry * kotlin.math.sin(a)).toFloat())
            }
        }

        private fun arrowSamples(): List<PointF> {
            val dx = shapeEndX - shapeStartX; val dy = shapeEndY - shapeStartY
            val length = hypot(dx, dy).coerceAtLeast(1f)
            val ux = dx / length; val uy = dy / length
            val px = -uy; val py = ux
            val head = (length * .18f).coerceAtLeast(10f)
            val wing = head * .55f
            val bx = shapeEndX - ux * head; val by = shapeEndY - uy * head
            return listOf(
                PointF(shapeStartX, shapeStartY), PointF(shapeEndX, shapeEndY),
                PointF(bx + px * wing, by + py * wing), PointF(shapeEndX, shapeEndY),
                PointF(bx - px * wing, by - py * wing)
            )
        }

        private fun drawGeometryPreview(canvas: Canvas) {
            when {
                currentTool() == EditorToolEngine.ToolType.LINE -> canvas.drawLine(shapeStartX, shapeStartY, shapeEndX, shapeEndY, overlayPaint)
                DrawingToolState.shapeMode == DrawingToolState.ShapeMode.RECTANGLE -> canvas.drawRect(minOf(shapeStartX, shapeEndX), minOf(shapeStartY, shapeEndY), maxOf(shapeStartX, shapeEndX), maxOf(shapeStartY, shapeEndY), overlayPaint)
                DrawingToolState.shapeMode == DrawingToolState.ShapeMode.ELLIPSE -> canvas.drawOval(RectF(minOf(shapeStartX, shapeEndX), minOf(shapeStartY, shapeEndY), maxOf(shapeStartX, shapeEndX), maxOf(shapeStartY, shapeEndY)), overlayPaint)
                else -> {
                    val points = arrowSamples(); for (i in 1 until points.size) canvas.drawLine(points[i-1].x, points[i-1].y, points[i].x, points[i].y, overlayPaint)
                }
            }
        }

        private fun handleTransform(e: MotionEvent): Boolean {
            val frame = currentFrame() ?: return true
            if (selection.strokeIds.isEmpty()) {
                if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                    val p = screenToCanvas(e.x, e.y)
                    val hit = frame.strokes.lastOrNull { stroke -> stroke.samples.any { hypot(it.x - p.first, it.y - p.second) <= max(12f, stroke.size * .75f) } }
                    if (hit != null) selection = EditorToolEngine.Selection(setOf(hit.id))
                    else Toast.makeText(activity, "Use Laço para selecionar vários traços", Toast.LENGTH_SHORT).show()
                    invalidate()
                }
                return true
            }
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { lastX = e.x; lastY = e.y; active = true }
                MotionEvent.ACTION_MOVE -> {
                    val a = screenToCanvas(lastX, lastY); val b = screenToCanvas(e.x, e.y)
                    val dx = b.first - a.first; val dy = b.second - a.second
                    if (dx != 0f || dy != 0f) engine.transform(frame, selection, EditorToolEngine.Transform(translateX = dx, translateY = dy))
                    lastX = e.x; lastY = e.y; refresh(); invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { active = false; refresh(); invalidate() }
            }
            return true
        }

        private fun handleEyedropper(e: MotionEvent): Boolean {
            if (e.actionMasked != MotionEvent.ACTION_DOWN) return true
            val frame = currentFrame() ?: return true
            val p = screenToCanvas(e.x, e.y)
            val hit = frame.strokes.flatMap { stroke -> stroke.samples.map { stroke to it } }.minByOrNull { (_, sample) -> hypot(sample.x - p.first, sample.y - p.second) }
            if (hit != null && hypot(hit.second.x - p.first, hit.second.y - p.second) < 100f) {
                writeField(editor, "accent", hit.first.color)
                BrushToolState.color = hit.first.color
                BrushToolState.save(activity)
                Toast.makeText(activity, "Cor capturada", Toast.LENGTH_SHORT).show(); refresh()
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
            if (boundary == null) { Toast.makeText(activity, "O Balde precisa de um contorno fechado", Toast.LENGTH_SHORT).show(); return true }
            val poly = boundary.samples.map { PointF(it.x, it.y) }
            val minX = poly.minOf { it.x }; val maxX = poly.maxOf { it.x }; val minY = poly.minOf { it.y }; val maxY = poly.maxOf { it.y }
            val size = readFloat(editor, "brushSize", 12f).coerceAtLeast(1f)
            val color = readInt(editor, "accent", ThemeColorStore.DEFAULT)
            val brushId = readString(editor, "brushId", "canvas_1")
            val settings = BrushDefaults.forPreset(brushId).copy(size = size).normalized()
            var y = minY + size * .5f; var lines = 0
            while (y <= maxY && lines < 2048) {
                val xs = mutableListOf<Float>()
                for (i in poly.indices) {
                    val a = poly[i]; val b = poly[(i + 1) % poly.size]
                    if ((a.y > y) != (b.y > y)) xs += a.x + (y - a.y) * (b.x - a.x) / (b.y - a.y)
                }
                xs.sort(); var i = 0
                while (i + 1 < xs.size) {
                    val x1 = xs[i]; val x2 = xs[i + 1]
                    if (x2 - x1 >= size * .15f) {
                        val samples = mutableListOf<StrokeSample>(); var x = x1; val step = max(.75f, size * .55f)
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
                MotionEvent.ACTION_DOWN -> { liquifyPath.clear(); val p = screenToCanvas(e.x, e.y); liquifyPath += StrokeSample(p.first, p.second, 1f, e.eventTime); cursorX = e.x; cursorY = e.y; invalidate() }
                MotionEvent.ACTION_MOVE -> {
                    val p = screenToCanvas(e.x, e.y); val previous = liquifyPath.last(); liquifyPath += StrokeSample(p.first, p.second, 1f, e.eventTime)
                    val mode = runCatching { LiquifyEngine.Mode.valueOf(readString(editor, "liquifyMode", LiquifyEngine.Mode.PUSH.name)) }.getOrDefault(LiquifyEngine.Mode.PUSH)
                    val settings = LiquifyEngine.Settings(radius, strength, mode)
                    frame.strokes.forEach { stroke -> val changedSamples = LiquifyEngine.apply(stroke.samples, p.first, p.second, p.first - previous.x, p.second - previous.y, settings); stroke.samples.clear(); stroke.samples.addAll(changedSamples) }
                    cursorX = e.x; cursorY = e.y; refresh(); invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { liquifyPath.clear(); cursorX = -1f; cursorY = -1f; refresh(); invalidate() }
            }
            return true
        }

        private fun currentTool(): EditorToolEngine.ToolType = runCatching { readField(editor, "tool") as EditorToolEngine.ToolType }.getOrDefault(EditorToolEngine.ToolType.BRUSH)
        private fun currentFrame(): DrawingFrame? = (readField(editor, "document") as? AnimationDocument)?.activeLayer?.ensureFrame((readField(editor, "document") as AnimationDocument).currentFrame)
        private fun refresh() { runCatching { editor.javaClass.getMethod("invalidate").invoke(editor); editor.javaClass.getMethod("refreshTimeline").invoke(editor) } }

        private fun screenToCanvas(x: Float, y: Float): Pair<Float, Float> {
            val w = (readField(editor, "width") as? Int)?.toFloat() ?: width.toFloat(); val h = (readField(editor, "height") as? Int)?.toFloat() ?: height.toFloat()
            val left = w * .07f; val right = w * .93f; val top = 66f; val bottom = h - 58f; val cx = (left + right) * .5f; val cy = (top + bottom) * .5f
            val zoom = readFloat(editor, "zoom", 1f).coerceAtLeast(.01f); val panX = readFloat(editor, "panX", 0f); val panY = readFloat(editor, "panY", 0f)
            val rotation = Math.toRadians(readFloat(editor, "viewportRotation", 0f).toDouble()); val rootY = y + dp(activity, 66).toFloat()
            val px = x - cx - panX; val py = rootY - cy - panY; val c = cos(-rotation); val s = sin(-rotation)
            val rx = px * c - py * s; val ry = px * s + py * c
            return cx + rx / zoom to cy + ry / zoom
        }

        private fun screenPoint(x: Float, y: Float): Pair<Float, Float> {
            val w = (readField(editor, "width") as? Int)?.toFloat() ?: width.toFloat(); val h = (readField(editor, "height") as? Int)?.toFloat() ?: height.toFloat()
            val left = w * .07f; val right = w * .93f; val top = 66f; val bottom = h - 58f; val cx = (left + right) * .5f; val cy = (top + bottom) * .5f
            val zoom = readFloat(editor, "zoom", 1f); val panX = readFloat(editor, "panX", 0f); val panY = readFloat(editor, "panY", 0f); val a = Math.toRadians(readFloat(editor, "viewportRotation", 0f).toDouble())
            val px = (x - cx) * zoom; val py = (y - cy) * zoom; val rx = px * cos(a) - py * sin(a); val ry = px * sin(a) + py * cos(a)
            return cx + rx + panX to cy + ry + panY - dp(activity, 66).toFloat()
        }

        private fun pointInPolygon(point: Pair<Float, Float>, polygon: List<PointF>): Boolean {
            if (polygon.size < 3) return false
            var inside = false; var j = polygon.lastIndex
            for (i in polygon.indices) { val a = polygon[i]; val b = polygon[j]; if ((a.y > point.second) != (b.y > point.second) && point.first < (b.x - a.x) * (point.second - a.y) / ((b.y - a.y).takeIf { it != 0f } ?: Float.MIN_VALUE) + a.x) inside = !inside; j = i }
            return inside
        }
    }

    private fun readField(target: Any, name: String): Any? { var type: Class<*>? = target.javaClass; while (type != null) { val result = runCatching { val field: Field = type!!.getDeclaredField(name); field.isAccessible = true; field.get(target) }.getOrNull(); if (result != null) return result; type = type.superclass }; return null }
    private fun writeField(target: Any, name: String, value: Any?) { var type: Class<*>? = target.javaClass; while (type != null) { runCatching { val field = type!!.getDeclaredField(name); field.isAccessible = true; field.set(target, value); return }; type = type.superclass } }
    private fun readFloat(target: Any, name: String, fallback: Float): Float = runCatching { (readField(target, name) as Number).toFloat() }.getOrDefault(fallback)
    private fun readInt(target: Any, name: String, fallback: Int): Int = runCatching { (readField(target, name) as Number).toInt() }.getOrDefault(fallback)
    private fun readString(target: Any, name: String, fallback: String): String = readField(target, name) as? String ?: fallback
    private fun dp(activity: Activity, value: Int): Int = (value * activity.resources.displayMetrics.density).toInt()
    private fun max(a: Float, b: Float) = if (a > b) a else b
}
