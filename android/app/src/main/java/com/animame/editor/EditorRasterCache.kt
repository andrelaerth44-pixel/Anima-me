package com.animame.editor

import android.graphics.Bitmap
import android.graphics.Canvas

/** Reusable screen-sized raster surface for the editor's committed frame content. */
class EditorRasterCache {
    private var bitmap: Bitmap? = null
    private var width = 0
    private var height = 0
    private var version = Long.MIN_VALUE

    fun draw(canvas: Canvas, width: Int, height: Int, contentVersion: Long, renderer: (Canvas) -> Unit) {
        val target = obtain(width, height, contentVersion)
        if (version != contentVersion || this.width != width || this.height != height) {
            target.eraseColor(android.graphics.Color.TRANSPARENT)
            renderer(Canvas(target))
            version = contentVersion
        }
        canvas.drawBitmap(target, 0f, 0f, null)
    }

    fun invalidate() { version = Long.MIN_VALUE }

    fun recycle() {
        bitmap?.let { if (!it.isRecycled) it.recycle() }
        bitmap = null
        width = 0
        height = 0
        version = Long.MIN_VALUE
    }

    private fun obtain(w: Int, h: Int, contentVersion: Long): Bitmap {
        if (bitmap == null || bitmap!!.isRecycled || width != w || height != h) {
            bitmap?.let { if (!it.isRecycled) it.recycle() }
            bitmap = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            width = w
            height = h
            version = Long.MIN_VALUE
        }
        return bitmap!!
    }
}
