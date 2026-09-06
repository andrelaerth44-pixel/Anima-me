package com.animame.editor

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class EditorView(context: Context) : View(context) {
    private enum class Tool { BRUSH, ERASER, LASSO, FILL, MOVE }
    private var tool = Tool.BRUSH
    private var brushSize = 12f
    private var opacity = 1f
    private var current: Path? = null
    private val strokes = mutableListOf<Pair<Path, Paint>>()
    private val canvasPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setBackgroundColor(Color.rgb(18, 18, 18))
        textPaint.typeface = Typeface.create("sans", Typeface.NORMAL)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        panelPaint.color = Color.rgb(30, 30, 30)
        c.drawRect(0f, 0f, w, 54f, panelPaint)
        c.drawRect(0f, 54f, 66f, h, panelPaint)
        c.drawRect(w - 250f, 54f, w, h, panelPaint)
        panelPaint.color = Color.rgb(245, 245, 245)
        c.drawRect(90f, 75f, w - 275f, h - 78f, panelPaint)

        // top bar
        drawText(c, "ANIMA-ME", 16f, 35f, Color.WHITE, 16f)
        drawText(c, "●  24 FPS", w - 150f, 34f, Color.LTGRAY, 12f)

        // tools
        val names = arrayOf("✎", "⌫", "⌁", "▣", "↔")
        val tools = Tool.values()
        for (i in names.indices) {
            val y = 90f + i * 58f
            if (tool == tools[i]) {
                panelPaint.color = Color.rgb(75, 75, 75)
                c.drawRoundRect(8f, y - 30f, 58f, y + 20f, 8f, 8f, panelPaint)
            }
            drawText(c, names[i], 23f, y + 5f, Color.WHITE, 22f)
        }

        // right settings panel
        drawText(c, "TOOL", w - 230f, 88f, Color.GRAY, 11f)
        drawText(c, tool.name, w - 230f, 114f, Color.WHITE, 15f)
        drawText(c, "SIZE", w - 230f, 152f, Color.GRAY, 11f)
        drawText(c, "${brushSize.toInt()} px", w - 230f, 177f, Color.WHITE, 14f)
        drawText(c, "OPACITY", w - 230f, 216f, Color.GRAY, 11f)
        drawText(c, "${(opacity * 100).toInt()}%", w - 230f, 241f, Color.WHITE, 14f)
        drawText(c, "FRAME  001", w - 230f, h - 125f, Color.LTGRAY, 12f)
        drawText(c, "◀   ▶   ▶|", w - 230f, h - 92f, Color.WHITE, 20f)

        // timeline strip
        panelPaint.color = Color.rgb(38, 38, 38)
        c.drawRect(66f, h - 66f, w - 250f, h, panelPaint)
        drawText(c, "LAYERS", 80f, h - 39f, Color.GRAY, 10f)
        drawText(c, "1", 140f, h - 39f, Color.WHITE, 12f)
        drawText(c, "FRAME", 185f, h - 39f, Color.GRAY, 10f)
        c.drawRect(245f, h - 51f, 285f, h - 18f, panelPaint)
        drawText(c, "●", 258f, h - 28f, Color.WHITE, 13f)

        // drawing area
        for ((p, paint) in strokes) c.drawPath(p, paint)
        current?.let { p ->
            val paint = makePaint()
            c.drawPath(p, paint)
        }
    }

    private fun makePaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = (255 * opacity).toInt()
        style = Paint.Style.STROKE
        strokeWidth = max(1f, brushSize)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        if (tool == Tool.ERASER) {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
    }

    private fun drawText(c: Canvas, s: String, x: Float, y: Float, color: Int, size: Float) {
        textPaint.color = color; textPaint.textSize = size
        c.drawText(s, x, y, textPaint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x; val y = e.y
        if (e.action == MotionEvent.ACTION_DOWN) {
            // Tool rail
            if (x < 70f && y > 60f && y < 390f) {
                val index = ((y - 60f) / 58f).toInt().coerceIn(0, 4)
                tool = Tool.values()[index]; invalidate(); return true
            }
            // Canvas interaction
            if (x in 90f..(width - 275f) && y in 75f..(height - 78f) && (tool == Tool.BRUSH || tool == Tool.ERASER)) {
                current = Path().apply { moveTo(x, y) }; invalidate(); return true
            }
        } else if (e.action == MotionEvent.ACTION_MOVE && current != null) {
            current!!.lineTo(x, y); invalidate(); return true
        } else if (e.action == MotionEvent.ACTION_UP && current != null) {
            current!!.lineTo(x, y)
            strokes += current!! to makePaint()
            current = null; invalidate(); return true
        }
        return true
    }
}
