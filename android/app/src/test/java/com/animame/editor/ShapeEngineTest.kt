package com.animame.editor

import android.graphics.PointF
import org.junit.Assert.assertTrue
import org.junit.Test

class ShapeEngineTest {
 @Test fun rectangleProducesClosedPath(){val p=ShapeEngine.rectangle(PointF(80f,50f),PointF(10f,120f));assertTrue(p.size==5);assertTrue(p.first().x==p.last().x&&p.first().y==p.last().y)}
 @Test fun ellipseProducesClosedPath(){val p=ShapeEngine.ellipse(PointF(0f,0f),PointF(100f,60f),48);assertTrue(p.size==49);assertTrue(kotlin.math.abs(p.first().x-p.last().x)<.001f&&kotlin.math.abs(p.first().y-p.last().y)<.001f)}
}
