package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class SettingsActivity : Activity() {
    private val colors = intArrayOf(
        Color.rgb(38, 166, 154), Color.rgb(63, 81, 181), Color.rgb(103, 58, 183),
        Color.rgb(233, 30, 99), Color.rgb(244, 67, 54), Color.rgb(255, 152, 0),
        Color.rgb(76, 175, 80), Color.rgb(0, 150, 136), Color.rgb(3, 169, 244),
        Color.rgb(255, 193, 7), Color.rgb(121, 85, 72), Color.rgb(96, 125, 139)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        val accent = ThemeColorStore.get(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 28, 40, 28)
            setBackgroundColor(Color.rgb(18, 20, 23))
        }
        root.addView(TextView(this).apply { text = "Definições do Anima-me"; textSize = 26f; setTextColor(Color.WHITE) })
        root.addView(TextView(this).apply {
            text = "A organização mantém as definições gerais separadas das opções rápidas do editor."
            textSize = 13f; setTextColor(Color.LTGRAY); setPadding(0, 4, 0, 8)
        })
        root.addView(Button(this).apply {
            text = "Opções do pincel"
            setOnClickListener { startActivity(Intent(this@SettingsActivity, ToolOptionsActivity::class.java)) }
        }, LinearLayout.LayoutParams(-1, 52).apply { setMargins(0, 6, 0, 8) })
        root.addView(TextView(this).apply {
            text = "Cor da interface"; textSize = 18f; setTextColor(Color.LTGRAY); setPadding(0, 14, 0, 12)
        })
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        colors.toList().chunked(4).forEach { rowColors ->
            val row = LinearLayout(this).apply { gravity = Gravity.CENTER }
            rowColors.forEach { color ->
                val button = Button(this).apply {
                    text = ""
                    setBackgroundColor(color)
                    setOnClickListener {
                        ThemeColorStore.set(this@SettingsActivity, color)
                        setResult(RESULT_OK)
                        finish()
                    }
                }
                row.addView(button, LinearLayout.LayoutParams(0, 68).apply { weight = 1f; setMargins(6, 6, 6, 6) })
            }
            grid.addView(row)
        }
        root.addView(grid)
        root.addView(TextView(this).apply {
            text = "Cor atual"; textSize = 16f; setTextColor(accent); setPadding(0, 18, 0, 0)
        })
        setContentView(root)
    }
}
