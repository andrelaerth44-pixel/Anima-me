package com.animame.editor

/** State exclusively for canvas viewport navigation; it does not mutate animation data. */
class EditorViewportState2 {
    var zoom: Float = 1f
        private set
    var panX: Float = 0f
        private set
    var panY: Float = 0f
        private set

    fun zoomBy(factor: Float, focusX: Float, focusY: Float) {
        val old = zoom
        val next = (zoom * factor).coerceIn(0.1f, 16f)
        if (next == old) return
        val scale = next / old
        panX = focusX - (focusX - panX) * scale
        panY = focusY - (focusY - panY) * scale
        zoom = next
    }

    fun panBy(dx: Float, dy: Float) {
        panX += dx
        panY += dy
    }

    fun reset() {
        zoom = 1f
        panX = 0f
        panY = 0f
    }
}
