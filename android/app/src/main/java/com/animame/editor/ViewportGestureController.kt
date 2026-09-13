package com.animame.editor

import android.view.MotionEvent
import kotlin.math.hypot

/**
 * Two-finger viewport navigation for the editor canvas.
 * Drawing tools remain responsible for single-pointer strokes; this controller
 * only consumes gestures while two or more pointers are active.
 */
class ViewportGestureController(
    private val state: EditorViewportState,
    private val onChanged: () -> Unit
) {
    private var lastX = 0f
    private var lastY = 0f
    private var lastDistance = 0f
    private var active = false

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount < 2) {
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                resetGesture()
            }
            return false
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_MOVE -> {
                val centerX = (event.getX(0) + event.getX(1)) * .5f
                val centerY = (event.getY(0) + event.getY(1)) * .5f
                val distance = hypot(
                    event.getX(1) - event.getX(0),
                    event.getY(1) - event.getY(0)
                )

                if (!active || lastDistance <= 0f) {
                    lastX = centerX
                    lastY = centerY
                    lastDistance = distance
                    active = true
                    return true
                }

                val dx = centerX - lastX
                val dy = centerY - lastY
                if (dx != 0f || dy != 0f) state.panBy(dx, dy)

                if (distance > 0f) {
                    val factor = distance / lastDistance
                    if (factor.isFinite() && kotlin.math.abs(factor - 1f) > .001f) {
                        state.zoomAt(factor, centerX, centerY)
                    }
                }

                lastX = centerX
                lastY = centerY
                lastDistance = distance
                onChanged()
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                resetGesture()
                return true
            }
        }
        return true
    }

    fun isActive(): Boolean = active

    fun resetGesture() {
        active = false
        lastX = 0f
        lastY = 0f
        lastDistance = 0f
    }
}
