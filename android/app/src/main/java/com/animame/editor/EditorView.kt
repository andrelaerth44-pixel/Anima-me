package com.animame.editor

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class EditorView(context: Context) : View(context) {
    private enum class Tool { BRUSH, ERASER, LASSO, FILL, MOVE, RECTANGLE, LINE, PICKER }

    private var tool = Tool.BRUSH
    private var brushSize = 12f
    private var opacity = 1f
    private var smoothing = 0.5f
    private var toolOptionsOpen = true
    private var current: Path? = null
    private val strokes = mutableListOf<Pair<Path, Paint>>()

    private val uiPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setBackgroundColor(Color.rgb(17, 18, 20))
        textPaint.typeface = Typeface.create("sans", Typeface.NORMAL)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat()
        val h = height.toFloat()
        val left = 58f
        val right = 258f
        val top = 48f
        val bottom = 92f

        // Header
        uiPaint.color = Color.rgb(27, 29, 32)
        c.drawRect(0f, 0f, w, top, uiPaint)
        drawText(c, "ANIMA-ME", 14f, 31f, Color.WHITE, 15f)
        drawText(c, "Untitled", 112f, 31f, Color.LTGRAY, 12f)
        drawText(c, "24 FPS", w - 112f, 31f, Color.LTGRAY, 12f)
        drawText(c, "100%", w - 58f, 31f, Color.WHITE, 12f)

        // Left tool rail
        uiPaint.color = Color.rgb(31, 33, 36)
        c.drawRect(0f, top, left, h - bottom, uiPaint)
        val toolStep = 48f
        Tool.values().forEachIndexed { i, t ->
            val y = top + 22f + i * toolStep
            if (y + 20f < h - bottom) {
                if (tool == t) {
                    uiPaint.color = Color.rgb(67, 72, 79)
                    c.drawRoundRect(7f, y - 19f, 51f, y + 19f, 8f, 8f, uiPaint)
                }
                drawToolIcon(c, t, 29f, y)
            }
        }

        // Canvas / stage
        uiPaint.color = Color.rgb(242, 242, 242)
        c.drawRect(left + 12f, top + 12f, w - right - 12f, h - bottom - 12f, uiPaint)
        for ((path, paint) in strokes) c.drawPath(path, paint)
        current?.let { c.drawPath(it, makePaint()) }

        // Right contextual panel
        uiPaint.color = Color.rgb(29, 31, 34)
        c.drawRect(w - right, top, w, h - bottom, uiPaint)
        drawText(c, "TOOL OPTIONS", w - right + 14f, top + 27f, Color.WHITE, 13f)
        drawText(c, if (toolOptionsOpen) "Hide" else "Show", w - 53f, top + 27f, Color.LTGRAY, 10f)

        if (toolOptionsOpen) drawOptions(c, w - right + 14f, top + 52f, right - 28f)

        // Bottom timeline
        uiPaint.color = Color.rgb(27, 29, 32)
        c.drawRect(0f, h - bottom, w, h, uiPaint)
        drawText(c, "LAYERS", 14f, h - 64f, Color.LTGRAY, 10f)
        drawText(c, "Layer 1", 70f, h - 64f, Color.WHITE, 12f)
        drawText(c, "1", 182f, h - 64f, Color.WHITE, 12f)
        drawText(c, "2", 224f, h - 64f, Color.GRAY, 12f)
        drawText(c, "3", 266f, h - 64f, Color.GRAY, 12f)
        drawText(c, "4", 308f, h - 64f, Color.GRAY, 12f)
        drawText(c, "5", 350f, h - 64f, Color.GRAY, 12f)
        uiPaint.color = Color.rgb(63, 68, 75)
        c.drawRect(174f, h - 77f, 205f, h - 48f, uiPaint)
        drawText(c, "FRAME 001", w - 174f, h - 64f, Color.LTGRAY, 11f)
        drawText(c, "|<   <   PLAY   >   >|", w - 170f, h - 27f, Color.WHITE, 12f)
    }

    private fun drawOptions(c: Canvas, x: Float, y: Float, width: Float) {
        when (tool) {
            Tool.BRUSH, Tool.ERASER -> {
                drawValue(c, "SIZE", "${brushSize.toInt()} px", x, y, width)
                drawValue(c, "OPACITY", "${(opacity * 100).toInt()}%", x, y + 52f, width)
                drawValue(c, "FLOW", "100%", x, y + 104f, width)
                drawValue(c, "SPACING", "12%", x, y + 156f, width)
                drawValue(c, "SMOOTHING", "${(smoothing * 100).toInt()}%", x, y + 208f, width)
                drawText(c, if (tool == Tool.BRUSH) "Brush" else "Eraser", x, y + 270f, Color.LTGRAY, 11f)
            }
            Tool.LASSO -> drawText(c, "Freehand selection\n", x, y, Color.LTGRAY, 12f)
            Tool.FILL -> {
                drawValue(c, "TOLERANCE", "32", x, y, width)
                drawValue(c, "GAP CLOSING", "8 px", x, y + 52f, width)
                drawValue(c, "REFERENCE", "Current layer", x, y + 104f, width)
            }
            Tool.MOVE -> drawValue(c, "TRANSFORM", "Move / Scale / Rotate", x, y, width)
            Tool.RECTANGLE, Tool.LINE, Tool.PICKER -> drawText(c, tool.name, x, y, Color.LTGRAY, 12f)
        }
    }

    private fun drawValue(c: Canvas, label: String, value: String, x: Float, y: Float, width: Float) {
        drawText(c, label, x, y, Color.GRAY, 10f)
        drawText(c, value, x, y + 23f, Color.WHITE, 13f)
        uiPaint.color = Color.rgb(55, 58, 63)
        c.drawRect(x, y + 34f, x + width, y + 35f, uiPaint)
    }

    private fun drawToolIcon(c: Canvas, tool: Tool, cx: Float, cy: Float) {
        uiPaint.color = Color.WHITE
        uiPaint.style = Paint.Style.STROKE
        uiPaint.strokeWidth = 2.2f
        when (tool) {
            Tool.BRUSH -> { c.drawLine(cx - 8f, cy + 7f, cx + 7f, cy - 8f, uiPaint); c.drawCircle(cx + 8f, cy - 9f, 2f, uiPaint) }
            Tool.ERASER -> c.drawRect(cx - 9f, cy - 6f, cx + 8f, cy + 7f, uiPaint)
            Tool.LASSO -> c.drawOval(cx - 10f, cy - 9f, cx + 10f, cy + 9f, uiPaint)
            Tool.FILL -> { c.drawRect(cx - 8f, cy - 8f, cx + 6f, cy + 6f, uiPaint); c.drawLine(cx + 5f, cy - 5f, cx + 10f, cy - 10f, uiPaint) }
            Tool.MOVE -> { c.drawLine(cx, cy - 10f, cx, cy + 10f, uiPaint); c.drawLine(cx - 10f, cy, cx + 10f, cy, uiPaint) }
            Tool.RECTANGLE -> c.drawRect(cx - 9f, cy - 7f, cx + 9f, cy + 7f, uiPaint)
            Tool.LINE -> c.drawLine(cx - 9f, cy + 8f, cx + 9f, cy - 8f, uiPaint)
            Tool.PICKER -> c.drawCircle(cx, cy, 8f, uiPaint)
        }
        uiPaint.style = Paint.Style.FILL
    }

    private fun makePaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = (255 * opacity).toInt()
        style = Paint.Style.STROKE
        strokeWidth = max(1f, brushSize)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        if (tool == Tool.ERASER) xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private fun drawText(c: Canvas, s: String, x: Float, y: Float, color: Int, size: Float) {
        textPaint.color = color
        textPaint.textSize = size
        c.drawText(s, x, y, textPaint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x
        val y = e.y
        val h = height.toFloat()
        val w = width.toFloat()

        if (e.action == MotionEvent.ACTION_DOWN) {
            // Tool rail
            if (x < 58f && y >= 48f && y < h - 92f) {
                val index = ((y - 51f) / 48f).toInt().coerceIn(0, Tool.values().lastIndex)
                tool = Tool.values()[index]
                invalidate()
                return true
            }

            // Tool Options expand/collapse
            if (x >= w - 258f && y in 48f..92f) {
                toolOptionsOpen = !toolOptionsOpen
                invalidate()
                return true
            }

            // Canvas drawing
            val canvasLeft = 70f
            val canvasRight = w - 270f
            if (x in canvasLeft..canvasRight && y in 60f..(h - 104f) && (tool == Tool.BRUSH || tool == Tool.ERASER)) {
                current = Path().apply { moveTo(x, y) }
                invalidate()
                return true
            }
        } else if (e.action == MotionEvent.ACTION_MOVE && current != null) {
            current!!.lineTo(x, y)
            invalidate()
            return true
        } else if (e.action == MotionEvent.ACTION_UP && current != null) {
            current!!.lineTo(x, y)
            strokes += current!! to makePaint()
            current = null
            invalidate()
            return true
        }
        return true
    }
}
