package com.animame

import android.content.Context

/**
 * Persisted "current brush tool" state shared between the quick toolbar
 * controls in MainActivity and the detailed slider screen in
 * ToolOptionsActivity (inspired by RoughAnimator's tool options panel).
 *
 * This was previously referenced by ToolOptionsActivity but never defined,
 * which meant that screen could not compile or run. It now backs both the
 * quick toolbar buttons and the full options screen so the two stay in
 * sync and persist across sessions.
 */
object BrushToolState {
    private const val PREFS = "anima_me_tool_state"

    var size: Float = 12f
    var opacity: Float = 1f
    var flow: Float = 1f
    var spacing: Float = 0.12f
    var smoothing: Float = 0f
    var pressure: Boolean = true
    var randomRotation: Boolean = false
    var drawsInside: Boolean = false
    var color: Int = ThemeColorStore.DEFAULT

    @Volatile private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        size = prefs.getFloat("size", size)
        opacity = prefs.getFloat("opacity", opacity)
        flow = prefs.getFloat("flow", flow)
        spacing = prefs.getFloat("spacing", spacing)
        smoothing = prefs.getFloat("smoothing", smoothing)
        pressure = prefs.getBoolean("pressure", pressure)
        randomRotation = prefs.getBoolean("randomRotation", randomRotation)
        drawsInside = prefs.getBoolean("drawsInside", drawsInside)
        color = prefs.getInt("color", color)
        loaded = true
        applyToEngine()
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("size", size)
            .putFloat("opacity", opacity)
            .putFloat("flow", flow)
            .putFloat("spacing", spacing)
            .putFloat("smoothing", smoothing)
            .putBoolean("pressure", pressure)
            .putBoolean("randomRotation", randomRotation)
            .putBoolean("drawsInside", drawsInside)
            .putInt("color", color)
            .apply()
        applyToEngine()
    }

    /**
     * Pushes the fields that have a real effect on stroke rendering into the
     * global stroke-correction defaults consumed by BrushEngine, so the
     * "Suavização" slider actually stabilizes strokes instead of only being
     * stored.
     */
    private fun applyToEngine() {
        com.animame.editor.StrokeCorrectionStore.constant = (smoothing * 100f).coerceIn(0f, 100f)
    }
}
