package com.animame.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquifyEngineTest {
    private val samples = listOf(
        StrokeSample(0f, 0f, 1f, 0L),
        StrokeSample(50f, 0f, 1f, 1L),
        StrokeSample(100f, 0f, 1f, 2L)
    )

    @Test
    fun outsideRadiusRemainsUnchanged() {
        val result = LiquifyEngine.apply(
            samples, 0f, 0f, 20f, 0f,
            LiquifyEngine.Settings(radius = 40f, strength = 1f)
        )
        assertEquals(samples[2], result[2])
    }

    @Test
    fun pushMovesCenterMoreThanEdge() {
        val result = LiquifyEngine.apply(
            samples, 50f, 0f, 20f, 0f,
            LiquifyEngine.Settings(radius = 100f, strength = 1f)
        )
        assertTrue(result[1].x > samples[1].x)
        assertTrue(result[0].x >= samples[0].x)
    }

    @Test
    fun pinchMovesPointTowardCenter() {
        val result = LiquifyEngine.apply(
            samples, 50f, 0f, 0f, 0f,
            LiquifyEngine.Settings(radius = 100f, strength = 1f, mode = LiquifyEngine.Mode.PINCH)
        )
        assertTrue(result[0].x > samples[0].x)
        assertTrue(result[2].x < samples[2].x)
    }

    @Test
    fun bloatMovesPointAwayFromCenter() {
        val result = LiquifyEngine.apply(
            samples, 50f, 0f, 0f, 0f,
            LiquifyEngine.Settings(radius = 100f, strength = 1f, mode = LiquifyEngine.Mode.BLOAT)
        )
        assertTrue(result[0].x < samples[0].x)
        assertTrue(result[2].x > samples[2].x)
    }
}
