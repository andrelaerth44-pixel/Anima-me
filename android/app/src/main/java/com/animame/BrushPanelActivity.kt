package com.animame

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.animame.editor.BrushCatalog

class BrushPanelActivity : Activity() {
    private val presets = BrushCatalog.all()
    private lateinit var list: LinearLayout
    private lateinit var title: TextView
    private var selectedId = "basic"
    private var selectedName = "Basic"
    private var size = 12f
    private var opacity = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        selectedId = intent.getStringExtra("brush_id") ?: "basic"
        size = intent.getFloatExtra("brush_size", 12f)
        opacity = intent.getFloatExtra("brush_opacity", 1f)
        selectedName = presets.firstOrNull { it.id == selectedId }?.name ?: "Basic"
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(18, 20, 23))
            setPadding(12, 12, 12, 12)
        }

        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(28, 32, 37))
            setPadding(10, 10, 10, 10)
        }
        title = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            text = "Pincéis"
            setPadding(4, 4, 4, 12)
        }
        left.addView(title)

        val search = EditText(this).apply {
            hint = "Pesquisar pincel"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.GRAY)
        }
        left.addView(search, LinearLayout.LayoutParams(-1, 48))

        val categories = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        BrushCatalog.categories().take(8).forEach { category ->
            categories.addView(button(category, 92) { renderList(category, search.text.toString()) })
        }
        left.addView(categories, LinearLayout.LayoutParams(-1, 54))

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scroll = android.widget.ScrollView(this).apply { addView(list) }
        left.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        search.setOnEditorActionListener { _, _, _ -> renderList(null, search.text.toString()); false }
        search.addTextChangedListener(SimpleTextWatcher { renderList(null, search.text.toString()) })

        root.addView(left, LinearLayout.LayoutParams(0, -1, 1.35f))

        val right = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(24, 27, 31))
            setPadding(16, 12, 16, 12)
        }
        val selected = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 20f
            text = selectedName
        }
        right.addView(selected, LinearLayout.LayoutParams(-1, 48))

        addParameter(right, "Tamanho", 1, 4096, size.toInt()) { value -> size = value.toFloat(); selected.text = "$selectedName  •  ${value}px" }
        addParameter(right, "Opacidade", 0, 100, (opacity * 100).toInt()) { value -> opacity = value / 100f }

        val info = TextView(this).apply {
            setTextColor(Color.LTGRAY)
            textSize = 13f
            text = "Os parâmetros do pincel são aplicados pelo motor do Anima-me."
            setPadding(0, 16, 0, 16)
        }
        right.addView(info)

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(button("Cancelar", 110) { finish() })
        actions.addView(button("Aplicar", 110) {
            intent.putExtra("brush_id", selectedId)
            intent.putExtra("brush_name", selectedName)
            intent.putExtra("brush_size", size)
            intent.putExtra("brush_opacity", opacity)
            setResult(Activity.RESULT_OK, intent)
            finish()
        })
        right.addView(actions)
        root.addView(right, LinearLayout.LayoutParams(0, -1, .65f))

        setContentView(root)
        renderList(null, "")
    }

    private fun renderList(category: String?, query: String) {
        list.removeAllViews()
        val q = query.trim().lowercase()
        presets.filter { p ->
            (category == null || p.category == category) && (q.isEmpty() || p.name.lowercase().contains(q))
        }.take(120).forEach { preset ->
            val b = button(preset.name, -1) {
                selectedId = preset.id
                selectedName = preset.name
                title.text = "Pincéis  •  ${preset.category}"
            }
            b.gravity = Gravity.START or Gravity.CENTER_VERTICAL
            b.setTextColor(Color.WHITE)
            b.setBackgroundColor(if (preset.id == selectedId) Color.rgb(55, 64, 74) else Color.rgb(38, 42, 48))
            list.addView(b, LinearLayout.LayoutParams(-1, 44).apply { setMargins(0, 2, 0, 2) })
        }
    }

    private fun addParameter(parent: LinearLayout, label: String, min: Int, max: Int, initial: Int, onChange: (Int) -> Unit) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, 10, 0, 10) }
        val text = TextView(this).apply { setTextColor(Color.WHITE); this.text = "$label: $initial" }
        row.addView(text)
        val seek = SeekBar(this).apply {
            max = max - min
            progress = (initial - min).coerceIn(0, max)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) { val v = p + min; text.text = "$label: $v"; onChange(v) }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        row.addView(seek)
        parent.addView(row)
    }

    private fun button(label: String, width: Int, action: () -> Unit): Button = Button(this).apply {
        text = label
        textSize = 11f
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(width, 48)
    }

    private class SimpleTextWatcher(private val action: () -> Unit) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { action() }
        override fun afterTextChanged(s: android.text.Editable?) {}
    }
}
