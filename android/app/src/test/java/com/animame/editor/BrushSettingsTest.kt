package com.animame.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrushSettingsTest {
    @Test fun normalizedClampsValues() {
        val s = BrushSettings(size = 999f, sizeMin = 4f, sizeMax = 20f, opacity = 2f, flow = -1f).normalized()
        assertEquals(20f, s.size, 0.001f)
        assertEquals(1f, s.opacity, 0.001f)
        assertEquals(0f, s.flow, 0.001f)
    }

    @Test fun pressureChangesRadiusWhenEnabled() {
        val s = BrushSettings(size = 10f, sizeMin = 1f, sizeMax = 100f, pressureSize = 1f)
        assertTrue(s.radiusFor(1f, 0f) > s.radiusFor(0f, 0f))
    }

    @Test fun opacityAlwaysStaysInRange() {
        val s = BrushSettings(opacity = 1f, alpha = 1f, flow = 1f, pressureOpacity = 1f)
        assertTrue(s.opacityFor(0f, 0f, 0f, 0f) in 0f..1f)
        assertTrue(s.opacityFor(1f, 0f, 0f, 0f) in 0f..1f)
    }
}
