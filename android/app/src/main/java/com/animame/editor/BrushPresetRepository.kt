package com.animame.editor

object BrushPresetRepository {
    private val base = BrushCatalog.all()
    private val expanded = BrushLibraryBridge.presets()

    fun all(): List<BrushLibraryBridge.Preset> = base.map { BrushLibraryBridge.Preset(it.id,it.name,it.category,"Anima-me Base",it.category) } + expanded

    fun find(id: String): BrushSettings = if (id.startsWith("anima_")) {
        BrushLibraryV2.byId(id)?.let(BrushLibraryBridge::settings) ?: BrushDefaults.forPreset(id)
    } else BrushCatalog.settings(id)

    fun byCategory(category: String): List<BrushLibraryBridge.Preset> = all().filter { it.category.equals(category,true) || it.session.equals(category,true) }

    fun search(query: String): List<BrushLibraryBridge.Preset> = if (query.isBlank()) all() else all().filter {
        it.name.contains(query,true) || it.category.contains(query,true) || it.session.contains(query,true) || it.subcategory.contains(query,true)
    }
}
