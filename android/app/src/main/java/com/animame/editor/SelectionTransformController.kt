package com.animame.editor

import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

/** Selection state kept separate from viewport/camera state. */
class SelectionTransformController {
    enum class Mode { NONE, LASSO, MOVE, TRANSFORM }
    enum class CombineMode { SET, ADD, SUBTRACT }

    var mode: Mode = Mode.NONE
        private set

    var combineMode: CombineMode = CombineMode.SET
    var featherRadius: Float = 0f
    var rotateEnabled: Boolean = false

    val path = Path()
    private val points = mutableListOf<PointF>()
    private var armedLasso = false
    var bounds = RectF()
        private set
    var offsetX = 0f
        private set
    var offsetY = 0f
        private set
    var scaleX = 1f
        private set
    var scaleY = 1f
        private set
    var rotation = 0f
        private set
    var inverted: Boolean = false
        private set

    fun activateLasso(mode: CombineMode = combineMode) {
        combineMode = mode
        this.mode = Mode.LASSO
        armedLasso = false
        points.clear()
        path.reset()
        bounds.setEmpty()
    }

    fun beginLasso(x: Float, y: Float) {
        mode = Mode.LASSO
        armedLasso = true
        points.clear()
        path.reset()
        path.moveTo(x, y)
        points += PointF(x, y)
    }

    fun addLassoPoint(x: Float, y: Float) {
        if (mode != Mode.LASSO) return
        armedLasso = false
        val last = points.lastOrNull()
        if (last != null && hypot(last.x - x, last.y - y) < 1.5f) return
        path.lineTo(x, y)
        points += PointF(x, y)
    }

    fun finishLasso(): Boolean {
        if (mode != Mode.LASSO || points.size < 3) return false
        armedLasso = false
        path.close()
        var left = Float.POSITIVE_INFINITY
        var top = Float.POSITIVE_INFINITY
        var right = Float.NEGATIVE_INFINITY
        var bottom = Float.NEGATIVE_INFINITY
        points.forEach {
            left = min(left, it.x); top = min(top, it.y)
            right = max(right, it.x); bottom = max(bottom, it.y)
        }
        bounds = RectF(left, top, right, bottom)
        mode = Mode.TRANSFORM
        return true
    }

    fun beginMove() {
        if (!bounds.isEmpty) mode = Mode.MOVE
    }

    fun beginTransform() {
        if (!bounds.isEmpty) mode = Mode.TRANSFORM
    }

    fun moveBy(dx: Float, dy: Float) {
        if (mode != Mode.MOVE && mode != Mode.TRANSFORM) return
        offsetX += dx
        offsetY += dy
        bounds.offset(dx, dy)
        path.offset(dx, dy)
    }

    fun scaleBy(factor: Float, pivotX: Float, pivotY: Float) {
        if (mode != Mode.TRANSFORM) return
        val f = factor.coerceIn(.05f, 20f)
        scaleX *= f
        scaleY *= f
        bounds.set(
            pivotX + (bounds.left - pivotX) * f,
            pivotY + (bounds.top - pivotY) * f,
            pivotX + (bounds.right - pivotX) * f,
            pivotY + (bounds.bottom - pivotY) * f
        )
    }

    fun rotateBy(degrees: Float) {
        if (mode != Mode.TRANSFORM || !rotateEnabled) return
        rotation += degrees
    }

    fun setCombineMode(value: CombineMode) {
        combineMode = value
        if (mode == Mode.NONE) mode = Mode.LASSO
    }

    fun invertSelection() {
        inverted = !inverted
    }

    fun doneTransform() {
        if (mode == Mode.MOVE || mode == Mode.TRANSFORM) mode = Mode.NONE
    }

    fun clear() {
        val keepLassoArmed = mode == Mode.LASSO && armedLasso
        mode = if (keepLassoArmed) Mode.LASSO else Mode.NONE
        armedLasso = false
        points.clear()
        path.reset()
        bounds.setEmpty()
        offsetX = 0f
        offsetY = 0f
        scaleX = 1f
        scaleY = 1f
        rotation = 0f
        inverted = false
    }

    private fun hypot(dx: Float, dy: Float): Float = kotlin.math.sqrt(dx * dx + dy * dy)
}
