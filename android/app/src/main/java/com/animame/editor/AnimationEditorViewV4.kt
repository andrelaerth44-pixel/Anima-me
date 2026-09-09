package com.animame.editor

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import kotlin.math.abs

/** Stable production interaction layer for the scrollable brush library. */
class AnimationEditorViewV4(context: android.content.Context) : AnimationEditorViewV5(context) {
    private var brushScroll = 0f
    private var brushScrollStartY = 0f
    private var brushScrollStartOffset = 0f
    private var brushDragging = false

    init { CustomBrushStore.initialize(context) }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (get("panel")?.toString()?.endsWith("BRUSHES") == true) drawBrushLibrary(canvas)
    }

    private fun get(name: String): Any? = runCatching {
        val field = AnimationEditorViewV5::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.get(this)
    }.getOrNull()

    private fun set(name: String, value: Any?) {
        runCatching {
            val field = AnimationEditorViewV5::class.java.getDeclaredField(name)
            field.isAccessible = true
            field.set(this, value)
        }
    }

    private fun drawBrushLibrary(canvas: Canvas) {
        val right = width - 300f
        val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.rgb(29, 31, 34)
        }
        canvas.drawRect(right, 150f, width.toFloat(), height.toFloat(), background)

        canvas.save()
        canvas.clipRect(RectF(right, 204f, width.toFloat(), height.toFloat()))
        var y = 206f - brushScroll
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; textSize = 9f }

        for (family in BrushCatalog.families) {
            if (y > 180f && y < height + 20f) {
                text.color = Color.WHITE
                text.textSize = 10f
                canvas.drawText(family, right + 14f, y, text)
            }
            y += 18f
            for (preset in BrushCatalog.presets) {
                if (preset.family != family) continue
                if (y > 185f && y < height + 20f) {
                    val selected = preset.id == (get("selectedBrush") as? BrushPreset)?.id
                    text.color = if (selected) Color.WHITE else Color.LTGRAY
                    text.textSize = 9f
                    val label = if (selected) "• ${preset.name}" else "  ${preset.name}"
                    canvas.drawText(label, right + 20f, y, text)
                }
                y += 16f
            }
            y += 4f
        }
        canvas.restore()

        val header = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = Color.WHITE; textSize = 9f }
        canvas.drawText("IMPORTAR QR / IMAGEM", right + 10f, 198f, header)
        header.color = Color.GRAY
        header.textSize = 8f
        canvas.drawText("deslize para ver todos os pincéis", right + 10f, height - 10f, header)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount >= 2) return super.onTouchEvent(event)

        val panel = get("panel")?.toString() ?: ""
        val right = width - 300f
        if (panel.endsWith("BRUSHES") && event.x >= right && event.y > 204f) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    brushDragging = true
                    brushScrollStartY = event.y
                    brushScrollStartOffset = brushScroll
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (brushDragging) {
                        val maxScroll = (brushContentHeight() - 260f).coerceAtLeast(0f)
                        brushScroll = (brushScrollStartOffset - (event.y - brushScrollStartY)).coerceIn(0f, maxScroll)
                        postInvalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (brushDragging) {
                        val moved = abs(event.y - brushScrollStartY)
                        brushDragging = false
                        if (moved < 12f) brushAt(event.y + brushScroll)?.let { selectBrush(it) }
                        postInvalidate()
                    }
                    return true
                }
                MotionEvent.ACTION_CANCEL -> {
                    brushDragging = false
                    postInvalidate()
                    return true
                }
                else -> return super.onTouchEvent(event)
            }
        }
        return super.onTouchEvent(event)
    }

    private fun selectBrush(preset: BrushPreset) {
        set("selectedBrush", preset)
        set("brushSettings", preset.defaults.copy())
        set("brushSize", preset.defaults.size)
        set("opacity", preset.defaults.opacity)
        postInvalidate()
    }

    private fun brushContentHeight(): Float {
        var contentHeight = 0f
        for (family in BrushCatalog.families) {
            contentHeight += 22f
            contentHeight += BrushCatalog.presets.count { it.family == family } * 16f + 4f
        }
        return contentHeight
    }

    private fun brushAt(inputY: Float): BrushPreset? {
        var y = 206f
        for (family in BrushCatalog.families) {
            y += 18f
            for (preset in BrushCatalog.presets) {
                if (preset.family != family) continue
                if (inputY in (y - 16f)..(y + 2f)) return preset
                y += 16f
            }
            y += 4f
        }
        return null
    }
}
