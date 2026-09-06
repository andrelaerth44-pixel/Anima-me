package com.animame.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentCodecTest {
 @Test fun roundTripKeepsProjectMetadata(){val d=AnimationDocument(name="Demo",width=1920,height=1080,fps=30,duration=12,currentFrame=4,audioPath="/data/audio/test.bin",audioStartFrame=2,audioOffsetMs=125,audioVolume=.65f);val layer=d.activeLayer;layer.ensureFrame(4).strokes+=StrokeData(brushId="ink",samples=mutableListOf(StrokeSample(10f,20f)));val r=DocumentCodec.decode(DocumentCodec.encode(d));assertEquals("Demo",r.name);assertEquals(1920,r.width);assertEquals(1080,r.height);assertEquals(30,r.fps);assertEquals(12,r.duration);assertEquals(4,r.currentFrame);assertEquals(d.audioPath,r.audioPath);assertEquals(2,r.audioStartFrame);assertEquals(125,r.audioOffsetMs);assertEquals(.65f,r.audioVolume,.0001f);assertTrue(r.layers.isNotEmpty())}
}
