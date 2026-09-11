package com.animame

import android.graphics.Bitmap
import kotlin.math.sqrt

/** In-memory media sequence with a bounded bitmap budget to prevent OOM during imports. */
object MediaSequenceStore {
    private const val MEMORY_BUDGET_BYTES = 96L * 1024L * 1024L
    private val frames = mutableListOf<Bitmap>()
    val count: Int get() = frames.size

    fun replace(bitmaps: List<Bitmap>) {
        clear()
        if (bitmaps.isEmpty()) return
        val total = bitmaps.sumOf { it.byteCount.toLong() }
        if (total <= MEMORY_BUDGET_BYTES) {
            frames.addAll(bitmaps)
            return
        }
        val scale = sqrt(MEMORY_BUDGET_BYTES.toDouble() / total.toDouble()).coerceAtMost(1.0)
        bitmaps.forEach { source ->
            val w = (source.width * scale).toInt().coerceAtLeast(1)
            val h = (source.height * scale).toInt().coerceAtLeast(1)
            val scaled = if (w == source.width && h == source.height) source else Bitmap.createScaledBitmap(source, w, h, true)
            if (scaled !== source && !source.isRecycled) source.recycle()
            frames += scaled
        }
    }

    fun all(): List<Bitmap> = frames.toList()

    fun clear() {
        frames.forEach { if (!it.isRecycled) it.recycle() }
        frames.clear()
    }
}
