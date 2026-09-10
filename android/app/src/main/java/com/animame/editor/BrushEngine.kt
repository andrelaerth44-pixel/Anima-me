package com.animame.editor

import kotlin.math.*
import kotlin.random.Random

object BrushEngine {
    /** A single raster stamp. antialias controls edge smoothing independently of stroke stabilization. */
    data class Stamp(
        val x: Float,
        val y: Float,
        val size: Float,
        val alpha: Float,
        val angle: Float,
        val colorShift: Float,
        val antialias: Boolean = true
    )

    fun stamps(samples: List<StrokeSample>, settings: BrushSettings, seed: Long = 0L): List<Stamp> {
        if (samples.isEmpty()) return emptyList()
        val s = settings.normalized()
        val global = StrokeCorrectionStore.settings()
        val hasBrushCorrection = s.stabilizerConstant > 0f || s.stabilizerFastStrokes > 0f ||
            (!s.disablePrediction && s.brushPrediction > 0f)
        val corrected = if (hasBrushCorrection) {
            StrokeStabilizer.apply(samples, StrokeStabilizer.Settings(
                constant = s.stabilizerConstant,
                fastStrokes = s.stabilizerFastStrokes,
                smoothing = 0f,
                prediction = if (s.disablePrediction) 0f else s.brushPrediction,
                mode = if (s.stabilizerConstant == 0f && s.stabilizerFastStrokes == 0f) {
                    StrokeStabilizer.Mode.AFTER
                } else {
                    StrokeStabilizer.Mode.REAL_TIME
                },
                legacy = s.useLegacyStabilization,
                forceFade = s.forceFade,
                fadeStart = s.fadeStartTime,
                fadeEnd = s.fadeEndTime
            ))
        } else if (global.constant > 0f || global.fastStrokes > 0f || global.prediction > 0f) {
            StrokeStabilizer.apply(samples, global.copy(smoothing = 0f))
        } else samples

        val rng = Random(seed xor s.id.hashCode().toLong())
        val out = ArrayList<Stamp>()
        var prev: StrokeSample? = null
        var distance = Float.POSITIVE_INFINITY
        corrected.forEach { p ->
            val q = prev
            val dx = if (q == null) 0f else p.x - q.x
            val dy = if (q == null) 0f else p.y - q.y
            val dt = if (q == null) 1L else max(1L, p.timeMs - q.timeMs)
            val speed = if (q == null) 0f else hypot(dx, dy) / dt.toFloat()
            val speed01 = (speed / 2f).coerceIn(0f, 1f)
            val pressure = p.pressure.coerceIn(0f, 1f)
            val pressureSize = lerp(s.minSizeFactor, 1f, pressure * s.pressureSizeFactor.coerceIn(0f, 1f))
            val pressureOpacity = lerp(s.minOpacity, 1f, pressure * s.pressureOpacityFactor.coerceIn(0f, 1f))
            val speedSize = lerp(1f, s.speedSizeFactor, speed01)
            val speedOpacity = lerp(1f, s.speedOpacityFactor, speed01)
            val fade = strokeFade(p, corrected, s)
            val materialSize = when (s.material) {
                BrushMaterial.PENCIL, BrushMaterial.CHARCOAL, BrushMaterial.CHALK -> 0.92f + rng.nextFloat() * 0.16f
                BrushMaterial.WATERCOLOR, BrushMaterial.GOUACHE -> 1.0f + rng.nextFloat() * 0.08f
                BrushMaterial.OIL, BrushMaterial.ACRYLIC -> 1.02f + rng.nextFloat() * 0.14f
                BrushMaterial.AIRBRUSH -> 1.35f
                BrushMaterial.PARTICLE, BrushMaterial.STAMP -> 0.9f + rng.nextFloat() * 0.3f
                else -> 1f
            }
            val size = s.size * pressureSize * speedSize * materialSize
            val baseAlpha = s.opacity * pressureOpacity * speedOpacity * fade
            val materialAlpha = when (s.algorithm) {
                BrushAlgorithm.AIRBRUSH -> baseAlpha * (0.45f + pressure * 0.55f)
                BrushAlgorithm.WATER -> baseAlpha * (0.55f + s.waterWetness.coerceIn(0f, 1f) * 0.45f)
                BrushAlgorithm.DOUBLE -> baseAlpha
                BrushAlgorithm.ERASER -> 1f
                else -> baseAlpha
            }
            val jitter = s.jitterPosition * size
            val jx = (rng.nextFloat() * 2f - 1f) * jitter
            val jy = (rng.nextFloat() * 2f - 1f) * jitter
            val scatter = s.scatterSize * size
            val sx = if (scatter > 0f) (rng.nextFloat() * 2f - 1f) * scatter else 0f
            val sy = if (scatter > 0f) (rng.nextFloat() * 2f - 1f) * scatter else 0f
            val rotation = s.initialAngle + (if (s.followRotation) atan2(dy, dx) else 0f) + s.rotationJitter * (rng.nextFloat() * 2f - 1f)
            val spacing = max(.5f, s.spacing * size * (1f + s.jitterSpacing * (rng.nextFloat() * 2f - 1f)))
            distance += hypot(dx, dy)
            if (prev == null || distance >= spacing) {
                out += Stamp(
                    p.x + jx + sx,
                    p.y + jy + sy,
                    size.coerceIn(.25f, 4096f),
                    materialAlpha.coerceIn(0f, 1f),
                    rotation,
                    (s.hueJitter + s.saturationJitter * .25f + s.brightnessJitter * .1f) * (rng.nextFloat() * 2f - 1f),
                    s.antialias
                )
                distance = 0f
            }
            prev = p
        }
        return out
    }

    private fun strokeFade(p: StrokeSample, samples: List<StrokeSample>, s: BrushSettings): Float {
        if (samples.size <= 1) return 1f
        val index = samples.indexOf(p).coerceAtLeast(0)
        val t = index.toFloat() / samples.lastIndex.coerceAtLeast(1).toFloat()
        val start = if (s.forceFade) s.fadeStartTime else s.fadeStart
        val end = if (s.forceFade) s.fadeEndTime else s.fadeEnd
        val safeStart = start.coerceIn(0f, 1f)
        val safeEnd = max(safeStart + .001f, end.coerceIn(0f, 1f))
        val target = if (s.forceFade) min(s.fadeOpacity, .15f) else s.fadeOpacity.coerceIn(0f, 1f)
        return when {
            t < safeStart -> 1f
            t > safeEnd -> target
            else -> lerp(1f, target, (t - safeStart) / (safeEnd - safeStart))
        }
    }

    /**
     * Kept for source compatibility. "Suavizar" is NOT stroke stabilization and
     * must never call StrokeStabilizer. Edge smoothing is controlled by BrushSettings.antialias.
     */
    @Deprecated("Suavizar is raster edge antialiasing; it is not stroke stabilization")
    fun smooth(samples: List<StrokeSample>, strength: Float): List<StrokeSample> = samples

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
