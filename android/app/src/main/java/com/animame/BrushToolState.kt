package com.animame

import android.content.Context
import android.graphics.Color

object BrushToolState {
    private const val PREFS = "anima_me_brush_state"
    private const val ID = "brush_id"
    private const val SIZE = "size"
    private const val OPACITY = "opacity"
    private const val FLOW = "flow"
    private const val SPACING = "spacing"
    private const val SMOOTHING = "smoothing"
    private const val PRESSURE = "pressure"
    private const val FRONT = "front"
    private const val INSIDE = "inside"
    private const val RANDOM_ROTATION = "random_rotation"
    private const val COLOR = "color"

    var brushId: String = "canvas_1"
    var size: Float = 12f
    var opacity: Float = 1f
    var flow: Float = 1f
    var spacing: Float = .12f
    var smoothing: Float = .18f
    var pressure: Boolean = true
    var drawsInFront: Boolean = true
    var drawsInside: Boolean = false
    var randomRotation: Boolean = false
    var color: Int = Color.BLACK

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        brushId = p.getString(ID, brushId) ?: brushId
        size = p.getFloat(SIZE, size)
        opacity = p.getFloat(OPACITY, opacity)
        flow = p.getFloat(FLOW, flow)
        spacing = p.getFloat(SPACING, spacing)
        smoothing = p.getFloat(SMOOTHING, smoothing)
        pressure = p.getBoolean(PRESSURE, pressure)
        drawsInFront = p.getBoolean(FRONT, drawsInFront)
        drawsInside = p.getBoolean(INSIDE, drawsInside)
        randomRotation = p.getBoolean(RANDOM_ROTATION, randomRotation)
        color = p.getInt(COLOR, color)
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(ID, brushId)
            .putFloat(SIZE, size)
            .putFloat(OPACITY, opacity)
            .putFloat(FLOW, flow)
            .putFloat(SPACING, spacing)
            .putFloat(SMOOTHING, smoothing)
            .putBoolean(PRESSURE, pressure)
            .putBoolean(FRONT, drawsInFront)
            .putBoolean(INSIDE, drawsInside)
            .putBoolean(RANDOM_ROTATION, randomRotation)
            .putInt(COLOR, color)
            .apply()
    }
}
