package com.animame

import android.content.Context

object ThemeColorStore {
    private const val PREFS = "anima_me_settings"
    private const val KEY_ACCENT = "interface_color"
    private const val LEGACY_DEFAULT = 0xFF26A69A.toInt()

    // Base identity from the Anima-me icon: deep blue, almost black.
    const val DEFAULT = 0xFF071A2B.toInt()

    fun get(context: Context): Int {
        LiquifyUiBootstrap.install(context)
        ProfessionalToolsBootstrap.install(context)
        RoughUiStateBootstrap.install(context)
        ContinuousStrokeOverlayBootstrap.install(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getInt(KEY_ACCENT, DEFAULT)
        if (stored == LEGACY_DEFAULT) {
            prefs.edit().putInt(KEY_ACCENT, DEFAULT).apply()
            return DEFAULT
        }
        return stored
    }

    fun set(context: Context, color: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ACCENT, color).apply()
    }
}
