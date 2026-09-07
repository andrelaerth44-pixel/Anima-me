package com.animame.editor

import android.view.MotionEvent

/** Production editor surface with a reliable interaction layer over V5. */
class AnimationEditorViewV4(context: android.content.Context) : AnimationEditorViewV5(context) {
    private var downX = 0f
    private var downY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount >= 2) return false
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            downX = event.x
            downY = event.y
            if (routeUiTap(downX, downY)) return true
        }
        if (event.actionMasked == MotionEvent.ACTION_UP &&
            kotlin.math.abs(event.x - downX) < 18f && kotlin.math.abs(event.y - downY) < 18f) {
            if (routeUiTap(event.x, event.y)) return true
        }
        return super.onTouchEvent(event)
    }

    private fun routeUiTap(x: Float, y: Float): Boolean {
        // Tool rail. The editor rail occupies the left 62 px and starts at y=50.
        if (y >= 54f && y < height - 112f && x <= 66f) {
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
        // Brush list: selecting a visible row is handled here rather than letting the
        // canvas consume the tap. This makes brush switching deterministic.
        val panel = getPrivate("panel")?.toString()
        if (panel?.endsWith("BRUSHES") == true && x >= width - 300f && y >= 96f && y < height - 112f) {
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
        return false
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
        return java.lang.Enum::class.java.enumConstants[index]
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
