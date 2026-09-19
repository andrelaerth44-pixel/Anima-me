package com.animame.editor

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Geometry-only liquify engine for vector stroke samples.
 * The caller owns the frame/history transaction; this class never mutates the input list.
 */
object LiquifyEngine {
    enum class Mode {
        PUSH,
        PULL,
        TWIRL_CW,
        TWIRL_CCW,
        PINCH,
        BLOAT,
        RECONSTRUCT
    }

    data class Settings(
        val radius: Float = 80f,
        val strength: Float = .45f,
        val mode: Mode = Mode.PUSH
    ) {
        init {
            require(radius > 0f) { "radius must be > 0" }
            require(strength in 0f..1f) { "strength must be between 0 and 1" }
        }
    }

    fun apply(
        samples: List<StrokeSample>,
        centerX: Float,
        centerY: Float,
        dx: Float,
        dy: Float,
        settings: Settings
    ): List<StrokeSample> {
        if (samples.isEmpty() || settings.strength <= 0f) return samples.map { it.copy() }
        val radius = settings.radius
        val radiusSq = radius * radius
        val strength = settings.strength.coerceIn(0f, 1f)

        return samples.map { sample ->
            val ox = sample.x - centerX
            val oy = sample.y - centerY
            val distanceSq = ox * ox + oy * oy
            if (distanceSq >= radiusSq) return@map sample.copy()

            val distance = hypot(ox, oy).coerceAtLeast(0.0001f)
            val normalized = (1f - distance / radius).coerceIn(0f, 1f)
            // Smooth falloff: zero at the edge, strongest at the brush center.
            val falloff = normalized * normalized * (3f - 2f * normalized)
            val amount = strength * falloff

            val result = when (settings.mode) {
                Mode.PUSH -> sample.x + dx * amount to sample.y + dy * amount
                Mode.PULL -> sample.x - dx * amount to sample.y - dy * amount
                Mode.PINCH -> {
                    val factor = 1f - amount
                    centerX + ox * factor to centerY + oy * factor
                }
                Mode.BLOAT -> {
                    val factor = 1f + amount
                    centerX + ox * factor to centerY + oy * factor
                }
                Mode.TWIRL_CW, Mode.TWIRL_CCW -> {
                    val direction = if (settings.mode == Mode.TWIRL_CW) -1f else 1f
                    val angle = direction * amount * 1.15f
                    val c = cos(angle)
                    val s = sin(angle)
                    centerX + ox * c - oy * s to centerY + ox * s + oy * c
                }
                // Reconstruct is intentionally conservative at the geometry layer:
                // the caller can blend toward the original sample using the same falloff.
                Mode.RECONSTRUCT -> sample.x - dx * amount to sample.y - dy * amount
            }
            sample.copy(x = result.first, y = result.second)
        }
    }

    /** Applies a complete drag path as successive local deformations. */
    fun applyStroke(
        samples: List<StrokeSample>,
        path: List<StrokeSample>,
        settings: Settings
    ): List<StrokeSample> {
        if (path.size < 2 || samples.isEmpty()) return samples.map { it.copy() }
        var result = samples.map { it.copy() }
        for (index in 1 until path.size) {
            val previous = path[index - 1]
            val current = path[index]
            result = apply(
                result,
                current.x,
                current.y,
                current.x - previous.x,
                current.y - previous.y,
                settings
            )
        }
        return result
    }
}
