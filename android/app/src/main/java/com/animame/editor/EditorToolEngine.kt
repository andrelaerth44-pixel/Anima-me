package com.animame.editor

import android.graphics.Bitmap
import android.graphics.PointF
import kotlin.math.cos
import kotlin.math.sin

/** Shared, document-level behavior for the professional editor tools. */
class EditorToolEngine {
    enum class ToolType { BRUSH, ERASER, BUCKET, LASSO, TRANSFORM, EYEDROPPER, LIQUIFY }

    data class Transform(
        val translateX: Float = 0f,
        val translateY: Float = 0f,
        val scaleX: Float = 1f,
        val scaleY: Float = 1f,
        val rotationDeg: Float = 0f
    )

    data class Selection(val strokeIds: Set<String> = emptySet()) {
        fun contains(stroke: StrokeData) = stroke.id in strokeIds
    }

    fun lassoSelect(frame: DrawingFrame, polygon: List<PointF>): Selection {
        if (polygon.size < 3) return Selection()
        return Selection(frame.strokes.filter { stroke ->
            stroke.samples.any { sample -> pointInPolygon(PointF(sample.x, sample.y), polygon) }
        }.map { it.id }.toSet())
    }

    fun transform(frame: DrawingFrame, selection: Selection, transform: Transform): Int {
        if (selection.strokeIds.isEmpty()) return 0
        val angle = Math.toRadians(transform.rotationDeg.toDouble())
        val c = cos(angle).toFloat()
        val s = sin(angle).toFloat()
        var changed = 0
        frame.strokes.forEach { stroke ->
            if (!selection.contains(stroke) || stroke.samples.isEmpty()) return@forEach
            val cx = stroke.samples.map { it.x }.average().toFloat()
            val cy = stroke.samples.map { it.y }.average().toFloat()
            stroke.samples.forEachIndexed { i, p ->
                val x = (p.x - cx) * transform.scaleX
                val y = (p.y - cy) * transform.scaleY
                stroke.samples[i] = p.copy(
                    x = cx + x * c - y * s + transform.translateX,
                    y = cy + x * s + y * c + transform.translateY
                )
            }
            changed++
        }
        return changed
    }

    fun pickColor(bitmap: Bitmap, x: Float, y: Float): Int? {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return null
        val ix = x.toInt().coerceIn(0, bitmap.width - 1)
        val iy = y.toInt().coerceIn(0, bitmap.height - 1)
        return bitmap.getPixel(ix, iy)
    }

    fun erase(frame: DrawingFrame, selection: Selection): Int {
        if (selection.strokeIds.isEmpty()) return 0
        val before = frame.strokes.size
        frame.strokes.removeAll { it.id in selection.strokeIds }
        return before - frame.strokes.size
    }

    private fun pointInPolygon(point: PointF, polygon: List<PointF>): Boolean {
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val a = polygon[i]
            val b = polygon[j]
            val dy = b.y - a.y
            val intersects = (a.y > point.y) != (b.y > point.y) &&
                point.x < (b.x - a.x) * (point.y - a.y) / (if (dy == 0f) Float.MIN_VALUE else dy) + a.x
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }
}
