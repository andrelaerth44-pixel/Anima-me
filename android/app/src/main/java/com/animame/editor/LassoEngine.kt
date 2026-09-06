package com.animame.editor

import android.graphics.PointF
import kotlin.math.abs

/** Pure lasso geometry used by the editor selection tool. */
object LassoEngine {
    fun normalize(points: List<PointF>, close: Boolean = true): List<PointF> {
        if (points.isEmpty()) return emptyList()
        val out = ArrayList<PointF>(points.size + 1)
        var last: PointF? = null
        points.forEach { p ->
            if (last == null || abs(p.x - last!!.x) > 0.5f || abs(p.y - last!!.y) > 0.5f) {
                out += PointF(p.x, p.y)
                last = p
            }
        }
        if (close && out.size > 2) {
            val first = out.first(); val end = out.last()
            if (abs(first.x - end.x) > 0.5f || abs(first.y - end.y) > 0.5f) out += PointF(first.x, first.y)
        }
        return out
    }

    fun contains(points: List<PointF>, point: PointF): Boolean {
        val polygon = normalize(points, true)
        if (polygon.size < 4) return false
        var inside = false
        var j = polygon.lastIndex
        for (i in polygon.indices) {
            val a = polygon[i]; val b = polygon[j]
            val crosses = (a.y > point.y) != (b.y > point.y)
            if (crosses) {
                val x = (b.x - a.x) * (point.y - a.y) / ((b.y - a.y).takeUnless { abs(it) < 0.000001f } ?: 0.000001f) + a.x
                if (point.x < x) inside = !inside
            }
            j = i
        }
        return inside
    }

    fun bounds(points: List<PointF>): FloatArray {
        if (points.isEmpty()) return floatArrayOf(0f, 0f, 0f, 0f)
        var l = points[0].x; var t = points[0].y; var r = l; var b = t
        for (p in points.drop(1)) { l = minOf(l, p.x); t = minOf(t, p.y); r = maxOf(r, p.x); b = maxOf(b, p.y) }
        return floatArrayOf(l, t, r, b)
    }
}
