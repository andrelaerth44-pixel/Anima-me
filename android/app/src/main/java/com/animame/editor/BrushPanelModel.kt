package com.animame.editor

data class BrushPanelState(
    var query: String = "",
    var category: String = "Todos",
    var selectedBrushId: String = "canvas_1"
) {
    fun filtered(): List<BrushCatalog.Preset> {
        val q = query.trim()
        return BrushCatalog.all().filter { brush ->
            (category == "Todos" || brush.category == category) &&
                (q.isEmpty() || brush.name.contains(q, true) || brush.category.contains(q, true))
        }
    }

    fun categories(): List<String> = listOf("Todos") + BrushCatalog.categories()
}
