package com.animame.editor

import android.graphics.Canvas
import android.graphics.Paint

fun text(canvas: Canvas, value: String, x: Float, y: Float, color: Int, size: Float) {
    val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
        style = Paint.Style.FILL
    }
    canvas.drawText(value, x, y, p)
}
