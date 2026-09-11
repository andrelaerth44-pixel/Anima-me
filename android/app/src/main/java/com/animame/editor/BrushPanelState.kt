package com.animame.editor

/** UI-facing brush library state. Rendering remains in BrushEngine; this class only manages selection/filtering. */
class BrushPanelState(
    private val catalog: BrushCatalog = BrushCatalog
) {
    var selectedBrushId: String = catalog.all().firstOrNull()?.id ?: "basic"
        private set
    var selectedCategory: String = "Todos"
        private set
    var query: String = ""
        private set

    fun setSelectedBrush(id: String) {
        if (catalog.all().any { it.id == id }) selectedBrushId = id
    }

    fun setCategory(category: String) {
        selectedCategory = category
    }

    fun setQuery(value: String) {
        query = value
    }

    fun categories(): List<String> = listOf("Todos") + catalog.categories()

    fun visibleBrushes(): List<BrushCatalog.Preset> {
        val q = query.trim()
        return catalog.all().filter { brush ->
            val categoryOk = selectedCategory == "Todos" || brush.category == selectedCategory
            val queryOk = q.isEmpty() || brush.name.contains(q, ignoreCase = true)
            categoryOk && queryOk
        }
    }

    fun selectedSettings(): BrushSettings = catalog.settings(selectedBrushId).normalized()
}
