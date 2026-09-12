package com.animame.editor

import android.graphics.RectF
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class SelectionTransformController {
    enum class Mode { NONE, RECT_SELECT, LASSO, TRANSFORM }

    var mode: Mode = Mode.NONE
        private set
    val selectedStrokeIds: MutableSet<String> = linkedSetOf()
    val selectionBounds: RectF = RectF()
    val marqueeBounds: RectF = RectF()
    val lassoPoints: MutableList<Pair<Float, Float>> = mutableListOf()
    var isDragging: Boolean = false
        private set

    fun setMode(value: Mode) {
        mode = value
        if (value != Mode.RECT_SELECT && value != Mode.LASSO) {
            marqueeBounds.setEmpty()
            lassoPoints.clear()
        }
    }

    fun clear() {
        selectedStrokeIds.clear()
        selectionBounds.setEmpty()
        marqueeBounds.setEmpty()
        lassoPoints.clear()
        isDragging = false
        mode = Mode.NONE
    }

    fun selectAll(strokes: List<StrokeData>) {
        selectedStrokeIds.clear()
        selectedStrokeIds.addAll(strokes.map { it.id })
        recalculateBounds(strokes)
        mode = if (selectedStrokeIds.isEmpty()) Mode.NONE else Mode.TRANSFORM
    }

    fun beginRect(x: Float, y: Float) {
        mode = Mode.RECT_SELECT
        marqueeBounds.set(x, y, x, y)
        isDragging = true
    }

    fun updateRect(x: Float, y: Float) {
        if (!isDragging) return
        marqueeBounds.set(min(marqueeBounds.left, x), min(marqueeBounds.top, y), max(marqueeBounds.left, x), max(marqueeBounds.top, y))
    }

    fun finishRect(strokes: List<StrokeData>) {
        if (!isDragging) return
        isDragging = false
        selectedStrokeIds.clear()
        val r = RectF(marqueeBounds)
        strokes.forEach { stroke -> if (stroke.samples.any { r.contains(it.x, it.y) }) selectedStrokeIds += stroke.id }
        recalculateBounds(strokes)
        marqueeBounds.setEmpty()
        mode = if (selectedStrokeIds.isEmpty()) Mode.NONE else Mode.TRANSFORM
    }

    fun beginLasso(x: Float, y: Float) {
        mode = Mode.LASSO
        lassoPoints.clear()
        lassoPoints += x to y
        isDragging = true
    }

    fun updateLasso(x: Float, y: Float) { if (isDragging) lassoPoints += x to y }

    fun finishLasso(strokes: List<StrokeData>) {
        if (!isDragging) return
        isDragging = false
        selectedStrokeIds.clear()
        if (lassoPoints.size >= 3) strokes.forEach { stroke -> if (stroke.samples.any { pointInPolygon(it.x, it.y, lassoPoints) }) selectedStrokeIds += stroke.id }
        recalculateBounds(strokes)
        mode = if (selectedStrokeIds.isEmpty()) Mode.NONE else Mode.TRANSFORM
    }

    fun move(strokes: List<StrokeData>, dx: Float, dy: Float) {
        selected(strokes).forEach { stroke ->
            for (i in stroke.samples.indices) {
                val p = stroke.samples[i]
                stroke.samples[i] = p.copy(x = p.x + dx, y = p.y + dy)
            }
        }
        selectionBounds.offset(dx, dy)
    }

    fun scale(strokes: List<StrokeData>, factor: Float, pivotX: Float = selectionBounds.centerX(), pivotY: Float = selectionBounds.centerY()) {
        val f = factor.coerceIn(.05f, 20f)
        selected(strokes).forEach { stroke ->
            for (i in stroke.samples.indices) {
                val p = stroke.samples[i]
                stroke.samples[i] = p.copy(x = pivotX + (p.x - pivotX) * f, y = pivotY + (p.y - pivotY) * f)
            }
        }
        recalculateBounds(strokes)
    }

    fun rotate(strokes: List<StrokeData>, degrees: Float, pivotX: Float = selectionBounds.centerX(), pivotY: Float = selectionBounds.centerY()) {
        val radians = Math.toRadians(degrees.toDouble())
        val co = cos(radians).toFloat()
        val si = sin(radians).toFloat()
        selected(strokes).forEach { stroke ->
            for (i in stroke.samples.indices) {
                val p = stroke.samples[i]
                val x = p.x - pivotX
                val y = p.y - pivotY
                stroke.samples[i] = p.copy(x = pivotX + x * co - y * si, y = pivotY + x * si + y * co)
            }
        }
        recalculateBounds(strokes)
    }

    fun flipHorizontal(strokes: List<StrokeData>) {
        selected(strokes).forEach { stroke ->
            val cx = selectionBounds.centerX()
            for (i in stroke.samples.indices) {
                val p = stroke.samples[i]
                stroke.samples[i] = p.copy(x = cx - (p.x - cx))
            }
        }
        recalculateBounds(strokes)
    }

    fun flipVertical(strokes: List<StrokeData>) {
        selected(strokes).forEach { stroke ->
            val cy = selectionBounds.centerY()
            for (i in stroke.samples.indices) {
                val p = stroke.samples[i]
                stroke.samples[i] = p.copy(y = cy - (p.y - cy))
            }
        }
        recalculateBounds(strokes)
    }

    fun deleteSelected(strokes: MutableList<StrokeData>) {
        strokes.removeAll { it.id in selectedStrokeIds }
        clear()
    }

    fun recalculateBounds(strokes: List<StrokeData>) {
        val points = selected(strokes).flatMap { it.samples }
        if (points.isEmpty()) { selectionBounds.setEmpty(); return }
        var l = Float.POSITIVE_INFINITY
        var t = Float.POSITIVE_INFINITY
        var r = Float.NEGATIVE_INFINITY
        var b = Float.NEGATIVE_INFINITY
        points.forEach { l = min(l, it.x); t = min(t, it.y); r = max(r, it.x); b = max(b, it.y) }
        selectionBounds.set(l, t, r, b)
    }

    fun selected(strokes: List<StrokeData>): List<StrokeData> = strokes.filter { it.id in selectedStrokeIds }

    private fun pointInPolygon(x: Float, y: Float, polygon: List<Pair<Float, Float>>): Boolean {
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val xi = polygon[i].first; val yi = polygon[i].second
            val xj = polygon[j].first; val yj = polygon[j].second
            val denom = if (yj - yi == 0f) 0.000001f else yj - yi
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / denom + xi) inside = !inside
            j = i
        }
        return inside
    }
}
