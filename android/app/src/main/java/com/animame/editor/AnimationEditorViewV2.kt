package com.animame.editor

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class AnimationEditorViewV2(context: Context) : View(context) {
    private enum class Mode { HOME, EDITOR }
    private enum class Panel { OPTIONS, BRUSHES, LAYERS, TIMELINE, CAMERA, RULERS }

    private var mode = Mode.HOME
    private var panel: Panel? = Panel.OPTIONS
    private var dialog = false
    private var project: AnimationProject? = null
    private var fps = 24
    private var cw = 1280
    private var ch = 720
    private var margin = 0
    private var projectName = "Untitled"

    private var selectedBrush = BrushCatalog.presets.first()
    private var brushSize = 12f
    private var opacity = 1f
    private var stabilizer = 55f
    private var ruler = 0
    private var symmetry = 6

    // Canvas viewport: display transform only. Never reused as camera transform.
    private val viewport = CanvasViewport()
    private val settings get() = EditorSettingsStore.current

    // Animation camera: scene transform, independent from viewport gestures.
    private var cameraX = 0f
    private var cameraY = 0f
    private var cameraScale = 1f
    private var cameraRotation = 0f

    private val strokes = mutableListOf<Pair<Path, Paint>>()
    private var current: Path? = null
    private val ui = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG)

    init { setLayerType(View.LAYER_TYPE_SOFTWARE, null) }

    override fun onDraw(c: Canvas) {
        if (mode == Mode.HOME) drawHome(c) else drawEditor(c)
        if (dialog) drawDialog(c)
    }

    private fun base(c: Canvas) {
        ui.style = Paint.Style.FILL
        ui.color = Color.rgb(16, 17, 19)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), ui)
    }

    private fun drawHome(c: Canvas) {
        base(c)
        ui.color = Color.rgb(27, 29, 32)
        c.drawRect(0f, 0f, width.toFloat(), 54f, ui)
        txt(c, "RoughAnimator", 22f, 35f, Color.WHITE, 18f)
        txt(c, "Sort by date", width - 115f, 35f, Color.LTGRAY, 10f)
        if (ProjectStore.projects.isEmpty()) {
            txt(c, "No projects", width / 2f - 35f, height / 2f - 18f, Color.LTGRAY, 14f)
        } else {
            var y = 80f
            ProjectStore.projects.forEach { p ->
                ui.color = Color.rgb(36, 39, 43)
                c.drawRoundRect(24f, y, 300f, y + 150f, 8f, 8f, ui)
                txt(c, p.name, 40f, y + 122f, Color.WHITE, 12f)
                txt(c, "${p.fps} fps", 40f, y + 141f, Color.GRAY, 9f)
                y += 170f
            }
        }
        ui.color = Color.rgb(52, 55, 60)
        c.drawRoundRect(width - 190f, height - 76f, width - 24f, height - 26f, 10f, 10f, ui)
        txt(c, "New project", width - 154f, height - 45f, Color.WHITE, 12f)
    }

    private fun drawDialog(c: Canvas) {
        ui.color = Color.argb(235, 10, 11, 13)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), ui)
        val l = width / 2f - 250f
        val t = height / 2f - 195f
        ui.color = Color.rgb(38, 40, 44)
        c.drawRoundRect(l, t, l + 500f, t + 390f, 12f, 12f, ui)
        txt(c, "New project", l + 24f, t + 35f, Color.WHITE, 17f)
        field(c, "New project name", projectName, l + 24f, t + 68f)
        field(c, "Frames per second", fps.toString(), l + 24f, t + 126f)
        field(c, "Camera size", "${cw} x ${ch}", l + 24f, t + 184f)
        field(c, "Margins", margin.toString(), l + 24f, t + 242f)
        field(c, "Canvas size", "${cw + margin * 2} x ${ch + margin * 2}", l + 24f, t + 300f)
        txt(c, "Cancel", l + 300f, t + 356f, Color.LTGRAY, 11f)
        txt(c, "New project", l + 395f, t + 356f, Color.WHITE, 11f)
    }

    private fun field(c: Canvas, a: String, b: String, x: Float, y: Float) {
        txt(c, a, x, y, Color.GRAY, 8f)
        txt(c, b, x, y + 22f, Color.WHITE, 12f)
        ui.color = Color.rgb(66, 69, 74)
        c.drawRect(x, y + 29f, x + 450f, y + 30f, ui)
    }

    private fun drawEditor(c: Canvas) {
        base(c)
        val w = width.toFloat()
        val h = height.toFloat()
        ui.color = Color.rgb(27, 29, 32)
        c.drawRect(0f, 0f, w, 50f, ui)
        txt(c, "ANIMA-ME", 14f, 32f, Color.WHITE, 15f)
        txt(c, project?.name ?: "Untitled", 116f, 32f, Color.LTGRAY, 12f)
        txt(c, "${project?.fps ?: 24} FPS", w - 116f, 32f, Color.LTGRAY, 12f)

        val right = if (panel == null) 62f else 300f
        val r = canvasRect()
        ui.color = Color.WHITE
        c.drawRect(r, ui)

        c.save()
        c.clipRect(r)
        c.concat(viewport.matrix(r.centerX(), r.centerY()))
        // Animation camera is applied to scene content, after the viewport transform.
        c.translate(cameraX, cameraY)
        c.scale(cameraScale, cameraScale, r.centerX(), r.centerY())
        c.rotate(cameraRotation, r.centerX(), r.centerY())
        strokes.forEach { c.drawPath(it.first, it.second) }
        current?.let { c.drawPath(it, paint()) }
        c.restore()

        drawRulers(c, r)
        tabs(c)
        rail(c)
        if (panel != null) drawPanel(c, w - 300f, h)
        timeline(c, h)
    }

    private fun tabs(c: Canvas) {
        listOf("OPTIONS", "BRUSHES", "LAYERS", "TIMELINE", "CAMERA", "RULERS").forEachIndexed { i, n ->
            val x = 190f + i * 78f
            ui.color = if (panel == Panel.values()[i]) Color.rgb(68, 73, 81) else Color.rgb(43, 46, 50)
            c.drawRoundRect(x, 9f, x + 74f, 41f, 7f, 7f, ui)
            txt(c, n, x + 7f, 29f, Color.WHITE, 8f)
        }
    }

    private fun rail(c: Canvas) {
        ui.color = Color.rgb(31, 33, 36)
        c.drawRect(0f, 50f, 62f, height - 112f, ui)
        val ns = listOf("BR", "PE", "ER", "LA", "FI", "PI", "MV", "TR", "LN", "RE", "EL")
        ns.forEachIndexed { i, n -> txt(c, n, 19f, 78f + i * 41f, Color.WHITE, 9f) }
    }

    private fun drawPanel(c: Canvas, x: Float, h: Float) {
        ui.color = Color.rgb(29, 31, 34)
        c.drawRect(x, 50f, width.toFloat(), h - 112f, ui)
        txt(c, when (panel) {
            Panel.OPTIONS -> "TOOL OPTIONS"
            Panel.BRUSHES -> "BRUSH LIBRARY"
            Panel.LAYERS -> "LAYERS"
            Panel.TIMELINE -> "TIMELINE"
            Panel.CAMERA -> "CAMERA / TRANSFORM"
            Panel.RULERS -> "RULERS"
            null -> ""
        }, x + 14f, 78f, Color.WHITE, 13f)
        when (panel) {
            Panel.OPTIONS -> options(c, x + 14f)
            Panel.BRUSHES -> brushes(c, x + 14f)
            Panel.LAYERS -> layers(c, x + 14f)
            Panel.TIMELINE -> timelinePanel(c, x + 14f)
            Panel.CAMERA -> camera(c, x + 14f)
            Panel.RULERS -> rulers(c, x + 14f)
            null -> Unit
        }
    }

    private fun options(c: Canvas, x: Float) {
        txt(c, "BRUSH  ${selectedBrush.name}", x, 108f, Color.WHITE, 12f)
        txt(c, "SIZE  ${brushSize.toInt()} px", x, 138f, Color.LTGRAY, 11f)
        txt(c, "OPACITY  ${(opacity * 100).toInt()}%", x, 166f, Color.LTGRAY, 11f)
        txt(c, "FLOW  100%", x, 194f, Color.LTGRAY, 11f)
        txt(c, "SPACING  2%", x, 222f, Color.LTGRAY, 11f)
        txt(c, "SMOOTHING  ${stabilizer.toInt()}", x, 250f, Color.LTGRAY, 11f)
        txt(c, "PRESSURE SENSITIVITY", x, 278f, Color.LTGRAY, 10f)
        txt(c, "RANDOMIZE ROTATION", x, 304f, Color.LTGRAY, 10f)
        txt(c, "ONION SKIN", x, 330f, Color.LTGRAY, 10f)
        txt(c, "VIEW ZOOM  ${(viewport.scale * 100).roundToInt()}%", x, 360f, Color.LTGRAY, 10f)
        txt(c, "VIEW ROTATION  ${viewport.rotation.roundToInt()}°", x, 384f, Color.LTGRAY, 10f)
    }

    private fun brushes(c: Canvas, x: Float) {
        var y = 108f
        BrushCatalog.families.forEach { f ->
            txt(c, f, x, y, Color.WHITE, 10f)
            y += 18f
            BrushCatalog.presets.filter { it.family == f }.forEach { p ->
                if (y < height - 135) {
                    txt(c, if (p.id == selectedBrush.id) "• ${p.name}" else "  ${p.name}", x + 6f, y, if (p.id == selectedBrush.id) Color.WHITE else Color.GRAY, 9f)
                    y += 16f
                }
            }
            y += 4f
        }
    }

    private fun layers(c: Canvas, x: Float) {
        listOf("+ NEW LAYER", "EYE   Layer 3", "EYE   Layer 2", "EYE   Layer 1", "EYE   Background", "LOCK   OPACITY   BLEND").forEachIndexed { i, s -> txt(c, s, x, 108f + i * 28f, Color.LTGRAY, 10f) }
    }

    private fun timelinePanel(c: Canvas, x: Float) {
        txt(c, "ONION SKIN   ON", x, 108f, Color.LTGRAY, 10f)
        txt(c, "PREVIOUS 2   NEXT 2", x, 136f, Color.LTGRAY, 10f)
        txt(c, "${project?.fps ?: 24} FPS", x, 164f, Color.LTGRAY, 10f)
    }

    private fun camera(c: Canvas, x: Float) {
        txt(c, "POSITION  ${cameraX.roundToInt()},${cameraY.roundToInt()}", x, 108f, Color.LTGRAY, 10f)
        txt(c, "SCALE  ${(cameraScale * 100).roundToInt()}%", x, 136f, Color.LTGRAY, 10f)
        txt(c, "ROTATION  ${cameraRotation.roundToInt()}°", x, 164f, Color.LTGRAY, 10f)
        txt(c, "FLIP H / FLIP V", x, 192f, Color.LTGRAY, 10f)
        txt(c, "RESET CAMERA", x, 220f, Color.WHITE, 10f)
        txt(c, "VIEWPORT  ${(viewport.scale * 100).roundToInt()}% / ${viewport.rotation.roundToInt()}°", x, 248f, Color.GRAY, 9f)
    }

    private fun rulers(c: Canvas, x: Float) {
        listOf("STRAIGHT RULER", "CIRCULAR RULER", "ELLIPSE RULER", "RADIAL RULER", "MIRROR RULER", "KALEIDOSCOPE RULER", "ROTATION RULER", "ARRAY RULER", "PERSPECTIVE ARRAY RULER").forEachIndexed { i, s ->
            txt(c, if (ruler == i + 1) "• $s" else "  $s", x, 108f + i * 24f, if (ruler == i + 1) Color.WHITE else Color.LTGRAY, 9f)
        }
    }

    private fun drawRulers(c: Canvas, r: RectF) {
        if (ruler == 0) return
        ui.style = Paint.Style.STROKE
        ui.color = Color.rgb(105, 140, 175)
        ui.strokeWidth = 1f
        when (ruler) {
            1 -> c.drawLine(r.left, r.centerY(), r.right, r.centerY(), ui)
            2 -> c.drawCircle(r.centerX(), r.centerY(), min(r.width(), r.height()) * .32f, ui)
            3 -> c.drawOval(r.centerX() - r.width() * .3f, r.centerY() - r.height() * .2f, r.centerX() + r.width() * .3f, r.centerY() + r.height() * .2f, ui)
            4 -> repeat(24) { i -> val a = i * PI / 12; c.drawLine(r.centerX(), r.centerY(), r.centerX() + cos(a).toFloat() * r.width(), r.centerY() + sin(a).toFloat() * r.height(), ui) }
            5 -> c.drawLine(r.centerX(), r.top, r.centerX(), r.bottom, ui)
            6, 7 -> repeat(symmetry) { i -> c.save(); c.rotate(i * 360f / symmetry, r.centerX(), r.centerY()); c.drawLine(r.centerX(), r.centerY(), r.centerX(), r.top, ui); c.restore() }
            8 -> repeat(4) { i -> c.drawLine(r.left + r.width() * (i + 1) / 5f, r.top, r.left + r.width() * (i + 1) / 5f, r.bottom, ui) }
            9 -> { val v = PointF(r.centerX(), r.top - 80f); repeat(9) { i -> val x = r.left + r.width() * i / 8f; c.drawLine(v.x, v.y, x, r.bottom, ui) } }
        }
        ui.style = Paint.Style.FILL
    }

    private fun timeline(c: Canvas, h: Float) {
        ui.color = Color.rgb(27, 29, 32)
        c.drawRect(0f, h - 112f, width.toFloat(), h, ui)
        txt(c, "LAYERS", 12f, h - 82f, Color.LTGRAY, 10f)
        txt(c, "Layer 1    1  2  3  4  5  6  7  8", 70f, h - 82f, Color.WHITE, 10f)
        txt(c, "|<   <   PLAY   >   >|", width - 175f, h - 38f, Color.WHITE, 10f)
    }

    private fun paint() = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeWidth = brushSize
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        alpha = (opacity * 255f).roundToInt().coerceIn(0, 255)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (dialog) return dialogTouch(e)
        if (mode == Mode.HOME) return homeTouch(e)

        if (e.pointerCount >= 2 || viewportGestureActive(e)) {
            if (e.actionMasked == MotionEvent.ACTION_POINTER_DOWN && e.pointerCount >= 2) {
                viewport.beginGesture(e)
                current = null
                return true
            }
            if (e.actionMasked == MotionEvent.ACTION_MOVE && e.pointerCount >= 2) {
                viewport.updateGesture(e)
                applyViewportSnapping()
                invalidate()
                return true
            }
            if (e.actionMasked == MotionEvent.ACTION_POINTER_UP) {
                viewport.endGesture()
                applyViewportSnapping()
                invalidate()
                return true
            }
        }

        val x = e.x
        val y = e.y
        val w = width.toFloat()
        if (e.action == MotionEvent.ACTION_DOWN) {
            if (y < 48f && x >= 190f) {
                val i = ((x - 190f) / 78f).toInt()
                if (i in Panel.values().indices) {
                    panel = if (panel == Panel.values()[i]) null else Panel.values()[i]
                    invalidate()
                    return true
                }
            }
            if (x < 62f && y in 50f..height - 112f) {
                panel = Panel.OPTIONS
                invalidate()
                return true
            }
            if (panel == Panel.BRUSHES && x > w - 300f && y > 90f) {
                selectBrush(y)
                return true
            }
            if (panel == Panel.RULERS && x > w - 300f && y > 90f) {
                val i = ((y - 108f) / 24f).toInt()
                if (i in 0..8) ruler = i + 1
                panel = Panel.OPTIONS
                invalidate()
                return true
            }
            if (panel == Panel.CAMERA && x > w - 300f && y in 195f..235f) {
                cameraX = 0f; cameraY = 0f; cameraScale = 1f; cameraRotation = 0f
                invalidate()
                return true
            }
            val r = canvasRect()
            if (r.contains(x, y)) {
                val p = viewport.inversePoint(x, y, r.centerX(), r.centerY())
                current = Path()
                current!!.moveTo(p.x, p.y)
                return true
            }
        } else if (e.action == MotionEvent.ACTION_MOVE && current != null) {
            val r = canvasRect()
            val p = viewport.inversePoint(x, y, r.centerX(), r.centerY())
            current!!.lineTo(p.x, p.y)
            invalidate()
            return true
        } else if (e.action == MotionEvent.ACTION_UP && current != null) {
            strokes.add(current!! to paint())
            current = null
            invalidate()
            return true
        }
        return true
    }

    private fun viewportGestureActive(e: MotionEvent): Boolean = e.pointerCount >= 2

    private fun applyViewportSnapping() {
        viewport.setScaleForSettings(settings.snapViewZoom)
        viewport.setRotationForSettings(settings.snapViewRotation)
    }

    private fun homeTouch(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP && e.x > width - 210f && e.y > height - 100f) {
            dialog = true
            invalidate()
            return true
        }
        if (e.action == MotionEvent.ACTION_UP && ProjectStore.projects.isNotEmpty() && e.y in 80f..230f) {
            project = ProjectStore.projects.first()
            mode = Mode.EDITOR
            viewport.reset()
            cameraX = 0f; cameraY = 0f; cameraScale = 1f; cameraRotation = 0f
            invalidate()
        }
        return true
    }

    private fun canvasRect() = RectF(72f, 60f, width.toFloat() - (if (panel == null) 62f else 300f) - 10f, height - 122f)

    private fun selectBrush(y: Float) {
        var yy = 108f
        for (f in BrushCatalog.families) {
            yy += 18f
            for (p in BrushCatalog.presets.filter { it.family == f }) {
                if (y in yy - 16f..yy + 2f) {
                    selectedBrush = p
                    panel = Panel.OPTIONS
                    invalidate()
                    return
                }
                yy += 16f
            }
            yy += 4f
        }
    }

    private fun dialogTouch(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_UP) return true
        val l = width / 2f - 250f
        val t = height / 2f - 195f
        if (e.x > l + 380f && e.y > t + 325f) {
            project = ProjectStore.create(projectName, fps, cw, ch, margin)
            mode = Mode.EDITOR
            dialog = false
            viewport.reset()
            cameraX = 0f; cameraY = 0f; cameraScale = 1f; cameraRotation = 0f
            invalidate()
            return true
        }
        if (e.x > l + 270f && e.x < l + 370f && e.y > t + 325f) {
            dialog = false
            invalidate()
            return true
        }
        if (e.y in t + 95f..t + 150f) {
            fps = when (fps) { 12 -> 24; 24 -> 30; 30 -> 60; else -> 12 }
            invalidate()
        }
        if (e.y in t + 153f..t + 208f) {
            cw = when (cw) { 1280 -> 1920; 1920 -> 1080; else -> 1280 }
            ch = when (ch) { 720 -> 1080; 1080 -> 1920; else -> 720 }
            invalidate()
        }
        if (e.y in t + 211f..t + 266f) {
            margin = when (margin) { 0 -> 100; 100 -> 200; else -> 0 }
            invalidate()
        }
        return true
    }

    private fun txt(c: Canvas, s: String, x: Float, y: Float, color: Int, size: Float) {
        text.color = color
        text.textSize = size
        text.typeface = Typeface.create("sans", Typeface.NORMAL)
        c.drawText(s, x, y, text)
    }
}
