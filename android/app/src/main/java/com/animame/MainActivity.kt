package com.animame

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.animame.editor.AnimationDocument
import com.animame.editor.BrushDefaults
import com.animame.editor.BrushEngine
import com.animame.editor.BrushPresetRepository
import com.animame.editor.DrawingFrame
import com.animame.editor.StrokeData
import com.animame.editor.StrokeSample

class MainActivity : Activity() {
    private lateinit var editor: EditorSurface
    private lateinit var timelineLabel: TextView
    private lateinit var frameStrip: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::editor.isInitialized) {
            editor.refreshTheme()
            editor.invalidate()
            editor.refreshTimeline()
        }
    }

    override fun onPause() {
        if (::editor.isInitialized) editor.stopPlayback()
        super.onPause()
    }

    private fun buildUi() {
        val root = FrameLayout(this)
        editor = EditorSurface()
        root.addView(editor, FrameLayout.LayoutParams(-1, -1))

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 6, 8, 6)
            setBackgroundColor(Color.rgb(30, 34, 39))
        }
        top.addView(button("⚙", 50) { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) })
        top.addView(button("Pincel", 76) { editor.tool = Tool.BRUSH; editor.invalidate() })
        top.addView(button("Borracha", 88) { editor.tool = Tool.ERASER; editor.invalidate() })
        top.addView(button("Pincéis…", 82) { editor.showBrushPicker() })
        top.addView(button("Tamanho", 82) { editor.adjustSize() })
        top.addView(button("Opacidade", 90) { editor.adjustOpacity() })
        top.addView(button("− Zoom", 72) { editor.adjustZoom(-0.1f) })
        top.addView(button("100%", 62) { editor.resetViewport() })
        top.addView(button("+ Zoom", 72) { editor.adjustZoom(0.1f) })
        top.addView(button("Camada +", 86) { editor.addLayer() })
        top.addView(button("▶ Play", 76) { editor.togglePlayback() })
        top.addView(button("FPS −", 68) { editor.adjustFps(-1) })
        top.addView(button("FPS +", 68) { editor.adjustFps(1) })
        root.addView(top, FrameLayout.LayoutParams(-1, 66))

        timelineLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(10, 4, 10, 4)
        }
        frameStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(4, 3, 4, 3)
        }
        val frameScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setBackgroundColor(Color.rgb(20, 23, 27))
            addView(frameStrip, LinearLayout.LayoutParams(-2, 58))
        }
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(24, 27, 31))
            addView(timelineLabel, LinearLayout.LayoutParams(190, 58))
            addView(button("◀", 48) { editor.previousFrame() })
            addView(button("＋", 48) { editor.insertFrame() })
            addView(button("⧉", 48) { editor.duplicateFrame() })
            addView(button("−", 48) { editor.deleteFrame() })
            addView(button("▶", 48) { editor.nextFrame() })
            addView(button("Onion", 68) { editor.toggleOnion() })
            addView(button("Limpar", 70) { editor.clearCurrentFrame() })
            addView(frameScroll, LinearLayout.LayoutParams(0, 58, 1f))
        }
        val p = FrameLayout.LayoutParams(-1, 58)
        p.gravity = android.view.Gravity.BOTTOM
        root.addView(controls, p)
        setContentView(root)
        editor.timelineLabel = timelineLabel
        editor.frameStrip = frameStrip
        editor.refreshTimeline()
    }

    private fun button(label: String, width: Int, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 10f
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(width, 54)
    }

    private enum class Tool { BRUSH, ERASER }

    private inner class EditorSurface : View(this@MainActivity), Choreographer.FrameCallback {
        private val document = AnimationDocument(duration = 1)
        private val currentSamples = mutableListOf<StrokeSample>()
        private var previewStamps = emptyList<BrushEngine.Stamp>()
        private var accent = ThemeColorStore.DEFAULT
        private var brushSize = 12f
        private var brushOpacity = 1f
        private var drawing = false
        private var onionEnabled = true
        private var playing = false
        private var lastPlaybackNanos = 0L
        private var playbackAccumulator = 0L
        private var activeLayerId: String = document.activeLayer.id
        private var brushId = "canvas_1"
        private var brushName = "Dip Pen (Soft)"
        private var brushCategory = "Simple"

        private var zoom = 1f
        private var panX = 0f
        private var panY = 0f
        private var pinchStartDistance = 0f
        private var pinchStartZoom = 1f
        private var lastTouchX = 0f
        private var lastTouchY = 0f
        private var twoFingerGesture = false

        var tool = Tool.BRUSH
        var timelineLabel: TextView? = null
        var frameStrip: LinearLayout? = null

        private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        private val panel = Paint(Paint.ANTI_ALIAS_FLAG)
        private val paper = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 25f }
        private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 15f }

        init {
            refreshTheme()
            isFocusable = true
        }

        fun refreshTheme() {
            accent = ThemeColorStore.get(this@MainActivity)
            bg.color = Color.rgb(18, 20, 23)
            panel.color = blend(accent, Color.rgb(30, 34, 39), .82f)
            paper.color = Color.rgb(245, 245, 245)
        }

        override fun onDraw(c: Canvas) {
            c.drawColor(bg.color)
            val top = 66f
            val bottom = height - 58f
            val left = width * .07f
            val right = width * .93f
            c.drawRect(0f, 0f, width.toFloat(), top, panel)

            val centerX = (left + right) * .5f
            val centerY = (top + bottom) * .5f
            c.save()
            c.clipRect(left, top + 14f, right, bottom - 14f)
            c.translate(centerX + panX, centerY + panY)
            c.scale(zoom, zoom)
            c.translate(-centerX, -centerY)
            c.drawRect(left, top + 14f, right, bottom - 14f, paper)
            drawOnionSkin(c)
            drawDocument(c)
            if (previewStamps.isNotEmpty()) drawStamps(c, previewStamps, accent, 1f)
            c.restore()

            c.drawText("ANIMA-ME", 78f, 38f, text)
            c.drawText("Frame ${document.currentFrame + 1}/${document.duration}  •  ${document.fps} FPS  •  ${document.layers.size} camada(s)", 78f, 58f, sub)
            c.drawText("${brushName}  •  ${if (playing) "PLAY" else "PAUSE"}  •  ${(zoom * 100).toInt()}%", width - 330f, 38f, sub)
        }

        private fun drawDocument(c: Canvas) {
            document.layers.asReversed().forEach { layer ->
                if (!layer.visible || layer.opacity <= 0f) return@forEach
                val frame = layer.frameAt(document.currentFrame) ?: return@forEach
                frame.strokes.forEach { stroke ->
                    drawStamps(c, BrushEngine.stamps(stroke.samples, stroke.resolvedBrushSettings(), stroke.id.hashCode().toLong()), stroke.color, layer.opacity)
                }
            }
        }

        private fun drawOnionSkin(c: Canvas) {
            if (!onionEnabled || playing) return
            val layer = document.layers.firstOrNull { it.id == activeLayerId } ?: return
            layer.previousFrames(document.currentFrame, document.onion.previousCount).forEach { (_, f) -> drawFrameGhost(c, f, Color.rgb(70, 145, 255), .22f) }
            layer.nextFrames(document.currentFrame, document.onion.nextCount).forEach { (_, f) -> drawFrameGhost(c, f, Color.rgb(255, 90, 100), .18f) }
        }

        private fun drawFrameGhost(c: Canvas, frame: DrawingFrame, color: Int, alpha: Float) {
            frame.strokes.forEach { stroke ->
                drawStamps(c, BrushEngine.stamps(stroke.samples, stroke.resolvedBrushSettings(), stroke.id.hashCode().toLong()), color, alpha)
            }
        }
