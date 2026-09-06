package com.animame.editor

import android.graphics.Path
import android.graphics.PointF
import kotlin.math.max

/** IbisPaint-inspired real-time/after stroke stabilizer. */
object Stabilizer {
    fun smooth(points: List<PointF>, strength: Float): Path {
        val out = Path()
        if (points.isEmpty()) return out
        if (points.size == 1) { out.moveTo(points[0].x, points[0].y); return out }
        val s = strength.coerceIn(0f, 1f)
        val window = max(1, (1f + s * 10f).toInt())
        fun averaged(index: Int): PointF {
            val a = (index - window).coerceAtLeast(0)
            val b = (index + window).coerceAtMost(points.lastIndex)
            var x = 0f; var y = 0f
            for (i in a..b) { x += points[i].x; y += points[i].y }
            val n = (b - a + 1).toFloat()
            return PointF(x / n, y / n)
        }
        val first = averaged(0)
        out.moveTo(first.x, first.y)
        for (i in 1 until points.size) {
            val p = averaged(i)
            val prev = averaged(i - 1)
            val mx = (prev.x + p.x) * 0.5f
            val my = (prev.y + p.y) * 0.5f
            out.quadTo(prev.x, prev.y, mx, my)
        }
        val last = averaged(points.lastIndex)
        out.lineTo(last.x, last.y)
        return out
    }
}
