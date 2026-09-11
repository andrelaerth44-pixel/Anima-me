package com.animame

import android.content.Context

object ThemeColorStore {
    private const val PREFS = "anima_me_settings"
    private const val KEY_ACCENT = "interface_color"
    const val DEFAULT = 0xFF26A69A.toInt()

    fun get(context: Context): Int {
        LiquifyUiBootstrap.install(context)
        ProfessionalToolsBootstrap.install(context)
        RoughUiStateBootstrap.install(context)
        ContinuousStrokeOverlayBootstrap.install(context)
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_ACCENT, DEFAULT)
    }

    fun set(context: Context, color: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ACCENT, color).apply()
    }
}
