package com.animame

import android.content.Context
import androidx.annotation.ColorInt

object ThemeColorStore {
    private const val PREFS = "anima_me_settings"
    private const val KEY_ACCENT = "interface_color"
    const val DEFAULT = 0xFF26A69A.toInt()

    @ColorInt
    fun get(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getInt(KEY_ACCENT, DEFAULT)

    fun set(context: Context, @ColorInt color: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_ACCENT, color).apply()
    }
}
