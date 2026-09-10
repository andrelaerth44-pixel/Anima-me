package com.animame

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.animame.editor.StrokeCorrectionStore

class SettingsActivity : Activity() {
    private val colors = intArrayOf(
        Color.rgb(38, 166, 154), Color.rgb(63, 81, 181), Color.rgb(103, 58, 183),
        Color.rgb(233, 30, 99), Color.rgb(244, 67, 54), Color.rgb(255, 152, 0),
        Color.rgb(76, 175, 80), Color.rgb(0, 150, 136), Color.rgb(3, 169, 244),
        Color.rgb(255, 193, 7), Color.rgb(121, 85, 72), Color.rgb(96, 125, 139)
    )

    private val prefs by lazy { getSharedPreferences("stroke_correction", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadCorrectionPrefs()
        val accent = ThemeColorStore.get(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 28, 40, 28)
            setBackgroundColor(Color.rgb(18, 20, 23))
        }
        root.addView(TextView(this).apply { text = "Definições do Anima-me"; textSize = 26f; setTextColor(Color.WHITE) })
        root.addView(TextView(this).apply { text = "Estabilização e suavização"; textSize = 19f; setTextColor(Color.LTGRAY); setPadding(0, 22, 0, 10) })

        val status = TextView(this).apply { setTextColor(Color.WHITE); textSize = 15f; setPadding(0, 6, 0, 12) }
        fun refreshStatus() {
            status.text = "Constante: ${StrokeCorrectionStore.constant.toInt()}  •  Rápidos: ${StrokeCorrectionStore.fastStrokes.toInt()}  •  Suavizar: ${StrokeCorrectionStore.smoothing.toInt()}  •  Predição: ${StrokeCorrectionStore.prediction.toInt()}"
        }
        fun action(label: String, onClick: () -> Unit) = Button(this).apply { text = label; setOnClickListener { onClick(); saveCorrectionPrefs(); refreshStatus() } }

        root.addView(status)
        root.addView(action("Estabilizador — Constante", { StrokeCorrectionStore.constant = cycle(StrokeCorrectionStore.constant, 0f, 20f, 40f, 60f, 80f) }))
        root.addView(action("Estabilizador — Fast Strokes", { StrokeCorrectionStore.fastStrokes = cycle(StrokeCorrectionStore.fastStrokes, 0f, 20f, 40f, 60f, 80f) }))
        root.addView(action("Suavizar / After", { StrokeCorrectionStore.smoothing = cycle(StrokeCorrectionStore.smoothing, 0f, 10f, 25f, 40f, 60f, 80f) }))
        root.addView(action("Previsão do traço", { StrokeCorrectionStore.prediction = cycle(StrokeCorrectionStore.prediction, 0f, 10f, 20f, 35f, 50f) }))
        root.addView(action("Forçar Fade: ${if (StrokeCorrectionStore.forceFade) "ON" else "OFF"}", { StrokeCorrectionStore.forceFade = !StrokeCorrectionStore.forceFade }))
        root.addView(action("Estabilização Legacy: ${if (StrokeCorrectionStore.useLegacy) "ON" else "OFF"}", { StrokeCorrectionStore.useLegacy = !StrokeCorrectionStore.useLegacy }))

        root.addView(TextView(this).apply { text = "Cor da interface"; textSize = 18f; setTextColor(Color.LTGRAY); setPadding(0, 20, 0, 10) })
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        colors.toList().chunked(4).forEach { rowColors ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER }
            rowColors.forEach { color ->
                val button = Button(this).apply {
                    text = "●"; textSize = 28f; setTextColor(color); setBackgroundColor(Color.rgb(42, 46, 52))
                    setOnClickListener { ThemeColorStore.set(this@SettingsActivity, color); setResult(RESULT_OK); finish() }
                }
                row.addView(button, LinearLayout.LayoutParams(0, 70).apply { weight = 1f; setMargins(6, 6, 6, 6) })
            }
            grid.addView(row)
        }
        root.addView(grid)
        root.addView(TextView(this).apply { text = "Cor atual"; textSize = 16f; setTextColor(accent); setPadding(0, 16, 0, 0) })
        setContentView(root)
        refreshStatus()
    }

    private fun cycle(value: Float, vararg options: Float): Float {
        val i = options.indexOfFirst { kotlin.math.abs(it - value) < .01f }
        return options[(i + 1).coerceAtLeast(1) % options.size]
    }

    private fun loadCorrectionPrefs() {
        StrokeCorrectionStore.constant = prefs.getFloat("constant", 0f)
        StrokeCorrectionStore.fastStrokes = prefs.getFloat("fast", 0f)
        StrokeCorrectionStore.smoothing = prefs.getFloat("smooth", 18f)
        StrokeCorrectionStore.prediction = prefs.getFloat("prediction", 0f)
        StrokeCorrectionStore.forceFade = prefs.getBoolean("forceFade", false)
        StrokeCorrectionStore.useLegacy = prefs.getBoolean("legacy", false)
    }

    private fun saveCorrectionPrefs() = prefs.edit()
        .putFloat("constant", StrokeCorrectionStore.constant)
        .putFloat("fast", StrokeCorrectionStore.fastStrokes)
        .putFloat("smooth", StrokeCorrectionStore.smoothing)
        .putFloat("prediction", StrokeCorrectionStore.prediction)
        .putBoolean("forceFade", StrokeCorrectionStore.forceFade)
        .putBoolean("legacy", StrokeCorrectionStore.useLegacy)
        .apply()
}
