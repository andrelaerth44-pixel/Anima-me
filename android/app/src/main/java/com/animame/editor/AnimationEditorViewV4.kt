package com.animame.editor

import android.content.Context
import android.graphics.*
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

/**
 * Functional Android editor surface. The document model is frame/layer based;
 * viewport gestures and the animation camera remain completely independent.
 */
class AnimationEditorViewV4(context: Context) : View(context) {
    private enum class Mode { HOME, EDITOR }
    private enum class Panel { OPTIONS, BRUSHES, LAYERS, TIMELINE, CAMERA, RULERS }
    private enum class Tool { BRUSH, ERASER, FILL, LASSO, MOVE, LINE, RECT, ELLIPSE, ZOOM, HAND, ROTATE }

    private var mode = Mode.HOME
    private var panel: Panel? = Panel.OPTIONS
    private var dialog = false
    private var nameInput = "Untitled"
    private var fpsInput = 24
    private var widthInput = 1280
    private var heightInput = 720
    private var marginInput = 0

    private var document = AnimationDocument()
    private var selectedLayerId = document.layers.first().id
    private var tool = Tool.BRUSH
    private var selectedBrush = BrushCatalog.presets.first()
    private var brushSize = 12f
    private var brushOpacity = 1f
    private var smoothing = 55f
    private var realtimeSmoothing = true
    private var antiAlias = true
    private var color = Color.BLACK
    private var ruler = 0
    private var symmetry = 6

    private val viewport = CanvasViewport()
    private val settings get() = EditorSettingsStore.current
    private val ui = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pathCache = HashMap<String, Path>()

    private var drawingSamples = mutableListOf<Stabilizer.Sample>()
    private var strokeStart = PointF()
    private var currentPreview: Path? = null
    private var lastTime = 0L
    private var lastPoint = PointF()

    private data class Snapshot(val layerId: String, val frame: Int, val drawing: DrawingFrame?)
    private val undo = ArrayDeque<Snapshot>()
    private val redo = ArrayDeque<Snapshot>()

    private var playing = false
    private val playTick = object : Runnable {
        override fun run() {
            if (!playing) return
            document.currentFrame++
            if (document.currentFrame > document.playbackEnd) document.currentFrame = document.playbackStart
            invalidate()
            postDelayed(this, (1000L / document.fps.coerceAtLeast(1)))
        }
    }

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDetachedFromWindow() {
        playing = false
        removeCallbacks(playTick)
        super.onDetachedFromWindow()
    }

    override fun onDraw(c: Canvas) {
        if (mode == Mode.HOME) drawHome(c) else drawEditor(c)
        if (dialog) drawNewProjectDialog(c)
    }

