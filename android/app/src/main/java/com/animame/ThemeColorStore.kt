package com.animame

import android.content.Context

object ThemeColorStore {
    private const val PREFS = "anima_me_settings"
    private const val KEY_ACCENT = "interface_color"

    // Anima-me palette derived from the supplied launcher artwork.
    const val DEFAULT = 0xFF2FD8E8.toInt()
    const val NAVY_950 = 0xFF06142D.toInt()
    const val NAVY_900 = 0xFF0A1D3D.toInt()
    const val NAVY_800 = 0xFF102A52.toInt()
    const val BLUE = 0xFF2E6BFF.toInt()
    const val CYAN = 0xFF31D9E8.toInt()
    const val TEXT = 0xFFF5FBFF.toInt()
    const val MUTED = 0xFFA9C5E8.toInt()

    fun get(context: Context): Int {
        LiquifyUiBootstrap.install(context)
        ProfessionalToolsBootstrap.install(context)
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_ACCENT, DEFAULT)
    }

    fun set(context: Context, color: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ACCENT, color).apply()
    }
}
