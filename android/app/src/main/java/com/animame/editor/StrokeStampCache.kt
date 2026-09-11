package com.animame.editor

import java.util.LinkedHashMap

/** Avoids re-running the procedural brush engine for every Canvas.onDraw(). */
class StrokeStampCache(private val maxEntries: Int = 4096) {
    private data class Entry(val signature: Int, val stamps: List<BrushEngine.Stamp>)

    private val cache = object : LinkedHashMap<String, Entry>(128, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>?): Boolean = size > maxEntries
    }

    @Synchronized
    fun get(stroke: StrokeData): List<BrushEngine.Stamp> {
        val settings = stroke.resolvedBrushSettings()
        val samples = stroke.samples
        val signature = 31 * settings.hashCode() + samples.size * 31 + (samples.lastOrNull()?.hashCode() ?: 0)
        val old = cache[stroke.id]
        if (old != null && old.signature == signature) return old.stamps
        val stamps = BrushEngine.stamps(samples, settings, stroke.id.hashCode().toLong())
        cache[stroke.id] = Entry(signature, stamps)
        return stamps
    }

    @Synchronized fun invalidate(strokeId: String? = null) {
        if (strokeId == null) cache.clear() else cache.remove(strokeId)
    }
}
