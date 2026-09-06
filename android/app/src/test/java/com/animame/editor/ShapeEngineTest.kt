package com.animame.editor

import android.graphics.PointF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeEngineTest {
    @Test fun rectangleIsClosedAndUsesBounds() {
        val p = ShapeEngine.rectangle(PointF(80f, 50f), PointF(10f, 120f))
        assertEquals(5, p.size)
        assertEquals(p.first().x, p.last().x, 0.001f)
        assertEquals(p.first().y, p.last().y, 0.001f)
        assertEquals(10f, p[0].x, 0.001f)
        assertEquals(50f, p[0].y, 0.001f)
    }

    @Test fun ellipseIsClosed() {
        val p = ShapeEngine.ellipse(PointF(0f, 0f), PointF(100f, 60f), 48)
        assertEquals(p.first().x, p.last().x, 0.001f)
        assertEquals(p.first().y, p.last().y, 0.001f)
        assertTrue(p.size > 40)
    }
}
