package com.animame.editor

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

/**
 * Silhouettes for stamp-style brushes (stars, hearts, paw prints, leaves...).
 * Everything here is drawn procedurally with Path/trig — no imported artwork.
 * Continuous brushes (pens, pencils, watercolor, airbrush...) keep using
 * CIRCLE, which is the correct look for a soft round dab.
 */
enum class StampShape { CIRCLE, SQUARE, TRIANGLE, DIAMOND, PENTAGON, HEXAGON, STAR, CROSS, HEART, DROP, LEAF, SNOWFLAKE, PAW }

object StampShapes {
    fun draw(c: Canvas, shape: StampShape, cx: Float, cy: Float, radius: Float, angle: Float, paint: Paint, filled: Boolean) {
        if (radius <= 0f) return
        val savedStyle = paint.style
        val savedWidth = paint.strokeWidth
        val forceStroke = shape == StampShape.SNOWFLAKE
        paint.style = if (filled && !forceStroke) Paint.Style.FILL else Paint.Style.STROKE
        if (paint.style == Paint.Style.STROKE) paint.strokeWidth = (radius * .22f).coerceAtLeast(1.2f)
        if (shape == StampShape.CIRCLE) {
            c.drawCircle(cx, cy, radius, paint)
        } else {
            c.save()
            c.rotate(angle, cx, cy)
            val path = path(shape, cx, cy, radius)
            if (path != null) c.drawPath(path, paint) else c.drawCircle(cx, cy, radius, paint)
            c.restore()
        }
        paint.style = savedStyle
        paint.strokeWidth = savedWidth
    }

    private fun polygon(cx: Float, cy: Float, radius: Float, sides: Int, startDeg: Float = -90f): Path {
        val path = Path()
        for (i in 0 until sides) {
            val a = Math.toRadians((startDeg + i * 360f / sides).toDouble())
            val x = cx + radius * cos(a).toFloat()
            val y = cy + radius * sin(a).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun star(cx: Float, cy: Float, radius: Float, points: Int = 5): Path {
        val path = Path()
        val inner = radius * .42f
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) radius else inner
            val a = Math.toRadians((-90f + i * 180f / points).toDouble())
            val x = cx + r * cos(a).toFloat()
            val y = cy + r * sin(a).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return path
    }

    private fun cross(cx: Float, cy: Float, radius: Float): Path {
        val arm = radius * .38f
        val path = Path()
        path.moveTo(cx - arm, cy - radius); path.lineTo(cx + arm, cy - radius)
        path.lineTo(cx + arm, cy - arm); path.lineTo(cx + radius, cy - arm)
        path.lineTo(cx + radius, cy + arm); path.lineTo(cx + arm, cy + arm)
        path.lineTo(cx + arm, cy + radius); path.lineTo(cx - arm, cy + radius)
        path.lineTo(cx - arm, cy + arm); path.lineTo(cx - radius, cy + arm)
        path.lineTo(cx - radius, cy - arm); path.lineTo(cx - arm, cy - arm)
        path.close()
        return path
    }

    private fun heart(cx: Float, cy: Float, radius: Float): Path {
        val path = Path()
        val top = cy - radius * .35f
        path.moveTo(cx, cy + radius)
        path.cubicTo(cx - radius * 1.3f, cy + radius * .1f, cx - radius * .9f, top - radius * .7f, cx, top)
        path.cubicTo(cx + radius * .9f, top - radius * .7f, cx + radius * 1.3f, cy + radius * .1f, cx, cy + radius)
        path.close()
        return path
    }

    private fun drop(cx: Float, cy: Float, radius: Float): Path {
        val path = Path()
        path.moveTo(cx, cy - radius)
        path.cubicTo(cx + radius, cy - radius * .1f, cx + radius * .75f, cy + radius, cx, cy + radius)
        path.cubicTo(cx - radius * .75f, cy + radius, cx - radius, cy - radius * .1f, cx, cy - radius)
        path.close()
        return path
    }

    private fun leaf(cx: Float, cy: Float, radius: Float): Path {
        val path = Path()
        path.moveTo(cx, cy - radius)
        path.cubicTo(cx + radius * 1.1f, cy - radius * .5f, cx + radius * 1.1f, cy + radius * .5f, cx, cy + radius)
        path.cubicTo(cx - radius * 1.1f, cy + radius * .5f, cx - radius * 1.1f, cy - radius * .5f, cx, cy - radius)
        path.close()
        return path
    }

    private fun snowflake(cx: Float, cy: Float, radius: Float): Path {
        val path = Path()
        for (i in 0 until 6) {
            val a = Math.toRadians((i * 60f).toDouble())
            val x = cx + radius * cos(a).toFloat()
            val y = cy + radius * sin(a).toFloat()
            val baseX = cx + radius * .28f * cos(a).toFloat()
            val baseY = cy + radius * .28f * sin(a).toFloat()
            path.moveTo(baseX, baseY); path.lineTo(x, y)
            val midX = cx + radius * .62f * cos(a).toFloat()
            val midY = cy + radius * .62f * sin(a).toFloat()
            val branchA = a + Math.toRadians(28.0)
            val branchB = a - Math.toRadians(28.0)
            path.moveTo(midX, midY)
            path.lineTo(midX + radius * .22f * cos(branchA).toFloat(), midY + radius * .22f * sin(branchA).toFloat())
            path.moveTo(midX, midY)
            path.lineTo(midX + radius * .22f * cos(branchB).toFloat(), midY + radius * .22f * sin(branchB).toFloat())
        }
        return path
    }

    private fun paw(cx: Float, cy: Float, radius: Float): Path {
        val path = Path()
        path.addCircle(cx, cy + radius * .15f, radius * .55f, Path.Direction.CW)
        val toe = radius * .3f
        val offsets = listOf(-1f to -1.05f, -.42f to -1.35f, .42f to -1.35f, 1f to -1.05f)
        offsets.forEach { (ox, oy) -> path.addCircle(cx + ox * radius * .5f, cy + oy * radius * .5f, toe, Path.Direction.CW) }
        return path
    }

    private fun path(shape: StampShape, cx: Float, cy: Float, radius: Float): Path? = when (shape) {
        StampShape.SQUARE -> polygon(cx, cy, radius, 4, -45f)
        StampShape.TRIANGLE -> polygon(cx, cy, radius, 3)
        StampShape.DIAMOND -> polygon(cx, cy, radius, 4)
        StampShape.PENTAGON -> polygon(cx, cy, radius, 5)
        StampShape.HEXAGON -> polygon(cx, cy, radius, 6)
        StampShape.STAR -> star(cx, cy, radius)
        StampShape.CROSS -> cross(cx, cy, radius)
        StampShape.HEART -> heart(cx, cy, radius)
        StampShape.DROP -> drop(cx, cy, radius)
        StampShape.LEAF -> leaf(cx, cy, radius)
        StampShape.SNOWFLAKE -> snowflake(cx, cy, radius)
        StampShape.PAW -> paw(cx, cy, radius)
        StampShape.CIRCLE -> null
    }
}
