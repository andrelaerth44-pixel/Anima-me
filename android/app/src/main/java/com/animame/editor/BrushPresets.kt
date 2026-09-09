package com.animame.editor

/** Compatibility aliases for UI code that expects a preset list. */
object BrushPresets {
    val all get() = BrushCatalog.all()
    val categories get() = BrushCatalog.categories()
    fun find(id: String) = BrushCatalog.settings(id)
}
