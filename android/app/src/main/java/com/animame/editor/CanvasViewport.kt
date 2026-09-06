package com.animame.editor

import android.graphics.Matrix
import android.graphics.PointF
import android.view.MotionEvent
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.round

/** Display transform for the canvas. It is independent from the animation camera. */
class CanvasViewport {
    var scale: Float = 1f
        private set
    var rotation: Float = 0f
        private set
    var translationX: Float = 0f
        private set

    var translationY: Float = 0f
        private set

    private var active = false
    private var lastDistance = 0f
    private var lastAngle = 0f
    private var lastMidX = 0f
    private var lastMidY = 0f

    fun reset() {
        scale = 1f
        rotation = 0f
        translationX = 0f
        translationY = 0f
    }

    fun fitToBounds(viewW: Float, viewH: Float, contentW: Float, contentH: Float, padding: Float = 24f) {
        if (viewW <= 0f || viewH <= 0f || contentW <= 0f || contentH <= 0f) return
        val sx = (viewW - padding * 2f) / contentW
        val sy = (viewH - padding * 2f) / contentH
        scale = minOf(sx, sy).coerceIn(0.05f, 32f)
        translationX = 0f
        translationY = 0f
        rotation = 0f
    }

    fun setScaleForSettings(snap: Boolean) {
        if (!snap) return
        val steps = floatArrayOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f, 4f, 8f)
        scale = steps.minByOrNull { kotlin.math.abs(it - scale) } ?: scale
    }

    fun setRotationForSettings(snap: Boolean) {
        if (!snap) return
        rotation = round(rotation / 15f) * 15f
        rotation = normalizeDegrees(rotation)
    }

    fun matrix(pivotX: Float, pivotY: Float): Matrix = Matrix().apply {
        postTranslate(translationX, translationY)
        postScale(scale, scale, pivotX, pivotY)
        postRotate(rotation, pivotX, pivotY)
    }

    fun inversePoint(screenX: Float, screenY: Float, pivotX: Float, pivotY: Float): PointF {
        val p = floatArrayOf(screenX, screenY)
        val inverse = Matrix()
        if (matrix(pivotX, pivotY).invert(inverse)) inverse.mapPoints(p)
        return PointF(p[0], p[1])
    }

    fun beginGesture(e: MotionEvent): Boolean {
        if (e.pointerCount < 2) return false
        val state = readState(e)
        lastDistance = state.distance
        lastAngle = state.angle
        lastMidX = state.midX
        lastMidY = state.midY
        active = true
        return true
    }

    fun updateGesture(e: MotionEvent, allowZoom: Boolean = true, allowRotation: Boolean = true): Boolean {
        if (!active || e.pointerCount < 2) return false
        val state = readState(e)
        if (lastDistance > 0f) {
            if (allowZoom) scale = (scale * (state.distance / lastDistance)).coerceIn(0.05f, 32f)
            if (allowRotation) rotation = normalizeDegrees(rotation + shortestAngle(state.angle - lastAngle))
            translationX += state.midX - lastMidX
            translationY += state.midY - lastMidY
        }
        lastDistance = state.distance
        lastAngle = state.angle
        lastMidX = state.midX
        lastMidY = state.midY
        return true
    }

    fun endGesture() {
        active = false
        lastDistance = 0f
    }

    private data class GestureState(val midX: Float, val midY: Float, val distance: Float, val angle: Float)

    private fun readState(e: MotionEvent): GestureState {
        val x1 = e.getX(0); val y1 = e.getY(0)
        val x2 = e.getX(1); val y2 = e.getY(1)
        return GestureState(
            (x1 + x2) * 0.5f,
            (y1 + y2) * 0.5f,
            hypot(x2 - x1, y2 - y1),
            Math.toDegrees(atan2((y2 - y1).toDouble(), (x2 - x1).toDouble())).toFloat()
        )
    }

    private fun shortestAngle(value: Float): Float {
        var v = value % 360f
        if (v > 180f) v -= 360f
        if (v < -180f) v += 360f
        return v
    }

    private fun normalizeDegrees(value: Float): Float {
        var v = value % 360f
        if (v <= -180f) v += 360f
        if (v > 180f) v -= 360f
        return v
    }
}
