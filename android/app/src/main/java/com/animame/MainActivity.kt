package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Choreographer
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import com.animame.editor.AnimationDocument
import com.animame.editor.AnimationLayer
import com.animame.editor.BrushCatalog
import com.animame.editor.BrushDefaults
import com.animame.editor.BrushEngine
import com.animame.editor.DrawingFrame
import com.animame.editor.EditorViewportState
import com.animame.editor.StrokeData
import com.animame.editor.StrokeSample

class MainActivity : Activity() {
    private lateinit var editor: EditorSurface
    private lateinit var timeline: TimelinePanel
    private val timelineHeightDp = 250

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        BrushToolState.load(this)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::editor.isInitialized) {
            BrushToolState.load(this)
            editor.refreshTheme()
            editor.invalidate()
            refreshTimeline()
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
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setBackgroundColor(Color.rgb(30, 34, 39))
        }
        top.addView(button("Definições", 82) { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) })
        top.addView(button("Pincéis", 72) { openBrushPicker() })
        top.addView(button("Borracha", 82) { editor.tool = Tool.ERASER; editor.invalidate() })
        top.addView(button("Tamanho", 74) { editor.adjustSize() })
        top.addView(button("Opacidade", 82) { editor.adjustOpacity() })
        top.addView(button("Nova camada", 96) { editor.addLayer() })
        top.addView(button("Zoom +", 68) { editor.zoom(1.2f) })
        top.addView(button("Zoom -", 68) { editor.zoom(.8333333f) })
        top.addView(button("Ajustar", 68) { editor.resetViewport() })
        top.addView(button("Reproduzir", 88) { editor.togglePlayback() })
        top.addView(button("FPS -", 60) { editor.adjustFps(-1) })
        top.addView(button("FPS +", 60) { editor.adjustFps(1) })
        root.addView(top, FrameLayout.LayoutParams(-1, dp(62)))

        timeline = TimelinePanel(
            this,
            editor.document,
            onFrameSelected = { layerId, frame ->
                editor.stopPlayback()
                editor.selectLayer(layerId)
                editor.document.currentFrame = frame.coerceIn(0, editor.document.duration - 1)
                editor.invalidate()
                refreshTimeline()
            },
            onLayerSelected = { layerId ->
                editor.selectLayer(layerId)
                editor.invalidate()
                refreshTimeline()
            },
            onLayerChanged = {
                editor.document.normalize()
                editor.invalidate()
                refreshTimeline()
            }
        )
        root.addView(timeline, FrameLayout.LayoutParams(-1, dp(timelineHeightDp)).apply { gravity = android.view.Gravity.BOTTOM })
        setContentView(root)
        refreshTimeline()
    }

    private fun openBrushPicker() {
        startActivityForResult(Intent(this, BrushPickerActivity::class.java), 42)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK) {
            BrushToolState.load(this)
            if (::editor.isInitialized) editor.invalidate()
        }
    }

    private fun refreshTimeline() {
        if (::timeline.isInitialized) timeline.refresh(ThemeColorStore.get(this))
    }

    private fun button(label: String, widthDp: Int, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 10f
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(dp(widthDp), dp(52))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private enum class Tool { BRUSH, ERASER }

    private inner class EditorSurface : View(this@MainActivity), Choreographer.FrameCallback {
        val document = AnimationDocument(
            name = intent.getStringExtra("project_name") ?: "Minha animação",
            width = intent.getIntExtra("project_width", 1280),
            height = intent.getIntExtra("project_height", 720),
            fps = intent.getIntExtra("project_fps", 24),
            duration = intent.getIntExtra("project_duration", 1).coerceAtLeast(1),
            transparentBackground = intent.getBooleanExtra("project_transparent", false)
        )
        private val currentSamples = mutableListOf<StrokeSample>()
        private var previewStamps = emptyList<BrushEngine.Stamp>()
        private var accent = ThemeColorStore.DEFAULT
        private var drawing = false
        private var onionEnabled = true
        private var playing = false
        private var lastPlaybackNanos = 0L
        private var playbackAccumulator = 0L
        private val viewport = EditorViewportState()
        private var panPointerId = MotionEvent.INVALID_POINTER_ID
        private var lastPanX = 0f
        private var lastPanY = 0f
        private var panActive = false
        var tool = Tool.BRUSH

        private val scaleDetector = ScaleGestureDetector(this@MainActivity, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                viewport.zoomAt(detector.scaleFactor, detector.focusX, detector.focusY)
                invalidate()
                return true
            }
        })
        private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        private val panel = Paint(Paint.ANTI_ALIAS_FLAG)
        private val paper = Paint(Paint.ANTI_ALIAS_FLAG)
        private val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 25f }
        private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 15f }

        init {
            document.normalize()
            refreshTheme()
            isFocusable = true
        }

        fun refreshTheme() {
            accent = ThemeColorStore.get(this@MainActivity)
            bg.color = Color.rgb(18, 20, 23)
            panel.color = blend(accent, Color.rgb(30, 34, 39), .82f)
            paper.color = document.backgroundColor
        }

        override fun onDraw(c: Canvas) {
            c.drawColor(bg.color)
            val top = dp(62).toFloat()
            val bottom = height - dp(timelineHeightDp).toFloat()
            c.drawRect(0f, 0f, width.toFloat(), top, panel)
            c.save()
            c.clipRect(0f, top, width.toFloat(), bottom)
            c.translate(viewport.offsetX, viewport.offsetY)
            c.scale(viewport.scale, viewport.scale)
            val left = width * .07f
            val right = width * .93f
            if (!document.transparentBackground) c.drawRect(left, top + dp(14), right, bottom - dp(14), paper)
            drawOnionSkin(c)
            drawDocument(c)
            if (previewStamps.isNotEmpty()) drawStamps(c, previewStamps, accent, 1f)
            c.restore()
            c.drawText("ANIMA-ME", dp(78).toFloat(), dp(38).toFloat(), text)
            c.drawText("${document.name}  |  Frame ${document.currentFrame + 1}/${document.duration}  |  ${document.fps} FPS  |  ${document.width} × ${document.height}  |  Zoom ${(viewport.scale * 100).toInt()}%", dp(78).toFloat(), dp(55).toFloat(), sub)
            c.drawText("${document.activeLayer.name}  |  ${if (playing) "REPRODUZINDO" else "PARADO"}", (width - dp(240)).toFloat(), dp(38).toFloat(), sub)
        }

        private fun drawingForFrame(layer: AnimationLayer, frame: Int): DrawingFrame? {
            layer.frameAt(frame)?.let { return it }
            var best = -1
            layer.frames.keys.forEach { candidate -> if (candidate <= frame && candidate > best) best = candidate }
            if (best < 0) return null
            val drawing = layer.frameAt(best) ?: return null
            return if (frame < best + drawing.exposure) drawing else null
        }

        private fun drawDocument(c: Canvas) {
            document.layers.asReversed().forEach { layer ->
                if (!layer.visible || layer.opacity <= 0f || layer.isBackground) return@forEach
                val frame = drawingForFrame(layer, document.currentFrame) ?: return@forEach
                frame.strokes.forEach { stroke -> drawStamps(c, BrushEngine.stamps(stroke.samples, stroke.resolvedBrushSettings(), stroke.id.hashCode().toLong()), stroke.color, layer.opacity) }
            }
        }

        private fun drawOnionSkin(c: Canvas) {
            if (!onionEnabled || playing) return
            val layer = document.activeLayer
            layer.previousFrames(document.currentFrame, document.onion.previousCount).forEach { (_, f) -> drawFrameGhost(c, f, Color.rgb(70, 145, 255), .22f) }
            layer.nextFrames(document.currentFrame, document.onion.nextCount).forEach { (_, f) -> drawFrameGhost(c, f, Color.rgb(255, 90, 100), .18f) }
        }

        private fun drawFrameGhost(c: Canvas, frame: DrawingFrame, color: Int, alpha: Float) {
            frame.strokes.forEach { stroke -> drawStamps(c, BrushEngine.stamps(stroke.samples, stroke.resolvedBrushSettings(), stroke.id.hashCode().toLong()), color, alpha) }
        }

        private fun drawStamps(c: Canvas, stamps: List<BrushEngine.Stamp>, color: Int, layerOpacity: Float) {
            stamps.forEach { stamp ->
                stampPaint.color = Color.argb((stamp.alpha * layerOpacity * 255f).toInt().coerceIn(1, 255), Color.red(color), Color.green(color), Color.blue(color))
                c.drawCircle(stamp.x, stamp.y, stamp.size * .5f, stampPaint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            scaleDetector.onTouchEvent(event)
            if (playing || document.activeLayer.locked) return true

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (event.y < dp(62) || event.y > height - dp(timelineHeightDp) - dp(8)) return false
                    panActive = false
                    panPointerId = MotionEvent.INVALID_POINTER_ID
                    currentSamples.clear()
                    drawing = true
                    addSample(event)
                    renderPreview()
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (event.pointerCount >= 2) {
                        drawing = false
                        currentSamples.clear()
                        previewStamps = emptyList()
                        panActive = true
                        val index = event.actionIndex
                        panPointerId = event.getPointerId(index)
                        lastPanX = event.getX(index)
                        lastPanY = event.getY(index)
                    }
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (panActive && event.pointerCount >= 2) {
                        val index = event.findPointerIndex(panPointerId).takeIf { it >= 0 } ?: 0
                        val x = event.getX(index)
                        val y = event.getY(index)
                        viewport.panBy(x - lastPanX, y - lastPanY)
                        lastPanX = x
                        lastPanY = y
                        invalidate()
                        return true
                    }
                    if (!drawing || event.pointerCount != 1) return true
                    addSample(event)
                    renderPreview()
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    if (event.pointerCount <= 2) {
                        panActive = false
                        panPointerId = MotionEvent.INVALID_POINTER_ID
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (drawing && event.actionMasked == MotionEvent.ACTION_UP) commitStroke()
                    drawing = false
                    panActive = false
                    panPointerId = MotionEvent.INVALID_POINTER_ID
                    currentSamples.clear()
                    previewStamps = emptyList()
                    invalidate()
                    refreshTimeline()
                    return true
                }
            }
            return true
        }

        private fun modelX(x: Float) = (x - viewport.offsetX) / viewport.scale
        private fun modelY(y: Float) = (y - viewport.offsetY) / viewport.scale
        private fun addSample(event: MotionEvent) {
            val pressure = if (BrushToolState.pressure) event.pressure.coerceIn(.05f, 1f) else 1f
            currentSamples += StrokeSample(modelX(event.x), modelY(event.y), pressure, event.eventTime)
        }

        private fun renderPreview() {
            val id = if (tool == Tool.ERASER) "eraser" else BrushToolState.brushId
            val base = if (tool == Tool.ERASER) BrushDefaults.forPreset("eraser") else BrushCatalog.settings(id)
            val settings = base.copy(size = BrushToolState.size, opacity = BrushToolState.opacity, spacing = BrushToolState.spacing)
            previewStamps = BrushEngine.stamps(BrushEngine.smooth(currentSamples, BrushToolState.smoothing), settings, document.currentFrame.toLong())
        }

        private fun commitStroke() {
            if (currentSamples.isEmpty()) return
            val frame = document.activeLayer.ensureFrame(document.currentFrame)
            val settings = if (tool == Tool.ERASER) BrushDefaults.forPreset("eraser").copy(size = BrushToolState.size, opacity = BrushToolState.opacity) else BrushCatalog.settings(BrushToolState.brushId).copy(size = BrushToolState.size, opacity = BrushToolState.opacity, spacing = BrushToolState.spacing)
            val color = if (tool == Tool.ERASER) Color.WHITE else accent
            frame.strokes += StrokeData(brushId = settings.id, color = color, size = BrushToolState.size, opacity = BrushToolState.opacity, settings = settings, samples = currentSamples.map { it.copy() }.toMutableList())
            BrushToolState.save(this@MainActivity)
        }

        fun selectLayer(id: String) { document.selectLayer(id); invalidate() }
        fun zoom(factor: Float) { viewport.zoomAt(factor, width * .5f, height * .5f); invalidate() }
        fun resetViewport() { viewport.reset(); invalidate() }
        fun insertFrame() { stopPlayback(); document.insertFrame(document.currentFrame); invalidate(); refreshTimeline() }
        fun duplicateFrame() { stopPlayback(); document.duplicateFrame(document.currentFrame); document.currentFrame = (document.currentFrame + 1).coerceAtMost(document.duration - 1); invalidate(); refreshTimeline() }
        fun deleteFrame() { stopPlayback(); if (document.duration <= 1) clearCurrentFrame() else { document.deleteFrame(document.currentFrame); invalidate(); refreshTimeline() } }
        fun previousFrame() { stopPlayback(); document.currentFrame = (document.currentFrame - 1).coerceAtLeast(0); invalidate(); refreshTimeline() }
        fun nextFrame() { stopPlayback(); document.currentFrame = (document.currentFrame + 1).coerceAtMost(document.duration - 1); invalidate(); refreshTimeline() }
        fun toggleOnion() { onionEnabled = !onionEnabled; invalidate(); refreshTimeline() }
        fun clearCurrentFrame() { document.activeLayer.frameAt(document.currentFrame)?.strokes?.clear(); invalidate(); refreshTimeline() }
        fun addLayer() { document.addLayer(); invalidate(); refreshTimeline() }
        fun togglePlayback() { if (playing) stopPlayback() else startPlayback() }
        fun startPlayback() { if (document.duration <= 1) { Toast.makeText(this@MainActivity, "Adicione pelo menos 2 frames para reproduzir", Toast.LENGTH_SHORT).show(); return }; playing = true; lastPlaybackNanos = System.nanoTime(); playbackAccumulator = 0L; Choreographer.getInstance().postFrameCallback(this); refreshTimeline(); invalidate() }
        fun stopPlayback() { if (!playing) return; playing = false; Choreographer.getInstance().removeFrameCallback(this); lastPlaybackNanos = 0L; playbackAccumulator = 0L; refreshTimeline(); invalidate() }
        override fun doFrame(frameTimeNanos: Long) { if (!playing) return; if (lastPlaybackNanos == 0L) lastPlaybackNanos = frameTimeNanos; playbackAccumulator += (frameTimeNanos - lastPlaybackNanos).coerceAtLeast(0L); lastPlaybackNanos = frameTimeNanos; val frameDuration = 1_000_000_000L / document.fps.coerceIn(1, 120); while (playbackAccumulator >= frameDuration) { playbackAccumulator -= frameDuration; document.advancePlaybackFrame() }; invalidate(); refreshTimeline(); Choreographer.getInstance().postFrameCallback(this) }
        fun adjustFps(delta: Int) { document.fps = (document.fps + delta).coerceIn(1, 60); Toast.makeText(this@MainActivity, "FPS: ${document.fps}", Toast.LENGTH_SHORT).show(); invalidate(); refreshTimeline() }
        fun adjustSize() { BrushToolState.size = when { BrushToolState.size < 8f -> 12f; BrushToolState.size < 24f -> 32f; BrushToolState.size < 64f -> 72f; else -> 6f }; BrushToolState.save(this@MainActivity); Toast.makeText(this@MainActivity, "Tamanho: ${BrushToolState.size.toInt()} px", Toast.LENGTH_SHORT).show() }
        fun adjustOpacity() { BrushToolState.opacity = when { BrushToolState.opacity > .85f -> .65f; BrushToolState.opacity > .55f -> .35f; else -> 1f }; BrushToolState.save(this@MainActivity); Toast.makeText(this@MainActivity, "Opacidade: ${(BrushToolState.opacity * 100).toInt()}%", Toast.LENGTH_SHORT).show() }
        private fun blend(a: Int, b: Int, amount: Float): Int { val t = amount.coerceIn(0f,1f); return Color.rgb((Color.red(a)*(1f-t)+Color.red(b)*t).toInt(), (Color.green(a)*(1f-t)+Color.green(b)*t).toInt(), (Color.blue(a)*(1f-t)+Color.blue(b)*t).toInt()) }
    }
}
