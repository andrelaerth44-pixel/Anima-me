package com.animame.editor

import kotlin.math.pow

/** Professional per-brush controls. Values are normalized unless noted. */
data class BrushSettings(
    var size: Float = 12f,
    var sizeMin: Float = 1f,
    var sizeMax: Float = 512f,
    var opacity: Float = 1f,
    var opacityMin: Float = 0f,
    var flow: Float = 1f,
    var alpha: Float = 1f,
    var hardness: Float = 1f,
    var feather: Float = 0f,
    var spacing: Float = 0.15f,
    var fade: Float = 0f,
    var fadeIn: Float = 0f,
    var fadeOut: Float = 0f,
    var jitter: Float = 0f,
    var pressureSize: Float = 0f,
    var pressureOpacity: Float = 0f,
    var pressureFlow: Float = 0f,
    var tiltSize: Float = 0f,
    var tiltOpacity: Float = 0f,
    var angle: Float = 0f,
    var roundness: Float = 1f,
    var lockAlpha: Boolean = false,
    var eraser: Boolean = false,
    var antialias: Boolean = true
) {
    fun normalized(): BrushSettings = copy(
        size = size.coerceIn(sizeMin.coerceAtLeast(0.1f), sizeMax.coerceAtLeast(sizeMin)),
        sizeMin = sizeMin.coerceAtLeast(0.1f),
        sizeMax = sizeMax.coerceAtLeast(sizeMin.coerceAtLeast(0.1f)),
        opacity = opacity.coerceIn(0f, 1f), opacityMin = opacityMin.coerceIn(0f, 1f),
        flow = flow.coerceIn(0f, 1f), alpha = alpha.coerceIn(0f, 1f),
        hardness = hardness.coerceIn(0f, 1f), feather = feather.coerceIn(0f, 1f),
        spacing = spacing.coerceIn(0.01f, 4f), fade = fade.coerceIn(0f, 1f),
        fadeIn = fadeIn.coerceIn(0f, 1f), fadeOut = fadeOut.coerceIn(0f, 1f),
        jitter = jitter.coerceIn(0f, 1f), pressureSize = pressureSize.coerceIn(-1f, 1f),
        pressureOpacity = pressureOpacity.coerceIn(-1f, 1f), pressureFlow = pressureFlow.coerceIn(-1f, 1f),
        tiltSize = tiltSize.coerceIn(-1f, 1f), tiltOpacity = tiltOpacity.coerceIn(-1f, 1f),
        roundness = roundness.coerceIn(0.05f, 1f)
    )

    fun radiusFor(pressure: Float, tilt: Float): Float {
        val p = pressure.coerceIn(0f, 1f)
        val t = tilt.coerceIn(0f, 1.5708f) / 1.5708f
        val factor = (1f + pressureSize.coerceIn(-1f, 1f) * (p - 0.5f) * 2f)
            .coerceIn(0.05f, 2f) * (1f + tiltSize * t).coerceIn(0.05f, 2f)
        return (size * factor).coerceIn(sizeMin, sizeMax)
    }

    fun opacityFor(pressure: Float, tilt: Float, distance: Float, strokeLength: Float): Float {
        val p = pressure.coerceIn(0f, 1f)
        val t = (tilt.coerceIn(0f, 1.5708f) / 1.5708f)
        var value = opacity * alpha * flow
        value *= (1f + pressureOpacity * (p - 0.5f) * 2f).coerceIn(0f, 2f)
        value *= (1f + tiltOpacity * t).coerceIn(0f, 2f)
        if (fade > 0f && strokeLength > 0f) value *= (1f - (distance / strokeLength).coerceIn(0f, 1f) * fade)
        if (fadeIn > 0f && strokeLength > 0f) value *= (distance / (strokeLength * fadeIn).coerceAtLeast(0.001f)).coerceIn(0f, 1f)
        if (fadeOut > 0f && strokeLength > 0f) value *= ((strokeLength - distance) / (strokeLength * fadeOut).coerceAtLeast(0.001f)).coerceIn(0f, 1f)
        return value.coerceIn(0f, 1f)
    }
}

object BrushDefaults {
    fun forPreset(id: String): BrushSettings = when (id) {
        "eraser", "aotz-eraser" -> BrushSettings(eraser = true, opacity = 1f, alpha = 1f, flow = 1f, hardness = .85f, feather = .08f, pressureSize = .65f, pressureOpacity = .15f)
        "pencil" -> BrushSettings(size = 6f, opacity = .72f, flow = .85f, hardness = .65f, feather = .2f, spacing = .08f, pressureSize = .65f, pressureOpacity = .25f, jitter = .04f)
        "ink", "aotz-ink" -> BrushSettings(size = 10f, opacity = 1f, hardness = .95f, feather = .02f, spacing = .07f, pressureSize = .8f, pressureOpacity = .25f)
        "paint" -> BrushSettings(size = 18f, opacity = .9f, flow = .75f, hardness = .65f, feather = .18f, spacing = .12f, pressureSize = .5f, pressureOpacity = .3f)
        "water", "aotz-water" -> BrushSettings(size = 24f, opacity = .55f, flow = .45f, hardness = .25f, feather = .5f, spacing = .18f, pressureSize = .35f, pressureOpacity = .35f)
        else -> BrushSettings()
    }
}
