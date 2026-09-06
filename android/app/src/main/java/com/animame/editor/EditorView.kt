package com.animame.editor

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class EditorView(context: Context) : View(context) {
    private enum class Tool { BRUSH, PENCIL, ERASER, LASSO, FILL, PICKER, MOVE, TRANSFORM, LINE, RECTANGLE, ELLIPSE }
    private enum class Panel { OPTIONS, BRUSHES, LAYERS, TIMELINE, CAMERA }

    private var tool = Tool.BRUSH
    private var brushSize = 12f
    private var opacity = 1f
    private var flow = 1f
    private var spacing = 0.12f
    private var smoothing = 0.5f
    private var selectedBrush = BrushCatalog.presets.first()
    private var openPanel: Panel? = Panel.OPTIONS
    private var current: Path? = null
    private val strokes = mutableListOf<Pair<Path, Paint>>()
    private val uiPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        setBackgroundColor(Color.rgb(16, 17, 19))
        textPaint.typeface = Typeface.create("sans", Typeface.NORMAL)
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        val w = width.toFloat(); val h = height.toFloat()
        val left = 62f; val right = if (openPanel == null) 62f else 292f; val top = 50f; val bottom = 112f

        uiPaint.color = Color.rgb(27, 29, 32); c.drawRect(0f, 0f, w, top, uiPaint)
        text(c, "ANIMA-ME", 14f, 32f, Color.WHITE, 15f)
        text(c, "Untitled", 116f, 32f, Color.LTGRAY, 12f)
        text(c, "24 FPS", w - 116f, 32f, Color.LTGRAY, 12f)
        text(c, "100%", w - 58f, 32f, Color.WHITE, 12f)
        drawTopPanels(c, w)

        drawRail(c, top, h - bottom)

        val canvasLeft = left + 10f
        val canvasRight = w - right - 10f
        val canvasTop = top + 10f
        val canvasBottom = h - bottom - 10f
        uiPaint.color = Color.rgb(244, 244, 244); c.drawRect(canvasLeft, canvasTop, canvasRight, canvasBottom, uiPaint)
        for ((path, paint) in strokes) c.drawPath(path, paint)
        current?.let { c.drawPath(it, makePaint()) }

        if (openPanel != null) drawPanel(c, w - right, top, right, h - bottom)
        drawTimeline(c, h, w)
    }

    private fun drawTopPanels(c: Canvas, w: Float) {
        val labels = listOf("OPTIONS", "BRUSHES", "LAYERS", "TIMELINE", "CAMERA")
        val panels = Panel.values()
        var x = 190f
        labels.forEachIndexed { i, label ->
            val active = openPanel == panels[i]
            uiPaint.color = if (active) Color.rgb(68, 73, 81) else Color.rgb(43, 46, 50)
            c.drawRoundRect(x, 9f, x + 76f, 41f, 7f, 7f, uiPaint)
            text(c, label, x + 9f, 29f, Color.WHITE, 8f)
            x += 82f
            if (x > w - 180f) return
        }
    }

    private fun drawRail(c: Canvas, top: Float, bottom: Float) {
        uiPaint.color = Color.rgb(31, 33, 36); c.drawRect(0f, top, 62f, bottom, uiPaint)
        val tools = Tool.values(); val step = 42f
        tools.forEachIndexed { i, t ->
            val y = top + 23f + i * step
            if (y < bottom - 18f) {
                if (t == tool) { uiPaint.color = Color.rgb(68, 73, 81); c.drawRoundRect(6f, y - 17f, 56f, y + 17f, 8f, 8f, uiPaint) }
                icon(c, t, 31f, y)
            }
        }
    }

    private fun drawPanel(c: Canvas, x: Float, top: Float, width: Float, bottom: Float) {
        uiPaint.color = Color.rgb(29, 31, 34); c.drawRect(x, top, x + width, bottom, uiPaint)
        text(c, when (openPanel) { Panel.OPTIONS -> "TOOL OPTIONS"; Panel.BRUSHES -> "BRUSH LIBRARY"; Panel.LAYERS -> "LAYERS"; Panel.TIMELINE -> "TIMELINE"; Panel.CAMERA -> "CAMERA / TRANSFORM" }, x + 14f, top + 28f, Color.WHITE, 13f)
        text(c, "TAP HEADER TO CLOSE", x + width - 128f, top + 28f, Color.GRAY, 8f)
        when (openPanel) {
            Panel.OPTIONS -> drawOptions(c, x + 14f, top + 56f, width - 28f)
            Panel.BRUSHES -> drawBrushes(c, x + 14f, top + 56f, width - 28f)
            Panel.LAYERS -> drawLayers(c, x + 14f, top + 56f)
            Panel.TIMELINE -> drawTimelinePanel(c, x + 14f, top + 56f, width - 28f)
            Panel.CAMERA -> drawCamera(c, x + 14f, top + 56f)
            null -> Unit
        }
    }

    private fun drawOptions(c: Canvas, x: Float, y: Float, width: Float) {
        if (tool == Tool.BRUSH || tool == Tool.PENCIL || tool == Tool.ERASER) {
            value(c, "BRUSH", selectedBrush.name, x, y, width)
            value(c, "SIZE", "${brushSize.toInt()} px", x, y + 48f, width)
            value(c, "OPACITY", "${(opacity * 100).toInt()}%", x, y + 96f, width)
            value(c, "FLOW", "${(flow * 100).toInt()}%", x, y + 144f, width)
            value(c, "SPACING", "${(spacing * 100).toInt()}%", x, y + 192f, width)
            value(c, "SMOOTHING", "${(smoothing * 100).toInt()}%", x, y + 240f, width)
            text(c, "PRESSURE   ON", x, y + 294f, Color.LTGRAY, 11f)
            text(c, "HARDNESS   100%", x, y + 318f, Color.LTGRAY, 11f)
            text(c, "LOCK ALPHA   OFF", x, y + 342f, Color.LTGRAY, 11f)
            text(c, "DRAW ORDER  OVER ALL", x, y + 366f, Color.LTGRAY, 11f)
            text(c, "ENGINE      ${selectedBrush.engine.name}", x, y + 390f, Color.GRAY, 9f)
        } else {
            value(c, "TOOL", tool.name, x, y, width)
            value(c, "MODE", "Standard", x, y + 48f, width)
        }
    }

    private fun drawBrushes(c: Canvas, x: Float, y: Float, width: Float) {
        text(c, "ALL OPEN TOONZ BRUSH FAMILIES", x, y, Color.LTGRAY, 10f)
        var yy = y + 24f
        for (family in BrushCatalog.families) {
            text(c, family, x, yy, Color.WHITE, 11f); yy += 21f
            val items = BrushCatalog.presets.filter { it.family == family }
            for (p in items) {
                text(c, if (p.id == selectedBrush.id) "• ${p.name}" else "  ${p.name}", x + 8f, yy, if (p.id == selectedBrush.id) Color.WHITE else Color.GRAY, 10f)
                yy += 18f
                if (yy > height - 145f) return
            }
            yy += 5f
        }
    }

    private fun drawLayers(c: Canvas, x: Float, y: Float) {
        text(c, "+  NEW LAYER", x, y, Color.WHITE, 11f)
        text(c, "EYE   Layer 3", x, y + 32f, Color.LTGRAY, 11f)
        text(c, "EYE   Layer 2", x, y + 58f, Color.LTGRAY, 11f)
        text(c, "EYE   Layer 1", x, y + 84f, Color.WHITE, 11f)
        text(c, "LOCK   OPACITY   BLEND", x, y + 120f, Color.GRAY, 9f)
    }

    private fun drawTimelinePanel(c: Canvas, x: Float, y: Float, width: Float) {
        text(c, "ONION SKIN   ON", x, y, Color.LTGRAY, 11f)
        text(c, "PREVIOUS   2     NEXT   2", x, y + 28f, Color.LTGRAY, 11f)
        text(c, "FRAME RATE   24 FPS", x, y + 56f, Color.LTGRAY, 11f)
        text(c, "1    2    3    4    5    6    7    8", x, y + 92f, Color.WHITE, 11f)
        text(c, "PLAYBACK     |<   <   PLAY   >   >|", x, y + 126f, Color.WHITE, 10f)
    }

    private fun drawCamera(c: Canvas, x: Float, y: Float) {
        text(c, "POSITION     0, 0", x, y, Color.LTGRAY, 11f)
        text(c, "SCALE        100%", x, y + 28f, Color.LTGRAY, 11f)
        text(c, "ROTATION     0°", x, y + 56f, Color.LTGRAY, 11f)
        text(c, "FLIP H / FLIP V", x, y + 84f, Color.LTGRAY, 11f)
        text(c, "TRANSFORM ORIGIN: CENTER", x, y + 112f, Color.GRAY, 9f)
    }

    private fun drawTimeline(c: Canvas, h: Float, w: Float) {
        uiPaint.color = Color.rgb(27, 29, 32); c.drawRect(0f, h - 112f, w, h, uiPaint)
        text(c, "LAYERS", 12f, h - 82f, Color.LTGRAY, 10f)
        text(c, "Layer 1", 70f, h - 82f, Color.WHITE, 11f)
        text(c, "1    2    3    4    5    6    7    8", 170f, h - 82f, Color.LTGRAY, 11f)
        text(c, "FRAME 001", w - 175f, h - 82f, Color.LTGRAY, 10f)
        text(c, "|<   <   PLAY   >   >|", w - 175f, h - 38f, Color.WHITE, 11f)
    }

    private fun value(c: Canvas, label: String, v: String, x: Float, y: Float, width: Float) {
        text(c, label, x, y, Color.GRAY, 9f); text(c, v, x, y + 20f, Color.WHITE, 12f)
        uiPaint.color = Color.rgb(56, 59, 64); c.drawRect(x, y + 28f, x + width, y + 29f, uiPaint)
    }

    private fun icon(c: Canvas, t: Tool, cx: Float, cy: Float) {
        uiPaint.color = Color.WHITE; uiPaint.style = Paint.Style.STROKE; uiPaint.strokeWidth = 2f
        when (t) {
            Tool.BRUSH -> { c.drawLine(cx - 8f, cy + 7f, cx + 7f, cy - 8f, uiPaint); c.drawCircle(cx + 8f, cy - 9f, 2f, uiPaint) }
            Tool.PENCIL -> { c.drawLine(cx - 8f, cy + 8f, cx + 7f, cy - 7f, uiPaint); c.drawLine(cx - 9f, cy + 9f, cx - 5f, cy + 9f, uiPaint) }
            Tool.ERASER -> c.drawRect(cx - 9f, cy - 6f, cx + 8f, cy + 7f, uiPaint)
            Tool.LASSO -> c.drawOval(cx - 10f, cy - 8f, cx + 10f, cy + 8f, uiPaint)
            Tool.FILL -> { c.drawRect(cx - 8f, cy - 7f, cx + 6f, cy + 6f, uiPaint); c.drawLine(cx + 5f, cy - 5f, cx + 10f, cy - 10f, uiPaint) }
            Tool.PICKER -> { c.drawCircle(cx, cy, 8f, uiPaint); c.drawCircle(cx, cy, 2f, uiPaint) }
            Tool.MOVE -> { c.drawLine(cx, cy - 10f, cx, cy + 10f, uiPaint); c.drawLine(cx - 10f, cy, cx + 10f, cy, uiPaint) }
            Tool.TRANSFORM -> c.drawRect(cx - 8f, cy - 8f, cx + 8f, cy + 8f, uiPaint)
            Tool.LINE -> c.drawLine(cx - 9f, cy + 8f, cx + 9f, cy - 8f, uiPaint)
            Tool.RECTANGLE -> c.drawRect(cx - 9f, cy - 7f, cx + 9f, cy + 7f, uiPaint)
            Tool.ELLIPSE -> c.drawOval(cx - 9f, cy - 7f, cx + 9f, cy + 7f, uiPaint)
        }
        uiPaint.style = Paint.Style.FILL
    }

    private fun makePaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK; alpha = (255 * opacity * flow).toInt().coerceIn(0, 255)
        style = Paint.Style.STROKE; strokeWidth = max(1f, brushSize); strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        if (tool == Tool.ERASER) xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, color: Int, size: Float) {
        textPaint.color = color; textPaint.textSize = size; c.drawText(s, x, y, textPaint)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val x = e.x; val y = e.y; val w = width.toFloat(); val h = height.toFloat()
        if (e.action == MotionEvent.ACTION_DOWN) {
            // Every panel follows the same rule: tap its header to close it.
            if (openPanel != null && x >= w - 292f && y in 50f..88f) { openPanel = null; invalidate(); return true }

            // Top panel buttons: tap opens; tapping the already-open panel closes it.
            val panelNames = Panel.values()
            if (y in 8f..44f && x >= 190f) {
                val idx = ((x - 190f) / 82f).toInt()
                if (idx in panelNames.indices) {
                    openPanel = if (openPanel == panelNames[idx]) null else panelNames[idx]
                    invalidate(); return true
                }
            }

            if (x < 62f && y in 50f..(h - 112f)) {
                val idx = ((y - 53f) / 42f).toInt().coerceIn(0, Tool.values().lastIndex)
                tool = Tool.values()[idx]; openPanel = Panel.OPTIONS; invalidate(); return true
            }

            if (openPanel == Panel.BRUSHES && x >= w - 292f) {
                val listTop = 50f + 56f + 24f
                val relative = y - listTop
                if (relative >= 0f) {
                    var cursor = 0f
                    for (family in BrushCatalog.families) {
                        cursor += 21f
                        val items = BrushCatalog.presets.filter { it.family == family }
                        for (preset in items) {
                            if (relative in cursor..(cursor + 18f)) {
                                selectedBrush = preset
                                tool = when (preset.engine) { Engine.VECTOR -> Tool.BRUSH; Engine.TOONZ_RASTER -> Tool.PENCIL; Engine.FULL_COLOR_MYPAINT -> Tool.BRUSH }
                                openPanel = Panel.OPTIONS
                                invalidate(); return true
                            }
                            cursor += 18f
                        }
                        cursor += 5f
                    }
                }
            }

            // Drawing area.
            val canvasLeft = 72f
            val canvasRight = w - if (openPanel == null) 72f else 302f
            if (x in canvasLeft..canvasRight && y in 60f..(h - 124f) && (tool == Tool.BRUSH || tool == Tool.PENCIL || tool == Tool.ERASER)) {
                current = Path().apply { moveTo(x, y) }; invalidate(); return true
            }
        } else if (e.action == MotionEvent.ACTION_MOVE && current != null) {
            current!!.lineTo(x, y); invalidate(); return true
        } else if (e.action == MotionEvent.ACTION_UP && current != null) {
            current!!.lineTo(x, y); strokes += current!! to makePaint(); current = null; invalidate(); return true
        }
        return true
    }
}
