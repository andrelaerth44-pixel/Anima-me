package com.animame.editor

import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentCodecTest {
 @Test fun encodeProducesCompleteProjectPayload(){val d=AnimationDocument(name="Demo",width=1920,height=1080,fps=30,duration=12,audioPath="/data/audio/test.bin",audioStartFrame=2,audioOffsetMs=125,audioVolume=.65f);d.activeLayer.ensureFrame(4).strokes+=StrokeData(brushId="ink",samples=mutableListOf(StrokeSample(10f,20f)));val json=DocumentCodec.encode(d);assertTrue(json.contains("Demo"));assertTrue(json.contains("audioPath"));assertTrue(json.contains("layers"));assertTrue(json.contains("frames"));assertTrue(json.contains("samples"))}
}
