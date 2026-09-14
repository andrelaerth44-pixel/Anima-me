package com.animame

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.animame.editor.AnimationDocument
import com.animame.editor.AnimationLayer
import com.animame.editor.DrawingFrame
import com.animame.editor.HistoryRegistry

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
    private val history = HistoryRegistry.forDocument(document)
    private var zoom = 1f
    private val baseFrameWidth = 42f
    private val layerHeaderWidth = 230
    private var cycleMode = "Independente"

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.rgb(22, 25, 29))
        buildRuler()
        addView(ruler, LinearLayout.LayoutParams(-1, 38))
        rows.orientation = VERTICAL
        horizontal.isHorizontalScrollBarEnabled = true
        horizontal.addView(rows, android.view.ViewGroup.LayoutParams(-2, -2))
        vertical.isFillViewport = true
        vertical.addView(horizontal, LinearLayout.LayoutParams(-1, -1))
        addView(vertical, LinearLayout.LayoutParams(-1, 0, 1f))
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
            addView(smallButton(cycleMode, 86) { showCycleMode() })
        }
        ruler.addView(tools, LinearLayout.LayoutParams(layerHeaderWidth, -1))
        for (frame in 0 until document.duration) {
            val width = frameWidth()
            val fps = document.fps.coerceAtLeast(1)
            val isSecond = frame % fps == 0
            val isMajor = frame % (fps / 2).coerceAtLeast(1) == 0
            ruler.addView(TextView(context).apply {
                text = if (isSecond) "${frame / fps}s" else ""
                textSize = if (isSecond) 8f else 6f
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                setPadding(0, 0, 0, 2)
                setTextColor(if (frame == document.currentFrame) Color.WHITE else Color.LTGRAY)
                setBackgroundColor(
                    when {
                        frame == document.currentFrame -> Color.rgb(58, 64, 72)
                        frame in document.playbackStart..document.playbackEnd && isSecond -> Color.rgb(43, 50, 58)
                        isMajor -> Color.rgb(37, 41, 47)
                        else -> Color.rgb(31, 35, 40)
                    }
                )
            }, LinearLayout.LayoutParams(width, 38))
        }
    }

    private fun showCycleMode() {
        val modes = arrayOf("Independente", "Sincronizar camadas", "Manter duração geral")
        val selected = modes.indexOf(cycleMode).coerceAtLeast(0)
        AlertDialog.Builder(context)
            .setTitle("Modo de duração")
            .setSingleChoiceItems(modes, selected) { dialog, which ->
                cycleMode = modes[which]
                dialog.dismiss()
                buildRuler()
                onLayerChanged()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun refresh(accent: Int) {
        buildRuler()
        rows.removeAllViews()
        document.layers.forEach { layer ->
            rows.addView(buildLayerRow(layer, accent), LinearLayout.LayoutParams(trackWidth() + layerHeaderWidth, 74))
        }
    }

    private fun record(before: com.animame.editor.AnimationDocumentSnapshot, label: String) {
        history.record(before, document.snapshot(), label)
        onLayerChanged()
    }

    private fun buildLayerRow(layer: AnimationLayer, accent: Int): View {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(if (layer.id == document.selectedLayerId) Color.rgb(39, 44, 51) else Color.rgb(27, 30, 35))
        }
        val header = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(8, 4, 6, 3)
            gravity = Gravity.CENTER_VERTICAL
        }
        val name = TextView(context).apply {
            text = if (layer.isBackground) "Fundo" else layer.name
            textSize = 12f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setSingleLine(true)
            setOnClickListener { if (!layer.isBackground) onLayerSelected(layer.id) }
            setOnLongClickListener {
                if (!layer.isBackground) showLayerProperties(layer)
                true
            }
        }
        header.addView(name, LinearLayout.LayoutParams(-1, 29))
        val controls = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        controls.addView(smallButton(if (layer.visible) "V" else "-", 26) {
            val before = document.snapshot()
            layer.visible = !layer.visible
            record(before, "Alterar visibilidade da camada")
        })
        controls.addView(smallButton(if (layer.locked) "L" else "U", 26) {
            if (!layer.isBackground) {
                val before = document.snapshot()
                layer.locked = !layer.locked
                record(before, "Alterar bloqueio da camada")
            }
        })
        if (!layer.isBackground) {
            controls.addView(smallButton("<", 24) {
                val before = document.snapshot()
                document.moveLayer(layer.id, -1)
                record(before, "Mover camada")
            })
            controls.addView(smallButton(">", 24) {
                val before = document.snapshot()
                document.moveLayer(layer.id, 1)
                record(before, "Mover camada")
            })
            controls.addView(smallButton("+", 24) {
                val before = document.snapshot()
                val created = document.addLayer()
                onLayerSelected(created.id)
                record(before, "Adicionar camada")
            })
            controls.addView(smallButton("x", 24) {
                val before = document.snapshot()
                document.deleteLayer(layer.id)
                record(before, "Excluir camada")
            })
        }
        header.addView(controls, LinearLayout.LayoutParams(-1, 30))
        row.addView(header, LinearLayout.LayoutParams(layerHeaderWidth, -1))
        val track = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setBackgroundColor(Color.rgb(20, 23, 27))
        }
        for (frame in 0 until document.duration) {
            val drawing = layer.frameAt(frame)
            val cell = TextView(context).apply {
                text = ""
                gravity = Gravity.CENTER
                setBackgroundColor(
                    when {
                        frame == document.currentFrame -> accent
                        drawing != null && drawing.strokes.isNotEmpty() -> Color.rgb(49, 56, 64)
                        drawing != null -> Color.rgb(42, 48, 55)
                        else -> Color.rgb(30, 34, 39)
                    }
                )
                setOnClickListener { onFrameSelected(layer.id, frame) }
                setOnLongClickListener {
                    if (!layer.isBackground && drawing != null) showFrameProperties(layer, frame, drawing)
                    true
                }
            }
            track.addView(cell, LinearLayout.LayoutParams(frameWidth(), 68).apply { setMargins(2, 3, 2, 3) })
        }
        row.addView(track, LinearLayout.LayoutParams(trackWidth(), -1))
        return row
    }

    private fun renameLayer(layer: AnimationLayer) {
        val before = document.snapshot()
        val input = EditText(context).apply { setSingleLine(true); setText(layer.name); selectAll() }
        AlertDialog.Builder(context)
            .setTitle("Nome da camada")
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar") { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) { layer.name = value; record(before, "Renomear camada") }
            }.show()
    }

    private fun showLayerProperties(layer: AnimationLayer) {
        val before = document.snapshot()
        val box = LinearLayout(context).apply { orientation = VERTICAL; setPadding(24, 8, 24, 0) }
        val opacity = SeekBar(context).apply { max = 100; progress = (layer.opacity.coerceIn(0f, 1f) * 100f).toInt() }
        val value = TextView(context).apply { text = "Opacidade: ${opacity.progress}%"; setTextColor(Color.DKGRAY) }
        opacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) { layer.opacity = progress / 100f; value.text = "Opacidade: $progress%" } }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        box.addView(value); box.addView(opacity)
        AlertDialog.Builder(context).setTitle("Propriedades da camada").setView(box)
            .setNegativeButton("Cancelar") { _, _ -> document.restore(before); onLayerChanged() }
            .setNeutralButton("Renomear") { _, _ -> renameLayer(layer) }
            .setPositiveButton("Guardar") { _, _ -> record(before, "Alterar propriedades da camada") }.show()
    }

    private fun showFrameProperties(layer: AnimationLayer, frameIndex: Int, drawing: DrawingFrame) {
        val before = document.snapshot()
        val box = LinearLayout(context).apply { orientation = VERTICAL; setPadding(24, 8, 24, 0) }
        val exposure = SeekBar(context).apply { max = 24; progress = drawing.exposure.coerceIn(1, 24) - 1 }
        val value = TextView(context).apply { text = "Exposição: ${drawing.exposure} frame(s)"; setTextColor(Color.DKGRAY) }
        exposure.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) { drawing.exposure = progress + 1; value.text = "Exposição: ${drawing.exposure} frame(s)" } }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        box.addView(value); box.addView(exposure)
        AlertDialog.Builder(context).setTitle("Frame ${frameIndex + 1}")
            .setMessage("A exposição mantém este desenho visível por vários frames sem duplicar o conteúdo.")
            .setView(box)
            .setNegativeButton("Cancelar") { _, _ -> document.restore(before); onLayerChanged() }
            .setPositiveButton("Guardar") { _, _ -> record(before, "Alterar exposição do frame") }.show()
    }

    private fun setZoom(value: Float) { zoom = value.coerceIn(.5f, 4f); refresh(ThemeColorStore.get(context)) }

    private fun setPlaybackStart() {
        val before = document.snapshot()
        document.playbackStart = document.currentFrame.coerceIn(0, document.duration - 1)
        if (document.playbackEnd < document.playbackStart) document.playbackEnd = document.playbackStart
        history.record(before, document.snapshot(), "Definir início da reprodução")
        refresh(ThemeColorStore.get(context))
    }

    private fun setPlaybackEnd() {
        val before = document.snapshot()
        document.playbackEnd = document.currentFrame.coerceIn(document.playbackStart, document.duration - 1)
        history.record(before, document.snapshot(), "Definir fim da reprodução")
        refresh(ThemeColorStore.get(context))
    }

    private fun label(text: String, width: Int) = TextView(context).apply {
        this.text = text; textSize = 9f; gravity = Gravity.CENTER; setTextColor(Color.LTGRAY); setPadding(2, 0, 2, 0)
    }

    private fun smallButton(text: String, width: Int, action: () -> Unit) = Button(context).apply {
        this.text = text; textSize = 8f; setPadding(0, 0, 0, 0); setOnClickListener { action() }; layoutParams = LinearLayout.LayoutParams(width, 32)
    }
}
