package com.animame.editor

import android.graphics.PointF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeEngineTest {
 @Test fun rectangleIsClosedAndUsesBounds(){val p=ShapeEngine.rectangle(PointF(80f,50f),PointF(10f,120f));assertEquals(5,p.size);assertEquals(p.first().x,p.last().x,.001f);assertEquals(p.first().y,p.last().y,.001f);assertTrue(p.any{it.x==10f});assertTrue(p.any{it.y==50f})}
 @Test fun ellipseIsClosed(){val p=ShapeEngine.ellipse(PointF(0f,0f),PointF(100f,60f),48);assertEquals(p.first().x,p.last().x,.001f);assertEquals(p.first().y,p.last().y,.001f);assertTrue(p.size>40)}
}