    private fun background(c: Canvas) {
        ui.style = Paint.Style.FILL
        ui.color = Color.rgb(16, 17, 19)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), ui)
    }

    private fun drawHome(c: Canvas) {
        background(c)
        ui.color = Color.rgb(27, 29, 32)
        c.drawRect(0f, 0f, width.toFloat(), 54f, ui)
        text(c, "ANIMA-ME", 22f, 35f, Color.WHITE, 18f)
        text(c, "PROJECTS", width - 100f, 35f, Color.LTGRAY, 10f)
        if (ProjectStore.projects.isEmpty()) {
            text(c, "No projects", width / 2f - 36f, height / 2f, Color.LTGRAY, 14f)
        } else {
            var y = 78f
            ProjectStore.projects.forEach { p ->
                ui.color = Color.rgb(37, 40, 44)
                c.drawRoundRect(24f, y, 330f, y + 140f, 8f, 8f, ui)
                text(c, p.name, 42f, y + 95f, Color.WHITE, 13f)
                text(c, "${p.cameraWidth} x ${p.cameraHeight}  ${p.fps} fps", 42f, y + 118f, Color.GRAY, 9f)
                y += 156f
            }
        }
        ui.color = Color.rgb(58, 62, 68)
        c.drawRoundRect(width - 190f, height - 76f, width - 24f, height - 26f, 10f, 10f, ui)
        text(c, "New project", width - 154f, height - 45f, Color.WHITE, 12f)
    }

    private fun drawNewProjectDialog(c: Canvas) {
        ui.color = Color.argb(235, 8, 9, 11)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), ui)
        val l = width / 2f - 270f
        val t = height / 2f - 220f
        ui.color = Color.rgb(39, 41, 45)
        c.drawRoundRect(l, t, l + 540f, t + 440f, 12f, 12f, ui)
        text(c, "New project", l + 24f, t + 38f, Color.WHITE, 17f)
        field(c, "Project name", nameInput, l + 24f, t + 68f)
        field(c, "Frames per second", fpsInput.toString(), l + 24f, t + 128f)
        field(c, "Camera width", widthInput.toString(), l + 24f, t + 188f)
        field(c, "Camera height", heightInput.toString(), l + 24f, t + 248f)
        field(c, "Margins", marginInput.toString(), l + 24f, t + 308f)
        text(c, "Cancel", l + 315f, t + 400f, Color.LTGRAY, 11f)
        text(c, "Create", l + 430f, t + 400f, Color.WHITE, 11f)
    }

    private fun field(c: Canvas, label: String, value: String, x: Float, y: Float) {
        text(c, label, x, y, Color.GRAY, 8f)
        text(c, value, x, y + 21f, Color.WHITE, 12f)
        ui.color = Color.rgb(68, 71, 76)
        c.drawRect(x, y + 29f, x + 490f, y + 30f, ui)
    }

    private fun drawEditor(c: Canvas) {
        background(c)
        val w = width.toFloat()
        val h = height.toFloat()
        ui.color = Color.rgb(27, 29, 32)
        c.drawRect(0f, 0f, w, 50f, ui)
        text(c, "ANIMA-ME", 14f, 32f, Color.WHITE, 15f)
        text(c, document.name, 112f, 32f, Color.LTGRAY, 12f)
        text(c, "${document.currentFrame + 1} / ${document.duration}", w - 185f, 32f, Color.LTGRAY, 11f)
        text(c, "${document.fps} FPS", w - 88f, 32f, Color.LTGRAY, 11f)

        val r = canvasRect()
        ui.color = Color.WHITE
        c.drawRect(r, ui)
        c.save()
        c.clipRect(r)
        c.concat(viewport.matrix(r.centerX(), r.centerY()))
        c.translate(document.camera.x, document.camera.y)
        c.scale(document.camera.scale, document.camera.scale, r.centerX(), r.centerY())
        c.rotate(document.camera.rotation, r.centerX(), r.centerY())
        drawAnimationContent(c, r)
        c.restore()

        drawRuler(c, r)
        drawTopTabs(c)
        drawToolRail(c)
        drawPanel(c, w - 300f, h)
        drawTimeline(c, h)
    }

    private fun drawAnimationContent(c: Canvas, r: RectF) {
        if (document.onion.enabled) drawOnion(c)
        document.layers.asReversed().forEach { layer ->
            if (!layer.visible) return@forEach
            val frame = layer.frameAt(document.currentFrame) ?: return@forEach
            drawFrame(c, frame, layer.opacity, false)
        }
        currentPreview?.let { p ->
            val paint = makePaint()
            c.drawPath(p, paint)
        }
    }

    private fun drawOnion(c: Canvas) {
        val layer = document.layers.firstOrNull { it.id == selectedLayerId } ?: return
        val previous = layer.previousFrames(document.currentFrame, document.onion.previousCount)
        val next = layer.nextFrames(document.currentFrame, document.onion.nextCount)
        previous.reversed().forEachIndexed { i, pair ->
            drawFrame(c, pair.second, layer.opacity, true, onionPaint(true, i + 1))
        }
        next.forEachIndexed { i, pair ->
            drawFrame(c, pair.second, layer.opacity, true, onionPaint(false, i + 1))
        }
    }

    private fun onionPaint(previous: Boolean, distance: Int): Paint {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val base = if (previous) Color.rgb(235, 72, 72) else Color.rgb(72, 145, 245)
        val a = (document.onion.opacity * 2.55f / (distance + 1)).roundToInt().coerceIn(8, 130)
        p.color = Color.argb(a, Color.red(base), Color.green(base), Color.blue(base))
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        return p
    }

    private fun drawFrame(c: Canvas, frame: DrawingFrame, layerOpacity: Float, onion: Boolean, overridePaint: Paint? = null) {
        frame.strokes.forEach { stroke ->
            val path = pathFor(stroke)
            val p = overridePaint ?: makePaint(stroke)
            if (!onion) p.alpha = (Color.alpha(p.color) * layerOpacity).roundToInt().coerceIn(0, 255)
            c.drawPath(path, p)
        }
    }

    private fun pathFor(stroke: StrokeData): Path = pathCache.getOrPut(stroke.id) {
        Stabilizer.smoothSamples(stroke.samples.map { Stabilizer.Sample(PointF(it.x, it.y), it.pressure, it.timeMs) }, 35f, false)
    }

    private fun makePaint(stroke: StrokeData? = null): Paint {
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.isAntiAlias = antiAlias
        p.color = stroke?.color ?: color
        p.strokeWidth = stroke?.size ?: brushSize
        p.alpha = ((stroke?.opacity ?: brushOpacity) * 255f).roundToInt().coerceIn(1, 255)
        if (tool == Tool.ERASER || stroke?.brushId == "eraser") {
            p.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        return p
    }

    private fun drawTopTabs(c: Canvas) {
        val names = listOf("OPTIONS", "BRUSHES", "LAYERS", "TIMELINE", "CAMERA", "RULERS")
        names.forEachIndexed { i, n ->
            val x = 188f + i * 79f
            ui.color = if (panel == Panel.values()[i]) Color.rgb(70, 75, 83) else Color.rgb(43, 46, 50)
            c.drawRoundRect(x, 9f, x + 75f, 41f, 7f, 7f, ui)
            text(c, n, x + 7f, 29f, Color.WHITE, 8f)
        }
    }

    private fun drawToolRail(c: Canvas) {
        ui.color = Color.rgb(31, 33, 36)
        c.drawRect(0f, 50f, 62f, height - 112f, ui)
        val names = listOf("BR", "ER", "FI", "LS", "MV", "LN", "RC", "EL", "ZM", "HD", "RT")
        names.forEachIndexed { i, n ->
            val active = tool.ordinal == i
            text(c, if (active) "[$n]" else n, 14f, 80f + i * 39f, if (active) Color.WHITE else Color.LTGRAY, 9f)
        }
    }

    private fun drawPanel(c: Canvas, x: Float, h: Float) {
        if (panel == null) return
        ui.color = Color.rgb(29, 31, 34)
        c.drawRect(x, 50f, width.toFloat(), h - 112f, ui)
        val title = when (panel) {
            Panel.OPTIONS -> "TOOL OPTIONS"
            Panel.BRUSHES -> "BRUSH LIBRARY"
            Panel.LAYERS -> "LAYERS"
            Panel.TIMELINE -> "TIMELINE"
            Panel.CAMERA -> "CAMERA / TRANSFORM"
            Panel.RULERS -> "RULERS"
            null -> ""
        }
        text(c, title, x + 14f, 78f, Color.WHITE, 13f)
        when (panel) {
            Panel.OPTIONS -> drawOptions(c, x + 14f)
            Panel.BRUSHES -> drawBrushes(c, x + 14f)
            Panel.LAYERS -> drawLayers(c, x + 14f)
            Panel.TIMELINE -> drawTimelineOptions(c, x + 14f)
            Panel.CAMERA -> drawCamera(c, x + 14f)
            Panel.RULERS -> drawRulers(c, x + 14f)
            null -> Unit
        }
    }

    private fun drawOptions(c: Canvas, x: Float) {
        text(c, "TOOL  ${tool.name}", x, 108f, Color.WHITE, 11f)
        text(c, "BRUSH  ${selectedBrush.name}", x, 134f, Color.LTGRAY, 10f)
        text(c, "SIZE  ${brushSize.roundToInt()} px", x, 162f, Color.LTGRAY, 10f)
        text(c, "OPACITY  ${(brushOpacity * 100).roundToInt()}%", x, 188f, Color.LTGRAY, 10f)
        text(c, "SMOOTHING  ${smoothing.roundToInt()}%", x, 214f, Color.LTGRAY, 10f)
        text(c, "STABILIZER  ${if (realtimeSmoothing) "REAL TIME" else "AFTER"}", x, 240f, Color.LTGRAY, 10f)
        text(c, "ANTI-ALIAS  ${if (antiAlias) "ON" else "OFF"}", x, 266f, Color.LTGRAY, 10f)
        text(c, "PRESSURE  ON", x, 292f, Color.LTGRAY, 10f)
        text(c, "VIEW  ${(viewport.scale * 100).roundToInt()}%  ${viewport.rotation.roundToInt()}°", x, 318f, Color.GRAY, 9f)
        text(c, "PINCH ZOOM  ${if (settings.allowPinchZoom) "ON" else "OFF"}", x, 344f, Color.GRAY, 9f)
        text(c, "PINCH ROTATION  ${if (settings.allowPinchRotation) "ON" else "OFF"}", x, 368f, Color.GRAY, 9f)
        text(c, "UNDO ${undo.size}    REDO ${redo.size}", x, 398f, Color.GRAY, 9f)
    }

    private fun drawBrushes(c: Canvas, x: Float) {
        var y = 108f
        BrushCatalog.families.forEach { family ->
            if (y > height - 135f) return@forEach
            text(c, family, x, y, Color.WHITE, 10f); y += 17f
            BrushCatalog.presets.filter { it.family == family }.forEach { b ->
                if (y > height - 135f) return@forEach
                val selected = b.id == selectedBrush.id
                text(c, if (selected) "• ${b.name}" else "  ${b.name}", x + 6f, y, if (selected) Color.WHITE else Color.GRAY, 9f)
                y += 16f
            }
            y += 4f
        }
    }

    private fun drawLayers(c: Canvas, x: Float) {
        text(c, "+ NEW LAYER", x, 108f, Color.WHITE, 10f)
        document.layers.forEachIndexed { i, layer ->
            val y = 140f + i * 30f
            val selected = layer.id == selectedLayerId
            text(c, if (selected) "• ${layer.name}" else "  ${layer.name}", x, y, if (selected) Color.WHITE else Color.LTGRAY, 10f)
            text(c, "${if (layer.visible) "EYE" else "OFF"}  ${if (layer.locked) "LOCK" else ""}", x + 150f, y, Color.GRAY, 8f)
        }
        text(c, "DELETE    UP    DOWN", x, 140f + document.layers.size * 30f + 12f, Color.GRAY, 9f)
    }

    private fun drawTimelineOptions(c: Canvas, x: Float) {
        text(c, if (playing) "STOP" else "PLAY", x, 108f, Color.WHITE, 10f)
        text(c, "ADD FRAME   DUPLICATE   DELETE", x, 136f, Color.LTGRAY, 9f)
        text(c, "ONION SKIN  ${if (document.onion.enabled) "ON" else "OFF"}", x, 166f, Color.LTGRAY, 10f)
        text(c, "PREV ${document.onion.previousCount}   NEXT ${document.onion.nextCount}", x, 192f, Color.LTGRAY, 10f)
        text(c, "OPACITY ${document.onion.opacity}%", x, 218f, Color.GRAY, 9f)
        text(c, "RANGE ${document.playbackStart + 1} - ${document.playbackEnd + 1}", x, 246f, Color.GRAY, 9f)
        text(c, "FPS ${document.fps}", x, 272f, Color.GRAY, 9f)
    }

    private fun drawCamera(c: Canvas, x: Float) {
        text(c, "POSITION  ${document.camera.x.roundToInt()}, ${document.camera.y.roundToInt()}", x, 108f, Color.LTGRAY, 10f)
        text(c, "SCALE  ${(document.camera.scale * 100).roundToInt()}%", x, 136f, Color.LTGRAY, 10f)
        text(c, "ROTATION  ${document.camera.rotation.roundToInt()}°", x, 164f, Color.LTGRAY, 10f)
        text(c, "RESET CAMERA", x, 196f, Color.WHITE, 10f)
        text(c, "KEY CAMERA AT FRAME", x, 224f, Color.WHITE, 10f)
        text(c, "VIEWPORT IS INDEPENDENT", x, 252f, Color.GRAY, 9f)
    }

    private fun drawRulers(c: Canvas, x: Float) {
        val names = listOf("STRAIGHT", "CIRCULAR", "ELLIPSE", "RADIAL", "MIRROR", "KALEIDOSCOPE", "ROTATION", "ARRAY", "PERSPECTIVE ARRAY")
        names.forEachIndexed { i, n ->
            text(c, if (ruler == i + 1) "• $n" else "  $n", x, 108f + i * 24f, if (ruler == i + 1) Color.WHITE else Color.LTGRAY, 9f)
        }
    }

    private fun drawRuler(c: Canvas, r: RectF) {
        if (ruler == 0) return
        ui.style = Paint.Style.STROKE
        ui.isAntiAlias = true
        ui.strokeWidth = 1f
        ui.color = Color.rgb(95, 135, 170)
        when (ruler) {
            1 -> c.drawLine(r.left, r.centerY(), r.right, r.centerY(), ui)
            2 -> c.drawCircle(r.centerX(), r.centerY(), min(r.width(), r.height()) * .32f, ui)
            3 -> c.drawOval(r.centerX() - r.width() * .3f, r.centerY() - r.height() * .2f, r.centerX() + r.width() * .3f, r.centerY() + r.height() * .2f, ui)
            4 -> repeat(24) { i ->
                val a = i * PI / 12.0
                c.drawLine(r.centerX(), r.centerY(), r.centerX() + cos(a).toFloat() * r.width(), r.centerY() + sin(a).toFloat() * r.height(), ui)
            }
            5 -> c.drawLine(r.centerX(), r.top, r.centerX(), r.bottom, ui)
            6, 7 -> repeat(symmetry) { i ->
                c.save(); c.rotate(i * 360f / symmetry, r.centerX(), r.centerY()); c.drawLine(r.centerX(), r.centerY(), r.centerX(), r.top, ui); c.restore()
            }
            8 -> repeat(4) { i ->
                val x = r.left + r.width() * (i + 1) / 5f
                c.drawLine(x, r.top, x, r.bottom, ui)
            }
            9 -> {
                val vx = r.centerX(); val vy = r.top - 80f
                repeat(9) { i -> val x = r.left + r.width() * i / 8f; c.drawLine(vx, vy, x, r.bottom, ui) }
            }
        }
        ui.style = Paint.Style.FILL
    }

    private fun drawTimeline(c: Canvas, h: Float) {
        val top = h - 112f
        ui.color = Color.rgb(27, 29, 32)
        c.drawRect(0f, top, width.toFloat(), h, ui)
        text(c, "TIMELINE", 10f, top + 18f, Color.WHITE, 10f)
        text(c, "|<", width - 220f, top + 22f, Color.WHITE, 9f)
        text(c, "<", width - 192f, top + 22f, Color.WHITE, 9f)
        text(c, if (playing) "STOP" else "PLAY", width - 158f, top + 22f, Color.WHITE, 9f)
        text(c, ">", width - 112f, top + 22f, Color.WHITE, 9f)
        text(c, ">|", width - 86f, top + 22f, Color.WHITE, 9f)

        val left = 90f
        val cell = 34f
        document.layers.take(3).forEachIndexed { li, layer ->
            val y = top + 42f + li * 22f
            text(c, layer.name.take(10), 8f, y + 11f, Color.LTGRAY, 8f)
            for (f in 0 until min(document.duration, 20)) {
                val x = left + f * cell
                ui.color = if (f == document.currentFrame && layer.id == selectedLayerId) Color.rgb(100, 106, 116) else Color.rgb(50, 53, 58)
                c.drawRect(x, y, x + cell - 2f, y + 18f, ui)
                if (layer.frames.containsKey(f)) {
                    ui.color = Color.rgb(210, 210, 210)
                    c.drawCircle(x + 9f, y + 9f, 3f, ui)
                }
            }
        }
    }

    private fun canvasRect(): RectF = RectF(72f, 60f, width.toFloat() - if (panel == null) 72f else 310f, height.toFloat() - 122f)

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (dialog) return dialogTouch(e)
        if (mode == Mode.HOME) return homeTouch(e)
        if (e.pointerCount >= 2) {
            if (drawingSamples.isNotEmpty()) finishStroke()
            when (e.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> viewport.beginGesture(e)
                MotionEvent.ACTION_MOVE -> { viewport.updateGesture(e); invalidate() }
                MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> viewport.endGesture()
            }
            return true
        }
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (handleUi(e.x, e.y)) return true
                if (canvasRect().contains(e.x, e.y)) beginStroke(e)
            }
            MotionEvent.ACTION_MOVE -> if (drawingSamples.isNotEmpty()) addSample(e)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (drawingSamples.isNotEmpty()) finishStroke()
        }
        return true
    }

    private fun homeTouch(e: MotionEvent): Boolean {
        if (e.actionMasked != MotionEvent.ACTION_UP) return true
        if (e.x > width - 210f && e.y > height - 100f) { dialog = true; invalidate(); return true }
        if (ProjectStore.projects.isNotEmpty() && e.y in 70f..250f) {
            val p = ProjectStore.projects.first()
            document = AnimationDocument(p.name, p.cameraWidth, p.cameraHeight, p.fps)
            document.normalize()
            selectedLayerId = document.layers.first().id
            mode = Mode.EDITOR
            viewport.reset()
            invalidate()
        }
        return true
    }

    private fun dialogTouch(e: MotionEvent): Boolean {
        if (e.actionMasked != MotionEvent.ACTION_UP) return true
        val l = width / 2f - 270f
        val t = height / 2f - 220f
        if (e.x in l + 280f..l + 390f && e.y in t + 365f..t + 430f) { dialog = false; invalidate(); return true }
        if (e.x in l + 400f..l + 535f && e.y in t + 365f..t + 430f) {
            val p = ProjectStore.create(nameInput.ifBlank { "Untitled" }, fpsInput.coerceIn(1, 240), widthInput.coerceAtLeast(1), heightInput.coerceAtLeast(1), marginInput.coerceAtLeast(0))
            document = AnimationDocument(p.name, p.canvasWidth, p.canvasHeight, p.fps)
            document.normalize()
            selectedLayerId = document.layers.first().id
            dialog = false; mode = Mode.EDITOR; invalidate(); return true
        }
        return true
    }

    private fun handleUi(x: Float, y: Float): Boolean {
        if (y < 48f && x >= 188f) {
            val i = ((x - 188f) / 79f).toInt()
            if (i in Panel.values().indices) { panel = if (panel == Panel.values()[i]) null else Panel.values()[i]; invalidate(); return true }
        }
        if (x < 62f && y in 50f..height - 112f) {
            val i = ((y - 55f) / 39f).toInt()
            if (i in Tool.values().indices) { tool = Tool.values()[i]; invalidate(); return true }
        }
        val right = width - 300f
        if (panel == Panel.BRUSHES && x >= right && y > 90f) { selectBrush(y); return true }
        if (panel == Panel.RULERS && x >= right && y > 90f) {
            val i = ((y - 108f) / 24f).toInt(); if (i in 0..8) ruler = i + 1; invalidate(); return true
        }
        if (panel == Panel.LAYERS && x >= right && y > 90f) { layerTouch(y); return true }
        if (panel == Panel.TIMELINE && x >= right && y > 90f) { timelinePanelTouch(y); return true }
        if (panel == Panel.CAMERA && x >= right && y > 90f) { cameraPanelTouch(y); return true }
        if (y >= height - 112f) { timelineTouch(x, y); return true }
        return false
    }

    private fun selectBrush(y: Float) {
        var cursor = 108f
        BrushCatalog.families.forEach { family ->
            if (y >= cursor && y < cursor + 18f) return
            cursor += 21f
            for (b in BrushCatalog.presets.filter { it.family == family }) {
                if (y >= cursor && y < cursor + 16f) { selectedBrush = b; tool = if (b.id == "eraser") Tool.ERASER else Tool.BRUSH; invalidate(); return }
                cursor += 16f
            }
            cursor += 4f
        }
    }

    private fun layerTouch(y: Float) {
        if (y in 90f..122f) { selectedLayerId = document.addLayer().id; invalidate(); return }
        val index = ((y - 126f) / 30f).toInt()
        if (index in document.layers.indices) { selectedLayerId = document.layers[index].id; invalidate(); return }
        val base = 140f + document.layers.size * 30f
        if (y > base) {
            val layer = document.layers.firstOrNull { it.id == selectedLayerId } ?: return
            document.deleteLayer(layer.id); selectedLayerId = document.layers.first().id; invalidate()
        }
    }

    private fun timelinePanelTouch(y: Float) {
        when {
            y in 90f..122f -> togglePlayback()
            y in 122f..151f -> addFrame()
            y in 151f..182f -> document.onion.enabled = !document.onion.enabled
            y in 182f..212f -> document.onion.previousCount = (document.onion.previousCount + 1).coerceAtMost(8)
            y in 212f..242f -> document.onion.nextCount = (document.onion.nextCount + 1).coerceAtMost(8)
        }
        invalidate()
    }

    private fun cameraPanelTouch(y: Float) {
        if (y in 176f..214f) { document.camera.x = 0f; document.camera.y = 0f; document.camera.scale = 1f; document.camera.rotation = 0f }
        invalidate()
    }

    private fun timelineTouch(x: Float, y: Float) {
        val top = height - 112f
        if (y < top + 36f) {
            when {
                x in width - 180f..width - 135f -> togglePlayback()
                x in width - 135f..width - 95f -> document.currentFrame = (document.currentFrame + 1).coerceAtMost(document.duration - 1)
                x in width - 220f..width - 180f -> document.currentFrame = (document.currentFrame - 1).coerceAtLeast(0)
            }
            invalidate(); return
        }
        val left = 90f; val cell = 34f
        if (x >= left) {
            val frame = floor((x - left) / cell).toInt().coerceIn(0, document.duration - 1)
            document.currentFrame = frame
            invalidate()
        }
    }

    private fun beginStroke(e: MotionEvent) {
        val p = screenToDocument(e.x, e.y)
        strokeStart = p
        lastPoint = p
        lastTime = e.eventTime
        drawingSamples.clear()
        drawingSamples += Stabilizer.Sample(p, pressureOf(e), e.eventTime)
        currentPreview = if (tool == Tool.BRUSH || tool == Tool.ERASER) Path().apply { moveTo(p.x, p.y) } else null
        snapshotForUndo()
        invalidate()
    }

    private fun addSample(e: MotionEvent) {
        val p = screenToDocument(e.x, e.y)
        val pressure = pressureOf(e)
        drawingSamples += Stabilizer.Sample(p, pressure, e.eventTime)
        lastPoint = p
        lastTime = e.eventTime
        if (tool == Tool.BRUSH || tool == Tool.ERASER) {
            currentPreview = Stabilizer.smoothSamples(drawingSamples, smoothing, realtimeSmoothing)
        } else {
            currentPreview = shapePreview(strokeStart, p)
        }
        invalidate()
    }

    private fun finishStroke() {
        if (drawingSamples.isEmpty()) return
        val layer = document.layers.firstOrNull { it.id == selectedLayerId } ?: document.activeLayer
        if (layer.locked) { drawingSamples.clear(); currentPreview = null; return }
        val frame = layer.ensureFrame(document.currentFrame)
        val points = drawingSamples.map { it.point }
        val isShape = tool == Tool.LINE || tool == Tool.RECT || tool == Tool.ELLIPSE
        if (isShape) {
            val shapeSamples = listOf(
                StrokeSample(strokeStart.x, strokeStart.y, 1f, drawingSamples.first().timeMs),
                StrokeSample(lastPoint.x, lastPoint.y, 1f, drawingSamples.last().timeMs)
            )
            frame.strokes += StrokeData(brushId = selectedBrush.id, color = color, size = brushSize, opacity = brushOpacity, samples = shapeSamples.toMutableList())
        } else if (points.size >= 1) {
            frame.strokes += StrokeData(brushId = if (tool == Tool.ERASER) "eraser" else selectedBrush.id, color = color, size = brushSize, opacity = brushOpacity, samples = drawingSamples.map { StrokeSample(it.point.x, it.point.y, it.pressure, it.timeMs) }.toMutableList())
        }
        frame.exposure = maxOf(frame.exposure, 1)
        pathCache.clear()
        drawingSamples.clear(); currentPreview = null
        redo.clear()
        invalidate()
    }

    private fun shapePreview(a: PointF, b: PointF): Path {
        val p = Path()
        when (tool) {
            Tool.LINE -> { p.moveTo(a.x, a.y); p.lineTo(b.x, b.y) }
            Tool.RECT -> p.addRect(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y), Path.Direction.CW)
            Tool.ELLIPSE -> p.addOval(min(a.x, b.x), min(a.y, b.y), max(a.x, b.x), max(a.y, b.y), Path.Direction.CW)
            else -> Unit
        }
        return p
    }

    private fun snapshotForUndo() {
        val layer = document.layers.firstOrNull { it.id == selectedLayerId } ?: return
        val frame = layer.frameAt(document.currentFrame)
        val copy = frame?.let { deepCopyFrame(it) }
        undo.addLast(Snapshot(layer.id, document.currentFrame, copy))
        while (undo.size > 80) undo.removeFirst()
    }

    private fun deepCopyFrame(src: DrawingFrame): DrawingFrame = DrawingFrame(exposure = src.exposure).also { dst ->
        src.strokes.forEach { s -> dst.strokes += s.copy(samples = s.samples.map { it.copy() }.toMutableList()) }
    }

    fun undoLast() {
        val s = if (undo.isEmpty()) null else undo.removeLast() ?: return
        val layer = document.layers.firstOrNull { it.id == s.layerId } ?: return
        val current = layer.frameAt(s.frame)?.let { deepCopyFrame(it) }
        redo.addLast(Snapshot(s.layerId, s.frame, current))
        if (s.drawing == null) layer.frames.remove(s.frame) else layer.frames[s.frame] = deepCopyFrame(s.drawing)
        pathCache.clear(); invalidate()
    }

    fun redoLast() {
        val s = if (redo.isEmpty()) null else redo.removeLast() ?: return
        val layer = document.layers.firstOrNull { it.id == s.layerId } ?: return
        val current = layer.frameAt(s.frame)?.let { deepCopyFrame(it) }
        undo.addLast(Snapshot(s.layerId, s.frame, current))
        if (s.drawing == null) layer.frames.remove(s.frame) else layer.frames[s.frame] = deepCopyFrame(s.drawing)
        pathCache.clear(); invalidate()
    }

    private fun addFrame() {
        document.insertFrame(document.currentFrame)
        document.currentFrame = (document.currentFrame + 1).coerceAtMost(document.duration - 1)
        invalidate()
    }

    private fun togglePlayback() {
        playing = !playing
        if (playing) post(playTick) else removeCallbacks(playTick)
        invalidate()
    }

    private fun pressureOf(e: MotionEvent): Float = e.getPressure(0).coerceIn(0.05f, 1.5f)

    private fun screenToDocument(sx: Float, sy: Float): PointF {
        val r = canvasRect()
        var x = sx
        var y = sy
        x -= document.camera.x
        y -= document.camera.y
        val cx = r.centerX(); val cy = r.centerY()
        val a = Math.toRadians((-document.camera.rotation).toDouble())
        val dx = x - cx; val dy = y - cy
        val rx = (dx * cos(a) - dy * sin(a)) / document.camera.scale
        val ry = (dx * sin(a) + dy * cos(a)) / document.camera.scale
        x = rx + cx; y = ry + cy
        return viewport.inversePoint(x, y, cx, cy)
    }
}
