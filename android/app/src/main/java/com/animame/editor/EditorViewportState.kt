package com.animame.editor

/** Editor-only viewport. Never serialized into or copied to the composition camera. */
data class EditorViewportState(
    var zoom: Float = 1f,
    var panX: Float = 0f,
    var panY: Float = 0f,
    var rotationDeg: Float = 0f
) {
    fun zoomBy(delta: Float) { zoom = (zoom + delta).coerceIn(0.1f, 16f) }
    fun zoomByFactor(factor: Float) { zoom = (zoom * factor).coerceIn(0.1f, 16f) }
    fun panBy(dx: Float, dy: Float) { panX += dx; panY += dy }
    fun rotateBy(deltaDeg: Float) { rotationDeg = normalize(rotationDeg + deltaDeg) }
    fun reset() { zoom = 1f; panX = 0f; panY = 0f; rotationDeg = 0f }
    private fun normalize(value: Float): Float { var v = value % 360f; if (v > 180f) v -= 360f; if (v < -180f) v += 360f; return v }
}
