package com.animame

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.animame.editor.AnimationDocument
import com.animame.editor.AnimationLayer
import com.animame.editor.DrawingFrame

/**
 * RoughAnimator-inspired exposure timeline.
 * Drawings are blocks whose width represents exposure duration. Small frame cells
 * intentionally do not show frame numbers or thumbnails; the current frame is
 * shown over the canvas by MainActivity and the ruler only labels seconds.
 */
class TimelinePanel(
    context: Context,
    private val document: AnimationDocument,
    private val onFrameSelected: (layerId: String, frame: Int) -> Unit,
    private val onLayerSelected: (layerId: String) -> Unit,
    private val onLayerChanged: () -> Unit
) : LinearLayout(context) {
    private val ruler = LinearLayout(context)
    private val rows = LinearLayout(context)
    private val horizontal = HorizontalScrollView(context)
    private val vertical = ScrollView(context)
    private var zoom = 1f
    private val baseFrameWidth = 42f
    private val layerHeaderWidth = 190

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(22, 25, 29))
        buildRuler()
        rows.orientation = VERTICAL
        horizontal.isHorizontalScrollBarEnabled = true
        horizontal.addView(rows, HorizontalScrollView.LayoutParams(-2, -2))
        vertical.isFillViewport = true
        vertical.addView(horizontal, ScrollView.LayoutParams(-1, -1))
        addView(vertical, LayoutParams(-1, 0, 1f))
    }

    private fun frameWidth(): Int = (baseFrameWidth * zoom).toInt().coerceAtLeast(14)

    private fun buildRuler() {
        ruler.removeAllViews()
        ruler.orientation = HORIZONTAL
        ruler.setBackgroundColor(Color.rgb(31, 35, 40))

        val tools = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 0, 4, 0)
            addView(label("TIMELINE", 76), LinearLayout.LayoutParams(76, -1))
            addView(smallButton("-", 30) { setZoom(zoom / 1.25f) })
            addView(smallButton("+", 30) { setZoom(zoom * 1.25f) })
            addView(label("${(zoom * 100).toInt()}%", 48), LinearLayout.LayoutParams(48, -1))
        }
        ruler.addView(tools, LinearLayout.LayoutParams(layerHeaderWidth, -1))

        for (frame in 0 until document.duration) {
            val width = frameWidth()
            val seconds = if (document.fps > 0) frame.toFloat() / document.fps else 0f
            val isSecond = frame % document.fps.coerceAtLeast(1) == 0
            ruler.addView(TextView(context).apply {
                text = if (isSecond) "${seconds.toInt()}s" else ""
                textSize = 8f
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setPadding(0, 0, 0, 2)
                setTextColor(if (frame == document.currentFrame) Color.WHITE else Color.LTGRAY)
                setBackgroundColor(if (frame == document.currentFrame) Color.rgb(58, 64, 72) else Color.rgb(31, 35, 40))
            }, LayoutParams(width, -1))
        }
    }

    fun setZoom(value: Float) {
        zoom = value.coerceIn(.35f, 4f)
        buildRuler()
        refreshRows()
    }

    fun refresh(accent: Int) {
        buildRuler()
        refreshRows(accent)
        requestLayout()
    }

    private fun refreshRows(accent: Int = Color.rgb(38, 44, 51)) {
        rows.removeAllViews()
        document.layers.forEach { layer ->
            rows.addView(buildLayerRow(layer, accent), LayoutParams(-2, 64))
        }
    }

    private fun buildLayerRow(layer: AnimationLayer, accent: Int): View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(if (layer.id == document.selectedLayerId) Color.rgb(38, 44, 51) else Color.rgb(27, 30, 34))
        }

        val controls = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(6, 3, 6, 3)
            setBackgroundColor(Color.rgb(34, 38, 43))
        }
        val top = LinearLayout(context).apply { orientation = HORIZONTAL }
        val name = EditText(context).apply {
            setText(layer.name)
            setTextColor(Color.WHITE)
            textSize = 12f
            setSingleLine(true)
            setTypeface(null, if (layer.id == document.selectedLayerId) Typeface.BOLD else Typeface.NORMAL)
            setPadding(2, 0, 2, 0)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) layer.name = text.toString().ifBlank { layer.name } }
            setOnEditorActionListener { _, _, _ ->
                layer.name = text.toString().ifBlank { layer.name }
                clearFocus()
                true
            }
        }
        top.addView(name, LinearLayout.LayoutParams(92, 30))
        top.addView(smallButton(if (layer.visible) "Vis" else "Oculta", 38) { layer.visible = !layer.visible; onLayerChanged() })
        top.addView(smallButton(if (layer.locked) "Lock" else "Edit", 38) { layer.locked = !layer.locked; onLayerChanged() })
        controls.addView(top)

        val bottom = LinearLayout(context).apply { orientation = HORIZONTAL }
        bottom.addView(smallButton("Subir", 48) { document.moveLayer(layer.id, -1); onLayerChanged() })
        bottom.addView(smallButton("Descer", 48) { document.moveLayer(layer.id, 1); onLayerChanged() })
        bottom.addView(smallButton("Excluir", 58) {
            if (document.layers.size > 1) document.deleteLayer(layer.id)
            else Toast.makeText(context, "A animação precisa de pelo menos uma camada", Toast.LENGTH_SHORT).show()
            onLayerChanged()
        })
        controls.addView(bottom)
        controls.setOnClickListener { onLayerSelected(layer.id) }
        row.addView(controls, LinearLayout.LayoutParams(layerHeaderWidth, 64))

        val track = ExposureTrackView(context, layer, accent)
        row.addView(track, LinearLayout.LayoutParams(trackWidth(), 60))
        return row
    }

    private fun trackWidth(): Int = (document.duration * frameWidth() + 8).coerceAtLeast(frameWidth())

    private inner class ExposureTrackView(
        context: Context,
        private val layer: AnimationLayer,
        private val accent: Int
    ) : View(context) {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val border = Paint(Paint.ANTI_ALIAS_FLAG)
        private var resizing: DrawingFrame? = null
        private var resizeStart = 0f
        private var originalExposure = 1
        private var selectedFrame = -1

        init {
            isClickable = true
            setBackgroundColor(Color.rgb(27, 30, 34))
        }

        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            val fw = frameWidth().toFloat()
            val h = height.toFloat()
            var frame = 0
            while (frame < document.duration) {
                val drawing = layer.frames[frame]
                if (drawing != null) {
                    val exposure = drawing.exposure.coerceAtLeast(1)
                    val end = (frame + exposure).coerceAtMost(document.duration)
                    val left = frame * fw + 2f
                    val right = end * fw - 2f
                    val selected = layer.id == document.selectedLayerId && document.currentFrame in frame until end
                    fill.color = when {
                        selected -> accent
                        drawing.strokes.isNotEmpty() -> Color.rgb(76, 84, 96)
                        else -> Color.rgb(54, 60, 68)
                    }
                    c.drawRect(left, 3f, right, h - 3f, fill)
                    border.color = if (selected) Color.WHITE else Color.rgb(100, 108, 118)
                    border.style = Paint.Style.STROKE
                    border.strokeWidth = if (selected) 2f else 1f
                    c.drawRect(left, 3f, right, h - 3f, border)
                    border.style = Paint.Style.FILL
                    if (drawing.strokes.isNotEmpty()) {
                        fill.color = Color.argb(70, 255, 255, 255)
                        c.drawRect(left + 2f, 5f, minOf(right - 2f, left + 6f), h - 5f, fill)
                    }
                    frame = end
                } else {
                    frame++
                }
            }
            val playheadX = document.currentFrame * fw + fw * .5f
            border.color = Color.WHITE
            border.strokeWidth = 2f
            c.drawRect(playheadX - 1f, 0f, playheadX + 1f, h, border)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val fw = frameWidth().toFloat()
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val frame = (event.x / fw).toInt().coerceIn(0, document.duration - 1)
                    val drawing = drawingAtStart(frame)
                    if (drawing != null) {
                        val start = startOf(drawing)
                        val end = start + drawing.exposure
                        if (event.x >= end * fw - maxOf(10f, fw * .25f) && event.x <= end * fw + 8f) {
                            resizing = drawing
                            resizeStart = event.x
                            originalExposure = drawing.exposure
                        } else {
                            selectedFrame = frame
                            onFrameSelected(layer.id, frame)
                        }
                    } else {
                        selectedFrame = frame
                        onFrameSelected(layer.id, frame)
                    }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val drawing = resizing ?: return true
                    val start = startOf(drawing)
                    val newExposure = ((event.x / fw) - start).toInt().coerceIn(1, 120)
                    val maxExposure = (document.duration - start).coerceAtLeast(1)
                    drawing.exposure = newExposure.coerceAtMost(maxExposure)
                    onLayerChanged()
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    resizing = null
                    invalidate()
                    return true
                }
            }
            return true
        }

        private fun drawingAtStart(frame: Int): DrawingFrame? {
            var best = -1
            layer.frames.keys.forEach { key -> if (key <= frame && key > best) best = key }
            return if (best >= 0) layer.frames[best] else null
        }

        private fun startOf(target: DrawingFrame): Int = layer.frames.entries.firstOrNull { it.value === target }?.key ?: 0
    }

    private fun label(text: String, width: Int): TextView = TextView(context).apply {
        this.text = text
        textSize = 8f
        gravity = Gravity.CENTER
        setTextColor(Color.LTGRAY)
    }

    private fun smallButton(label: String, width: Int, action: () -> Unit): TextView = TextView(context).apply {
        text = label
        textSize = 8f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(48, 54, 61))
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(width, 26).apply { setMargins(2, 0, 2, 0) }
    }
}
