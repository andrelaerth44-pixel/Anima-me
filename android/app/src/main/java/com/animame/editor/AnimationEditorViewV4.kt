package com.animame.editor

import android.graphics.PointF
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.roundToInt

/** Production editor interaction layer over V5. */
class AnimationEditorViewV4(context: android.content.Context) : AnimationEditorViewV5(context) {
    private var downX = 0f
    private var downY = 0f
    private var adjusting = false
    private var adjustKind = ""
    private var smoothEnabled = true
    private val rawStroke = mutableListOf<Stabilizer.Sample>()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount >= 2) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                if (routeUiTouch(event.x, event.y, true)) return true
                rawStroke.clear()
                val handled = super.onTouchEvent(event)
                captureAndProcessLive()
                return handled
            }
            MotionEvent.ACTION_MOVE -> {
                if (adjusting) {
                    updateAdjustment(event.x, event.y)
                    return true
                }
                val handled = super.onTouchEvent(event)
                captureAndProcessLive()
                return handled
            }
            MotionEvent.ACTION_UP -> {
                if (adjusting) {
                    updateAdjustment(event.x, event.y)
                    adjusting = false
                    adjustKind = ""
                    invalidate()
                    return true
                }
                val handled = super.onTouchEvent(event)
                captureAndCommitProcessedStroke()
                if (abs(event.x - downX) < 18f && abs(event.y - downY) < 18f) {
                    routeUiTouch(event.x, event.y, false)
                }
                return handled
            }
            MotionEvent.ACTION_CANCEL -> {
                adjusting = false
                adjustKind = ""
                rawStroke.clear()
                return super.onTouchEvent(event)
            }
        }
        return super.onTouchEvent(event)
    }

    private fun captureAndProcessLive() {
        val list = getPrivate("samples") as? MutableList<Stabilizer.Sample> ?: return
        val latest = list.lastOrNull() ?: return
        if (rawStroke.isEmpty() || rawStroke.last().timeMs != latest.timeMs || rawStroke.last().x != latest.x || rawStroke.last().y != latest.y) {
            rawStroke += latest.copy()
        }
        val processed = StrokeProcessingEngine.process(
            rawStroke,
            smoothing = smoothEnabled,
            stabilizerPercent = getPrivate("stabilizer") as? Float ?: 20f,
            realTime = getPrivate("realTimeStabilizer") as? Boolean ?: true
        )
        list.clear()
        list.addAll(processed)
        invalidate()
    }

    private fun captureAndCommitProcessedStroke() {
        val doc = getPrivate("document") as? AnimationDocument ?: return
        val layerId = getPrivate("selectedLayerId") as? String ?: return
        val layer = doc.layers.firstOrNull { it.id == layerId } ?: return
        val frame = layer.frameAt(doc.currentFrame) ?: return
        val stroke = frame.strokes.lastOrNull() ?: return
        if (rawStroke.isEmpty()) return
        stroke.samples.clear()
        stroke.samples.addAll(
            StrokeProcessingEngine.process(
                rawStroke,
                smoothing = smoothEnabled,
                stabilizerPercent = getPrivate("stabilizer") as? Float ?: 20f,
                realTime = getPrivate("realTimeStabilizer") as? Boolean ?: true
            ).map { StrokeSample(it.point.x, it.point.y, it.pressure, it.timeMs, it.tilt, it.orientation) }
        )
        rawStroke.clear()
        invalidate()
    }

    private fun routeUiTouch(x: Float, y: Float, down: Boolean): Boolean {
        // Tool rail. Eleven visible buttons are laid out from the top of the rail.
        if (x <= 66f && y >= 54f && y < height - 112f) {
            val i = ((y - 57f) / 41f).toInt()
            if (i in 0..9) {
                setPrivate("tool", enumValue("com.animame.editor.AnimationEditorViewV5$Tool", i))
                invalidate()
                return true
            }
        }

        // Top panel tabs.
        if (y in 5f..47f && x >= 186f && x < 675f) {
            val i = ((x - 190f) / 78f).toInt()
            if (i in 0..5) {
                setPrivate("panel", enumValue("com.animame.editor.AnimationEditorViewV5$Panel", i))
                invalidate()
                return true
            }
        }

        // Brush library rows.
        if (getPrivate("panel")?.toString()?.endsWith("BRUSHES") == true &&
            x >= width - 300f && y >= 96f && y < height - 112f) {
            val chosen = brushAtRow(y)
            if (chosen != null) {
                setPrivate("selectedBrush", chosen)
                setPrivate("brushSettings", chosen.defaults.copy())
                setPrivate("brushSize", chosen.defaults.size)
                setPrivate("opacity", chosen.defaults.opacity)
                invalidate()
                return true
            }
        }

        // Options panel: tap/drag controls. These are deliberately independent from
        // the Stabilizer control so Smooth can be disabled without changing stabilization.
        if (getPrivate("panel")?.toString()?.endsWith("OPTIONS") == true && x >= width - 300f) {
            if (y in 132f..158f) {
                adjusting = down
                adjustKind = "size"
                updateAdjustment(x, y)
                return true
            }
            if (y in 158f..184f) {
                adjusting = down
                adjustKind = "opacity"
                updateAdjustment(x, y)
                return true
            }
            if (y in 316f..344f) {
                // Smooth is a true on/off mode; it never aliases the stabilizer value.
                if (down) smoothEnabled = !smoothEnabled
                invalidate()
                return true
            }
            if (y in 344f..370f) {
                adjusting = down
                adjustKind = "stabilizer"
                updateAdjustment(x, y)
                return true
            }
            if (y in 370f..398f) {
                if (down) {
                    val aa = getPrivate("antiAlias") as? Boolean ?: true
                    setPrivate("antiAlias", !aa)
                    invalidate()
                }
                return true
            }
        }

        // Timeline panel onion opacity: 0..100, matching the document model.
        if (getPrivate("panel")?.toString()?.endsWith("TIMELINE") == true && x >= width - 300f && y in 120f..190f) {
            adjusting = down
            adjustKind = "onion"
            updateAdjustment(x, y)
            return true
        }
        return false
    }

    private fun updateAdjustment(x: Float, y: Float) {
        val left = width - 285f
        val right = width - 30f
        val t = ((x - left) / (right - left)).coerceIn(0f, 1f)
        when (adjustKind) {
            "size" -> setPrivate("brushSize", 1f + t * 255f)
            "opacity" -> setPrivate("opacity", t)
            "stabilizer" -> setPrivate("stabilizer", t * 100f)
            "onion" -> {
                val doc = getPrivate("document") as? AnimationDocument ?: return
                doc.onion.opacity = (t * 100f).roundToInt()
            }
        }
        invalidate()
    }

    private fun brushAtRow(y: Float): BrushPreset? {
        var rowY = 108f
        for (family in BrushCatalog.families) {
            rowY += 18f
            for (p in BrushCatalog.presets.filter { it.family == family }) {
                if (y >= rowY - 13f && y < rowY + 7f) return p
                rowY += 16f
            }
            rowY += 4f
        }
        return null
    }

    private fun enumValue(className: String, index: Int): Any {
        val c = Class.forName(className)
        val values = c.enumConstants ?: error("No enum constants for $className")
        return values[index]
    }

    private fun getPrivate(name: String): Any? {
        val f = AnimationEditorViewV5::class.java.getDeclaredField(name)
        f.isAccessible = true
        return f.get(this)
    }

    private fun setPrivate(name: String, value: Any?) {
        val f = AnimationEditorViewV5::class.java.getDeclaredField(name)
        f.isAccessible = true
        f.set(this, value)
    }
}
