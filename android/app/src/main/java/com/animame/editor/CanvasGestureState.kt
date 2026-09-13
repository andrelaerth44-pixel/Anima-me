package com.animame.editor

import kotlin.math.max
import kotlin.math.min

/** Shared, UI-independent state for professional canvas navigation. */
data class CanvasGestureState(
    var scale: Float = 1f,
    var rotation: Float = 0f,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f
) {
    fun reset() {
        scale = 1f
        rotation = 0f
        offsetX = 0f
        offsetY = 0f
    }

    fun zoomAt(factor: Float, focusX: Float, focusY: Float) {
        val old = scale
        val next = min(16f, max(.1f, old * factor))
        if (next == old) return
        offsetX = focusX - (focusX - offsetX) * (next / old)
        offsetY = focusY - (focusY - offsetY) * (next / old)
        scale = next
    }

    fun panBy(dx: Float, dy: Float) {
        offsetX += dx
        offsetY += dy
    }

    fun rotateBy(degrees: Float) {
        rotation = normalize(rotation + degrees)
    }

    private fun normalize(value: Float): Float {
        var result = value % 360f
        if (result > 180f) result -= 360f
        if (result < -180f) result += 360f
        return result
    }
}
