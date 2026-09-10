package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.animame.editor.AnimationDocument
import com.animame.editor.BrushPresets

class MainActivity : Activity() {
    private lateinit var editor: EditorSurface
    private lateinit var toolbar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::editor.isInitialized) {
            editor.refreshTheme()
            editor.invalidate()
        }
    }

    private fun buildUi() {
        val root = FrameLayout(this)
        editor = EditorSurface()
        root.addView(editor, FrameLayout.LayoutParams(-1, -1))

        toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.rgb(30, 34, 39))
        }
        val settings = Button(this).apply {
            text = "⚙"
            textSize = 20f
            setOnClickListener { startActivityForResult(Intent(this@MainActivity, SettingsActivity::class.java), 20) }
        }
        val brush = Button(this).apply {
            text = "Pincel"
            setOnClickListener { editor.tool = Tool.BRUSH; editor.invalidate() }
        }
        val eraser = Button(this).apply {
            text = "Borracha"
            setOnClickListener { editor.tool = Tool.ERASER; editor.invalidate() }
        }
        val clear = Button(this).apply {
            text = "Limpar"
            setOnClickListener { editor.clearDrawing(); editor.invalidate() }
        }
        toolbar.addView(settings, LinearLayout.LayoutParams(58, 58))
        toolbar.addView(brush, LinearLayout.LayoutParams(110, 58))
        toolbar.addView(eraser, LinearLayout.LayoutParams(120, 58))
        toolbar.addView(clear, LinearLayout.LayoutParams(100, 58))
        root.addView(toolbar, FrameLayout.LayoutParams(-1, 74))
        setContentView(root)
    }

    private enum class Tool { BRUSH, ERASER }

    private inner class EditorSurface : View(this@MainActivity) {
        private val document = AnimationDocument()
        private val strokes = mutableListOf<Path>()
        private var currentPath: Path? = null
        private var accent = ThemeColorStore.DEFAULT
        var tool = Tool.BRUSH

        private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        private val panel = Paint(Paint.ANTI_ALIAS_FLAG)
        private val paper = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = 10f
        }
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 25f }
        private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 16f }

        init { refreshTheme(); isFocusable = true }

        fun refreshTheme() {
            accent = ThemeColorStore.get(this@MainActivity)
            bg.color = Color.rgb(18, 20, 23)
            panel.color = blend(accent, Color.rgb(30, 34, 39), 0.82f)
            paper.color = Color.rgb(245, 245, 245)
            ink.color = accent
        }

        fun clearDrawing() { strokes.clear(); currentPath = null }

        override fun onDraw(c: Canvas) {
            c.drawColor(bg.color)
            val top = 74f
            val bottom = height * 0.72f
            val left = width * 0.09f
            val right = width * 0.91f
            c.drawRect(0f, 0f, width.toFloat(), top, panel)
            c.drawRect(0f, bottom, width.toFloat(), height.toFloat(), panel)
            c.drawRect(left, top + 18f, right, bottom - 18f, paper)
            strokes.forEach { c.drawPath(it, ink) }
            currentPath?.let { c.drawPath(it, ink) }
            c.drawText("ANIMA-ME", 86f, 43f, text)
            c.drawText("Frame ${document.currentFrame + 1}  •  ${document.fps} FPS", 86f, 66f, sub)
            c.drawText("${BrushPresets.all.size} pincéis", width - 190f, 42f, sub)
            c.drawText("Timeline  •  Layer 1", 28f, bottom + 38f, text)
            c.drawText("Pincel • Borracha • Lasso • Balde • Onion Skin • QR", 28f, height - 22f, sub)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    currentPath = Path().apply { moveTo(event.x, event.y) }
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    currentPath?.lineTo(event.x, event.y)
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    currentPath?.let { strokes.add(it) }
                    currentPath = null
                    invalidate()
                    return true
                }
            }
            return true
        }

        private fun blend(a: Int, b: Int, amount: Float): Int {
            val t = amount.coerceIn(0f, 1f)
            val r = (Color.red(a) * (1f - t) + Color.red(b) * t).toInt()
            val g = (Color.green(a) * (1f - t) + Color.green(b) * t).toInt()
            val bl = (Color.blue(a) * (1f - t) + Color.blue(b) * t).toInt()
            return Color.rgb(r, g, bl)
        }
    }
}
