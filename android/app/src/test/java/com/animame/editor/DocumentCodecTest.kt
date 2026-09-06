package com.animame.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentCodecTest {
 @Test fun roundTripKeepsAnimationContent(){val d=AnimationDocument(name="Demo",width=1920,height=1080,fps=30,duration=12,currentFrame=4,audioPath="/data/audio/test.bin",audioStartFrame=2,audioOffsetMs=125,audioVolume=.65f);val layer=d.activeLayer;val f=layer.ensureFrame(4);f.exposure=3;f.rasterPath="/data/imports/frame.png";f.strokes+=StrokeData(brushId="ink",color=0xFF112233.toInt(),size=18f,opacity=.8f,samples=mutableListOf(StrokeSample(10f,20f,.5f,11,3f,4f)));val r=DocumentCodec.decode(DocumentCodec.encode(d));assertEquals("Demo",r.name);assertEquals(1920,r.width);assertEquals(1080,r.height);assertEquals(30,r.fps);assertEquals(12,r.duration);assertEquals(4,r.currentFrame);assertEquals(d.audioPath,r.audioPath);assertEquals(2,r.audioStartFrame);assertEquals(125,r.audioOffsetMs);assertEquals(.65f,r.audioVolume,.0001f);assertTrue(r.layers.isNotEmpty());val rf=r.layers.firstNotNullOfOrNull{it.frameAt(4)};assertNotNull(rf);assertEquals(3,rf!!.exposure);assertEquals("/data/imports/frame.png",rf.rasterPath);assertTrue(rf.strokes.isNotEmpty());assertEquals("ink",rf.strokes[0].brushId);assertTrue(rf.strokes[0].samples.isNotEmpty());assertEquals(3f,rf.strokes[0].samples[0].tilt,.0001f)}
}
