package com.animame.editor

import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import kotlin.math.max
import kotlin.math.roundToInt

/** RoughAnimator-style image-tip brush renderer with pressure, spacing and rotation. */
object ImportedBrushRenderer {
    fun draw(canvas: Canvas, samples: List<StrokeSample>, settings: BrushSettings, color: Int, smooth: Boolean, antiAlias: Boolean) {
        val path = settings.brushPattern.removePrefix("image:")
        val bitmap = BitmapFactory.decodeFile(path) ?: return
        if (samples.isEmpty()) { bitmap.recycle(); return }
        val points = ContinuousStrokeRenderer.resample(samples, max(1f, settings.size * settings.spacing.coerceAtLeast(.04f)))
        val paint = Paint(if (antiAlias && settings.antialias) Paint.ANTI_ALIAS_FLAG else 0).apply {
            alpha = (settings.opacity * settings.flow * 255f).roundToInt().coerceIn(0,255)
            colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            isFilterBitmap = true
        }
        val matrix = Matrix()
        for (i in points.indices) {
            val p = points[i]
            val radius = max(1f, settings.radiusFor(p.pressure, p.tilt))
            val width = radius * 2f
            val height = width * settings.aspect.coerceIn(.05f,4f)
            val scaleX = width / bitmap.width.coerceAtLeast(1)
            val scaleY = height / bitmap.height.coerceAtLeast(1)
            val rotation = if (settings.followingRotation && i > 0) {
                kotlin.math.atan2(p.y - points[i-1].y, p.x - points[i-1].x) * 180f / Math.PI.toFloat()
            } else settings.initialAngle
            matrix.reset()
            matrix.postScale(scaleX, scaleY)
            matrix.postRotate(rotation)
            matrix.postTranslate(p.x, p.y)
            val a = settings.opacityFor(p.pressure, p.tilt, i.toFloat(), points.size.toFloat())
            paint.alpha = (a * 255f).roundToInt().coerceIn(0,255)
            canvas.drawBitmap(bitmap, matrix, paint)
        }
        bitmap.recycle()
    }
}
