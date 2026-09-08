package com.animame.editor

import org.junit.Assert.assertTrue
import org.junit.Test

class BrushCatalogTest {
    @Test fun catalogContainsHundredsOfDistinctProceduralPresets() {
        val builtIns = BrushCatalog.presets.filter { it.family != "Imported" }
        assertTrue("Expected at least 800 built-in brushes", builtIns.size >= 800)
        assertTrue("Brush ids must be unique", builtIns.map { it.id }.toSet().size == builtIns.size)
        assertTrue("Brush names must be unique", builtIns.map { it.name }.toSet().size == builtIns.size)
        assertTrue("Brush settings must be unique", builtIns.map { it.defaults }.toSet().size == builtIns.size)
    }
}
