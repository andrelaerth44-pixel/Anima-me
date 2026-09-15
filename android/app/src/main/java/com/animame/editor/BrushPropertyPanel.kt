package com.animame.editor

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.ToggleButton
import kotlin.math.roundToInt

/**
 * Reusable brush-properties panel for the horizontal editor UI.
 * It edits the same BrushSettings model used by BrushEngine, rather than
 * keeping presentation-only values.
 */
class BrushPropertyPanel(
    private val host: android.content.Context,
    private val initial: BrushSettings,
    private val onChanged: (BrushSettings) -> Unit
) {
    private var value = initial.normalized()

    fun build(): LinearLayout {
        return LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 10, 14, 10)
            setBackgroundColor(Color.rgb(25, 28, 33))

            addTitle("Propriedades do pincel")
            addLabel("${value.name}  |  ${value.category}")
            addSeek("Tamanho", 1, 4096, value.size.roundToInt()) { v -> update(value.copy(size = v.toFloat())) }
            addSeek("Opacidade", 1, 100, (value.opacity * 100f).roundToInt()) { v -> update(value.copy(opacity = v / 100f)) }
            addSeek("Tamanho mínimo", 0, 100, (value.minSizeFactor * 100f).roundToInt()) { v -> update(value.copy(minSizeFactor = v / 100f)) }
            addSeek("Opacidade mínima", 0, 100, (value.minOpacity * 100f).roundToInt()) { v -> update(value.copy(minOpacity = v / 100f)) }
            addSeek("Espaçamento", 1, 400, (value.spacing * 100f).roundToInt()) { v -> update(value.copy(spacing = v / 100f)) }
            addSeek("Fade início", 0, 100, (value.fadeStart * 100f).roundToInt()) { v -> update(value.copy(fadeStart = v / 100f)) }
            addSeek("Fade fim", 0, 100, (value.fadeEnd * 100f).roundToInt()) { v -> update(value.copy(fadeEnd = v / 100f)) }
            addSeek("Jitter posição", 0, 100, (value.jitterPosition * 100f).roundToInt()) { v -> update(value.copy(jitterPosition = v / 100f)) }
            addSeek("Jitter tamanho", 0, 100, (value.jitterThickness * 100f).roundToInt()) { v -> update(value.copy(jitterThickness = v / 100f)) }
            addSeek("Jitter opacidade", 0, 100, (value.jitterOpacity * 100f).roundToInt()) { v -> update(value.copy(jitterOpacity = v / 100f)) }
            addSeek("Rotação", -180, 180, value.initialAngle.roundToInt()) { v -> update(value.copy(initialAngle = v.toFloat())) }
            addSeek("Aspecto", 5, 2000, (value.aspect * 100f).roundToInt()) { v -> update(value.copy(aspect = v / 100f)) }
            addSeek("Desfoque", 0, 100, (value.blur * 100f).roundToInt()) { v -> update(value.copy(blur = v / 100f)) }
            addSeek("Dispersão", 0, 100, (value.scatterSize * 100f).roundToInt()) { v -> update(value.copy(scatterSize = v / 100f)) }
            addSeek("Densidade de partículas", 0, 100, (value.scatterDensity * 100f).roundToInt()) { v -> update(value.copy(scatterDensity = v / 100f)) }
            addSeek("Pressão no tamanho", 0, 200, (value.pressureSizeFactor * 100f).roundToInt()) { v -> update(value.copy(pressureSizeFactor = v / 100f)) }
            addSeek("Pressão na opacidade", 0, 200, (value.pressureOpacityFactor * 100f).roundToInt()) { v -> update(value.copy(pressureOpacityFactor = v / 100f)) }
            addSeek("Velocidade no tamanho", 0, 200, (value.speedSizeFactor * 100f).roundToInt()) { v -> update(value.copy(speedSizeFactor = v / 100f)) }
            addSeek("Velocidade na opacidade", 0, 200, (value.speedOpacityFactor * 100f).roundToInt()) { v -> update(value.copy(speedOpacityFactor = v / 100f)) }
            addSeek("Mistura da aguada", 0, 100, (value.waterColorMix * 100f).roundToInt()) { v -> update(value.copy(waterColorMix = v / 100f)) }
            addSeek("Humidade", 0, 100, (value.waterWetness * 100f).roundToInt()) { v -> update(value.copy(waterWetness = v / 100f)) }
            addToggle("Anti-alias", value.antialias) { update(value.copy(antialias = it)) }
            addToggle("Seguir rotação", value.followRotation) { update(value.copy(followRotation = it)) }
            addToggle("Opacidade constante", value.constantOpacity) { update(value.copy(constantOpacity = it)) }
            addToggle("Adicionar opacidade", value.addOpacity) { update(value.copy(addOpacity = it)) }
            addToggle("Separar traços", value.separateStroke) { update(value.copy(separateStroke = it)) }
            addToggle("Espaçamento fino", value.thinSpacing) { update(value.copy(thinSpacing = it)) }
            addToggle("Tamanho absoluto da textura", value.textureAbsoluteSize) { update(value.copy(textureAbsoluteSize = it)) }
            addToggle("Textura invertida", value.textureInvert) { update(value.copy(textureInvert = it)) }
        }
    }

    private fun update(next: BrushSettings) {
        value = next.normalized()
        onChanged(value)
    }

    private fun LinearLayout.addTitle(textValue: String) {
        addView(TextView(host).apply {
            text = textValue
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 8)
        })
    }

    private fun LinearLayout.addLabel(textValue: String) {
        addView(TextView(host).apply {
            text = textValue
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, 8)
        })
    }

    private fun LinearLayout.addSeek(label: String, min: Int, max: Int, progress: Int, onValue: (Int) -> Unit) {
        addView(TextView(host).apply {
            text = label
            textSize = 12f
            setTextColor(Color.LTGRAY)
        })
        addView(SeekBar(host).apply {
            this.max = (max - min).coerceAtLeast(1)
            this.progress = (progress - min).coerceIn(0, this.max)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    if (fromUser) onValue(p + min)
                }
                override fun onStartTrackingTouch(s: SeekBar?) = Unit
                override fun onStopTrackingTouch(s: SeekBar?) = Unit
            })
        })
    }

    private fun LinearLayout.addToggle(label: String, checked: Boolean, onValue: (Boolean) -> Unit) {
        addView(ToggleButton(host).apply {
            textOn = label
            textOff = label
            isChecked = checked
            gravity = Gravity.CENTER
            setOnCheckedChangeListener { _, enabled -> onValue(enabled) }
        })
    }
}
