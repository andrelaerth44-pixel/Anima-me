package com.animame.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentCodecTest {
    @Test fun roundTripKeepsAnimationContent() {
        val d = AnimationDocument(name="Demo", width=640, height=360, fps=30, duration=8)
        val layer = d.activeLayer
        val frame = layer.ensureFrame(3)
        frame.strokes += StrokeData(brushId="ink", color=0xFF112233.toInt(), size=17f, opacity=.72f, samples=mutableListOf(StrokeSample(10f,20f,.5f,1,0.2f,0.3f), StrokeSample(40f,60f,1f,2,0.4f,0.5f)))
        val restored = DocumentCodec.decode(DocumentCodec.encode(d))
        assertEquals("Demo", restored.name)
        assertEquals(640, restored.width); assertEquals(30, restored.fps)
        assertTrue(restored.layers.first().frameAt(3)!!.strokes.isNotEmpty())
        val s=restored.layers.first().frameAt(3)!!.strokes.first()
        assertEquals("ink", s.brushId); assertEquals(2, s.samples.size); assertEquals(40f, s.samples[1].x, .001f)
    }
}
