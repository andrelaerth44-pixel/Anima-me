package com.animame.editor

import android.graphics.PointF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Generates constrained geometric paths for line, rectangle and ellipse tools. */
object ShapeEngine {
    fun line(a: PointF, b: PointF, samples: Int = 2): List<PointF> = listOf(PointF(a.x, a.y), PointF(b.x, b.y))

    fun rectangle(a: PointF, b: PointF): List<PointF> {
        val l = minOf(a.x, b.x); val r = maxOf(a.x, b.x)
        val t = minOf(a.y, b.y); val bot = maxOf(a.y, b.y)
        return listOf(PointF(l,t), PointF(r,t), PointF(r,bot), PointF(l,bot), PointF(l,t))
    }

    fun ellipse(a: PointF, b: PointF, segments: Int = 72): List<PointF> {
        val cx = (a.x + b.x) * .5f; val cy = (a.y + b.y) * .5f
        val rx = kotlin.math.abs(b.x - a.x) * .5f; val ry = kotlin.math.abs(b.y - a.y) * .5f
        val n = segments.coerceIn(12, 360)
        return (0..n).map { i ->
            val ang = -PI * .5 + i.toDouble() * PI * 2.0 / n
            PointF(cx + cos(ang).toFloat() * rx, cy + sin(ang).toFloat() * ry)
        }
    }
}
