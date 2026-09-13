package com.animame

import kotlin.math.atan2
import kotlin.math.hypot

class EditorViewportGestureState {
    val viewport = EditorViewportState()

    private var lastDistance = 0f
    private var lastAngle = 0f
    private var lastMidX = 0f
    private var lastMidY = 0f

    fun begin(x1: Float, y1: Float, x2: Float, y2: Float) {
        lastDistance = distance(x1, y1, x2, y2)
        lastAngle = angle(x1, y1, x2, y2)
        lastMidX = (x1 + x2) * .5f
        lastMidY = (y1 + y2) * .5f
    }

    fun update(x1: Float, y1: Float, x2: Float, y2: Float, pivotX: Float, pivotY: Float) {
        val distance = distance(x1, y1, x2, y2)
        val midX = (x1 + x2) * .5f
        val midY = (y1 + y2) * .5f
        if (lastDistance > 0f && distance > 0f) {
            viewport.zoomBy(distance / lastDistance, pivotX, pivotY)
        }
        viewport.panBy(midX - lastMidX, midY - lastMidY)
        if (lastDistance > 0f) {
            var delta = Math.toDegrees((angle(x1, y1, x2, y2) - lastAngle).toDouble()).toFloat()
            while (delta > 180f) delta -= 360f
            while (delta < -180f) delta += 360f
            viewport.rotateBy(delta)
        }
        lastDistance = distance
        lastAngle = angle(x1, y1, x2, y2)
        lastMidX = midX
        lastMidY = midY
    }

    fun end() {
        lastDistance = 0f
        lastAngle = 0f
        lastMidX = 0f
        lastMidY = 0f
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float) = hypot(x2 - x1, y2 - y1)
    private fun angle(x1: Float, y1: Float, x2: Float, y2: Float) = atan2(y2 - y1, x2 - x1)
}
