package com.animame.editor

import kotlin.math.max
import kotlin.math.min

data class EditorViewportState(
    var scale: Float = 1f,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f
) {
    fun reset() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    fun zoomAt(factor: Float, focusX: Float, focusY: Float) {
        val old = scale
        val next = min(8f, max(.25f, old * factor))
        if (next == old) return
        offsetX = focusX - (focusX - offsetX) * (next / old)
        offsetY = focusY - (focusY - offsetY) * (next / old)
        scale = next
    }
}
