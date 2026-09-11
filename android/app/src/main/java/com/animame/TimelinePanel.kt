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
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.animame.editor.AnimationDocument
import com.animame.editor.AnimationLayer
import com.animame.editor.DrawingFrame
import kotlin.math.max
import kotlin.math.min

/**
 * Professional frame-by-frame timeline inspired by the interaction model of
 * RoughAnimator, while keeping Anima-me's own UI and data model.
 *
 * Layer order is explicit: index 0 is the visual top layer. Moving a layer
 * up decreases its index; moving it down increases its index.
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
    private val layerHeaderWidth = 230
    private var rangeMode = RangeMode.NONE
    private enum class RangeMode { NONE, START, END }

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
    private fun trackWidth(): Int = (document.duration * frameWidth() + 12).coerceAtLeast(frameWidth())

    private fun buildRuler() {
        ruler.removeAllViews()
        ruler.orientation = HORIZONTAL
        ruler.setBackgroundColor(Color.rgb(31, 35, 40))

        val tools = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(4, 0, 4, 0)
            addView(label("TIMELINE", 66), LinearLayout.LayoutParams(66, -1))
            addView(smallButton("-", 28) { setZoom(zoom / 1.25f) })
            addView(smallButton("+", 28) { setZoom(zoom * 1.25f) })
            addView(label("${(zoom * 100).toInt()}%", 42), LinearLayout.LayoutParams(42, -1))
            addView(smallButton("In", 30) { setPlaybackStart() })
            addView(smallButton("Out", 30) { setPlaybackEnd() })
        }
        ruler.addView(tools, LinearLayout.LayoutParams(layerHeaderWidth, -1))

        for (frame in 0 until document.duration) {
            val width = frameWidth()
            val isSecond = frame % document.fps.coerceAtLeast(1) == 0
            val isMajor = frame % (document.fps.coerceAtLeast(1) / 2).coerceAtLeast(1) == 0
            ruler.addView(TextView(context).apply {
                text = if (isSecond) "${frame / document.fps.coerceAtLeast(1)}s" else ""
                textSize = if (isSecond) 8f else 6f
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setPadding(0, 0, 0, 2)
                setTextColor(if (frame == document.currentFrame) Color.WHITE else Color.LTGRAY)
                setBackgroundColor(
                    when {
                        frame in document.playbackStart..document.playbackEnd && isSecond -> Color.rgb(43, 50, 58)
                        frame == document.currentFrame -> Color.rgb(58, 64, 72)
                        isMajor -> Color.rgb(37, 41, 47)
                        else -> Color.rgb(31, 35, 40)
                    }
                )
            }, LinearLayout.LayoutParams(width, 38))
        }

        ruler.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE || event.actionMasked == MotionEvent.ACTION_UP) {
                val x = event.x + horizontal.scrollX - layerHeaderWidth
                if (x >= 0f) {
                    val frame = (x / frameWidth()).toInt().coerceIn(0, document.duration - 1)
                    when (rangeMode) {
                        RangeMode.START -> document.setPlaybackRange(frame, document.playbackEnd)
                        RangeMode.END -> document.setPlaybackRange(document.playbackStart, frame)
                        RangeMode.NONE -> onFrameSelected(document.activeLayer.id, frame)
                    }
                    onLayerChanged()
                }
                true
            } else false
        }

        addView(ruler, 0, LayoutParams(-1, 38))
    }

    private fun setPlaybackStart() {
        rangeMode = if (rangeMode == RangeMode.START) RangeMode.NONE else RangeMode.START
        Toast.makeText(context, if (rangeMode == RangeMode.START) "Arraste na régua para definir o início" else "Início definido", Toast.LENGTH_SHORT).show()
    }

    private fun setPlaybackEnd() {
        rangeMode = if (rangeMode == RangeMode.END) RangeMode.NONE else RangeMode.END
        Toast.makeText(context, if (rangeMode == RangeMode.END) "Arraste na régua para definir o fim" else "Fim definido", Toast.LENGTH_SHORT).show()
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
        document.layers.forEachIndexed { index, layer ->
            rows.addView(buildLayerRow(layer, accent, index), LayoutParams(-2, 68))
        }
    }

    private fun buildLayerRow(layer: AnimationLayer, accent: Int, layerIndex: Int): View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(if (layer.id == document.selectedLayerId) Color.rgb(38, 44, 51) else Color.rgb(27, 30, 34))
        }

        val controls = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(5, 3, 5, 3)
            setBackgroundColor(if (layer.id == document.selectedLayerId) Color.rgb(42, 47, 54) else Color.rgb(34, 38, 43))
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
            setOnFocusChangeListener { _, hasFocus -> if (!hasFocus) commitName(layer, text.toString()) }
            setOnEditorActionListener { _, _, _ -> commitName(layer, text.toString()); clearFocus(); true }
        }
        top.addView(name, LinearLayout.LayoutParams(84, 30))
        top.addView(smallButton(if (layer.visible) "Vis" else "Oculta", 38) { layer.visible = !layer.visible; onLayerChanged() })
        top.addView(smallButton(if (layer.locked) "Lock" else "Edit", 38) { layer.locked = !layer.locked; onLayerChanged() })
        controls.addView(top)

        val bottom = LinearLayout(context).apply { orientation = HORIZONTAL }
        bottom.addView(smallButton("Cima", 42) {
            document.selectLayer(layer.id)
            document.moveLayer(layer.id, -1)
            onLayerChanged()
        })
        bottom.addView(smallButton("Baixo", 42) {
            document.selectLayer(layer.id)
            document.moveLayer(layer.id, 1)
            onLayerChanged()
        })
        bottom.addView(smallButton("Duplicar", 54) { duplicateLayer(layer) })
        bottom.addView(smallButton("Mesclar", 52) { mergeDown(layer) })
        bottom.addView(smallButton("Excluir", 52) {
            if (document.layers.size > 1) document.deleteLayer(layer.id)
            else Toast.makeText(context, "A animação precisa de pelo menos uma camada", Toast.LENGTH_SHORT).show()
            onLayerChanged()
        })
        controls.addView(bottom)

        val opacity = SeekBar(context).apply {
            max = 100
            progress = (layer.opacity.coerceIn(0f, 1f) * 100f).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) { layer.opacity = value / 100f; onLayerChanged() }
                }
                override fun onStartTrackingTouch(bar: SeekBar?) {}
                override fun onStopTrackingTouch(bar: SeekBar?) {}
            })
        }
        controls.addView(opacity, LinearLayout.LayoutParams(-1, 22))
        controls.setOnClickListener { onLayerSelected(layer.id) }
        controls.setOnLongClickListener { startLayerDrag(layer.id); true }

        row.addView(controls, LinearLayout.LayoutParams(layerHeaderWidth, 68))
        row.addView(ExposureTrackView(context, layer, accent), LinearLayout.LayoutParams(trackWidth(), 64))
        return row
    }

    private fun commitName(layer: AnimationLayer, value: String) {
        val clean = value.trim()
        if (clean.isNotEmpty()) layer.name = clean
        onLayerChanged()
    }

    private fun startLayerDrag(layerId: String) {
        val current = document.layers.indexOfFirst { it.id == layerId }
        if (current < 0) return
        Toast.makeText(context, "Camada ${current + 1}: use Cima/Baixo para reposicionar", Toast.LENGTH_SHORT).show()
    }

    private fun duplicateLayer(source: AnimationLayer) {
        val copy = AnimationLayer(name = "${source.name} cópia", visible = source.visible, locked = source.locked, opacity = source.opacity, blendMode = source.blendMode)
        source.frames.forEach { (frame, drawing) ->
            val clone = DrawingFrame(exposure = drawing.exposure)
            drawing.strokes.forEach { stroke -> clone.strokes += stroke.copy(samples = stroke.samples.map { it.copy() }.toMutableList()) }
            copy.frames[frame] = clone
        }
        val index = document.layers.indexOfFirst { it.id == source.id }.coerceAtLeast(0)
        document.layers.add(index, copy)
        document.selectLayer(copy.id)
        onLayerChanged()
    }

    private fun mergeDown(source: AnimationLayer) {
        val index = document.layers.indexOfFirst { it.id == source.id }
        if (index < 0 || index >= document.layers.lastIndex) {
            Toast.makeText(context, "Não existe uma camada abaixo para mesclar", Toast.LENGTH_SHORT).show()
            return
        }
        val target = document.layers[index + 1]
        source.frames.forEach { (frame, drawing) ->
            val destination = target.ensureFrame(frame)
            drawing.strokes.forEach { stroke -> destination.strokes += stroke.copy(samples = stroke.samples.map { it.copy() }.toMutableList()) }
            destination.exposure = max(destination.exposure, drawing.exposure)
        }
        document.deleteLayer(source.id)
        document.selectLayer(target.id)
        onLayerChanged()
    }

    private inner class ExposureTrackView(
        context: Context,
        private val layer: AnimationLayer,
        private val accent: Int
    ) : View(context) {
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val border = Paint(Paint.ANTI_ALIAS_FLAG)
        private var resizing: DrawingFrame? = null

        init { isClickable = true; setBackgroundColor(Color.rgb(27, 30, 34)) }

        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            val fw = frameWidth().toFloat()
            val h = height.toFloat()
            var frame = 0
            while (frame < document.duration) {
                val drawing = layer.frames[frame]
                if (drawing == null) { frame++; continue }
                val exposure = drawing.exposure.coerceAtLeast(1)
                val end = min(document.duration, frame + exposure)
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
                frame = end
            }
            val playheadX = document.currentFrame * fw + fw * .5f
            border.color = Color.WHITE
            border.strokeWidth = 2f
            c.drawRect(playheadX - 1f, 0f, playheadX + 1f, h, border)
            if (document.currentFrame in document.playbackStart..document.playbackEnd) {
                border.color = Color.argb(80, 255, 255, 255)
                c.drawRect(document.playbackStart * fw, 0f, (document.playbackEnd + 1) * fw, 2f, border)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val fw = frameWidth().toFloat()
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val frame = (event.x / fw).toInt().coerceIn(0, document.duration - 1)
                    val drawing = drawingAtStart(frame)
                    if (drawing != null) {
                        val end = startOf(drawing) + drawing.exposure
                        if (event.x >= end * fw - max(10f, fw * .25f) && event.x <= end * fw + 8f) resizing = drawing
                        else onFrameSelected(layer.id, frame)
                    } else onFrameSelected(layer.id, frame)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val drawing = resizing ?: return true
                    val start = startOf(drawing)
                    val newExposure = ((event.x / fw) - start).toInt().coerceIn(1, 120)
                    drawing.exposure = newExposure.coerceAtMost((document.duration - start).coerceAtLeast(1))
                    onLayerChanged(); invalidate(); return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { resizing = null; invalidate(); return true }
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
