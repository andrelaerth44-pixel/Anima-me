package com.animame.editor

/**
 * User-level overrides applied on top of every selected brush preset.
 * The preset remains the source of its material/algorithm; these values are
 * the controls exposed by the editor's brush properties panel.
 */
object BrushSettingsOverrides {
    var pressureMinSize: Float = .25f
    var pressureMinOpacity: Float = .15f
    var fadeStart: Float = 0f
    var fadeEnd: Float = 1f
    var jitterPosition: Float = 0f
    var jitterThickness: Float = 0f
    var jitterOpacity: Float = 0f
    var rotationJitter: Float = 0f
    var blur: Float = 0f
    var hueJitter: Float = 0f
    var saturationJitter: Float = 0f
    var brightnessJitter: Float = 0f

    fun apply(base: BrushSettings): BrushSettings = base.copy(
        minSizeFactor = pressureMinSize.coerceIn(0f, 1f),
        minOpacity = pressureMinOpacity.coerceIn(0f, 1f),
        fadeStart = fadeStart.coerceIn(0f, 1f),
        fadeEnd = fadeEnd.coerceIn(fadeStart.coerceIn(0f, 1f), 1f),
        jitterPosition = jitterPosition.coerceAtLeast(0f),
        jitterThickness = jitterThickness.coerceAtLeast(0f),
        jitterOpacity = jitterOpacity.coerceAtLeast(0f),
        rotationJitter = rotationJitter.coerceAtLeast(0f),
        blur = blur.coerceAtLeast(0f),
        hueJitter = hueJitter,
        saturationJitter = saturationJitter,
        brightnessJitter = brightnessJitter
    )
}
