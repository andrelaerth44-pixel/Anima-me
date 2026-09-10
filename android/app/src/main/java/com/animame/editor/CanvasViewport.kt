package com.animame.editor

import kotlin.math.max
import kotlin.math.min

/** Pure canvas navigation state. Keeps editor content independent from screen coordinates. */
data class CanvasViewport(
    var zoom: Float = 1f,
    var panX: Float = 0f,
    var panY: Float = 0f
) {
    fun reset() { zoom = 1f; panX = 0f; panY = 0f }

    fun setZoom(value: Float, focusX: Float, focusY: Float) {
        val next = value.coerceIn(0.25f, 8f)
        if (next == zoom) return
        val factor = next / zoom
        panX = focusX - (focusX - panX) * factor
        panY = focusY - (focusY - panY) * factor
        zoom = next
    }

    fun translate(dx: Float, dy: Float) {
        panX += dx
        panY += dy
    }

    fun screenToCanvas(x: Float, y: Float): Pair<Float, Float> =
        Pair((x - panX) / zoom, (y - panY) / zoom)

    fun canvasToScreen(x: Float, y: Float): Pair<Float, Float> =
        Pair(x * zoom + panX, y * zoom + panY)

    fun clampPan(width: Float, height: Float, canvasWidth: Float, canvasHeight: Float) {
        val scaledW = canvasWidth * zoom
        val scaledH = canvasHeight * zoom
        val minX = min(width - scaledW, 0f)
        val maxX = max(width - scaledW, 0f)
        val minY = min(height - scaledH, 0f)
        val maxY = max(height - scaledH, 0f)
        panX = panX.coerceIn(minX, maxX)
        panY = panY.coerceIn(minY, maxY)
    }
}
