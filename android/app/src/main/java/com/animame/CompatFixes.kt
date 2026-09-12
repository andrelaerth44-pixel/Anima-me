package com.animame

import android.content.res.Resources
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF

/** Int overload used by the compact editor layout helpers. */
fun Any.dp(value: Int): Float = value * Resources.getSystem().displayMetrics.density

/** Minimal region wrapper used by the lasso stroke hit-test. */
class Region {
    private val path = Path()
    private val bounds = RectF()

    fun setPath(source: Path, clip: Rect) {
        path.reset()
        path.set(source)
        bounds.set(clip.left.toFloat(), clip.top.toFloat(), clip.right.toFloat(), clip.bottom.toFloat())
    }

    fun contains(x: Int, y: Int): Boolean {
        if (!bounds.contains(x.toFloat(), y.toFloat())) return false
        return path.contains(x.toFloat(), y.toFloat())
    }
}
