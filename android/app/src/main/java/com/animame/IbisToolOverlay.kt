package com.animame

import android.app.Activity
import android.graphics.Color
import android.graphics.PointF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.animame.editor.AnimationDocument
import com.animame.editor.AnimationLayer
import com.animame.editor.BrushDefaults
import com.animame.editor.StrokeData
import com.animame.editor.StrokeSample
import java.lang.reflect.Field
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Anima-me implementation of the ibisPaint-style tool family.
 * It is independent of ibisPaint code/assets and operates on Anima-me's own
 * stroke model: Magic Wand, Lasso, Bucket, Eyedropper, Smudge, Blur and the
 * Special Pen family (Lasso Fill/Eraser, Liquify, Copy Pen variants).
 */
object IbisToolOverlay {
    enum class Tool {
        BRUSH, ERASER, MAGIC_WAND, LASSO, BUCKET, SMUDGE, BLUR, EYEDROPPER,
        LASSO_FILL, LASSO_ERASER, LIQUIFY_DRAG, LIQUIFY_SHRINK, LIQUIFY_EXPAND,
        LIQUIFY_SMOOTH, COPY_RELATIVE, COPY_FIXED, COPY_MOVE
    }

    private const val TAG = "anima-me-ibis-tools"
    private var installedFor: Activity? = null
    private var activeTool = Tool.BRUSH
    private var tolerance = 42f
    private var radius = 44f
    private var lasso = mutableListOf<PointF>()
    private var gestureStart = PointF()
    private var sourceStroke: StrokeData? = null

