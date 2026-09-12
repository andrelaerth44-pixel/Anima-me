package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
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
import android.widget.Toast
import com.animame.editor.BrushCatalog
import com.animame.editor.BrushEngine
import com.animame.editor.BrushLibraryBridge
import com.animame.editor.BrushLibraryV2
import com.animame.editor.BrushQrCodec
import com.animame.editor.BrushQrImageDecoder
import com.animame.editor.BrushSettings
import com.animame.editor.StrokeSample

class BrushPickerActivity : Activity() {
    private lateinit var grid: GridLayout
    private lateinit var search: EditText
    private lateinit var sizeLabel: TextView
    private lateinit var opacityLabel: TextView
    private lateinit var spacingLabel: TextView
    private lateinit var smoothingLabel: TextView
    private lateinit var sourceLabel: TextView
    private var selectedCategory = "Todos"
    private var selectedSource = Source.ALL
    private val base = BrushCatalog.all()
    private val procedural = BrushLibraryBridge.presets()
    private val accent by lazy { ThemeColorStore.get(this) }
    private val pageBackground = Color.rgb(7, 26, 43)
    private val panelBackground = Color.rgb(10, 31, 49)
    private val fieldBackground = Color.rgb(17, 39, 57)
    private val cellBackground = Color.rgb(15, 36, 54)
    private val qrRequest = 4101

    private enum class Source { ALL, BASE, PROCEDURAL }
    private data class Entry(val id: String, val name: String, val category: String, val procedural: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        BrushToolState.load(this)
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(pageBackground)
            setPadding(14, 12, 14, 12)
        }
        val browser = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        browser.addView(TextView(this).apply {
            text = "Pincéis"
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(4, 0, 4, 8)
        }, LinearLayout.LayoutParams(-1, 46))

