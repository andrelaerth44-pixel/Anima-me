package com.animame

import android.content.Context
import com.animame.editor.BrushSettingsOverrides

object BrushToolState {
    private const val PREFS = "anima_me_brush_state"
    private const val ID = "brush_id"
    private const val SIZE = "size"
    private const val OPACITY = "opacity"
    private const val SPACING = "spacing"
    private const val SMOOTHING = "smoothing"
    private const val PRESSURE = "pressure"
    private const val FRONT = "front"
    private const val INSIDE = "inside"
    private const val RANDOM_ROTATION = "random_rotation"
    private const val MIN_SIZE = "pressure_min_size"
    private const val MIN_OPACITY = "pressure_min_opacity"
    private const val FADE_START = "fade_start"
    private const val FADE_END = "fade_end"
    private const val JITTER_POSITION = "jitter_position"
    private const val JITTER_THICKNESS = "jitter_thickness"
    private const val JITTER_OPACITY = "jitter_opacity"
    private const val ROTATION_JITTER = "rotation_jitter"
    private const val BLUR = "blur"
    private const val HUE_JITTER = "hue_jitter"
    private const val SATURATION_JITTER = "saturation_jitter"
    private const val BRIGHTNESS_JITTER = "brightness_jitter"

    var brushId: String = "canvas_1"
    var size: Float = 12f
    var opacity: Float = 1f
    var spacing: Float = .12f
    var smoothing: Float = .18f
    var pressure: Boolean = true
    var drawsInFront: Boolean = true
    var drawsInside: Boolean = false
    var randomRotation: Boolean = false

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        brushId = p.getString(ID, brushId) ?: brushId
        size = p.getFloat(SIZE, size)
        opacity = p.getFloat(OPACITY, opacity)
        spacing = p.getFloat(SPACING, spacing)
        smoothing = p.getFloat(SMOOTHING, smoothing)
        pressure = p.getBoolean(PRESSURE, pressure)
        drawsInFront = p.getBoolean(FRONT, drawsInFront)
        drawsInside = p.getBoolean(INSIDE, drawsInside)
        randomRotation = p.getBoolean(RANDOM_ROTATION, randomRotation)
        BrushSettingsOverrides.pressureMinSize = p.getFloat(MIN_SIZE, BrushSettingsOverrides.pressureMinSize)
        BrushSettingsOverrides.pressureMinOpacity = p.getFloat(MIN_OPACITY, BrushSettingsOverrides.pressureMinOpacity)
        BrushSettingsOverrides.fadeStart = p.getFloat(FADE_START, BrushSettingsOverrides.fadeStart)
        BrushSettingsOverrides.fadeEnd = p.getFloat(FADE_END, BrushSettingsOverrides.fadeEnd)
        BrushSettingsOverrides.jitterPosition = p.getFloat(JITTER_POSITION, BrushSettingsOverrides.jitterPosition)
        BrushSettingsOverrides.jitterThickness = p.getFloat(JITTER_THICKNESS, BrushSettingsOverrides.jitterThickness)
        BrushSettingsOverrides.jitterOpacity = p.getFloat(JITTER_OPACITY, BrushSettingsOverrides.jitterOpacity)
        BrushSettingsOverrides.rotationJitter = p.getFloat(ROTATION_JITTER, BrushSettingsOverrides.rotationJitter)
        BrushSettingsOverrides.blur = p.getFloat(BLUR, BrushSettingsOverrides.blur)
        BrushSettingsOverrides.hueJitter = p.getFloat(HUE_JITTER, BrushSettingsOverrides.hueJitter)
        BrushSettingsOverrides.saturationJitter = p.getFloat(SATURATION_JITTER, BrushSettingsOverrides.saturationJitter)
        BrushSettingsOverrides.brightnessJitter = p.getFloat(BRIGHTNESS_JITTER, BrushSettingsOverrides.brightnessJitter)
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ID, brushId)
            .putFloat(SIZE, size)
            .putFloat(OPACITY, opacity)
            .putFloat(SPACING, spacing)
            .putFloat(SMOOTHING, smoothing)
            .putBoolean(PRESSURE, pressure)
            .putBoolean(FRONT, drawsInFront)
            .putBoolean(INSIDE, drawsInside)
            .putBoolean(RANDOM_ROTATION, randomRotation)
            .putFloat(MIN_SIZE, BrushSettingsOverrides.pressureMinSize)
            .putFloat(MIN_OPACITY, BrushSettingsOverrides.pressureMinOpacity)
            .putFloat(FADE_START, BrushSettingsOverrides.fadeStart)
            .putFloat(FADE_END, BrushSettingsOverrides.fadeEnd)
            .putFloat(JITTER_POSITION, BrushSettingsOverrides.jitterPosition)
            .putFloat(JITTER_THICKNESS, BrushSettingsOverrides.jitterThickness)
            .putFloat(JITTER_OPACITY, BrushSettingsOverrides.jitterOpacity)
            .putFloat(ROTATION_JITTER, BrushSettingsOverrides.rotationJitter)
            .putFloat(BLUR, BrushSettingsOverrides.blur)
            .putFloat(HUE_JITTER, BrushSettingsOverrides.hueJitter)
            .putFloat(SATURATION_JITTER, BrushSettingsOverrides.saturationJitter)
            .putFloat(BRIGHTNESS_JITTER, BrushSettingsOverrides.brightnessJitter)
            .apply()
    }
}
