package com.animame.editor

import android.graphics.PointF
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LassoAndTransformTest {
 @Test fun lassoContainsInteriorAndRejectsExterior(){val p=listOf(PointF(0f,0f),PointF(100f,0f),PointF(100f,100f),PointF(0f,100f));assertTrue(LassoEngine.contains(p,PointF(50f,50f)));assertFalse(LassoEngine.contains(p,PointF(150f,50f)))}
 @Test fun transformRoundTrip(){val t=DocumentTransform(640f,360f,1.75f,27f,42f,-18f);val p=t.toScreen(321f,517f);val q=t.toDocument(p[0],p[1]);assertTrue(kotlin.math.abs(q[0]-321f)<.01f&&kotlin.math.abs(q[1]-517f)<.01f)}
}
