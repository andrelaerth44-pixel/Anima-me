package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.animame.editor.BrushCatalog

class BrushPickerActivity : Activity() {
    private lateinit var grid: GridLayout
    private lateinit var search: EditText
    private var selectedCategory = "Todos"
    private var all = BrushCatalog.all()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(18, 20, 23))
            setPadding(18, 14, 18, 14)
        }

        val title = TextView(this).apply {
            text = "Pincéis"
            textSize = 22f
            setTextColor(Color.WHITE)
            setPadding(4, 0, 4, 10)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, 48))

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
        root.addView(search, LinearLayout.LayoutParams(-1, 48))

        val categories = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 10, 0, 10)
        }
        val categoryNames = listOf("Todos") + BrushCatalog.categories()
        categoryNames.forEach { category ->
            val b = Button(this).apply {
                text = category
                textSize = 10f
                setTextColor(Color.WHITE)
                setOnClickListener { selectedCategory = category; refreshGrid() }
                layoutParams = LinearLayout.LayoutParams(118, 44).apply { setMargins(3, 0, 3, 0) }
            }
            categories.addView(b)
        }
        val catScroll = android.widget.HorizontalScrollView(this).apply { addView(categories) }
        root.addView(catScroll, LinearLayout.LayoutParams(-1, 58))

        grid = GridLayout(this).apply {
            columnCount = 6
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
        }
        val scroll = ScrollView(this).apply { addView(grid) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val close = Button(this).apply {
            text = "Fechar"
            setOnClickListener { finish() }
        }
        root.addView(close, LinearLayout.LayoutParams(-1, 48))

        setContentView(root)
        refreshGrid()
    }

    private fun refreshGrid() {
        grid.removeAllViews()
        val q = search.text?.toString()?.trim()?.lowercase().orEmpty()
        val filtered = all.filter { preset ->
            (selectedCategory == "Todos" || preset.category == selectedCategory) &&
                (q.isEmpty() || preset.name.lowercase().contains(q))
        }
        filtered.forEach { preset ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(8, 6, 8, 6)
                setBackgroundColor(Color.rgb(38, 42, 48))
                setOnClickListener {
                    setResult(RESULT_OK, Intent().apply {
                        putExtra("brush_id", preset.id)
                        putExtra("brush_name", preset.name)
                        putExtra("brush_category", preset.category)
                    })
                    finish()
                }
            }
            val preview = View(this).apply {
                setBackgroundColor(Color.WHITE)
            }
            cell.addView(preview, LinearLayout.LayoutParams(72, 8))
            val name = TextView(this).apply {
                text = preset.name
                textSize = 10f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                maxLines = 2
            }
            cell.addView(name, LinearLayout.LayoutParams(-1, 42))
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = 64
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(4, 4, 4, 4)
            }
            grid.addView(cell, lp)
        }
    }
}
