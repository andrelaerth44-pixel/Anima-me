package com.animame.editor

import kotlin.math.max
import kotlin.math.pow

/**
 * Raster edge smoothing profiles.
 * This intentionally does not modify stroke trajectory; stabilization remains
 * exclusively in StrokeStabilizer.
 */
object EdgeSmoothing {
    data class Profile(
        val enabled: Boolean = true,
        val quality: Quality = Quality.HIGH,
        val coverageGamma: Float = 1f,
        val subpixel: Boolean = true
    )

    enum class Quality { FAST, HIGH, MAX }

    fun coverage(alpha: Float, distance: Float, radius: Float, profile: Profile): Float {
        if (!profile.enabled) return alpha.coerceIn(0f, 1f)
        if (radius <= 0f) return 0f
        val aaWidth = when (profile.quality) {
            Quality.FAST -> max(0.75f, radius * 0.035f)
            Quality.HIGH -> max(1.0f, radius * 0.055f)
            Quality.MAX -> max(1.25f, radius * 0.075f)
        }
        val edge = 1f - smoothstep(radius - aaWidth, radius + aaWidth, distance)
        val corrected = if (profile.coverageGamma == 1f) edge else {
            edge.pow(1f / profile.coverageGamma.coerceIn(.1f, 4f))
        }
        return (alpha * corrected).coerceIn(0f, 1f)
    }

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        if (edge0 == edge1) return if (x < edge0) 0f else 1f
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
