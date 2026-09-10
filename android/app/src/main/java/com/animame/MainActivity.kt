package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.animame.editor.AnimationDocument
import com.animame.editor.BrushDefaults
import com.animame.editor.BrushEngine
import com.animame.editor.BrushPresets
import com.animame.editor.BrushSettings
import com.animame.editor.StrokeSample
import kotlin.math.hypot

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
            setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) }
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
        private val samples = mutableListOf<StrokeSample>()
        private var renderedStamps = emptyList<BrushEngine.Stamp>()
        private var lastX = 0f
        private var lastY = 0f
        private var lastTime = 0L
        private var drawing = false
        private var accent = ThemeColorStore.DEFAULT
        var tool = Tool.BRUSH

        private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        private val panel = Paint(Paint.ANTI_ALIAS_FLAG)
        private val paper = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 25f }
        private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 16f }

        init {
            refreshTheme()
            isFocusable = true
        }

        fun refreshTheme() {
            accent = ThemeColorStore.get(this@MainActivity)
            bg.color = Color.rgb(18, 20, 23)
            panel.color = blend(accent, Color.rgb(30, 34, 39), 0.82f)
            paper.color = Color.rgb(245, 245, 245)
        }

        fun clearDrawing() {
            samples.clear()
            renderedStamps = emptyList()
            drawing = false
        }

        override fun onDraw(c: Canvas) {
            c.drawColor(bg.color)
            val top = 74f
            val bottom = height * 0.72f
            val left = width * 0.09f
            val right = width * 0.91f
            c.drawRect(0f, 0f, width.toFloat(), top, panel)
            c.drawRect(0f, bottom, width.toFloat(), height.toFloat(), panel)
            c.drawRect(left, top + 18f, right, bottom - 18f, paper)

            for (stamp in renderedStamps) {
                val radius = stamp.size * 0.5f
                stampPaint.color = if (tool == Tool.ERASER) {
                    Color.argb((stamp.alpha * 255f).toInt().coerceIn(1, 255), 245, 245, 245)
                } else {
                    Color.argb((stamp.alpha * 255f).toInt().coerceIn(1, 255), Color.red(accent), Color.green(accent), Color.blue(accent))
                }
                c.drawCircle(stamp.x, stamp.y, radius, stampPaint)
            }

            c.drawText("ANIMA-ME", 86f, 43f, text)
            c.drawText("Frame ${document.currentFrame + 1}  •  ${document.fps} FPS", 86f, 66f, sub)
            c.drawText("${BrushPresets.all.size} pincéis", width - 190f, 42f, sub)
            c.drawText("Timeline  •  Layer 1", 28f, bottom + 38f, text)
            c.drawText("Pincel • Borracha • Lasso • Balde • Onion Skin • QR", 28f, height - 22f, sub)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x
            val y = event.y
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    samples.clear()
                    lastX = x
                    lastY = y
                    lastTime = event.eventTime
                    drawing = true
                    addSample(event)
                    renderStroke()
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!drawing) return true
                    addSample(event)
                    renderStroke()
                    lastX = x
                    lastY = y
                    lastTime = event.eventTime
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    drawing = false
                    renderStroke()
                    invalidate()
                    return true
                }
            }
            return true
        }

        private fun addSample(event: MotionEvent) {
            val pressure = normalizedPressure(event.getPressure())
            samples += StrokeSample(event.x, event.y, pressure, event.eventTime)
        }

        private fun renderStroke() {
            val base = if (tool == Tool.ERASER) BrushDefaults.forPreset("eraser") else BrushDefaults.forPreset("basic")
            val settings: BrushSettings = if (tool == Tool.ERASER) {
                base.copy(size = 34f, opacity = 1f)
            } else {
                base.copy(size = 12f, opacity = 1f, pressureSizeFactor = 1f, pressureOpacityFactor = .85f)
            }
            val smoothed = BrushEngine.smooth(samples, .18f)
            renderedStamps = BrushEngine.stamps(smoothed, settings, document.currentFrame.toLong())
        }

        private fun normalizedPressure(value: Float): Float {
            if (!value.isFinite()) return .5f
            val normalized = if (value > 1f) value / 2f else value
            return normalized.coerceIn(.05f, 1f)
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
