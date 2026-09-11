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
import android.view.ScaleGestureDetector
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
import com.animame.editor.EditorViewportState
import com.animame.editor.StrokeData
import com.animame.editor.StrokeSample

class MainActivity : Activity() {
    private lateinit var editor: EditorSurface
    private lateinit var timelineLabel: TextView
    private lateinit var frameStrip: LinearLayout

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
        top.addView(button("Definições", 82) { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) })
        top.addView(button("Pincéis", 72) { openBrushPicker() })
        top.addView(button("Camadas", 76) { editor.showLayersDialog() })
        top.addView(button("Borracha", 82) { editor.tool = Tool.ERASER; editor.invalidate() })
        top.addView(button("Tamanho", 74) { editor.adjustSize() })
        top.addView(button("Opacidade", 82) { editor.adjustOpacity() })
        top.addView(button("Camada +", 78) { editor.addLayer() })
        top.addView(button("Zoom +", 68) { editor.zoom(1.2f) })
        top.addView(button("Zoom -", 68) { editor.zoom(.8333333f) })
        top.addView(button("Ajustar", 68) { editor.resetViewport() })
        top.addView(button("Reproduzir", 88) { editor.togglePlayback() })
        top.addView(button("FPS -", 60) { editor.adjustFps(-1) })
        top.addView(button("FPS +", 60) { editor.adjustFps(1) })
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
            addView(button("Anterior", 70) { editor.previousFrame() })
            addView(button("Inserir", 66) { editor.insertFrame() })
            addView(button("Duplicar", 70) { editor.duplicateFrame() })
            addView(button("Eliminar", 70) { editor.deleteFrame() })
            addView(button("Seguinte", 70) { editor.nextFrame() })
            addView(button("Onion", 62) { editor.toggleOnion() })
            addView(button("Limpar", 66) { editor.clearCurrentFrame() })
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
        private var drawing = false
        private var onionEnabled = true
        private var playing = false
        private var lastPlaybackNanos = 0L
        private var playbackAccumulator = 0L
        private var activeLayerId: String = document.activeLayer.id
        private val viewport = EditorViewportState()
        private var lastPanX = 0f
        private var lastPanY = 0f
        private var panning = false
        var tool = Tool.BRUSH
        var timelineLabel: TextView? = null
        var frameStrip: LinearLayout? = null

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

        init { refreshTheme(); isFocusable = true }

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
            c.drawRect(0f, 0f, width.toFloat(), top, panel)
            c.save()
            c.clipRect(0f, top, width.toFloat(), bottom)
            c.translate(viewport.offsetX, viewport.offsetY)
            c.scale(viewport.scale, viewport.scale)
            val left = width * .07f
            val right = width * .93f
            c.drawRect(left, top + 14f, right, bottom - 14f, paper)
            drawOnionSkin(c)
            drawDocument(c)
            if (previewStamps.isNotEmpty()) drawStamps(c, previewStamps, accent, 1f)
            c.restore()
            c.drawText("ANIMA-ME", 78f, 38f, text)
            c.drawText("Frame ${document.currentFrame + 1}/${document.duration}  |  ${document.fps} FPS  |  ${document.layers.size} camada(s)  |  Zoom ${(viewport.scale * 100).toInt()}%", 78f, 58f, sub)
            c.drawText("${document.activeLayer.name}  |  ${if (playing) "REPRODUZINDO" else "PARADO"}", width - 250f, 38f, sub)
        }

        private fun drawDocument(c: Canvas) {
            document.layers.asReversed().forEach { layer ->
                if (!layer.visible || layer.opacity <= 0f) return@forEach
                val frame = layer.frameAt(document.currentFrame) ?: return@forEach
                frame.strokes.forEach { stroke -> drawStamps(c, BrushEngine.stamps(stroke.samples, stroke.resolvedBrushSettings(), stroke.id.hashCode().toLong()), stroke.color, layer.opacity) }
            }
        }

        private fun drawOnionSkin(c: Canvas) {
            if (!onionEnabled || playing) return
            val layer = document.layers.firstOrNull { it.id == activeLayerId } ?: return
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
            if (playing) return true
            if (event.pointerCount >= 2) {
                if (event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                    panning = true
                    lastPanX = (event.getX(0) + event.getX(1)) * .5f
                    lastPanY = (event.getY(0) + event.getY(1)) * .5f
                } else if (event.actionMasked == MotionEvent.ACTION_MOVE && panning) {
                    val cx = (event.getX(0) + event.getX(1)) * .5f
                    val cy = (event.getY(0) + event.getY(1)) * .5f
                    viewport.offsetX += cx - lastPanX
                    viewport.offsetY += cy - lastPanY
                    lastPanX = cx
                    lastPanY = cy
                    invalidate()
                }
                return true
            }
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (event.y < 70f || event.y > height - 64f) return false
                    panning = false
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
                    panning = false
                    invalidate()
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

        fun showLayersDialog() {
            val labels = document.layers.mapIndexed { index, layer ->
                val marker = if (layer.id == activeLayerId) "[ativo] " else ""
                "${marker}${index + 1}. ${layer.name}"
            }.toTypedArray()
            AlertDialog.Builder(this@MainActivity)
                .setTitle("Camadas")
                .setItems(labels) { dialog, which ->
                    activeLayerId = document.layers[which].id
                    invalidate()
                    refreshTimeline()
                    dialog.dismiss()
                }
                .setPositiveButton("Nova camada") { _, _ -> addLayer() }
                .setNegativeButton("Fechar", null)
                .show()
        }

        fun zoom(factor: Float) { viewport.zoomAt(factor, width * .5f, height * .5f); invalidate() }
        fun resetViewport() { viewport.reset(); invalidate() }
        fun insertFrame() { document.insertFrame(document.currentFrame); document.currentFrame = (document.currentFrame + 1).coerceAtMost(document.duration - 1); invalidate(); refreshTimeline() }
        fun duplicateFrame() { document.duplicateFrame(document.currentFrame); document.currentFrame = (document.currentFrame + 1).coerceAtMost(document.duration - 1); invalidate(); refreshTimeline() }
        fun deleteFrame() { if (document.duration <= 1) clearCurrentFrame() else { document.deleteFrame(document.currentFrame); document.currentFrame = document.currentFrame.coerceIn(0, document.duration - 1); invalidate(); refreshTimeline() } }
        fun previousFrame() { stopPlayback(); document.currentFrame = (document.currentFrame - 1).coerceAtLeast(0); invalidate(); refreshTimeline() }
        fun nextFrame() { stopPlayback(); document.currentFrame = (document.currentFrame + 1).coerceAtMost(document.duration - 1); invalidate(); refreshTimeline() }
        fun toggleOnion() { onionEnabled = !onionEnabled; invalidate(); refreshTimeline() }
        fun clearCurrentFrame() { document.activeLayer.frameAt(document.currentFrame)?.strokes?.clear(); invalidate(); refreshTimeline() }
        fun addLayer() { val layer = document.addLayer(); activeLayerId = layer.id; invalidate(); refreshTimeline() }
        fun togglePlayback() { if (playing) stopPlayback() else startPlayback() }
        fun startPlayback() { if (document.duration <= 1) { Toast.makeText(this@MainActivity, "Adicione pelo menos 2 frames para reproduzir", Toast.LENGTH_SHORT).show(); return }; playing = true; lastPlaybackNanos = System.nanoTime(); playbackAccumulator = 0L; Choreographer.getInstance().postFrameCallback(this); refreshTimeline(); invalidate() }
        fun stopPlayback() { if (!playing) return; playing = false; Choreographer.getInstance().removeFrameCallback(this); lastPlaybackNanos = 0L; playbackAccumulator = 0L; refreshTimeline(); invalidate() }
        override fun doFrame(frameTimeNanos: Long) { if (!playing) return; if (lastPlaybackNanos == 0L) lastPlaybackNanos = frameTimeNanos; playbackAccumulator += (frameTimeNanos - lastPlaybackNanos).coerceAtLeast(0L); lastPlaybackNanos = frameTimeNanos; val frameDuration = 1_000_000_000L / document.fps.coerceIn(1, 120); while (playbackAccumulator >= frameDuration) { playbackAccumulator -= frameDuration; document.currentFrame = (document.currentFrame + 1) % document.duration }; invalidate(); refreshTimeline(); Choreographer.getInstance().postFrameCallback(this) }
        fun adjustFps(delta: Int) { document.fps = (document.fps + delta).coerceIn(1, 60); refreshTimeline(); invalidate() }
        fun adjustSize() { BrushToolState.size = when { BrushToolState.size < 8f -> 12f; BrushToolState.size < 24f -> 32f; BrushToolState.size < 64f -> 72f; else -> 6f }; BrushToolState.save(this@MainActivity); Toast.makeText(this@MainActivity, "Tamanho: ${BrushToolState.size.toInt()} px", Toast.LENGTH_SHORT).show(); invalidate() }
        fun adjustOpacity() { BrushToolState.opacity = when { BrushToolState.opacity > .85f -> .65f; BrushToolState.opacity > .55f -> .35f; else -> 1f }; BrushToolState.save(this@MainActivity); Toast.makeText(this@MainActivity, "Opacidade: ${(BrushToolState.opacity * 100).toInt()}%", Toast.LENGTH_SHORT).show(); invalidate() }
        fun refreshTimeline() { timelineLabel?.text = "Frame ${document.currentFrame + 1}/${document.duration}  |  ${document.activeLayer.name}\n${document.fps} FPS  |  Zoom ${(viewport.scale * 100).toInt()}%"; frameStrip?.let { strip -> strip.removeAllViews(); for (index in 0 until document.duration) { val cell = TextView(this@MainActivity).apply { text = "${index + 1}"; textSize = 12f; gravity = android.view.Gravity.CENTER; setTextColor(if (index == document.currentFrame) Color.WHITE else Color.LTGRAY); setBackgroundColor(when { index == document.currentFrame -> accent; hasContent(index) -> Color.rgb(65,72,82); else -> Color.rgb(38,42,48) }); setOnClickListener { stopPlayback(); document.currentFrame = index; invalidate(); refreshTimeline() } }; strip.addView(cell, LinearLayout.LayoutParams(52, 50).apply { setMargins(3,4,3,4) }) } } }
        private fun hasContent(index: Int) = document.layers.any { !it.frameAt(index)?.strokes.isNullOrEmpty() }
        private fun blend(a: Int, b: Int, amount: Float): Int { val t = amount.coerceIn(0f,1f); return Color.rgb((Color.red(a)*(1f-t)+Color.red(b)*t).toInt(), (Color.green(a)*(1f-t)+Color.green(b)*t).toInt(), (Color.blue(a)*(1f-t)+Color.blue(b)*t).toInt()) }
    }
}