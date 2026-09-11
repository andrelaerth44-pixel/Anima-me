package com.animame.editor

import kotlin.math.max
import kotlin.math.min

/**
 * Transform state for the editor canvas. It deliberately contains only viewport state;
 * document coordinates remain unchanged while the user zooms or pans.
 */
data class EditorViewportState(
    var scale: Float = 1f,
    var offsetX: Float = 0f,
    var offsetY: Float = 0f,
    val minScale: Float = 0.25f,
    val maxScale: Float = 8f
) {
    fun normalized(): EditorViewportState {
        scale = scale.coerceIn(minScale, maxScale)
        return this
    }

    fun zoomAround(screenX: Float, screenY: Float, factor: Float) {
        val oldScale = scale
        val newScale = (oldScale * factor).coerceIn(minScale, maxScale)
        if (newScale == oldScale) return
        val worldX = (screenX - offsetX) / oldScale
        val worldY = (screenY - offsetY) / oldScale
        scale = newScale
        offsetX = screenX - worldX * newScale
        offsetY = screenY - worldY * newScale
    }

    fun panBy(dx: Float, dy: Float) {
        offsetX += dx
        offsetY += dy
    }

    fun reset() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    fun screenToWorld(x: Float, y: Float): Pair<Float, Float> {
        val s = max(scale, 0.0001f)
        return Pair((x - offsetX) / s, (y - offsetY) / s)
    }

    fun worldToScreen(x: Float, y: Float): Pair<Float, Float> {
        return Pair(x * scale + offsetX, y * scale + offsetY)
    }
}
