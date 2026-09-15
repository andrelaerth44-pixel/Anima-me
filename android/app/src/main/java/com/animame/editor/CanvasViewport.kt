package com.animame.editor

import android.graphics.PointF
import kotlin.math.max
import kotlin.math.min

/** Viewport state shared by the drawing surface: zoom and pan around a stable focal point. */
class CanvasViewport {
    var scale: Float = 1f
        private set
    var panX: Float = 0f
        private set
    var panY: Float = 0f
        private set

    fun zoomBy(factor: Float, focalX: Float, focalY: Float) {
        val old = scale
        val next = (old * factor).coerceIn(0.5f, 8f)
        if (next == old) return
        panX = focalX - (focalX - panX) * (next / old)
        panY = focalY - (focalY - panY) * (next / old)
        scale = next
    }

    fun panBy(dx: Float, dy: Float) {
        panX += dx
        panY += dy
    }

    fun reset() {
        scale = 1f
        panX = 0f
        panY = 0f
    }

    fun screenToCanvas(x: Float, y: Float): PointF =
        PointF((x - panX) / scale, (y - panY) / scale)

    fun canvasToScreen(x: Float, y: Float): PointF =
        PointF(x * scale + panX, y * scale + panY)

    fun clampPan(viewWidth: Float, viewHeight: Float, contentWidth: Float, contentHeight: Float) {
        if (scale <= 1f) return
        val scaledW = contentWidth * scale
        val scaledH = contentHeight * scale
        val minX = min(0f, viewWidth - scaledW)
        val minY = min(0f, viewHeight - scaledH)
        panX = if (scaledW <= viewWidth) (viewWidth - scaledW) * .5f else panX.coerceIn(minX, 0f)
        panY = if (scaledH <= viewHeight) (viewHeight - scaledH) * .5f else panY.coerceIn(minY, 0f)
    }

    fun zoomPercent(): Int = (scale * 100f).toInt()
}
