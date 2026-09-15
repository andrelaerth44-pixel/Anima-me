package com.animame.editor

import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF

/**
 * Selection rules shared by raster-like strokes and vector-like shapes.
 * This is an original implementation of the behaviors documented by ibisPaint:
 * Set, Add, Subtract, Invert, and intersection-based vector selection.
 */
object LassoSelectionEngine {
    enum class Mode { SET, ADD, SUBTRACT }

    data class Region(
        val path: Path,
        val bounds: RectF,
        val featherRadius: Float = 0f
    )

    data class ItemBounds(val id: String, val bounds: RectF)

    fun combine(current: Set<String>, incoming: Set<String>, mode: Mode): Set<String> = when (mode) {
        Mode.SET -> incoming.toSet()
        Mode.ADD -> current + incoming
        Mode.SUBTRACT -> current - incoming
    }

    fun invert(current: Set<String>, allIds: Collection<String>): Set<String> {
        val all = allIds.toSet()
        return all - current
    }

    /** Select a stroke when any sample lies inside the lasso region. */
    fun selectStrokeIds(
        region: Region,
        strokeSamples: Map<String, List<PointF>>,
        mode: Mode,
        current: Set<String> = emptySet()
    ): Set<String> {
        val incoming = linkedSetOf<String>()
        strokeSamples.forEach { (id, samples) ->
            if (samples.any { contains(region, it.x, it.y) }) incoming += id
        }
        return combine(current, incoming, mode)
    }

    /**
     * Vector-lasso behavior: a shape is selected when its bounds intersect
     * the lasso bounds. The caller can add exact path intersection testing
     * for vector geometry without changing the selection contract.
     */
    fun selectIntersectingShapes(
        region: Region,
        shapes: Collection<ItemBounds>,
        mode: Mode,
        current: Set<String> = emptySet()
    ): Set<String> {
        val incoming = shapes.asSequence()
            .filter { RectF.intersects(region.bounds, it.bounds) }
            .map { it.id }
            .toSet()
        return combine(current, incoming, mode)
    }

    fun contains(region: Region, x: Float, y: Float): Boolean {
        if (!region.bounds.contains(x, y)) return false
        return pointInPolygon(region.path, x, y)
    }

    /** Even-odd point-in-polygon test from the Path's sampled contour. */
    private fun pointInPolygon(path: Path, x: Float, y: Float): Boolean {
        val measure = android.graphics.PathMeasure(path, false)
        val length = measure.length
        if (length <= 0f) return false
        val points = ArrayList<PointF>()
        val step = (length / 256f).coerceAtLeast(2f)
        val pos = FloatArray(2)
        var d = 0f
        while (d <= length) {
            if (measure.getPosTan(d, pos, null)) points += PointF(pos[0], pos[1])
            d += step
        }
        if (points.size < 3) return false
        var inside = false
        var j = points.lastIndex
        for (i in points.indices) {
            val a = points[i]
            val b = points[j]
            val crosses = ((a.y > y) != (b.y > y)) &&
                (x < (b.x - a.x) * (y - a.y) / ((b.y - a.y).takeUnless { kotlin.math.abs(it) < 0.00001f } ?: 0.00001f) + a.x)
            if (crosses) inside = !inside
            j = i
        }
        return inside
    }
}
