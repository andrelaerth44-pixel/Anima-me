package com.animame

import android.content.res.Resources
import android.graphics.Path
import android.graphics.Rect

/** Int overload used by the compact editor layout helpers. */
fun Any.dp(value: Int): Int = (value * Resources.getSystem().displayMetrics.density).toInt()

/** Minimal region wrapper used by the lasso stroke hit-test. */
class Region {
    private val region = android.graphics.Region()

    fun setPath(source: Path, clip: Rect) {
        region.setPath(source, android.graphics.Region(clip))
    }

    fun contains(x: Int, y: Int): Boolean = region.contains(x, y)
}
