package com.animame.editor

import kotlin.math.*
import kotlin.random.Random

object BrushEngine {
    data class Stamp(val x: Float, val y: Float, val size: Float, val alpha: Float, val angle: Float, val colorShift: Float, val antialias: Boolean = true, val edgeQuality: EdgeSmoothing.Quality = EdgeSmoothing.Quality.HIGH)

    fun stamps(samples: List<StrokeSample>, settings: BrushSettings, seed: Long = 0L): List<Stamp> {
        if (samples.isEmpty()) return emptyList()
        val s = settings.normalized()
        val global = StrokeCorrectionStore.settings()
        val hasBrushCorrection = s.stabilizerConstant > 0f || s.stabilizerFastStrokes > 0f || (!s.disablePrediction && s.brushPrediction > 0f)
        val corrected = if (hasBrushCorrection) {
            StrokeStabilizer.apply(samples, StrokeStabilizer.Settings(
                constant = s.stabilizerConstant, fastStrokes = s.stabilizerFastStrokes, smoothing = 0f,
                prediction = if (s.disablePrediction) 0f else s.brushPrediction,
                mode = if (s.stabilizerConstant == 0f && s.stabilizerFastStrokes == 0f) StrokeStabilizer.Mode.AFTER else StrokeStabilizer.Mode.REAL_TIME,
                legacy = s.useLegacyStabilization, forceFade = s.forceFade, fadeStart = s.fadeStartTime, fadeEnd = s.fadeEndTime
            ))
        } else if (global.constant > 0f || global.fastStrokes > 0f || global.prediction > 0f) {
            StrokeStabilizer.apply(samples, global.copy(smoothing = 0f))
        } else samples

        val rng = Random(seed xor s.id.hashCode().toLong())
        val out = ArrayList<Stamp>()
        var prev: StrokeSample? = null
        var distance = Float.POSITIVE_INFINITY
        corrected.forEachIndexed { index, p ->
            val q = prev
            val dx = if (q == null) 0f else p.x - q.x
            val dy = if (q == null) 0f else p.y - q.y
            val dt = if (q == null) 1L else max(1L, p.timeMs - q.timeMs)
            val speed01 = ((if (q == null) 0f else hypot(dx, dy) / dt.toFloat()) / 2f).coerceIn(0f, 1f)
            val pressure = p.pressure.coerceIn(0f, 1f).pow(s.pressureExponent)
            val pressureSize = lerp(s.minSizeFactor, 1f, pressure * s.pressureSizeFactor.coerceIn(0f, 1f))
            val pressureOpacity = lerp(s.minOpacity, 1f, pressure * s.pressureOpacityFactor.coerceIn(0f, 1f))
            val speedSize = lerp(1f, s.speedSizeFactor, speed01)
            val speedOpacity = lerp(1f, s.speedOpacityFactor, speed01)
            val fade = strokeFade(index, corrected.size, s)
            val materialSize = when (s.material) {
                BrushMaterial.PENCIL, BrushMaterial.CHARCOAL, BrushMaterial.CHALK -> .92f + rng.nextFloat() * .16f
                BrushMaterial.WATERCOLOR, BrushMaterial.GOUACHE -> 1f + rng.nextFloat() * .08f
                BrushMaterial.OIL, BrushMaterial.ACRYLIC -> 1.02f + rng.nextFloat() * .14f
                BrushMaterial.AIRBRUSH -> 1.35f
                BrushMaterial.PARTICLE, BrushMaterial.STAMP -> .9f + rng.nextFloat() * .3f
                else -> 1f
            }
            val size = s.size * pressureSize * speedSize * materialSize * (1f + s.jitterThickness * (rng.nextFloat() * 2f - 1f)).coerceIn(.1f, 4f)
            val baseAlpha = s.opacity * pressureOpacity * speedOpacity * fade * s.flow.coerceIn(0f, 1f)
            val grainNoise = proceduralGrain(p.x, p.y, s.textureScale, seed)
            val textureStrength = (s.textureOpacity * .7f + s.textureGrain * .3f).coerceIn(0f, 1f)
            val textureFloor = s.textureLowerLimit.coerceIn(0f, 1f)
            val textureFactor = lerp(1f, textureFloor + grainNoise * (1f - textureFloor), textureStrength)
            val materialAlpha = when (s.algorithm) {
                BrushAlgorithm.AIRBRUSH -> baseAlpha * (.45f + pressure * .55f)
                BrushAlgorithm.WATER -> baseAlpha * (.55f + s.waterWetness.coerceIn(0f, 1f) * .45f)
                BrushAlgorithm.DOUBLE, BrushAlgorithm.ERASER -> if (s.algorithm == BrushAlgorithm.ERASER) 1f else baseAlpha
                else -> baseAlpha
            } * textureFactor
            val jitter = s.jitterPosition * size
            val scatter = s.scatterSize * size
            val rotation = s.initialAngle + (if (s.followRotation) atan2(dy, dx) else 0f) + s.rotationJitter * (rng.nextFloat() * 2f - 1f)
            val spacing = max(.5f, s.spacing * size * (1f + s.jitterSpacing * (rng.nextFloat() * 2f - 1f)))
            distance += hypot(dx, dy)
            if (prev == null || distance >= spacing) {
                out += Stamp(
                    p.x + (rng.nextFloat() * 2f - 1f) * jitter + (if (scatter > 0f) (rng.nextFloat() * 2f - 1f) * scatter else 0f),
                    p.y + (rng.nextFloat() * 2f - 1f) * jitter + (if (scatter > 0f) (rng.nextFloat() * 2f - 1f) * scatter else 0f),
                    size.coerceIn(.25f, 4096f), materialAlpha.coerceIn(0f, 1f), rotation,
                    (s.hueJitter + s.saturationJitter * .25f + s.brightnessJitter * .1f) * (rng.nextFloat() * 2f - 1f),
                    s.antialias, if (s.antialias) EdgeSmoothing.Quality.MAX else EdgeSmoothing.Quality.FAST
                )
                distance = 0f
            }
            prev = p
        }
        return out
    }

    private fun strokeFade(index: Int, sampleCount: Int, s: BrushSettings): Float {
        if (sampleCount <= 1) return 1f
        val t = index.toFloat() / (sampleCount - 1).coerceAtLeast(1).toFloat()
        val start = (if (s.forceFade) s.fadeStartTime else s.fadeStart).coerceIn(0f, 1f)
        val end = max(start + .001f, (if (s.forceFade) s.fadeEndTime else s.fadeEnd).coerceIn(0f, 1f))
        val target = if (s.forceFade) min(s.fadeOpacity, .15f) else s.fadeOpacity.coerceIn(0f, 1f)
        return when { t < start -> 1f; t > end -> target; else -> lerp(1f, target, (t - start) / (end - start)) }
    }

    private fun proceduralGrain(x: Float, y: Float, scale: Float, seed: Long): Float {
        val sx = floor(x / scale.coerceAtLeast(.01f)).toLong(); val sy = floor(y / scale.coerceAtLeast(.01f)).toLong()
        var h = seed xor (sx * -7046029254386353131L) xor (sy * 7640891576956012809L)
        h = (h xor (h ushr 30)) * -4658895280553007687L; h = (h xor (h ushr 27)) * -7723592293110705685L; h = h xor (h ushr 31)
        return ((h ushr 40).toFloat() / (1L shl 24).toFloat()).coerceIn(0f, 1f)
    }

    @Deprecated("Suavizar is raster edge antialiasing; it is not stroke stabilization")
    fun smooth(samples: List<StrokeSample>, strength: Float): List<StrokeSample> = samples
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
