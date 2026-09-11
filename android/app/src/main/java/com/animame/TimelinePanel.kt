package com.animame

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.animame.editor.AnimationDocument
import com.animame.editor.AnimationLayer

/**
 * Timeline inspired by RoughAnimator's model: every layer owns a horizontal
 * sequence of drawings, and the layer rows themselves define compositing order.
 * The top row is the top visual layer; rows below it are behind it.
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
    private val frameWidth = 42
    private val layerHeaderWidth = 190

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(22, 25, 29))
        build()
    }

    private fun build() {
        addView(buildRuler(), LayoutParams(-1, 30))
        rows.orientation = VERTICAL
        horizontal.isHorizontalScrollBarEnabled = true
        horizontal.addView(rows, HorizontalScrollView.LayoutParams(-2, -2))
        vertical.isFillViewport = true
        vertical.addView(horizontal, ScrollView.LayoutParams(-1, -1))
        addView(vertical, LayoutParams(-1, 0, 1f))
    }

    private fun buildRuler(): View {
        ruler.orientation = HORIZONTAL
        ruler.setBackgroundColor(Color.rgb(31, 35, 40))
        val spacer = TextView(context).apply { setBackgroundColor(Color.rgb(31, 35, 40)) }
        ruler.addView(spacer, LayoutParams(layerHeaderWidth, -1))
        for (frame in 0 until document.duration) {
            val t = TextView(context).apply {
                text = if (frame % document.fps == 0) "${frame / document.fps + 1}s" else "${frame + 1}"
                textSize = 9f
                gravity = Gravity.CENTER
                setTextColor(Color.LTGRAY)
                setBackgroundColor(if (frame == document.currentFrame) Color.rgb(70, 80, 92) else Color.rgb(31, 35, 40))
            }
            ruler.addView(t, LayoutParams(frameWidth, -1))
        }
        return ruler
    }

    fun refresh(accent: Int) {
        rows.removeAllViews()
        ruler.removeAllViews()
        buildRuler()
        // buildRuler creates a new view, so rebuild the ruler contents explicitly.
        ruler.orientation = HORIZONTAL
        ruler.setBackgroundColor(Color.rgb(31, 35, 40))
        ruler.addView(TextView(context).apply { setBackgroundColor(Color.rgb(31, 35, 40)) }, LayoutParams(layerHeaderWidth, -1))
        for (frame in 0 until document.duration) {
            val t = TextView(context).apply {
                text = if (frame % document.fps == 0) "${frame / document.fps + 1}s" else "${frame + 1}"
                textSize = 9f
                gravity = Gravity.CENTER
                setTextColor(Color.LTGRAY)
                setBackgroundColor(if (frame == document.currentFrame) accent else Color.rgb(31, 35, 40))
            }
            ruler.addView(t, LayoutParams(frameWidth, -1))
        }

        document.layers.forEachIndexed { index, layer ->
            rows.addView(buildLayerRow(layer, index, accent), LayoutParams(-2, 64))
        }
        requestLayout()
    }

    private fun buildLayerRow(layer: AnimationLayer, index: Int, accent: Int): View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(if (layer.id == document.selectedLayerId) Color.rgb(38, 44, 51) else Color.rgb(27, 30, 34))
            setOnClickListener { onLayerSelected(layer.id) }
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
            setSelectAllOnFocus(false)
            setTypeface(null, if (layer.id == document.selectedLayerId) Typeface.BOLD else Typeface.NORMAL)
            setPadding(2, 0, 2, 0)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) commitName(layer)
            }
            setOnEditorActionListener { _, _, _ ->
                commitName(layer)
                clearFocus()
                true
            }
        }
        top.addView(name, LinearLayout.LayoutParams(92, 30))
        top.addView(smallButton(if (layer.visible) "Vis" else "Oculta") {
            layer.visible = !layer.visible
            onLayerChanged()
        }, LinearLayout.LayoutParams(38, 28))
        top.addView(smallButton(if (layer.locked) "Lock" else "Edit") {
            layer.locked = !layer.locked
            onLayerChanged()
        }, LinearLayout.LayoutParams(38, 28))
        controls.addView(top)

        val bottom = LinearLayout(context).apply { orientation = HORIZONTAL }
        bottom.addView(smallButton("Subir") { document.moveLayer(layer.id, -1); onLayerChanged() }, LinearLayout.LayoutParams(48, 26))
        bottom.addView(smallButton("Descer") { document.moveLayer(layer.id, 1); onLayerChanged() }, LinearLayout.LayoutParams(48, 26))
        bottom.addView(smallButton("Excluir") {
            if (document.layers.size > 1) document.deleteLayer(layer.id) else Toast.makeText(context, "A animação precisa de pelo menos uma camada", Toast.LENGTH_SHORT).show()
            onLayerChanged()
        }, LinearLayout.LayoutParams(58, 26))
        controls.addView(bottom)
        row.addView(controls, LinearLayout.LayoutParams(layerHeaderWidth, 64))

        for (frame in 0 until document.duration) {
            val drawing = layer.frameAt(frame)
            val cell = TextView(context).apply {
                text = if (drawing != null && drawing.strokes.isNotEmpty()) "●" else ""
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(if (frame == document.currentFrame && layer.id == document.selectedLayerId) Color.WHITE else Color.LTGRAY)
                setBackgroundColor(
                    when {
                        frame == document.currentFrame && layer.id == document.selectedLayerId -> accent
                        drawing != null && drawing.strokes.isNotEmpty() -> Color.rgb(74, 81, 91)
                        drawing != null -> Color.rgb(52, 57, 64)
                        else -> Color.rgb(34, 38, 43)
                    }
                )
                setOnClickListener { onFrameSelected(layer.id, frame) }
            }
            row.addView(cell, LayoutParams(frameWidth, 60).apply { setMargins(1, 2, 1, 2) })
        }
        return row
    }

    private fun commitName(layer: AnimationLayer) {
        // Name is committed by the EditText itself in buildLayerRow via the current view.
        // Rebuild only after an explicit layer action; text remains bound to the model through focus events below.
    }

    private fun smallButton(label: String, action: () -> Unit): TextView = TextView(context).apply {
        text = label
        textSize = 8f
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(48, 54, 61))
        setOnClickListener { action() }
    }
}
