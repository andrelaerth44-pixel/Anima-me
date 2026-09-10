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
import com.animame.editor.BrushDefaults
import com.animame.editor.BrushPresets
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
        toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(12, 8, 12, 8); setBackgroundColor(Color.rgb(30, 34, 39)) }
        val settings = Button(this).apply { text = "⚙"; textSize = 20f; setOnClickListener { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) } }
        val brush = Button(this).apply { text = "Pincel"; setOnClickListener { editor.tool = Tool.BRUSH; editor.invalidate() } }
        val eraser = Button(this).apply { text = "Borracha"; setOnClickListener { editor.tool = Tool.ERASER; editor.invalidate() } }
        val clear = Button(this).apply { text = "Limpar"; setOnClickListener { editor.clearDrawing(); editor.invalidate() } }
        toolbar.addView(settings, LinearLayout.LayoutParams(58, 58)); toolbar.addView(brush, LinearLayout.LayoutParams(110, 58)); toolbar.addView(eraser, LinearLayout.LayoutParams(120, 58)); toolbar.addView(clear, LinearLayout.LayoutParams(100, 58))
        root.addView(toolbar, FrameLayout.LayoutParams(-1, 74))
        setContentView(root)
    }

    private enum class Tool { BRUSH, ERASER }

    private inner class EditorSurface : View(this@MainActivity) {
        private val document = AnimationDocument()
        private data class Segment(val path: Path, val width: Float, val alpha: Int, val erase: Boolean)
        private val segments = mutableListOf<Segment>()
        private var lastX = 0f
        private var lastY = 0f
        private var lastTime = 0L
        private var drawing = false
        private var accent = ThemeColorStore.DEFAULT
        var tool = Tool.BRUSH
        private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        private val panel = Paint(Paint.ANTI_ALIAS_FLAG)
        private val paper = Paint(Paint.ANTI_ALIAS_FLAG)
        private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 25f }
        private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 16f }
        init { refreshTheme(); isFocusable = true }
        fun refreshTheme() { accent = ThemeColorStore.get(this@MainActivity); bg.color = Color.rgb(18, 20, 23); panel.color = blend(accent, Color.rgb(30, 34, 39), 0.82f); paper.color = Color.rgb(245, 245, 245) }
        fun clearDrawing() { segments.clear(); drawing = false }

        override fun onDraw(c: Canvas) {
            c.drawColor(bg.color)
            val top = 74f; val bottom = height * 0.72f; val left = width * 0.09f; val right = width * 0.91f
            c.drawRect(0f, 0f, width.toFloat(), top, panel); c.drawRect(0f, bottom, width.toFloat(), height.toFloat(), panel); c.drawRect(left, top + 18f, right, bottom - 18f, paper)
            for (segment in segments) { ink.strokeWidth = segment.width; ink.color = if (segment.erase) Color.WHITE else Color.argb(segment.alpha, Color.red(accent), Color.green(accent), Color.blue(accent)); c.drawPath(segment.path, ink) }
            c.drawText("ANIMA-ME", 86f, 43f, text); c.drawText("Frame ${document.currentFrame + 1}  •  ${document.fps} FPS", 86f, 66f, sub); c.drawText("${BrushPresets.all.size} pincéis", width - 190f, 42f, sub); c.drawText("Timeline  •  Layer 1", 28f, bottom + 38f, text); c.drawText("Pincel • Borracha • Lasso • Balde • Onion Skin • QR", 28f, height - 22f, sub)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x; val y = event.y
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> { lastX = x; lastY = y; lastTime = event.eventTime; drawing = true; addSegment(x, y, x + 0.01f, y + 0.01f, normalizedPressure(event.getPressure()), 0f); invalidate(); return true }
                MotionEvent.ACTION_MOVE -> { if (!drawing) return true; val dx = x - lastX; val dy = y - lastY; val dt = (event.eventTime - lastTime).coerceAtLeast(1L); val speed = hypot(dx, dy) / dt.toFloat(); addSegment(lastX, lastY, x, y, normalizedPressure(event.getPressure()), speed); lastX = x; lastY = y; lastTime = event.eventTime; invalidate(); return true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { drawing = false; invalidate(); return true }
            }
            return true
        }

        private fun addSegment(x1: Float, y1: Float, x2: Float, y2: Float, pressure: Float, speed: Float) {
            val base = if (tool == Tool.ERASER) BrushDefaults.forPreset("eraser") else BrushDefaults.forPreset("basic")
            val sizeFactor = if (tool == Tool.ERASER) 1.0f else 0.35f + pressure * 0.65f
            val speedFactor = 1f - (speed * 0.8f).coerceIn(0f, 0.35f)
            val width = (base.size * sizeFactor * speedFactor).coerceIn(1.5f, 160f)
            val alpha = ((base.opacity * (0.35f + pressure * 0.65f)) * 255f).toInt().coerceIn(1, 255)
            val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
            segments += Segment(path, width, alpha, tool == Tool.ERASER)
        }

        private fun normalizedPressure(value: Float): Float = if (value.isFinite() && value > 1f) (value / 2f).coerceIn(0f, 1f) else value.coerceIn(0f, 1f).let { if (it == 0f) 0.5f else it }
        private fun blend(a: Int, b: Int, amount: Float): Int { val t = amount.coerceIn(0f, 1f); val r = (Color.red(a) * (1f - t) + Color.red(b) * t).toInt(); val g = (Color.green(a) * (1f - t) + Color.green(b) * t).toInt(); val bl = (Color.blue(a) * (1f - t) + Color.blue(b) * t).toInt(); return Color.rgb(r, g, bl) }
    }
}
