package com.animame

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView

class ToolOptionsActivity : Activity() {
    private val colors = intArrayOf(
        Color.BLACK, Color.WHITE, Color.rgb(244, 67, 54), Color.rgb(255, 152, 0),
        Color.rgb(255, 193, 7), Color.rgb(76, 175, 80), Color.rgb(0, 150, 136),
        Color.rgb(3, 169, 244), Color.rgb(63, 81, 181), Color.rgb(156, 39, 176)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BrushToolState.load(this)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 22, 28, 22)
            setBackgroundColor(Color.rgb(18, 20, 23))
        }
        root.addView(TextView(this).apply {
            text = "Opções do pincel"
            textSize = 24f
            setTextColor(Color.WHITE)
        })
        root.addView(TextView(this).apply {
            text = "Configurações inspiradas no fluxo de ferramentas do RoughAnimator."
            textSize = 13f
            setTextColor(Color.LTGRAY)
            setPadding(0, 4, 0, 12)
        })

        addSlider(root, "Tamanho", 1, 200, BrushToolState.size.toInt()) { BrushToolState.size = it.toFloat() }
        addSlider(root, "Opacidade", 1, 100, (BrushToolState.opacity * 100).toInt()) { BrushToolState.opacity = it / 100f }
        addSlider(root, "Flow", 1, 100, (BrushToolState.flow * 100).toInt()) { BrushToolState.flow = it / 100f }
        addSlider(root, "Espaçamento", 1, 400, (BrushToolState.spacing * 1000).toInt()) { BrushToolState.spacing = it / 1000f }
        addSlider(root, "Suavização", 0, 100, (BrushToolState.smoothing * 100).toInt()) { BrushToolState.smoothing = it / 100f }

        val toggles = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        toggles.addView(check("Pressão", BrushToolState.pressure) { BrushToolState.pressure = it })
        toggles.addView(check("Aleatorizar rotação", BrushToolState.randomRotation) { BrushToolState.randomRotation = it })
        toggles.addView(check("Desenhar dentro", BrushToolState.drawsInside) { BrushToolState.drawsInside = it })
        root.addView(toggles)

        root.addView(TextView(this).apply {
            text = "Cor"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 5)
        })
        val palette = LinearLayout(this).apply { gravity = Gravity.CENTER; orientation = LinearLayout.HORIZONTAL }
        colors.forEach { color ->
            palette.addView(Button(this).apply {
                text = ""
                setBackgroundColor(color)
                setOnClickListener { BrushToolState.color = color; BrushToolState.save(this@ToolOptionsActivity) }
            }, LinearLayout.LayoutParams(0, 48, 1f).apply { setMargins(3, 2, 3, 2) })
        }
        root.addView(palette)

        root.addView(Button(this).apply {
            text = "Guardar"
            setOnClickListener { BrushToolState.save(this@ToolOptionsActivity); setResult(RESULT_OK); finish() }
        }, LinearLayout.LayoutParams(-1, 52).apply { setMargins(0, 14, 0, 0) })
        setContentView(root)
    }

    private fun addSlider(root: LinearLayout, title: String, min: Int, max: Int, value: Int, onChanged: (Int) -> Unit) {
        val label = TextView(this).apply { textSize = 13f; setTextColor(Color.WHITE); text = "$title: $value" }
        val bar = SeekBar(this).apply {
            this.max = max - min
            progress = (value - min).coerceIn(0, this.max)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    val actual = progress + min
                    label.text = "$title: $actual"
                    onChanged(actual)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
            })
        }
        root.addView(label)
        root.addView(bar, LinearLayout.LayoutParams(-1, 42))
    }

    private fun check(title: String, checked: Boolean, onChanged: (Boolean) -> Unit): CheckBox = CheckBox(this).apply {
        text = title
        isChecked = checked
        setTextColor(Color.WHITE)
        setOnCheckedChangeListener { _, value -> onChanged(value) }
    }
}
