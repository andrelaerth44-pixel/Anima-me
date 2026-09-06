package com.animame.editor

import kotlin.math.cos
import kotlin.math.sin

/** Pure screen/document transform. Keeping this math independent makes coordinate bugs testable on JVM. */
data class DocumentTransform(val cx: Float, val cy: Float, val scale: Float = 1f, val rotationDeg: Float = 0f, val translateX: Float = 0f, val translateY: Float = 0f) {
    fun toScreen(x: Float, y: Float): FloatArray {
        val s = scale.coerceAtLeast(0.0001f)
        val rad = Math.toRadians(rotationDeg.toDouble()); val c = cos(rad).toFloat(); val si = sin(rad).toFloat()
        val dx = (x - cx) * s; val dy = (y - cy) * s
        return floatArrayOf(cx + dx * c - dy * si + translateX, cy + dx * si + dy * c + translateY)
    }
    fun toDocument(x: Float, y: Float): FloatArray {
        val s = scale.coerceAtLeast(0.0001f)
        val px = x - cx - translateX; val py = y - cy - translateY
        val rad = Math.toRadians(rotationDeg.toDouble()); val c = cos(rad).toFloat(); val si = sin(rad).toFloat()
        val dx = (px * c + py * si) / s; val dy = (-px * si + py * c) / s
        return floatArrayOf(cx + dx, cy + dy)
    }
}
