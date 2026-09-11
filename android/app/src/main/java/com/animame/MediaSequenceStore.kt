package com.animame

import android.graphics.Bitmap

/** In-memory media sequence imported from still images. Kept separate from editor strokes. */
object MediaSequenceStore {
    private val frames = mutableListOf<Bitmap>()
    val count: Int get() = frames.size
    fun replace(bitmaps: List<Bitmap>) {
        frames.forEach { if (!it.isRecycled) it.recycle() }
        frames.clear()
        frames.addAll(bitmaps)
    }
    fun all(): List<Bitmap> = frames.toList()
    fun clear() {
        frames.forEach { if (!it.isRecycled) it.recycle() }
        frames.clear()
    }
}
