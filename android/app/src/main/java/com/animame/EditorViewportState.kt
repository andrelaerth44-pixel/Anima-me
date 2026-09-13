package com.animame

data class EditorViewportState(
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

    fun zoomBy(factor: Float, pivotX: Float, pivotY: Float) {
        val old = scale
        val next = (scale * factor).coerceIn(0.25f, 8f)
        if (next == old) return
        val ratio = next / old
        offsetX = pivotX - (pivotX - offsetX) * ratio
        offsetY = pivotY - (pivotY - offsetY) * ratio
        scale = next
    }

    // Public alias used by the editor surface and toolbar.
    fun zoomAt(factor: Float, pivotX: Float, pivotY: Float) = zoomBy(factor, pivotX, pivotY)

    fun panBy(dx: Float, dy: Float) {
        offsetX += dx
        offsetY += dy
    }

    fun rotateBy(degrees: Float) {
        rotation = (rotation + degrees).let { value ->
            var normalized = value % 360f
            if (normalized < 0f) normalized += 360f
            normalized
        }
    }
}
