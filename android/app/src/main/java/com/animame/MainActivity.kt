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
            setPadding(dp(6), dp(5), dp(6), dp(5))
            setBackgroundColor(Color.rgb(30, 34, 39))
        }
        top.addView(button("Definições", 78) { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) })
        top.addView(button("Pincéis", 68) { startActivityForResult(Intent(this@MainActivity, BrushPickerActivity::class.java), 42) })
        top.addView(button("Opções", 68) { startActivityForResult(Intent(this@MainActivity, ToolOptionsActivity::class.java), 43) })
        top.addView(button("Borracha", 76) { editor.tool = Tool.ERASER; editor.invalidate() })
        top.addView(button("Desfazer", 72) { editor.undo() })
        top.addView(button("Refazer", 72) { editor.redo() })
        top.addView(button("Camada +", 78) { editor.addLayer() })
        top.addView(button("Zoom +", 62) { editor.zoom(1.2f) })
        top.addView(button("Zoom -", 62) { editor.zoom(.8333333f) })
        top.addView(button("Ajustar", 62) { editor.resetViewport() })
        top.addView(button("Reproduzir", 82) { editor.togglePlayback() })
        top.addView(button("FPS -", 56) { editor.adjustFps(-1) })
        top.addView(button("FPS +", 56) { editor.adjustFps(1) })
        root.addView(top, FrameLayout.LayoutParams(-1, dp(60)))

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
            onLayerSelected = { layerId -> editor.selectLayer(layerId) },
            onLayerChanged = { editor.invalidate(); refreshTimeline() }
        )
        root.addView(timeline, FrameLayout.LayoutParams(-1, dp(timelineHeightDp)).apply { gravity = android.view.Gravity.BOTTOM })
        setContentView(root)
        refreshTimeline()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == 42 || requestCode == 43) && resultCode == RESULT_OK) {
            BrushToolState.load(this)
            editor.invalidate()
            refreshTimeline()
        }
    }

    private fun refreshTimeline() { if (::timeline.isInitialized) timeline.refresh(ThemeColorStore.get(this)) }
    private fun button(label: String, widthDp: Int, action: () -> Unit) = Button(this).apply { text = label; textSize = 9f; setOnClickListener { action() }; layoutParams = LinearLayout.LayoutParams(dp(widthDp), dp(50)) }
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
        private var playing = false
        private var lastPlaybackNanos = 0L
        private var playbackAccumulator = 0L
        private val viewport = EditorViewportState()
        private val undo = ArrayDeque<List<StrokeData>>()
        private val redo = ArrayDeque<List<StrokeData>>()
        private val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
        private val panel = Paint(Paint.ANTI_ALIAS_FLAG)
        private val paper = Paint(Paint.ANTI_ALIAS_FLAG)
        private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 25f }
        private val info = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 14f }
        var tool = Tool.BRUSH

        private val scaleDetector = ScaleGestureDetector(this@MainActivity, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean { viewport.zoomAt(detector.scaleFactor, detector.focusX, detector.focusY); invalidate(); return true }
        })

        init { document.normalize(); refreshTheme(); isFocusable = true }

        fun refreshTheme() {
            accent = ThemeColorStore.get(this@MainActivity)
            bg.color = Color.rgb(18, 20, 23)
            panel.color = blend(accent, Color.rgb(30, 34, 39), .82f)
            paper.color = document.backgroundColor
        }

        override fun onDraw(c: Canvas) {
            c.drawColor(bg.color)
            val top = dp(60).toFloat(); val bottom = height - dp(timelineHeightDp).toFloat()
            c.drawRect(0f, 0f, width.toFloat(), top, panel)
            c.save(); c.clipRect(0f, top, width.toFloat(), bottom)
            c.translate(viewport.offsetX, viewport.offsetY); c.scale(viewport.scale, viewport.scale)
            val left = width * .07f; val right = width * .93f
            if (!document.transparentBackground) c.drawRect(left, top + dp(14), right, bottom - dp(14), paper)
            drawOnion(c); drawDocument(c)
            if (previewStamps.isNotEmpty()) drawStamps(c, previewStamps, BrushToolState.color, 1f)
            c.restore()
            c.drawText("ANIMA-ME", dp(76).toFloat(), dp(37).toFloat(), title)
            c.drawText("${document.name} | Frame ${document.currentFrame + 1}/${document.duration} | ${document.fps} FPS | ${document.width} × ${document.height} | Zoom ${(viewport.scale * 100).toInt()}%", dp(76).toFloat(), dp(53).toFloat(), info)
            c.drawText(if (playing) "REPRODUZINDO" else document.activeLayer.name, (width - dp(170)).toFloat(), dp(37).toFloat(), info)
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
                drawingForFrame(layer, document.currentFrame)?.strokes?.forEach { stroke ->
                    drawStamps(c, BrushEngine.stamps(stroke.samples, stroke.resolvedBrushSettings(), stroke.id.hashCode().toLong()), stroke.color, layer.opacity)
                }
            }
        }

        private fun drawOnion(c: Canvas) {
            if (!document.onion.enabled || playing) return
            val layer = document.activeLayer
            layer.previousFrames(document.currentFrame, document.onion.previousCount).forEach { (_, frame) -> drawFrameGhost(c, frame, Color.rgb(70, 145, 255), document.onion.opacity / 100f) }
            layer.nextFrames(document.currentFrame, document.onion.nextCount).forEach { (_, frame) -> drawFrameGhost(c, frame, Color.rgb(255, 90, 100), document.onion.opacity / 100f) }
        }

        private fun drawFrameGhost(c: Canvas, frame: DrawingFrame, color: Int, alpha: Float) {
            frame.strokes.forEach { stroke -> drawStamps(c, BrushEngine.stamps(stroke.samples, stroke.resolvedBrushSettings(), stroke.id.hashCode().toLong()), color, alpha) }
        }

        private fun drawStamps(c: Canvas, stamps: List<BrushEngine.Stamp>, color: Int, layerOpacity: Float) {
            stamps.forEach { s ->
                stampPaint.color = Color.argb((s.alpha * layerOpacity * 255f).toInt().coerceIn(1, 255), Color.red(color), Color.green(color), Color.blue(color))
                c.drawCircle(s.x, s.y, s.size * .5f, stampPaint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            scaleDetector.onTouchEvent(event)
            if (playing || document.activeLayer.locked || event.pointerCount >= 2) return true
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (event.y < dp(60) || event.y > height - dp(timelineHeightDp) - dp(6)) return false
                    currentSamples.clear(); drawing = true; addSample(event); renderPreview(); invalidate(); return true
                }
                MotionEvent.ACTION_MOVE -> { if (!drawing) return true; addSample(event); renderPreview(); invalidate(); return true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (drawing && event.actionMasked == MotionEvent.ACTION_UP) commitStroke()
                    drawing = false; currentSamples.clear(); previewStamps = emptyList(); invalidate(); refreshTimeline(); return true
                }
            }
            return true
        }

        private fun modelX(x: Float) = (x - viewport.offsetX) / viewport.scale
        private fun modelY(y: Float) = (y - viewport.offsetY) / viewport.scale
        private fun addSample(e: MotionEvent) {
            val pressure = if (BrushToolState.pressure) e.pressure.coerceIn(.05f, 1f) else 1f
            currentSamples += StrokeSample(modelX(e.x), modelY(e.y), pressure, e.eventTime)
        }

        private fun settings(): com.animame.editor.BrushSettings {
            val base = if (tool == Tool.ERASER) BrushDefaults.forPreset("eraser") else BrushCatalog.settings(BrushToolState.brushId)
            return base.copy(size = BrushToolState.size, opacity = BrushToolState.opacity, flow = BrushToolState.flow, spacing = BrushToolState.spacing).normalized()
        }

        private fun renderPreview() {
            previewStamps = BrushEngine.stamps(BrushEngine.smooth(currentSamples, BrushToolState.smoothing), settings(), document.currentFrame.toLong())
        }

        private fun copyStrokes(): List<StrokeData> = document.activeLayer.frameAt(document.currentFrame)?.strokes?.map { s -> s.copy(samples = s.samples.map { it.copy() }.toMutableList()) } ?: emptyList()
        private fun restoreStrokes(snapshot: List<StrokeData>) { val frame = document.activeLayer.ensureFrame(document.currentFrame); frame.strokes.clear(); snapshot.forEach { frame.strokes += it.copy(samples = it.samples.map { p -> p.copy() }.toMutableList()) }; invalidate(); refreshTimeline() }
        private fun commitStroke() {
            if (currentSamples.isEmpty()) return
            undo.addLast(copyStrokes()); redo.clear()
            val frame = document.activeLayer.ensureFrame(document.currentFrame)
            val s = settings()
            val color = if (tool == Tool.ERASER) Color.WHITE else BrushToolState.color
            frame.strokes += StrokeData(brushId = s.id, color = color, size = BrushToolState.size, opacity = BrushToolState.opacity, settings = s, samples = currentSamples.map { it.copy() }.toMutableList())
            BrushToolState.save(this@MainActivity)
        }

        fun undo() { if (undo.isEmpty()) return; redo.addLast(copyStrokes()); restoreStrokes(undo.removeLast()) }
        fun redo() { if (redo.isEmpty()) return; undo.addLast(copyStrokes()); restoreStrokes(redo.removeLast()) }
        fun selectLayer(id: String) { document.selectLayer(id); undo.clear(); redo.clear(); invalidate(); refreshTimeline() }
        fun zoom(f: Float) { viewport.zoomAt(f, width * .5f, height * .5f); invalidate() }
        fun resetViewport() { viewport.reset(); invalidate() }
        fun addLayer() { document.addLayer(); undo.clear(); redo.clear(); invalidate(); refreshTimeline() }
        fun togglePlayback() { if (playing) stopPlayback() else startPlayback() }
        fun startPlayback() { if (document.duration <= 1) { Toast.makeText(this@MainActivity, "Adicione pelo menos 2 frames para reproduzir", Toast.LENGTH_SHORT).show(); return }; playing = true; lastPlaybackNanos = System.nanoTime(); playbackAccumulator = 0L; Choreographer.getInstance().postFrameCallback(this); invalidate(); refreshTimeline() }
        fun stopPlayback() { if (!playing) return; playing = false; Choreographer.getInstance().removeFrameCallback(this); lastPlaybackNanos = 0L; playbackAccumulator = 0L; invalidate(); refreshTimeline() }
        override fun doFrame(t: Long) { if (!playing) return; if (lastPlaybackNanos == 0L) lastPlaybackNanos = t; playbackAccumulator += (t - lastPlaybackNanos).coerceAtLeast(0L); lastPlaybackNanos = t; val frameNs = 1_000_000_000L / document.fps.coerceIn(1, 120); while (playbackAccumulator >= frameNs) { playbackAccumulator -= frameNs; document.advancePlaybackFrame() }; invalidate(); refreshTimeline(); Choreographer.getInstance().postFrameCallback(this) }
        fun adjustFps(delta: Int) { document.fps = (document.fps + delta).coerceIn(1, 60); document.normalize(); invalidate(); refreshTimeline() }
        private fun blend(a: Int, b: Int, amount: Float): Int { val t = amount.coerceIn(0f, 1f); return Color.rgb((Color.red(a) * (1f - t) + Color.red(b) * t).toInt(), (Color.green(a) * (1f - t) + Color.green(b) * t).toInt(), (Color.blue(a) * (1f - t) + Color.blue(b) * t).toInt()) }
    }
}
