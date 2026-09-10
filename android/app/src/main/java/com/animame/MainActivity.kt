package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.SeekBar
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
    private lateinit var brushPanel: LinearLayout
    private lateinit var brushList: LinearLayout

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
        top.addView(button("Definições", 82) { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) })
        top.addView(button("Pincel", 72) { editor.tool = Tool.BRUSH; editor.invalidate() })
        top.addView(button("Borracha", 82) { editor.tool = Tool.ERASER; editor.invalidate() })
        top.addView(button("Pincéis", 76) { toggleBrushPanel() })
        brushLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            setPadding(8, 0, 8, 0)
            gravity = Gravity.CENTER_VERTICAL
            text = "Basic"
        }
        top.addView(brushLabel, LinearLayout.LayoutParams(145, 54))
        top.addView(button("Tamanho", 78) { editor.adjustSize() })
        top.addView(button("Opacidade", 88) { editor.adjustOpacity() })
        top.addView(button("Camada +", 84) { editor.addLayer() })
        top.addView(button("Zoom -", 66) { editor.zoomOut() })
        top.addView(button("100%", 58) { editor.resetView() })
        top.addView(button("Zoom +", 66) { editor.zoomIn() })
        top.addView(button("Reproduzir", 82) { editor.togglePlayback() })
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
            addView(button("Anterior", 72) { editor.previousFrame() })
            addView(button("Inserir", 64) { editor.insertFrame() })
            addView(button("Duplicar", 70) { editor.duplicateFrame() })
            addView(button("Eliminar", 70) { editor.deleteFrame() })
            addView(button("Seguinte", 72) { editor.nextFrame() })
            addView(button("Onion", 62) { editor.toggleOnion() })
            addView(button("Limpar", 64) { editor.clearCurrentFrame() })
            addView(frameScroll, LinearLayout.LayoutParams(0, 58, 1f))
        }
        val bottom = FrameLayout.LayoutParams(-1, 58).apply { gravity = Gravity.BOTTOM }
        root.addView(controls, bottom)

        brushPanel = buildBrushPanel()
        val panelParams = FrameLayout.LayoutParams(390, -1).apply {
            gravity = Gravity.END
            topMargin = 66
            bottomMargin = 58
        }
        root.addView(brushPanel, panelParams)
        brushPanel.visibility = View.GONE

        setContentView(root)
        editor.timelineLabel = timelineLabel
        editor.frameStrip = frameStrip
        editor.brushLabel = brushLabel
        editor.refreshTimeline()
    }

    private fun buildBrushPanel(): LinearLayout {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(25, 28, 33))
        }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        header.addView(TextView(this).apply {
            text = "Biblioteca de pincéis"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
        }, LinearLayout.LayoutParams(0, 48, 1f))
        header.addView(button("Fechar", 72) { brushPanel.visibility = View.GONE })
        panel.addView(header)

        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL }
            BrushCatalog.categories().forEach { category ->
                row.addView(button(category, 105) { populateBrushes(category) })
            }
            addView(row, HorizontalScrollView.LayoutParams(-2, 54))
        }
        panel.addView(categoryScroll)

        val sizeTitle = TextView(this).apply {
            text = "Tamanho"
            setTextColor(Color.LTGRAY)
            textSize = 12f
        }
        panel.addView(sizeTitle)
        val sizeBar = SeekBar(this).apply {
            max = 4096
            progress = editorBrushSize()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) editor.setBrushSize(value.coerceAtLeast(1).toFloat())
                }
                override fun onStartTrackingTouch(s: SeekBar?) = Unit
                override fun onStopTrackingTouch(s: SeekBar?) = Unit
            })
        }
        panel.addView(sizeBar)

        val opacityTitle = TextView(this).apply {
            text = "Opacidade"
            setTextColor(Color.LTGRAY)
            textSize = 12f
        }
        panel.addView(opacityTitle)
        val opacityBar = SeekBar(this).apply {
            max = 100
            progress = (editorBrushOpacity() * 100f).toInt()
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) editor.setBrushOpacity((value / 100f).coerceIn(.01f, 1f))
                }
                override fun onStartTrackingTouch(s: SeekBar?) = Unit
                override fun onStopTrackingTouch(s: SeekBar?) = Unit
            })
        }
        panel.addView(opacityBar)

        brushList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val listScroll = android.widget.ScrollView(this).apply {
            addView(brushList, android.widget.ScrollView.LayoutParams(-1, -2))
        }
        panel.addView(listScroll, LinearLayout.LayoutParams(-1, 0, 1f))
        populateBrushes(BrushCatalog.categories().firstOrNull() ?: "Simple")
        return panel
    }

    private fun populateBrushes(category: String) {
        if (!::brushList.isInitialized) return
        brushList.removeAllViews()
        BrushCatalog.all().filter { it.category == category }.forEach { preset ->
            val row = Button(this).apply {
                text = preset.name
                textSize = 12f
                setTextColor(Color.WHITE)
                setOnClickListener {
                    editor.selectBrush(preset.id, preset.name)
                    brushPanel.visibility = View.GONE
                }
            }
            brushList.addView(row, LinearLayout.LayoutParams(-1, 48).apply { setMargins(0, 2, 0, 2) })
        }
    }

    private fun editorBrushSize(): Int = 12
    private fun editorBrushOpacity(): Float = 1f

    private fun toggleBrushPanel() {
        brushPanel.visibility = if (brushPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        if (brushPanel.visibility == View.VISIBLE) populateBrushes(BrushCatalog.categories().firstOrNull() ?: "Simple")
    }

    private fun button(label: String, width: Int, action: () -> Unit) = Button(this).apply {
        text = label
        textSize = 11f
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(width, 54)
    }

    private enum class Tool { BRUSH, ERASER }

    private inner class EditorSurface : View(this@MainActivity) {
        private val document = AnimationDocument(duration = 1)
        private val currentSamples = mutableListOf<StrokeSample>()
        private var previewStamps = emptyList<BrushEngine.Stamp>()
        private var accent = ThemeColorStore.DEFAULT
        private var brushSize = 12f
        private var brushOpacity = 1f
        private var drawing = false
        private var onionEnabled = true
        private var playing = false
        private var activeLayerId = document.activeLayer.id
        private var selectedBrushId = "basic"
        private var zoom = 1f
        private var panX = 0f
        private var panY = 0f
        private var transformMode = false
        private var lastMidX = 0f
        private var lastMidY = 0f
        private var lastDistance = 0f
        private var playbackFrameNanos = 0L
        private var playbackAccumulator = 0L
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

        init { refreshTheme(); isFocusable = true }

        fun refreshTheme() {
            accent = ThemeColorStore.get(this@MainActivity)
            bg.color = Color.rgb(18, 20, 23)
            panel.color = blend(accent, Color.rgb(30, 34, 39), .82f)
            paper.color = Color.rgb(245, 245, 245)
        }

        override fun onDraw(c: Canvas) {
            c.drawColor(bg.color)
            val left = width * .07f
            val right = width * .93f
            val top = 80f
            val bottom = height - 72f
            c.save()
            c.translate(panX, panY)
            c.scale(zoom, zoom)
            c.drawRect(left, top, right, bottom, paper)
            drawOnionSkin(c)
            drawDocument(c)
            if (previewStamps.isNotEmpty()) drawStamps(c, previewStamps, accent, 1f)
            c.restore()
            c.drawRect(0f, 0f, width.toFloat(), 66f, panel)
            c.drawText("ANIMA-ME", 78f, 38f, text)
            c.drawText("Frame ${document.currentFrame + 1}/${document.duration}  |  ${document.fps} FPS  |  ${document.layers.size} camada(s)", 78f, 58f, sub)
            c.drawText("${document.activeLayer.name}  |  ${if (playing) "REPRODUZINDO" else "PARADO"}", width - 250f, 38f, sub)
            c.drawText("${(zoom * 100).toInt()}%", width - 75f, height - 70f, sub)
        }

        private fun drawDocument(c: Canvas) {
            document.layers.asReversed().forEach { layer ->
                if (!layer.visible || layer.opacity <= 0f) return@forEach
                layer.frameAt(document.currentFrame)?.strokes?.forEach { stroke ->
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
            frame.strokes.forEach { stroke -> drawStamps(c, BrushEngine.stamps(stroke.samples, stroke.resolvedBrushSettings(), stroke.id.hashCode().toLong()), color, alpha) }
        }

        private fun drawStamps(c: Canvas, stamps: List<BrushEngine.Stamp>, color: Int, layerOpacity: Float) {
            stamps.forEach { stamp ->
                stampPaint.color = Color.argb((stamp.alpha * layerOpacity * 255f).toInt().coerceIn(1, 255), Color.red(color), Color.green(color), Color.blue(color))
                c.drawCircle(stamp.x, stamp.y, stamp.size * .5f, stampPaint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            if (playing) return true
            if (event.pointerCount >= 2) return handleTransform(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (event.y < 70f || event.y > height - 64f) return false
                    currentSamples.clear(); drawing = true; addSample(event); renderPreview(); invalidate(); return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!drawing) return true
                    addSample(event); renderPreview(); invalidate(); return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (drawing && event.actionMasked == MotionEvent.ACTION_UP) commitStroke()
                    drawing = false; currentSamples.clear(); previewStamps = emptyList(); invalidate(); return true
                }
            }
            return true
        }

        private fun handleTransform(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    drawing = false; transformMode = true
                    lastMidX = (event.getX(0) + event.getX(1)) * .5f
                    lastMidY = (event.getY(0) + event.getY(1)) * .5f
                    lastDistance = distance(event); currentSamples.clear(); previewStamps = emptyList(); return true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!transformMode || event.pointerCount < 2) return true
                    val midX = (event.getX(0) + event.getX(1)) * .5f
                    val midY = (event.getY(0) + event.getY(1)) * .5f
                    panX += midX - lastMidX; panY += midY - lastMidY
                    val newDistance = distance(event)
                    if (lastDistance > 0f) zoomAround((newDistance / lastDistance).coerceIn(.85f, 1.18f), midX, midY)
                    lastMidX = midX; lastMidY = midY; lastDistance = newDistance; invalidate(); return true
                }
                MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    transformMode = false; drawing = false; currentSamples.clear(); previewStamps = emptyList(); invalidate(); return true
                }
            }
            return true
        }

        private fun distance(e: MotionEvent): Float = hypot(e.getX(1) - e.getX(0), e.getY(1) - e.getY(0))
        private fun screenToCanvas(x: Float, y: Float): Pair<Float, Float> = Pair((x - panX) / zoom, (y - panY) / zoom)
        private fun zoomAround(factor: Float, x: Float, y: Float) {
            val old = zoom; zoom = (zoom * factor).coerceIn(.35f, 6f); val scale = zoom / old
            panX = x - (x - panX) * scale; panY = y - (y - panY) * scale
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
            previewStamps = BrushEngine.stamps(BrushEngine.smooth(currentSamples, .18f), base.copy(size = brushSize, opacity = brushOpacity), document.currentFrame.toLong())
        }

        private fun commitStroke() {
            if (currentSamples.isEmpty()) return
            val frame = document.activeLayer.ensureFrame(document.currentFrame)
            val color = if (tool == Tool.ERASER) Color.WHITE else accent
            val settings = (if (tool == Tool.ERASER) BrushDefaults.forPreset("eraser") else BrushCatalog.settings(selectedBrushId)).copy(size = brushSize, opacity = brushOpacity)
            frame.strokes += StrokeData(settings.id, color, brushSize, brushOpacity, settings, currentSamples.map { it.copy() }.toMutableList())
        }

        fun selectBrush(id: String, name: String) {
            selectedBrushId = id; brushLabel?.text = name; tool = Tool.BRUSH; invalidate()
        }
        fun setBrushSize(value: Float) { brushSize = value.coerceIn(1f, 4096f); invalidate() }
        fun setBrushOpacity(value: Float) { brushOpacity = value.coerceIn(.01f, 1f); invalidate() }
        fun adjustSize() { setBrushSize(if (brushSize < 8f) 12f else if (brushSize < 24f) 32f else if (brushSize < 64f) 72f else 6f) }
        fun adjustOpacity() { setBrushOpacity(if (brushOpacity > .85f) .65f else if (brushOpacity > .55f) .35f else 1f) }

        fun insertFrame() { document.insertFrame(document.currentFrame); document.currentFrame = (document.currentFrame + 1).coerceAtMost(document.duration - 1); invalidate(); refreshTimeline() }
        fun duplicateFrame() { document.duplicateFrame(document.currentFrame); document.currentFrame = (document.currentFrame + 1).coerceAtMost(document.duration - 1); invalidate(); refreshTimeline() }
        fun deleteFrame() { if (document.duration <= 1) clearCurrentFrame() else { document.deleteFrame(document.currentFrame); document.currentFrame = document.currentFrame.coerceIn(0, document.duration - 1); invalidate(); refreshTimeline() } }
        fun previousFrame() { stopPlayback(); document.currentFrame = (document.currentFrame - 1).coerceAtLeast(0); invalidate(); refreshTimeline() }
        fun nextFrame() { stopPlayback(); document.currentFrame = (document.currentFrame + 1).coerceAtMost(document.duration - 1); invalidate(); refreshTimeline() }
        fun toggleOnion() { onionEnabled = !onionEnabled; invalidate(); refreshTimeline() }
        fun clearCurrentFrame() { document.activeLayer.frameAt(document.currentFrame)?.strokes?.clear(); invalidate(); refreshTimeline() }
        fun addLayer() { val layer = document.addLayer(); activeLayerId = layer.id; invalidate(); refreshTimeline() }

        fun togglePlayback() { if (playing) stopPlayback() else startPlayback() }
        fun startPlayback() {
            if (document.duration <= 1) { Toast.makeText(this@MainActivity, "Adicione pelo menos 2 frames para reproduzir", Toast.LENGTH_SHORT).show(); return }
            playing = true; playbackFrameNanos = System.nanoTime(); playbackAccumulator = 0L; postInvalidateOnAnimation(); schedulePlayback(); refreshTimeline()
        }
        private fun schedulePlayback() { postDelayed({ playbackTick() }, 8L) }
        private fun playbackTick() {
            if (!playing) return
            val now = System.nanoTime(); playbackAccumulator += (now - playbackFrameNanos).coerceAtLeast(0L); playbackFrameNanos = now
            val duration = 1_000_000_000L / document.fps.coerceIn(1, 60)
            while (playbackAccumulator >= duration) { playbackAccumulator -= duration; document.currentFrame = (document.currentFrame + 1) % document.duration }
            invalidate(); refreshTimeline(); schedulePlayback()
        }
        fun stopPlayback() { if (!playing) return; playing = false; playbackFrameNanos = 0L; playbackAccumulator = 0L; refreshTimeline(); invalidate() }
        fun adjustFps(delta: Int) { document.fps = (document.fps + delta).coerceIn(1, 60); refreshTimeline(); invalidate() }

        fun refreshTimeline() {
            timelineLabel?.text = "Frame ${document.currentFrame + 1}/${document.duration}  |  ${document.activeLayer.name}\n${document.fps} FPS  |  ${if (playing) "Reproduzindo" else "Parado"}"
            frameStrip?.let { strip ->
                strip.removeAllViews()
                for (index in 0 until document.duration) {
                    val cell = TextView(this@MainActivity).apply {
                        text = "${index + 1}"; textSize = 12f; gravity = Gravity.CENTER
                        setTextColor(if (index == document.currentFrame) Color.WHITE else Color.LTGRAY)
                        setBackgroundColor(when { index == document.currentFrame -> accent; hasContent(index) -> Color.rgb(65, 72, 82); else -> Color.rgb(38, 42, 48) })
                        setOnClickListener { stopPlayback(); document.currentFrame = index; invalidate(); refreshTimeline() }
                    }
                    strip.addView(cell, LinearLayout.LayoutParams(52, 50).apply { setMargins(3, 4, 3, 4) })
                }
            }
        }
        private fun hasContent(index: Int): Boolean = document.layers.any { layer -> !layer.frameAt(index)?.strokes.isNullOrEmpty() }
        private fun blend(a: Int, b: Int, amount: Float): Int {
            val t = amount.coerceIn(0f, 1f)
            return Color.rgb((Color.red(a) * (1f - t) + Color.red(b) * t).toInt(), (Color.green(a) * (1f - t) + Color.green(b) * t).toInt(), (Color.blue(a) * (1f - t) + Color.blue(b) * t).toInt())
        }
    }
}