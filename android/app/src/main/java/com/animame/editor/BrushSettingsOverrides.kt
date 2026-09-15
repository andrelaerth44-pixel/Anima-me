package com.animame.editor

import android.content.Context

/** Session-persistent advanced overrides layered over the selected brush preset. */
object BrushSettingsOverrides {
    var pressureMinSize: Float = 0.25f
    var pressureMinOpacity: Float = 0.15f
    var fadeStart: Float = 0f
    var fadeEnd: Float = 1f
    var jitterPosition: Float = 0f
    var jitterThickness: Float = 0f
    var jitterOpacity: Float = 0f
    var blur: Float = 0f
    var hueJitter: Float = 0f
    var saturationJitter: Float = 0f
    var brightnessJitter: Float = 0f
    var rotationJitter: Float = 0f

    private const val PREFS = "anima_me_brush_overrides"

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        pressureMinSize = p.getFloat("pressureMinSize", .25f)
        pressureMinOpacity = p.getFloat("pressureMinOpacity", .15f)
        fadeStart = p.getFloat("fadeStart", 0f)
        fadeEnd = p.getFloat("fadeEnd", 1f)
        jitterPosition = p.getFloat("jitterPosition", 0f)
        jitterThickness = p.getFloat("jitterThickness", 0f)
        jitterOpacity = p.getFloat("jitterOpacity", 0f)
        blur = p.getFloat("blur", 0f)
        hueJitter = p.getFloat("hueJitter", 0f)
        saturationJitter = p.getFloat("saturationJitter", 0f)
        brightnessJitter = p.getFloat("brightnessJitter", 0f)
        rotationJitter = p.getFloat("rotationJitter", 0f)
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("pressureMinSize", pressureMinSize)
            .putFloat("pressureMinOpacity", pressureMinOpacity)
            .putFloat("fadeStart", fadeStart)
            .putFloat("fadeEnd", fadeEnd)
            .putFloat("jitterPosition", jitterPosition)
            .putFloat("jitterThickness", jitterThickness)
            .putFloat("jitterOpacity", jitterOpacity)
            .putFloat("blur", blur)
            .putFloat("hueJitter", hueJitter)
            .putFloat("saturationJitter", saturationJitter)
            .putFloat("brightnessJitter", brightnessJitter)
            .putFloat("rotationJitter", rotationJitter)
            .apply()
    }

    fun apply(settings: BrushSettings): BrushSettings = settings.copy(
        minSizeFactor = pressureMinSize.coerceIn(0f, 1f),
        minOpacity = pressureMinOpacity.coerceIn(0f, 1f),
        fadeStart = fadeStart.coerceIn(0f, 1f),
        fadeEnd = fadeEnd.coerceIn(fadeStart.coerceIn(0f, 1f), 1f),
        jitterPosition = jitterPosition.coerceIn(0f, 2f),
        jitterThickness = jitterThickness.coerceIn(0f, 2f),
        jitterOpacity = jitterOpacity.coerceIn(0f, 1f),
        blur = blur.coerceIn(0f, 1f),
        hueJitter = hueJitter.coerceIn(0f, 1f),
        saturationJitter = saturationJitter.coerceIn(0f, 1f),
        brightnessJitter = brightnessJitter.coerceIn(0f, 1f),
        rotationJitter = rotationJitter.coerceIn(0f, 6.2831855f)
    )
}
