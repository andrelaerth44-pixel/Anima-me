package com.animame

import android.graphics.Canvas
import android.graphics.Paint

/** Compatibility overloads for the editor's dp helpers. */
fun Canvas.drawRect(left: Float, top: Int, right: Float, bottom: Int, paint: Paint) {
    drawRect(left, top.toFloat(), right, bottom.toFloat(), paint)
}
