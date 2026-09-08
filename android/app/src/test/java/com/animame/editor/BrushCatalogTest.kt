package com.animame.editor

import org.junit.Assert.assertTrue
import org.junit.Test

class BrushCatalogTest {
    @Test fun catalogContainsHundredsOfDistinctProceduralPresets() {
        val builtIns = BrushCatalog.presets.filter { it.family != "Imported" }
        assertTrue("Expected at least 800 built-in brushes", builtIns.size >= 800)
        assertTrue("Brush ids must be unique", builtIns.map { it.id }.toSet().size == builtIns.size)
        assertTrue("Variants must carry distinct names", builtIns.map { it.name }.toSet().size >= 800)
    }
}
