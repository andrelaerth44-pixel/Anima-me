package com.animame

import android.content.Context

/** Persists editor choices that should survive Activity recreation without storing drawing data. */
object EditorPreferences {
    private const val PREFS = "anima_me_editor"
    private const val BRUSH_ID = "brush_id"
    private const val BRUSH_SIZE = "brush_size"
    private const val BRUSH_OPACITY = "brush_opacity"
    private const val ZOOM = "zoom"

    fun brushId(context: Context): String = prefs(context).getString(BRUSH_ID, "basic") ?: "basic"
    fun brushSize(context: Context): Float = prefs(context).getFloat(BRUSH_SIZE, 12f)
    fun brushOpacity(context: Context): Float = prefs(context).getFloat(BRUSH_OPACITY, 1f)
    fun zoom(context: Context): Float = prefs(context).getFloat(ZOOM, 1f)

    fun saveBrush(context: Context, id: String, size: Float, opacity: Float) {
        prefs(context).edit()
            .putString(BRUSH_ID, id)
            .putFloat(BRUSH_SIZE, size.coerceIn(1f, 4096f))
            .putFloat(BRUSH_OPACITY, opacity.coerceIn(.01f, 1f))
            .apply()
    }

    fun saveZoom(context: Context, value: Float) {
        prefs(context).edit().putFloat(ZOOM, value.coerceIn(.35f, 6f)).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
