package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.animame.editor.BrushCatalog

class BrushPickerActivity : Activity() {
    private lateinit var grid: GridLayout
    private lateinit var search: EditText
    private lateinit var sizeLabel: TextView
    private lateinit var opacityLabel: TextView
    private lateinit var spacingLabel: TextView
    private lateinit var smoothingLabel: TextView
    private var selectedCategory = "Todos"
    private val all = BrushCatalog.all()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        BrushToolState.load(this)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(18, 20, 23))
            setPadding(14, 12, 14, 12)
        }

        val browser = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val title = TextView(this).apply {
            text = "Pincéis"
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(4, 0, 4, 8)
        }
        browser.addView(title, LinearLayout.LayoutParams(-1, 46))

        search = EditText(this).apply {
            hint = "Pesquisar pincel"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setBackgroundColor(Color.rgb(38, 42, 48))
            setPadding(14, 0, 14, 0)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refreshGrid() }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        browser.addView(search, LinearLayout.LayoutParams(-1, 46))

        val categories = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 8, 0, 8)
        }
        (listOf("Todos") + BrushCatalog.categories()).forEach { category ->
            categories.addView(Button(this).apply {
                text = category
                textSize = 10f
                setTextColor(Color.WHITE)
                setOnClickListener { selectedCategory = category; refreshGrid() }
                layoutParams = LinearLayout.LayoutParams(110, 42).apply { setMargins(3, 0, 3, 0) }
            })
        }
        browser.addView(android.widget.HorizontalScrollView(this).apply { addView(categories) }, LinearLayout.LayoutParams(-1, 56))

        grid = GridLayout(this).apply { columnCount = 5; useDefaultMargins = false }
        browser.addView(ScrollView(this).apply { addView(grid) }, LinearLayout.LayoutParams(0, 1, 1f))
        root.addView(browser, LinearLayout.LayoutParams(0, 1, 1f))

        val options = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 8, 4, 8)
            setBackgroundColor(Color.rgb(27, 30, 35))
        }
        options.addView(TextView(this).apply {
            text = "Opções do pincel"
            textSize = 18f
            setTextColor(Color.WHITE)
        }, LinearLayout.LayoutParams(-1, 40))

        sizeLabel = optionLabel()
        opacityLabel = optionLabel()
        spacingLabel = optionLabel()
        smoothingLabel = optionLabel()
        options.addView(sizeLabel)
        options.addView(seek("Tamanho", 1, 4096, BrushToolState.size.toInt()) { BrushToolState.size = it.toFloat(); updateLabels() })
        options.addView(opacityLabel)
        options.addView(seek("Opacidade", 0, 100, (BrushToolState.opacity * 100).toInt()) { BrushToolState.opacity = it / 100f; updateLabels() })
        options.addView(spacingLabel)
        options.addView(seek("Espaçamento", 1, 400, (BrushToolState.spacing * 100).toInt()) { BrushToolState.spacing = it / 100f; updateLabels() })
        options.addView(smoothingLabel)
        options.addView(seek("Suavização", 0, 100, (BrushToolState.smoothing * 100).toInt()) { BrushToolState.smoothing = it / 100f; updateLabels() })
        options.addView(check("Pressão do stylus", BrushToolState.pressure) { BrushToolState.pressure = it })
        options.addView(check("Rotação aleatória", BrushToolState.randomRotation) { BrushToolState.randomRotation = it })
        options.addView(check("Desenhar à frente", BrushToolState.drawsInFront) { BrushToolState.drawsInFront = it; if (it) BrushToolState.drawsInside = false })
        options.addView(check("Desenhar dentro", BrushToolState.drawsInside) { BrushToolState.drawsInside = it; if (it) BrushToolState.drawsInFront = false })
        options.addView(TextView(this).apply {
            text = "Os controles seguem a organização do painel de opções do RoughAnimator, mas a implementação e o motor do Anima-me são próprios."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 12)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        options.addView(Button(this).apply { text = "Fechar"; setOnClickListener { BrushToolState.save(this@BrushPickerActivity); finish() } })
        root.addView(options, LinearLayout.LayoutParams(330, -1))

        setContentView(root)
        updateLabels()
        refreshGrid()
    }

    private fun optionLabel() = TextView(this).apply { setTextColor(Color.WHITE); textSize = 13f; setPadding(0, 4, 0, 0) }

    private fun seek(name: String, min: Int, max: Int, initial: Int, onChange: (Int) -> Unit): SeekBar {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(TextView(this).apply { text = name; setTextColor(Color.LTGRAY); textSize = 11f })
        return SeekBar(this).also { bar ->
            bar.max = max - min
            bar.progress = (initial - min).coerceIn(0, max - min)
            bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { onChange(progress + min) }
                override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
            })
        }
    }

    private fun check(label: String, checked: Boolean, onChange: (Boolean) -> Unit): android.widget.CheckBox = android.widget.CheckBox(this).apply {
        text = label
        textSize = 12f
        setTextColor(Color.WHITE)
        isChecked = checked
        setOnCheckedChangeListener { _, value -> onChange(value) }
    }

    private fun updateLabels() {
        sizeLabel.text = "Tamanho: ${BrushToolState.size.toInt()} px"
        opacityLabel.text = "Opacidade: ${(BrushToolState.opacity * 100).toInt()}%"
        spacingLabel.text = "Espaçamento: ${"%.2f".format(BrushToolState.spacing)}"
        smoothingLabel.text = "Suavização: ${(BrushToolState.smoothing * 100).toInt()}%"
    }

    private fun refreshGrid() {
        grid.removeAllViews()
        val q = search.text?.toString()?.trim()?.lowercase().orEmpty()
        all.filter { p -> (selectedCategory == "Todos" || p.category == selectedCategory) && (q.isEmpty() || p.name.lowercase().contains(q)) }
            .forEach { preset ->
                val cell = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(7, 6, 7, 6)
                    setBackgroundColor(if (preset.id == BrushToolState.brushId) Color.rgb(45, 105, 104) else Color.rgb(38, 42, 48))
                    setOnClickListener {
                        BrushToolState.brushId = preset.id
                        BrushToolState.save(this@BrushPickerActivity)
                        setResult(RESULT_OK, Intent().apply {
                            putExtra("brush_id", preset.id)
                            putExtra("brush_name", preset.name)
                            putExtra("brush_category", preset.category)
                        })
                        refreshGrid()
                    }
                }
                cell.addView(TextView(this).apply { text = preset.category; textSize = 8f; setTextColor(Color.LTGRAY) }, LinearLayout.LayoutParams(-1, 18))
                cell.addView(TextView(this).apply { text = preset.name; textSize = 10f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); maxLines = 2 }, LinearLayout.LayoutParams(-1, 42))
                grid.addView(cell, GridLayout.LayoutParams().apply { width = 0; height = 64; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(4, 4, 4, 4) })
            }
    }
}
