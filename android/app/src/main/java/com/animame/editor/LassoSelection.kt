package com.animame.editor

import kotlin.math.abs

data class SelectionBounds(val left: Float, val top: Float, val right: Float, val bottom: Float)

object LassoSelection {
    fun contains(points: List<Pair<Float, Float>>, x: Float, y: Float): Boolean {
        if (points.size < 3) return false
        var inside = false
        var j = points.lastIndex
        for (i in points.indices) {
            val xi = points[i].first
            val yi = points[i].second
            val xj = points[j].first
            val yj = points[j].second
            val intersects = ((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / ((yj - yi).let { if (abs(it) < 0.0001f) 0.0001f else it }) + xi)
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }

    fun bounds(points: List<Pair<Float, Float>>): SelectionBounds? {
        if (points.isEmpty()) return null
        var left = points[0].first
        var right = left
        var top = points[0].second
        var bottom = top
        points.drop(1).forEach {
            left = minOf(left, it.first)
            right = maxOf(right, it.first)
            top = minOf(top, it.second)
            bottom = maxOf(bottom, it.second)
        }
        return SelectionBounds(left, top, right, bottom)
    }

    fun strokeSelected(samples: List<StrokeSample>, polygon: List<Pair<Float, Float>>): Boolean {
        if (samples.isEmpty() || polygon.size < 3) return false
        val stride = maxOf(1, samples.size / 24)
        return samples.indices.step(stride).any { i ->
            contains(polygon, samples[i].x, samples[i].y)
        }
    }
}
