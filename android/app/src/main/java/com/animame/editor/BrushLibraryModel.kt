package com.animame.editor

/** UI-facing model for the complete brush library. Keeps search/category logic outside the canvas. */
class BrushLibraryModel {
    private val catalog = BrushCatalog.all()
    var query: String = ""
    var category: String = "Todas"

    fun categories(): List<String> = listOf("Todas") + catalog.map { it.category }.distinct()

    fun filtered(): List<BrushCatalog.Preset> {
        val q = query.trim()
        return catalog.filter { preset ->
            val categoryOk = category == "Todas" || preset.category == category
            val queryOk = q.isBlank() || preset.name.contains(q, ignoreCase = true)
            categoryOk && queryOk
        }
    }

    fun settings(preset: BrushCatalog.Preset): BrushSettings = BrushCatalog.settings(preset.id)
}
