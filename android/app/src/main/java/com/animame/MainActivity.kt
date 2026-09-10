package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.animame.editor.AnimationDocument
import com.animame.editor.BrushCatalog
import com.animame.editor.BrushDefaults
import com.animame.editor.BrushEngine
import com.animame.editor.DrawingFrame
import com.animame.editor.StrokeData
import com.animame.editor.StrokeSample
import kotlin.math.hypot

class MainActivity : Activity() {
    private lateinit var editor: EditorSurface
    private lateinit var timelineLabel: TextView
    private lateinit var frameStrip: LinearLayout
    private lateinit var brushLabel: TextView

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
        top.addView(button("⚙", 52) { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) })
        top.addView(button("Pincel", 82) { editor.tool = Tool.BRUSH; editor.invalidate() })
        top.addView(button("Borracha", 92) { editor.tool = Tool.ERASER; editor.invalidate() })
        top.addView(button("Pincéis", 82) { editor.cycleBrush() })
        brushLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            setPadding(10, 0, 10, 0)
            gravity = android.view.Gravity.CENTER_VERTICAL
            text = "Basic"
        }
        top.addView(brushLabel, LinearLayout.LayoutParams(150, 54))
        top.addView(button("Tamanho", 88) { editor.adjustSize() })
        top.addView(button("Opacidade", 94) { editor.adjustOpacity() })
        top.addView(button("Camada +", 90) { editor.addLayer() })
        top.addView(button("−", 50) { editor.zoomOut() })
        top.addView(button("100%", 62) { editor.resetView() })
        top.addView(button("+", 50) { editor.zoomIn() })
        top.addView(button("▶ Play", 82) { editor.togglePlayback() })
        top.addView(button("FPS −", 72) { editor.adjustFps(-1) })
        top.addView(button("FPS +", 72) { editor.adjustFps(1) })
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
            addView(frameStrip, HorizontalScrollView.LayoutParams(-2, 58))
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
            addView(button("Onion", 70) { editor.toggleOnion() })
            addView(button("Limpar", 72) { editor.clearCurrentFrame() })
            addView(frameScroll, LinearLayout.LayoutParams(0, 58, 1f))
        }
        val p = FrameLayout.LayoutParams(-1, 58)
        p.gravity = android.view.Gravity.BOTTOM
        root.addView(controls, p)
        setContentView(root)
        editor.timelineLabel = timelineLabel
        editor.frameStrip = frameStrip
        editor.brushLabel = brushLabel
        editor.refreshTimeline()
    }

    private fun button(label: String, width: Int, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 11f
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
        private var brushIndex = 0
        private var selectedBrushId = "basic"
        private var zoom = 1f
        private var panX = 0f
        private var panY = 0f
        private var gestureMode = false
        private var lastMidX = 0f
        private var lastMidY = 0f
        private var lastDistance = 0f
        var tool = Tool.BRUSH
        var timelineLabel: TextView? = null
        var frameStrip: LinearLayout? = null
        var brushLabel: TextView? = null

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

        private fun contentBounds(): FloatArray {
            val top = 80f
            val bottom = height - 72f
            val left = width * .07f
            val right = width * .93f
            return floatArrayOf(left, top, right, bottom)
        }

        override fun onDraw(c: Canvas) {
            c.drawColor(bg.color)
            val b = contentBounds()
            c.save()
            c.translate(panX, panY)
            c.scale(zoom, zoom)
            c.drawRect(b[0], b[1], b[2], b[3], paper)
            drawOnionSkin(c)
            drawDocument(c)
            if (previewStamps.isNotEmpty()) drawStamps(c, previewStamps, accent, 1f)
            c.restore()
            c.drawRect(0f, 0f, width.toFloat(), 66f, panel)
            c.drawText("ANIMA-ME", 78f, 38f, text)
            c.drawText("Frame ${document.currentFrame + 1}/${document.duration}  •  ${document.fps} FPS  •  ${document.layers.size} camada(s)", 78f, 58f, sub)
            c.drawText("${document.activeLayer.name}  •  ${if (playing) "PLAY" else "PAUSE"}", width - 230f, 38f, sub)
            c.drawText("${(zoom * 100).toInt()}%", width - 80f, height - 70f, sub)
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

        private fun drawStamps(c: Canvas, stamps: List<BrushEngine.Stamp>, color: Int, layerOpacity: Float) {
            stamps.forEach { stamp ->
                stampPaint.color = Color.argb(
                    (stamp.alpha * layerOpacity * 255f).toInt().coerceIn(1, 255),
                    Color.red(color), Color.green(color), Color.blue(color)
                )
                c.drawCircle(stamp.x, stamp.y, stamp.size * .5f, stampPaint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (playing) return true
            if (event.pointerCount >= 2) return handleTransform(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (event.y < 70f || event.y > height - 64f) return false
                    currentSamples.clear()
                    drawing = true
                    addSample(event)
                    renderPreview()
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!drawing) return true
                    addSample(event)
                    renderPreview()
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (drawing && event.actionMasked == MotionEvent.ACTION_UP) commitStroke()
                    drawing = false
                    currentSamples.clear()
                    previewStamps = emptyList()
                    invalidate()
                    return true
                }
            }
            return true
        }

        private fun handleTransform(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    drawing = false
                    gestureMode = true
                    lastMidX = (event.getX(0) + event.getX(1)) * .5f
                    lastMidY = (event.getY(0) + event.getY(1)) * .5f
                    lastDistance = distance(event)
                    currentSamples.clear()
                    previewStamps = emptyList()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!gestureMode || event.pointerCount < 2) return true
                    val midX = (event.getX(0) + event.getX(1)) * .5f
                    val midY = (event.getY(0) + event.getY(1)) * .5f
                    panX += midX - lastMidX
                    panY += midY - lastMidY
                    val newDistance = distance(event)
                    if (lastDistance > 0f && newDistance > 0f) {
                        val factor = (newDistance / lastDistance).coerceIn(.85f, 1.18f)
                        zoomAround(factor, midX, midY)
                    }
                    lastMidX = midX
                    lastMidY = midY
                    lastDistance = newDistance
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    gestureMode = false
                    drawing = false
                    currentSamples.clear()
                    previewStamps = emptyList()
                    invalidate()
                    return true
                }
            }
            return true
        }

        private fun distance(e: MotionEvent): Float {
            return hypot(e.getX(1) - e.getX(0), e.getY(1) - e.getY(0))
        }

        private fun screenToCanvas(x: Float, y: Float): Pair<Float, Float> {
            return Pair((x - panX) / zoom, (y - panY) / zoom)
        }

        private fun zoomAround(factor: Float, x: Float, y: Float) {
            val old = zoom
            zoom = (zoom * factor).coerceIn(.35f, 6f)
            val scale = zoom / old
            panX = x - (x - panX) * scale
            panY = y - (y - panY) * scale
        }

        fun zoomIn() { zoomAround(1.25f, width * .5f, height * .5f); invalidate() }
        fun zoomOut() { zoomAround(.8f, width * .5f, height * .5f); invalidate() }
        fun resetView() { zoom = 1f; panX = 0f; panY = 0f; invalidate() }

        private fun addSample(event: MotionEvent) {
            val p = screenToCanvas(event.x, event.y)
            val pressure = (if (event.pressure > 1f) event.pressure / 2f else event.pressure).coerceIn(.05f, 1f)
            currentSamples += StrokeSample(p.first, p.second, pressure, event.eventTime)
        }

        private fun renderPreview() {
            val base = if (tool == Tool.ERASER) BrushDefaults.forPreset("eraser") else BrushCatalog.settings(selectedBrushId)
            previewStamps = BrushEngine.stamps(
                BrushEngine.smooth(currentSamples, .18f),
                base.copy(size = brushSize, opacity = brushOpacity),
                document.currentFrame.toLong()
            )
        }

        private fun commitStroke() {
            if (currentSamples.isEmpty()) return
            val frame = document.activeLayer.ensureFrame(document.currentFrame)
            val color = if (tool == Tool.ERASER) Color.WHITE else accent
            val settings = (if (tool == Tool.ERASER) BrushDefaults.forPreset("eraser") else BrushCatalog.settings(selectedBrushId))
                .copy(size = brushSize, opacity = brushOpacity)
            frame.strokes += StrokeData(
                brushId = settings.id,
                color = color,
                size = brushSize,
                opacity = brushOpacity,
                settings = settings,
                samples = currentSamples.map { it.copy() }.toMutableList()
            )
        }

        fun cycleBrush() {
            val all = BrushCatalog.all()
            if (all.isEmpty()) return
            brushIndex = (brushIndex + 1) % all.size
            selectedBrushId = all[brushIndex].id
            brushLabel?.text = all[brushIndex].name
            tool = Tool.BRUSH
            Toast.makeText(this@MainActivity, all[brushIndex].name, Toast.LENGTH_SHORT).show()
        }

        fun insertFrame() {
            document.insertFrame(document.currentFrame)
            document.currentFrame = (document.currentFrame + 1).coerceAtMost(document.duration - 1)
            invalidate()
            refreshTimeline()
        }

        fun duplicateFrame() {
            document.duplicateFrame(document.currentFrame)
            document.currentFrame = (document.currentFrame + 1).coerceAtMost(document.duration - 1)
            invalidate()
            refreshTimeline()
        }

        fun deleteFrame() {
            if (document.duration <= 1) {
                clearCurrentFrame()
                return
            }
            document.deleteFrame(document.currentFrame)
            document.currentFrame = document.currentFrame.coerceIn(0, document.duration - 1)
            invalidate()
            refreshTimeline()
        }

        fun previousFrame() {
            stopPlayback()
            document.currentFrame = (document.currentFrame - 1).coerceAtLeast(0)
            invalidate()
            refreshTimeline()
        }

        fun nextFrame() {
            stopPlayback()
            document.currentFrame = (document.currentFrame + 1).coerceAtMost(document.duration - 1)
            invalidate()
            refreshTimeline()
        }

        fun toggleOnion() {
            onionEnabled = !onionEnabled
            invalidate()
            refreshTimeline()
        }

        fun clearCurrentFrame() {
            document.activeLayer.frameAt(document.currentFrame)?.strokes?.clear()
            invalidate()
            refreshTimeline()
        }

        fun addLayer() {
            val layer = document.addLayer()
            activeLayerId = layer.id
            invalidate()
            refreshTimeline()
        }

        fun togglePlayback() {
            if (playing) stopPlayback() else startPlayback()
        }

        fun startPlayback() {
            if (document.duration <= 1) {
                Toast.makeText(this@MainActivity, "Adicione pelo menos 2 frames para reproduzir", Toast.LENGTH_SHORT).show()
                return
            }
            playing = true
            lastPlaybackNanos = System.nanoTime()
            playbackAccumulator = 0L
            Choreographer.getInstance().postFrameCallback(this)
            refreshTimeline()
            invalidate()
        }

        fun stopPlayback() {
            if (!playing) return
            playing = false
            Choreographer.getInstance().removeFrameCallback(this)
            lastPlaybackNanos = 0L
            playbackAccumulator = 0L
            refreshTimeline()
            invalidate()
        }

        override fun doFrame(frameTimeNanos: Long) {
            if (!playing) return
            if (lastPlaybackNanos == 0L) lastPlaybackNanos = frameTimeNanos
            val elapsed = (frameTimeNanos - lastPlaybackNanos).coerceAtLeast(0L)
            lastPlaybackNanos = frameTimeNanos
            playbackAccumulator += elapsed
            val frameDuration = 1_000_000_000L / document.fps.coerceIn(1, 120)
            while (playbackAccumulator >= frameDuration) {
                playbackAccumulator -= frameDuration
                document.currentFrame++
                if (document.currentFrame >= document.duration) document.currentFrame = 0
            }
            invalidate()
            refreshTimeline()
            Choreographer.getInstance().postFrameCallback(this)
        }

        fun adjustFps(delta: Int) {
            val next = (document.fps + delta).coerceIn(1, 60)
            document.fps = next
            Toast.makeText(this@MainActivity, "FPS: $next", Toast.LENGTH_SHORT).show()
            refreshTimeline()
            invalidate()
        }

        fun refreshTimeline() {
            timelineLabel?.text = "Frame ${document.currentFrame + 1}/${document.duration}  •  ${document.activeLayer.name}\n${document.fps} FPS  •  ${if (playing) "Reproduzindo" else "Parado"}"
            frameStrip?.let { strip ->
                strip.removeAllViews()
                for (index in 0 until document.duration) {
                    val cell = TextView(this@MainActivity).apply {
                        text = "${index + 1}"
                        textSize = 12f
                        gravity = android.view.Gravity.CENTER
                        setTextColor(if (index == document.currentFrame) Color.WHITE else Color.LTGRAY)
                        setBackgroundColor(
                            when {
                                index == document.currentFrame -> accent
                                hasContent(index) -> Color.rgb(65, 72, 82)
                                else -> Color.rgb(38, 42, 48)
                            }
                        )
                        setOnClickListener {
                            stopPlayback()
                            document.currentFrame = index
                            invalidate()
                            refreshTimeline()
                        }
                    }
                    strip.addView(cell, LinearLayout.LayoutParams(52, 50).apply { setMargins(3, 4, 3, 4) })
                }
            }
        }

        private fun hasContent(index: Int): Boolean {
            return document.layers.any { layer -> !layer.frameAt(index)?.strokes.isNullOrEmpty() }
        }

        fun adjustSize() {
            brushSize = when {
                brushSize < 8f -> 12f
                brushSize < 24f -> 32f
                brushSize < 64f -> 72f
                else -> 6f
            }
            Toast.makeText(this@MainActivity, "Tamanho: ${brushSize.toInt()} px", Toast.LENGTH_SHORT).show()
        }

        fun adjustOpacity() {
            brushOpacity = when {
                brushOpacity > .85f -> .65f
                brushOpacity > .55f -> .35f
                else -> 1f
            }
            Toast.makeText(this@MainActivity, "Opacidade: ${(brushOpacity * 100).toInt()}%", Toast.LENGTH_SHORT).show()
        }

        private fun blend(a: Int, b: Int, amount: Float): Int {
            val t = amount.coerceIn(0f, 1f)
            return Color.rgb(
                (Color.red(a) * (1f - t) + Color.red(b) * t).toInt(),
                (Color.green(a) * (1f - t) + Color.green(b) * t).toInt(),
                (Color.blue(a) * (1f - t) + Color.blue(b) * t).toInt()
            )
        }
    }
}
