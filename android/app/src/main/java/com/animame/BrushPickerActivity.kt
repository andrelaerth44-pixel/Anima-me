package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import com.animame.editor.BrushCatalog
import com.animame.editor.BrushEngine
import com.animame.editor.StrokeSample

class BrushPickerActivity : Activity() {
    private lateinit var grid: GridLayout
    private lateinit var search: EditText
    private lateinit var sizeLabel: TextView
    private lateinit var opacityLabel: TextView
    private lateinit var spacingLabel: TextView
    private lateinit var smoothingLabel: TextView
    private var selectedCategory = "Todos"
    private val all = BrushCatalog.all()
    private val accent by lazy { ThemeColorStore.get(this) }
    private val pageBackground = Color.rgb(7, 26, 43)
    private val panelBackground = Color.rgb(10, 31, 49)
    private val fieldBackground = Color.rgb(17, 39, 57)
    private val cellBackground = Color.rgb(15, 36, 54)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        BrushToolState.load(this)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(pageBackground); setPadding(14, 12, 14, 12) }
        val browser = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        browser.addView(TextView(this).apply { text = "Pincéis"; textSize = 22f; setTextColor(Color.WHITE); setPadding(4, 0, 4, 8) }, LinearLayout.LayoutParams(-1, 46))
        search = EditText(this).apply {
            hint = "Pesquisar pincel"; setSingleLine(true); setTextColor(Color.WHITE); setHintTextColor(Color.LTGRAY); setBackgroundColor(fieldBackground); setPadding(14, 0, 14, 0)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refreshGrid() }
                override fun afterTextChanged(s: Editable?) = Unit
            })
        }
        browser.addView(search, LinearLayout.LayoutParams(-1, 46))
        val categories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 8, 0, 8) }
        (listOf("Todos") + BrushCatalog.categories()).forEach { category -> categories.addView(Button(this).apply { text = category; textSize = 10f; setTextColor(Color.WHITE); setOnClickListener { selectedCategory = category; refreshGrid() }; layoutParams = LinearLayout.LayoutParams(110, 42).apply { setMargins(3, 0, 3, 0) } }) }
        browser.addView(HorizontalScrollView(this).apply { addView(categories) }, LinearLayout.LayoutParams(-1, 56))
        grid = GridLayout(this).apply { columnCount = 4; useDefaultMargins = false }
        browser.addView(ScrollView(this).apply { addView(grid) }, LinearLayout.LayoutParams(0, 1, 1f))
        root.addView(browser, LinearLayout.LayoutParams(0, 1, 1f))

        val options = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 8, 4, 8); setBackgroundColor(panelBackground) }
        options.addView(TextView(this).apply { text = "Opções do pincel"; textSize = 18f; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(-1, 40))
        sizeLabel = optionLabel(); opacityLabel = optionLabel(); spacingLabel = optionLabel(); smoothingLabel = optionLabel()
        options.addView(sizeLabel)
        options.addView(seek("Tamanho", 1, 4096, BrushToolState.size.toInt()) { BrushToolState.size = it.toFloat(); updateLabels(); refreshGrid() })
        options.addView(opacityLabel)
        options.addView(seek("Opacidade", 0, 100, (BrushToolState.opacity * 100).toInt()) { BrushToolState.opacity = it / 100f; updateLabels(); refreshGrid() })
        options.addView(spacingLabel)
        options.addView(seek("Espaçamento", 1, 400, (BrushToolState.spacing * 100).toInt()) { BrushToolState.spacing = it / 100f; updateLabels() })
        options.addView(smoothingLabel)
        options.addView(seek("Suavização", 0, 100, (BrushToolState.smoothing * 100).toInt()) { BrushToolState.smoothing = it / 100f; updateLabels() })
        options.addView(check("Pressão do stylus", BrushToolState.pressure) { BrushToolState.pressure = it })
        options.addView(check("Rotação aleatória", BrushToolState.randomRotation) { BrushToolState.randomRotation = it })
        options.addView(check("Desenhar à frente", BrushToolState.drawsInFront) { BrushToolState.drawsInFront = it; if (it) BrushToolState.drawsInside = false })
        options.addView(check("Desenhar dentro", BrushToolState.drawsInside) { BrushToolState.drawsInside = it; if (it) BrushToolState.drawsInFront = false })
        options.addView(TextView(this).apply { text = "Biblioteca própria do Anima-me. Cada preset resolve as suas definições no motor, mantendo o catálogo separado da interface."; textSize = 12f; setTextColor(Color.LTGRAY); setPadding(0, 12, 0, 12) }, LinearLayout.LayoutParams(-1, 0, 1f))
        options.addView(Button(this).apply { text = "Fechar"; setOnClickListener { BrushToolState.save(this@BrushPickerActivity); setResult(RESULT_OK); finish() } })
        root.addView(options, LinearLayout.LayoutParams(330, -1))
        setContentView(root); updateLabels(); refreshGrid()
    }

    private fun optionLabel() = TextView(this).apply { setTextColor(Color.WHITE); textSize = 13f; setPadding(0, 4, 0, 0) }
    private fun seek(name: String, min: Int, max: Int, initial: Int, onChange: (Int) -> Unit): SeekBar {
        val bar = SeekBar(this); bar.max = max - min; bar.progress = (initial - min).coerceIn(0, max - min)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { onChange(progress + min) }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        }); return bar
    }
    private fun check(label: String, checked: Boolean, onChange: (Boolean) -> Unit): CheckBox = CheckBox(this).apply { text = label; textSize = 12f; setTextColor(Color.WHITE); isChecked = checked; setOnCheckedChangeListener { _, value -> onChange(value) } }
    private fun updateLabels() {
        sizeLabel.text = "Tamanho: ${BrushToolState.size.toInt()} px"
        opacityLabel.text = "Opacidade: ${(BrushToolState.opacity * 100).toInt()}%"
        spacingLabel.text = "Espaçamento: ${"%.2f".format(BrushToolState.spacing)}"
        smoothingLabel.text = "Suavização: ${(BrushToolState.smoothing * 100).toInt()}%"
    }
    private fun refreshGrid() {
        grid.removeAllViews(); val q = search.text?.toString()?.trim()?.lowercase().orEmpty()
        all.filter { p -> (selectedCategory == "Todos" || p.category == selectedCategory) && (q.isEmpty() || p.name.lowercase().contains(q)) }.forEach { preset ->
            val selected = preset.id == BrushToolState.brushId
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(5, 5, 5, 5); setBackgroundColor(if (selected) accent else cellBackground)
                setOnClickListener { BrushToolState.brushId = preset.id; BrushToolState.save(this@BrushPickerActivity); setResult(RESULT_OK, Intent().apply { putExtra("brush_id", preset.id); putExtra("brush_name", preset.name); putExtra("brush_category", preset.category) }); refreshGrid() }
            }
            cell.addView(BrushPreviewView(this, preset.id, accent), LinearLayout.LayoutParams(-1, 46))
            cell.addView(TextView(this).apply { text = preset.name; textSize = 9f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); maxLines = 2 }, LinearLayout.LayoutParams(-1, 32))
            cell.addView(TextView(this).apply { text = preset.category; textSize = 7f; gravity = Gravity.CENTER; setTextColor(Color.LTGRAY) }, LinearLayout.LayoutParams(-1, 16))
            grid.addView(cell, GridLayout.LayoutParams().apply { width = 0; height = 98; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(4, 4, 4, 4) })
        }
    }

    private class BrushPreviewView(context: android.content.Context, private val brushId: String, private val accent: Int) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val samples = listOf(StrokeSample(10f, 28f, .35f, 0L), StrokeSample(28f, 22f, .55f, 16L), StrokeSample(46f, 28f, .8f, 32L), StrokeSample(64f, 20f, 1f, 48L), StrokeSample(82f, 26f, .65f, 64L))
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val settings = BrushCatalog.settings(brushId).copy(size = 8f, opacity = .95f)
            val stamps = BrushEngine.stamps(samples, settings, brushId.hashCode().toLong())
            paint.color = accent
            stamps.forEach { stamp -> paint.alpha = (stamp.alpha * 255f).toInt().coerceIn(20, 255); canvas.drawCircle(stamp.x, stamp.y, stamp.size * .5f, paint) }
        }
    }
}