    fun install(activity: Activity) {
        if (installedFor === activity) return
        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (content.findViewWithTag<View>(TAG) != null) return
        installedFor = activity

        val root = LinearLayout(activity).apply {
            tag = TAG
            orientation = LinearLayout.VERTICAL
            setPadding(4, 4, 4, 4)
            setBackgroundColor(Color.argb(235, 6, 20, 45))
        }
        root.addView(TextView(activity).apply {
            text = "Ferramentas"
            textSize = 11f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, 28))

        val scroll = HorizontalScrollView(activity).apply { isHorizontalScrollBarEnabled = false }
        val strip = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(
            Tool.BRUSH to "Pincel", Tool.ERASER to "Borracha", Tool.MAGIC_WAND to "Varinha",
            Tool.LASSO to "Laço", Tool.BUCKET to "Preencher", Tool.SMUDGE to "Borrar",
            Tool.BLUR to "Desfoque", Tool.EYEDROPPER to "Conta-gotas"
        ).forEach { (tool, label) -> strip.addView(toolButton(activity, label, tool)) }
        strip.addView(toolButton(activity, "Especial", Tool.LASSO_FILL) { openSpecialMenu(activity) })
        scroll.addView(strip)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 52))
        content.addView(root, FrameLayout.LayoutParams(-1, 84).apply { gravity = Gravity.TOP; topMargin = 54 })

        val capture = CanvasCaptureView(activity)
        content.addView(capture, FrameLayout.LayoutParams(-1, -1))
        capture.bringToFront()
        root.bringToFront()
    }

    private fun toolButton(activity: Activity, label: String, tool: Tool, action: (() -> Unit)? = null): Button = Button(activity).apply {
        text = label
        textSize = 9f
        setTextColor(Color.WHITE)
        setOnClickListener {
            activeTool = tool
            action?.invoke()
            if (action == null) Toast.makeText(activity, toolLabel(tool), Toast.LENGTH_SHORT).show()
        }
        layoutParams = LinearLayout.LayoutParams(78, 46).apply { setMargins(2, 0, 2, 0) }
    }

    private fun openSpecialMenu(activity: Activity) {
        val labels = arrayOf(
            "Preenchimento de Laço", "Borracha de Laço", "Liquify: Arrastar",
            "Liquify: Encolher", "Liquify: Expandir", "Liquify: Suavizar",
            "Cópia Relativa", "Cópia Fixa", "Cópia Mover"
        )
        val tools = arrayOf(
            Tool.LASSO_FILL, Tool.LASSO_ERASER, Tool.LIQUIFY_DRAG,
            Tool.LIQUIFY_SHRINK, Tool.LIQUIFY_EXPAND, Tool.LIQUIFY_SMOOTH,
            Tool.COPY_RELATIVE, Tool.COPY_FIXED, Tool.COPY_MOVE
        )
        android.app.AlertDialog.Builder(activity)
            .setTitle("Caneta Especial")
            .setItems(labels) { _, which ->
                activeTool = tools[which]
                Toast.makeText(activity, toolLabel(activeTool), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun toolLabel(tool: Tool): String = when (tool) {
        Tool.MAGIC_WAND -> "Varinha Mágica"
        Tool.LASSO_FILL -> "Caneta Especial: Preenchimento de Laço"
        Tool.LASSO_ERASER -> "Caneta Especial: Borracha de Laço"
        Tool.LIQUIFY_DRAG -> "Caneta Especial: Liquify Arrastar"
        Tool.LIQUIFY_SHRINK -> "Caneta Especial: Liquify Encolher"
        Tool.LIQUIFY_EXPAND -> "Caneta Especial: Liquify Expandir"
        Tool.LIQUIFY_SMOOTH -> "Caneta Especial: Liquify Suavizar"
        Tool.COPY_RELATIVE -> "Caneta Especial: Cópia Relativa"
        Tool.COPY_FIXED -> "Caneta Especial: Cópia Fixa"
        Tool.COPY_MOVE -> "Caneta Especial: Cópia Mover"
        else -> tool.name
    }

    private class CanvasCaptureView(private val activity: Activity) : View(activity) {
        private val editorField: Field? = try {
            MainActivity::class.java.getDeclaredField("editor").apply { isAccessible = true }
        } catch (_: Throwable) { null }
        private var drawingGesture = false

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val editor = editorField?.get(activity) as? View ?: return false
            if (!isCanvasArea(event.x, event.y)) return false
            if (activeTool == Tool.BRUSH || activeTool == Tool.ERASER) return editor.onTouchEvent(event)

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    drawingGesture = true
                    gestureStart = modelPoint(editor, event.x, event.y)
                    lasso.clear()
                    lasso += PointF(gestureStart.x, gestureStart.y)
                    sourceStroke = nearestStroke(editor, gestureStart.x, gestureStart.y)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!drawingGesture) return true
                    val p = modelPoint(editor, event.x, event.y)
                    if (activeTool == Tool.LASSO_FILL || activeTool == Tool.LASSO_ERASER || activeTool == Tool.LASSO) {
                        lasso += p
                    } else if (activeTool == Tool.LIQUIFY_DRAG || activeTool == Tool.LIQUIFY_SHRINK || activeTool == Tool.LIQUIFY_EXPAND || activeTool == Tool.LIQUIFY_SMOOTH || activeTool == Tool.SMUDGE || activeTool == Tool.BLUR) {
                        applyLocal(editor, p.x, p.y, gestureStart.x, gestureStart.y)
                    }
                    editor.invalidate()
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!drawingGesture) return true
                    drawingGesture = false
                    val p = modelPoint(editor, event.x, event.y)
                    when (activeTool) {
                        Tool.MAGIC_WAND -> magicWand(editor, p.x, p.y)
                        Tool.BUCKET -> bucket(editor, p.x, p.y)
                        Tool.EYEDROPPER -> eyedropper(editor, p.x, p.y)
                        Tool.LASSO_FILL -> lassoFill(editor)
                        Tool.LASSO_ERASER -> lassoErase(editor)
                        Tool.LASSO -> selectLasso(editor)
                        Tool.COPY_RELATIVE, Tool.COPY_FIXED, Tool.COPY_MOVE -> copyPen(editor, p.x, p.y)
                        else -> Unit
                    }
                    lasso.clear()
                    sourceStroke = null
                    editor.invalidate()
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    drawingGesture = false
                    lasso.clear()
                    sourceStroke = null
                    return true
                }
            }
            return true
        }

        private fun isCanvasArea(x: Float, y: Float): Boolean = x > 120f && y > 292f && y < height - 238f

        private fun modelPoint(editor: View, x: Float, y: Float): PointF {
            return try {
                val viewportField = editor.javaClass.getDeclaredField("viewport").apply { isAccessible = true }
                val viewport = viewportField.get(editor)
                val scale = viewport.javaClass.getDeclaredField("scale").apply { isAccessible = true }.getFloat(viewport)
                val ox = viewport.javaClass.getDeclaredField("offsetX").apply { isAccessible = true }.getFloat(viewport)
                val oy = viewport.javaClass.getDeclaredField("offsetY").apply { isAccessible = true }.getFloat(viewport)
                PointF((x - ox) / scale, (y - oy) / scale)
            } catch (_: Throwable) { PointF(x, y) }
        }

        private fun getDocument(editor: View): AnimationDocument? = try {
            val field = editor.javaClass.getDeclaredField("document").apply { isAccessible = true }
            field.get(editor) as? AnimationDocument
        } catch (_: Throwable) { null }

        private fun getActiveLayer(editor: View): AnimationLayer? = getDocument(editor)?.activeLayer

        @Suppress("UNCHECKED_CAST")
        private fun selectedIds(editor: View): MutableSet<String>? = try {
            val field = editor.javaClass.getDeclaredField("selectedStrokeIds").apply { isAccessible = true }
            field.get(editor) as? MutableSet<String>
        } catch (_: Throwable) { null }

        private fun nearestStroke(editor: View, x: Float, y: Float): StrokeData? {
            val doc = getDocument(editor) ?: return null
            val frame = doc.activeLayer.frameAt(doc.currentFrame) ?: return null
            return frame.strokes.minByOrNull { stroke -> stroke.samples.minOfOrNull { hypot(it.x - x, it.y - y) } ?: Float.MAX_VALUE }
        }

        private fun magicWand(editor: View, x: Float, y: Float) {
            val doc = getDocument(editor) ?: return
            val frame = doc.activeLayer.frameAt(doc.currentFrame) ?: return
            val target = nearestStroke(editor, x, y) ?: return
            val ids = selectedIds(editor) ?: return
            ids.clear()
            frame.strokes.forEach { stroke ->
                val near = stroke.samples.minOfOrNull { hypot(it.x - x, it.y - y) } ?: Float.MAX_VALUE
                if (near <= radius * 3f && colorDistance(target.color, stroke.color) <= tolerance) ids += stroke.id
            }
            Toast.makeText(activity, "Varinha Mágica: ${ids.size} traço(s)", Toast.LENGTH_SHORT).show()
        }

        private fun selectLasso(editor: View) {
            if (lasso.size < 3) return
            val doc = getDocument(editor) ?: return
            val frame = doc.activeLayer.frameAt(doc.currentFrame) ?: return
            val ids = selectedIds(editor) ?: return
            ids.clear()
            frame.strokes.forEach { stroke -> if (stroke.samples.any { inside(lasso, it.x, it.y) }) ids += stroke.id }
            Toast.makeText(activity, "Laço: ${ids.size} traço(s)", Toast.LENGTH_SHORT).show()
        }

        private fun lassoErase(editor: View) {
            val doc = getDocument(editor) ?: return
            val frame = doc.activeLayer.ensureFrame(doc.currentFrame)
            frame.strokes.removeAll { stroke -> stroke.samples.any { inside(lasso, it.x, it.y) } }
            Toast.makeText(activity, "Borracha de Laço aplicada", Toast.LENGTH_SHORT).show()
        }

        private fun lassoFill(editor: View) {
            if (lasso.size < 3) return
            val doc = getDocument(editor) ?: return
            val frame = doc.activeLayer.ensureFrame(doc.currentFrame)
            val minY = lasso.minOf { it.y }
            val maxY = lasso.maxOf { it.y }
            val samples = mutableListOf<StrokeSample>()
            val step = max(2f, BrushToolState.size)
            var y = minY
            while (y <= maxY) {
                val intersections = mutableListOf<Float>()
                for (i in lasso.indices) {
                    val a = lasso[i]; val b = lasso[(i + 1) % lasso.size]
                    if ((a.y > y) != (b.y > y)) intersections += a.x + (y - a.y) * (b.x - a.x) / (b.y - a.y)
                }
                intersections.sort()
                var i = 0
                while (i + 1 < intersections.size) {
                    samples += StrokeSample(intersections[i], y, 1f, System.currentTimeMillis())
                    samples += StrokeSample(intersections[i + 1], y, 1f, System.currentTimeMillis())
                    i += 2
                }
                y += step
            }
            if (samples.isNotEmpty()) {
                val settings = BrushDefaults.forPreset("basic").copy(size = step, opacity = BrushToolState.opacity)
                frame.strokes += StrokeData(brushId = "lasso_fill", color = BrushToolState.color, size = step, opacity = BrushToolState.opacity, settings = settings, samples = samples)
            }
            Toast.makeText(activity, "Preenchimento de Laço aplicado", Toast.LENGTH_SHORT).show()
        }

        private fun bucket(editor: View, x: Float, y: Float) {
            val doc = getDocument(editor) ?: return
            val frame = doc.activeLayer.ensureFrame(doc.currentFrame)
            val r = max(BrushToolState.size * 4f, 80f)
            val points = mutableListOf<StrokeSample>()
            val step = max(2, BrushToolState.size.toInt())
            for (yy in (y - r).toInt()..(y + r).toInt() step step) {
                val half = kotlin.math.sqrt(max(0f, r * r - (yy - y) * (yy - y)))
                points += StrokeSample(x - half, yy.toFloat(), 1f, System.currentTimeMillis())
                points += StrokeSample(x + half, yy.toFloat(), 1f, System.currentTimeMillis())
            }
            val settings = BrushDefaults.forPreset("basic").copy(size = max(2f, BrushToolState.size), opacity = BrushToolState.opacity)
            if (points.isNotEmpty()) frame.strokes += StrokeData(brushId = "bucket", color = BrushToolState.color, size = settings.size, opacity = settings.opacity, settings = settings, samples = points)
            Toast.makeText(activity, "Preenchimento aplicado", Toast.LENGTH_SHORT).show()
        }

        private fun eyedropper(editor: View, x: Float, y: Float) {
            nearestStroke(editor, x, y)?.let {
                BrushToolState.color = it.color
                BrushToolState.save(activity)
                Toast.makeText(activity, "Cor capturada", Toast.LENGTH_SHORT).show()
            }
        }

        private fun copyPen(editor: View, x: Float, y: Float) {
            val source = sourceStroke ?: nearestStroke(editor, gestureStart.x, gestureStart.y) ?: return
            val doc = getDocument(editor) ?: return
            val dx = x - gestureStart.x
            val dy = y - gestureStart.y
            val copy = source.copy(samples = source.samples.map { it.copy(x = it.x + dx, y = it.y + dy) }.toMutableList())
            doc.activeLayer.ensureFrame(doc.currentFrame).strokes += copy
            Toast.makeText(activity, "Cópia criada", Toast.LENGTH_SHORT).show()
        }

        private fun applyLocal(editor: View, x: Float, y: Float, startX: Float, startY: Float) {
            val doc = getDocument(editor) ?: return
            val frame = doc.activeLayer.frameAt(doc.currentFrame) ?: return
            val dx = x - startX
            val dy = y - startY
            frame.strokes.forEach { stroke ->
                for (i in stroke.samples.indices) {
                    val s = stroke.samples[i]
                    val d = hypot(s.x - x, s.y - y)
                    if (d > radius) continue
                    val influence = (1f - d / radius).coerceIn(0f, 1f)
                    val factor = influence * .18f
                    stroke.samples[i] = when (activeTool) {
                        Tool.LIQUIFY_DRAG -> s.copy(x = s.x + dx * factor, y = s.y + dy * factor)
                        Tool.LIQUIFY_SHRINK -> s.copy(x = x + (s.x - x) * (1f + factor), y = y + (s.y - y) * (1f + factor))
                        Tool.LIQUIFY_EXPAND -> s.copy(x = x + (s.x - x) * (1f - factor), y = y + (s.y - y) * (1f - factor))
                        Tool.LIQUIFY_SMOOTH, Tool.BLUR, Tool.SMUDGE -> {
                            val neighbors = stroke.samples.subList(max(0, i - 1), min(stroke.samples.size, i + 2))
                            val ax = neighbors.map { it.x }.average().toFloat()
                            val ay = neighbors.map { it.y }.average().toFloat()
                            s.copy(x = s.x + (ax - s.x) * factor, y = s.y + (ay - s.y) * factor)
                        }
                        else -> s
                    }
                }
            }
        }

        private fun colorDistance(a: Int, b: Int): Int = kotlin.math.abs(Color.red(a) - Color.red(b)) + kotlin.math.abs(Color.green(a) - Color.green(b)) + kotlin.math.abs(Color.blue(a) - Color.blue(b))
        private fun inside(poly: List<PointF>, x: Float, y: Float): Boolean {
            var inside = false
            var j = poly.lastIndex
            for (i in poly.indices) {
                val xi = poly[i].x; val yi = poly[i].y
                val xj = poly[j].x; val yj = poly[j].y
                if (((yi > y) != (yj > y)) && x < (xj - xi) * (y - yi) / (yj - yi + .00001f) + xi) inside = !inside
                j = i
            }
            return inside
        }
    }
}
