package com.animame.editor

object BrushPresetRepository {
    fun all(): List<BrushCatalog.Preset> = BrushCatalog.all()

    fun find(id: String): BrushSettings = when {
        id.startsWith("canvas_") -> BrushBehaviorLibrary.settings(id)
        else -> BrushCatalog.settings(id)
    }

    fun byCategory(category: String): List<BrushCatalog.Preset> = all().filter { it.category.equals(category, true) }

    fun search(query: String): List<BrushCatalog.Preset> = if (query.isBlank()) {
        all()
    } else {
        all().filter { it.name.contains(query, true) || it.category.contains(query, true) }
    }
}
