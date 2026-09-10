package com.animame.editor

/** Session-level brush overrides for advanced brush tuning. Kept separate from the catalog. */
object BrushOverrides {
    private val overrides = mutableMapOf<String, BrushSettings>()

    @Synchronized
    fun put(id: String, settings: BrushSettings) {
        overrides[id] = settings.normalized()
    }

    @Synchronized
    fun get(id: String): BrushSettings? = overrides[id]

    @Synchronized
    fun resolve(id: String): BrushSettings = overrides[id] ?: BrushCatalog.settings(id)

    @Synchronized
    fun clear(id: String) {
        overrides.remove(id)
    }

    @Synchronized
    fun clearAll() {
        overrides.clear()
    }
}
