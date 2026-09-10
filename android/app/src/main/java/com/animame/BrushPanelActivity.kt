package com.animame

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import com.animame.editor.BrushCatalog
import com.animame.editor.BrushPanelState

/** Compact brush browser. The editor owns the selected brush id through the result Intent. */
class BrushPanelActivity : Activity() {
    private val state = BrushPanelState()
    private lateinit var list: LinearLayout
    private lateinit var categoryRow: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        buildUi()
        refresh()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(18, 20, 23))
            setPadding(12, 10, 12, 10)
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        header.addView(TextView(this).apply {
            text = "Pincéis"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(0, 52, 1f))
        header.addView(Button(this).apply { text = "Fechar"; setOnClickListener { finish() } }, LinearLayout.LayoutParams(92, 52))
        root.addView(header)

        val search = EditText(this).apply {
            hint = "Pesquisar pincel"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setSingleLine(true)
            setOnEditorActionListener { _, _, _ -> state.query = text.toString(); refresh(); false }
            addTextChangedListener(SimpleTextWatcher { state.query = it; refresh() })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, 52))

        val categoryScroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
        categoryRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        categoryScroll.addView(categoryRow, ViewGroup.LayoutParams(-2, 52))
        root.addView(categoryScroll)

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val listScroll = android.widget.ScrollView(this).apply { addView(list) }
        root.addView(listScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
    }

    private fun refresh() {
        categoryRow.removeAllViews()
        state.categories().forEach { category ->
            categoryRow.addView(Button(this).apply {
                text = category
                textSize = 10f
                setOnClickListener { state.category = category; refresh() }
            }, LinearLayout.LayoutParams(-2, 48))
        }
        list.removeAllViews()
        state.filtered().take(120).forEach { brush ->
            list.addView(Button(this).apply {
                text = "${brush.name}  ·  ${brush.category}"
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                textSize = 12f
                setOnClickListener {
                    setResult(RESULT_OK, intent.putExtra("brush_id", brush.id))
                    finish()
                }
            }, LinearLayout.LayoutParams(-1, 48))
        }
    }

    private class SimpleTextWatcher(private val callback: (String) -> Unit) : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { callback(s?.toString().orEmpty()) }
        override fun afterTextChanged(s: android.text.Editable?) = Unit
    }
}
