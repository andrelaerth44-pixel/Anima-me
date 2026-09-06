package com.animame.editor

import android.graphics.PointF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LassoAndTransformTest {
    @Test fun lassoContainsInteriorAndRejectsExterior() {
        val polygon = listOf(PointF(0f,0f), PointF(100f,0f), PointF(100f,100f), PointF(0f,100f))
        assertTrue(LassoEngine.contains(polygon, PointF(50f,50f)))
        assertTrue(!LassoEngine.contains(polygon, PointF(150f,50f)))
    }

    @Test fun transformRoundTrip() {
        val t = DocumentTransform(640f, 360f, 1.75f, 27f, 42f, -18f)
        val p = t.toScreen(321f, 517f)
        val q = t.toDocument(p[0], p[1])
        assertEquals(321f, q[0], 0.001f)
        assertEquals(517f, q[1], 0.001f)
    }
}
