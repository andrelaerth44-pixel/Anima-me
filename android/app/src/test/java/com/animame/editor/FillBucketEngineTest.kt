package com.animame.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class FillBucketEngineTest {
    @Test
    fun fillsConnectedRegionWithTolerance() {
        val pixels = intArrayOf(
            0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(),
            0xFF000000.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(),
            0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt()
        )
        val result = FillBucketEngine.fill(pixels, 3, 3, 0, 0, 0xFFFF0000.toInt())
        assertEquals(4, result.pixelsChanged)
        assertEquals(0xFFFF0000.toInt(), pixels[0])
        assertEquals(0xFFFF0000.toInt(), pixels[4])
        assertEquals(0xFFFFFFFF.toInt(), pixels[2])
    }
}
