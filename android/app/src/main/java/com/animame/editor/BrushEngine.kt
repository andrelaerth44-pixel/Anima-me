package com.animame.editor

import kotlin.math.*
import kotlin.random.Random

object BrushEngine {
    data class Stamp(val x: Float, val y: Float, val size: Float, val alpha: Float, val angle: Float, val colorShift: Float)

    fun stamps(samples: List<StrokeSample>, settings: BrushSettings, seed: Long = 0L): List<Stamp> {
        if (samples.isEmpty()) return emptyList()
        val s = settings.normalized()
        val rng = Random(seed xor s.id.hashCode().toLong())
        val out = ArrayList<Stamp>()
        var prev: StrokeSample? = null
        var distance = Float.POSITIVE_INFINITY
        samples.forEach { p ->
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
            val fade = strokeFade(p, samples, s)
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
                    (s.hueJitter + s.saturationJitter * .25f + s.brightnessJitter * .1f) * (rng.nextFloat() * 2f - 1f)
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
        val t = index.toFloat() / (samples.lastIndex.coerceAtLeast(1)).toFloat()
        val start = s.fadeStart.coerceIn(0f, 1f)
        val end = max(start + .001f, s.fadeEnd.coerceIn(0f, 1f))
        return when {
            t < start -> 1f
            t > end -> s.fadeOpacity.coerceIn(0f, 1f)
            else -> lerp(1f, s.fadeOpacity.coerceIn(0f, 1f), (t - start) / (end - start))
        }
    }

    fun smooth(samples: List<StrokeSample>, strength: Float): List<StrokeSample> {
        val k = strength.coerceIn(0f, 1f)
        if (samples.size < 3 || k == 0f) return samples
        return samples.mapIndexed { i, p ->
            if (i == 0 || i == samples.lastIndex) p else {
                val a = samples[i - 1]; val b = samples[i + 1]
                StrokeSample(lerp(p.x, (a.x + b.x) / 2f, k), lerp(p.y, (a.y + b.y) / 2f, k), p.pressure, p.timeMs)
            }
        }
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
}