        search = EditText(this).apply {
            hint = "Pesquisar pincel"
            setSingleLine(true)
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setBackgroundColor(fieldBackground)
            setPadding(14, 0, 14, 0)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refreshGrid() }
                override fun afterTextChanged(s: android.text.Editable?) = Unit
            })
        }
        browser.addView(search, LinearLayout.LayoutParams(-1, 46))

        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this).apply {
            text = "Importar pincel por QR"
            setOnClickListener { openQrImagePicker() }
        }, LinearLayout.LayoutParams(0, 44, 1f).apply { setMargins(0, 6, 4, 2) })
        actions.addView(Button(this).apply {
            text = "Fechar"
            setOnClickListener { BrushToolState.save(this@BrushPickerActivity); setResult(RESULT_OK); finish() }
        }, LinearLayout.LayoutParams(0, 44, .35f).apply { setMargins(4, 6, 0, 2) })
        browser.addView(actions)

        sourceLabel = TextView(this).apply { setTextColor(Color.WHITE); textSize = 12f; setPadding(4, 6, 4, 2) }
        browser.addView(sourceLabel, LinearLayout.LayoutParams(-1, 30))
        val sources = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Todos" to Source.ALL, "Base" to Source.BASE, "Procedurais" to Source.PROCEDURAL).forEach { (label, source) ->
            sources.addView(Button(this).apply {
                text = label
                textSize = 10f
                setTextColor(Color.WHITE)
                setOnClickListener { selectedSource = source; selectedCategory = "Todos"; refreshGrid() }
                layoutParams = LinearLayout.LayoutParams(0, 40, 1f).apply { setMargins(2, 0, 2, 0) }
            })
        }
        browser.addView(sources)

        val categories = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 5, 0, 5) }
        val categoryList = listOf("Todos") + BrushCatalog.categories() + BrushLibraryV2.categories()
        categoryList.distinct().forEach { category ->
            categories.addView(Button(this).apply {
                text = category
                textSize = 9f
                setTextColor(Color.WHITE)
                setOnClickListener { selectedCategory = category; refreshGrid() }
                layoutParams = LinearLayout.LayoutParams(92, 38).apply { setMargins(2, 0, 2, 0) }
            })
        }
        browser.addView(HorizontalScrollView(this).apply { addView(categories) }, LinearLayout.LayoutParams(-1, 50))

        grid = GridLayout(this).apply { columnCount = 5; useDefaultMargins = false }
        browser.addView(ScrollView(this).apply { addView(grid) }, LinearLayout.LayoutParams(0, 1, 1f))
        root.addView(browser, LinearLayout.LayoutParams(0, 1, 1f))

        val options = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 8, 4, 8)
            setBackgroundColor(panelBackground)
        }
        options.addView(TextView(this).apply { text = "Opções do pincel"; textSize = 18f; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(-1, 40))
        sizeLabel = optionLabel(); opacityLabel = optionLabel(); spacingLabel = optionLabel(); smoothingLabel = optionLabel()
        options.addView(sizeLabel)
        options.addView(seek("Tamanho", 1, 4096, BrushToolState.size.toInt()) { BrushToolState.size = it.toFloat(); BrushToolState.save(this); updateLabels(); refreshGrid() })
        options.addView(opacityLabel)
        options.addView(seek("Opacidade", 0, 100, (BrushToolState.opacity * 100).toInt()) { BrushToolState.opacity = it / 100f; BrushToolState.save(this); updateLabels(); refreshGrid() })
        options.addView(spacingLabel)
        options.addView(seek("Espaçamento", 1, 400, (BrushToolState.spacing * 100).toInt()) { BrushToolState.spacing = it / 100f; BrushToolState.save(this); updateLabels() })
        options.addView(smoothingLabel)
        options.addView(seek("Suavização", 0, 100, (BrushToolState.smoothing * 100).toInt()) { BrushToolState.smoothing = it / 100f; BrushToolState.save(this); updateLabels() })
        options.addView(check("Pressão do stylus", BrushToolState.pressure) { BrushToolState.pressure = it; BrushToolState.save(this) })
        options.addView(check("Rotação aleatória", BrushToolState.randomRotation) { BrushToolState.randomRotation = it; BrushToolState.save(this) })
        options.addView(check("Desenhar à frente", BrushToolState.drawsInFront) { BrushToolState.drawsInFront = it; if (it) BrushToolState.drawsInside = false; BrushToolState.save(this) })
        options.addView(check("Desenhar dentro", BrushToolState.drawsInside) { BrushToolState.drawsInside = it; if (it) BrushToolState.drawsInFront = false; BrushToolState.save(this) })
        options.addView(TextView(this).apply {
            text = "Biblioteca do Anima-me: ${base.size} presets base + ${procedural.size} presets procedurais, todos ligados ao modelo real de BrushSettings."
            textSize = 12f
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 12)
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(options, LinearLayout.LayoutParams(292, -1))
        setContentView(root)
        updateLabels()
        refreshGrid()
    }

    private fun openQrImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, qrRequest)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != qrRequest || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        try {
            contentResolver.openInputStream(uri).use { input ->
                val bitmap = BitmapFactory.decodeStream(input) ?: error("Não foi possível abrir a imagem")
                val decoded = BrushQrImageDecoder.decode(bitmap)
                val imported = BrushQrCodec.decode(decoded.raw)
                val message = buildString {
                    append("QR reconhecido.\n\n")
                    append("Formato: ${decoded.format}\n")
                    append("Texto detectado: ${decoded.text ?: "binário"}\n")
                    append("Bytes brutos: ${decoded.raw.size}\n")
                    append("Magic: ${imported.magic ?: "não identificado"}\n")
                    append("Versão: ${imported.version ?: "não identificada"}\n")
                    append("Registros: ${imported.records.size}\n")
                    append("Payload descomprimido: ${imported.payload?.size ?: 0} bytes")
                }
                android.app.AlertDialog.Builder(this)
                    .setTitle("Teste de QR do pincel")
                    .setMessage(message)
                    .setPositiveButton("OK", null)
                    .show()
            }
        } catch (t: Throwable) {
            Toast.makeText(this, "Falha ao ler QR: ${t.message ?: "payload inválido"}", Toast.LENGTH_LONG).show()
        }
    }

    private fun entries(): List<Entry> {
        val q = search.text?.toString()?.trim()?.lowercase().orEmpty()
        return when (selectedSource) {
            Source.BASE -> base.map { Entry(it.id, it.name, it.category, false) }
            Source.PROCEDURAL -> procedural.map { Entry(it.id, it.name, it.category, true) }
            Source.ALL -> base.map { Entry(it.id, it.name, it.category, false) } + procedural.map { Entry(it.id, it.name, it.category, true) }
        }.filter { entry ->
            (selectedCategory == "Todos" || entry.category.equals(selectedCategory, true)) &&
                (q.isEmpty() || entry.name.lowercase().contains(q) || entry.category.lowercase().contains(q))
        }
    }

    private fun refreshGrid() {
        grid.removeAllViews()
        val items = entries()
        sourceLabel.text = "${items.size} pincéis visíveis  |  ${base.size} base  |  ${procedural.size} procedurais"
        items.forEach { preset ->
            val selected = preset.id == BrushToolState.brushId
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(5, 5, 5, 5)
                setBackgroundColor(if (selected) accent else cellBackground)
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
            cell.addView(BrushPreviewView(this, preset.id, accent, preset.procedural), LinearLayout.LayoutParams(-1, 46))
            cell.addView(TextView(this).apply { text = preset.name; textSize = 8f; gravity = Gravity.CENTER; setTextColor(Color.WHITE); maxLines = 2 }, LinearLayout.LayoutParams(-1, 34))
            cell.addView(TextView(this).apply { text = if (preset.procedural) "Procedural" else preset.category; textSize = 7f; gravity = Gravity.CENTER; setTextColor(Color.LTGRAY) }, LinearLayout.LayoutParams(-1, 14))
            grid.addView(cell, GridLayout.LayoutParams().apply { width = 0; height = 98; columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); setMargins(3, 3, 3, 3) })
        }
    }

    private fun optionLabel() = TextView(this).apply { setTextColor(Color.WHITE); textSize = 13f; setPadding(0, 4, 0, 0) }
    private fun seek(name: String, min: Int, max: Int, initial: Int, onChange: (Int) -> Unit): SeekBar {
        val bar = SeekBar(this)
        bar.max = max - min
        bar.progress = (initial - min).coerceIn(0, max - min)
        bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) onChange(progress + min) }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        return bar
    }
    private fun check(label: String, checked: Boolean, onChange: (Boolean) -> Unit): CheckBox = CheckBox(this).apply { text = label; textSize = 12f; setTextColor(Color.WHITE); isChecked = checked; setOnCheckedChangeListener { _, value -> onChange(value) } }
    private fun updateLabels() {
        sizeLabel.text = "Tamanho: ${BrushToolState.size.toInt()} px"
        opacityLabel.text = "Opacidade: ${(BrushToolState.opacity * 100).toInt()}%"
        spacingLabel.text = "Espaçamento: ${"%.2f".format(BrushToolState.spacing)}"
        smoothingLabel.text = "Suavização: ${(BrushToolState.smoothing * 100).toInt()}%"
    }

    private class BrushPreviewView(context: android.content.Context, private val brushId: String, private val accent: Int, private val procedural: Boolean) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val samples = listOf(
            StrokeSample(8f, 28f, .25f, 0L, .15f, 0f),
            StrokeSample(26f, 21f, .45f, 16L, .25f, .3f),
            StrokeSample(44f, 28f, .7f, 32L, .4f, .6f),
            StrokeSample(62f, 20f, 1f, 48L, .55f, 1f),
            StrokeSample(82f, 26f, .6f, 64L, .35f, .2f)
        )
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val settings: BrushSettings = if (procedural) {
                val spec = BrushLibraryV2.byId(brushId) ?: return
                BrushLibraryBridge.settings(spec)
            } else {
                BrushCatalog.settings(brushId)
            }.copy(size = 8f, opacity = .95f)
            val stamps = BrushEngine.stamps(samples, settings, brushId.hashCode().toLong())
            paint.color = accent
            stamps.forEach { stamp ->
                paint.alpha = (stamp.alpha * 255f).toInt().coerceIn(20, 255)
                canvas.drawCircle(stamp.x, stamp.y, stamp.size * .5f, paint)
            }
        }
    }
}
