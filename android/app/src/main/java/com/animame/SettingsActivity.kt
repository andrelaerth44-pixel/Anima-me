package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.animame.editor.StrokeCorrectionStore

class SettingsActivity : Activity() {
    private val colors = intArrayOf(
        ThemeColorStore.CYAN, ThemeColorStore.BLUE, 0xFF4B8DFF.toInt(),
        0xFF7C5CFF.toInt(), 0xFFE85C9F.toInt(), 0xFFFF7B54.toInt(),
        0xFF55C878.toInt(), 0xFFFFC857.toInt(), 0xFF7DD3FC.toInt(),
        0xFFB0BEC5.toInt(), 0xFF90A4AE.toInt(), ThemeColorStore.NAVY_800
    )

    private val prefs by lazy { getSharedPreferences("stroke_correction", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadCorrectionPrefs()
        val accent = ThemeColorStore.get(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 28, 40, 28)
            setBackgroundColor(ThemeColorStore.NAVY_950)
        }
        root.addView(TextView(this).apply { text = "Definições do Anima-me"; textSize = 26f; setTextColor(ThemeColorStore.TEXT) })
        root.addView(TextView(this).apply {
            text = "Estabilizador e Suavizar são independentes.\nSuavizar reduz serrilhado/pixelização das bordas; não altera a trajetória do traço."
            textSize = 17f; setTextColor(ThemeColorStore.MUTED); setPadding(0, 18, 0, 12)
        })

        val status = TextView(this).apply { setTextColor(ThemeColorStore.TEXT); textSize = 15f; setPadding(0, 6, 0, 12) }
        fun refreshStatus() { status.text = "Constante: ${StrokeCorrectionStore.constant.toInt()}  •  Rápidos: ${StrokeCorrectionStore.fastStrokes.toInt()}  •  Predição: ${StrokeCorrectionStore.prediction.toInt()}" }
        fun action(label: String, onClick: () -> Unit) = Button(this).apply {
            text = label; setTextColor(ThemeColorStore.TEXT); setBackgroundColor(ThemeColorStore.NAVY_800)
            setOnClickListener { onClick(); saveCorrectionPrefs(); refreshStatus() }
        }

        root.addView(status)
        root.addView(action("Estabilizador — Constante", { StrokeCorrectionStore.constant = cycle(StrokeCorrectionStore.constant, 0f, 20f, 40f, 60f, 80f) }))
        root.addView(action("Estabilizador — Fast Strokes", { StrokeCorrectionStore.fastStrokes = cycle(StrokeCorrectionStore.fastStrokes, 0f, 20f, 40f, 60f, 80f) }))
        root.addView(action("Previsão do traço", { StrokeCorrectionStore.prediction = cycle(StrokeCorrectionStore.prediction, 0f, 10f, 20f, 35f, 50f) }))
        root.addView(action("Forçar Fade: ${if (StrokeCorrectionStore.forceFade) "ON" else "OFF"}", { StrokeCorrectionStore.forceFade = !StrokeCorrectionStore.forceFade }))
        root.addView(action("Estabilização Legacy: ${if (StrokeCorrectionStore.useLegacy) "ON" else "OFF"}", { StrokeCorrectionStore.useLegacy = !StrokeCorrectionStore.useLegacy }))
        root.addView(Button(this).apply {
            text = "Importação / Exportação  •  QR • Imagens • Vídeo • GIF • MP4"
            setTextColor(ThemeColorStore.TEXT); setBackgroundColor(ThemeColorStore.BLUE)
            setOnClickListener { startActivity(Intent(this@SettingsActivity, ImportExportActivity::class.java)) }
        })

        root.addView(TextView(this).apply { text = "Cor da interface"; textSize = 18f; setTextColor(ThemeColorStore.MUTED); setPadding(0, 20, 0, 10) })
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        colors.toList().chunked(4).forEach { rowColors ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER }
            rowColors.forEach { color ->
                val button = Button(this).apply {
                    text = "●"; textSize = 28f; setTextColor(color); setBackgroundColor(ThemeColorStore.NAVY_800)
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
        StrokeCorrectionStore.smoothing = 0f
        StrokeCorrectionStore.prediction = prefs.getFloat("prediction", 0f)
        StrokeCorrectionStore.forceFade = prefs.getBoolean("forceFade", false)
        StrokeCorrectionStore.useLegacy = prefs.getBoolean("legacy", false)
    }

    private fun saveCorrectionPrefs() = prefs.edit()
        .putFloat("constant", StrokeCorrectionStore.constant)
        .putFloat("fast", StrokeCorrectionStore.fastStrokes)
        .putFloat("smooth", 0f)
        .putFloat("prediction", StrokeCorrectionStore.prediction)
        .putBoolean("forceFade", StrokeCorrectionStore.forceFade)
        .putBoolean("legacy", StrokeCorrectionStore.useLegacy)
        .apply()
}
