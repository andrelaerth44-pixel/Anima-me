package com.animame

import android.graphics.Bitmap
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import kotlin.math.min

/** Native Android H.264 encoder. Uses MediaCodec/MediaMuxer, no FFmpeg dependency. */
object VideoSequenceEncoder {
    fun encode(frames: List<Bitmap>, output: File, fps: Int = 24) {
        require(frames.isNotEmpty()) { "No frames" }
        val first = frames.first(); val width = first.width and -2; val height = first.height and -2
        require(width > 0 && height > 0) { "Video dimensions must be at least 2x2" }
        val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
            setInteger(MediaFormat.KEY_BIT_RATE, (width.toLong()*height.toLong()*fps/4).coerceIn(500_000L, 20_000_000L).toInt())
            setInteger(MediaFormat.KEY_FRAME_RATE, fps.coerceIn(1,60))
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }
        val codec=MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);codec.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);codec.start()
        var muxer:MediaMuxer?=null;var track=-1;var started=false;val info=MediaCodec.BufferInfo();var pts=0L;val frameUs=1_000_000L/fps.coerceIn(1,60)
        try {
            frames.forEach { bitmap ->
                var queued=false
                while(!queued){
                    val index=codec.dequeueInputBuffer(10_000)
                    if(index>=0){val input=codec.getInputBuffer(index)!!;input.clear();putI420(bitmap,width,height,input);codec.queueInputBuffer(index,0,input.position(),pts,0);pts+=frameUs;queued=true}
                    drain(codec,info,{if(!started){muxer=MediaMuxer(output.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);track=muxer!!.addTrack(it);muxer!!.start();started=true};muxer!!.writeSampleData(track,it.second,it.first)})
                }
            }
            val eos=codec.dequeueInputBuffer(100_000);if(eos>=0)codec.queueInputBuffer(eos,0,0,pts,MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            var done=false;while(!done){drain(codec,info,{if(!started){muxer=MediaMuxer(output.absolutePath,MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);track=muxer!!.addTrack(it);muxer!!.start();started=true};muxer!!.writeSampleData(track,it.second,it.first)});done=info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM!=0}
        } finally { try{codec.stop()}catch(_:Throwable){};codec.release();if(started)muxer?.stop();muxer?.release() }
    }

    private fun drain(codec:MediaCodec,info:MediaCodec.BufferInfo,consumer:(Pair<MediaCodec.BufferInfo,java.nio.ByteBuffer>)->Unit){
        while(true){val index=codec.dequeueOutputBuffer(info,0);when{index==MediaCodec.INFO_TRY_AGAIN_LATER->return;index==MediaCodec.INFO_OUTPUT_FORMAT_CHANGED->return;index>=0->{val data=codec.getOutputBuffer(index)!!;if(info.size>0){data.position(info.offset);data.limit(info.offset+info.size);consumer(Pair(MediaCodec.BufferInfo().also{it.set(info.offset,info.size,info.presentationTimeUs,info.flags)},data.slice()))};codec.releaseOutputBuffer(index,false)}}}
    }
    private fun putI420(src:Bitmap,w:Int,h:Int,buffer:java.nio.ByteBuffer){val pixels=IntArray(w*h);src.getPixels(pixels,0,src.width,0,0,min(w,src.width),min(h,src.height));val y=ByteArray(w*h);val u=ByteArray(w*h/4);val v=ByteArray(w*h/4);for(j in 0 until h){for(i in 0 until w){val c=pixels[j*src.width+i];val r=(c shr 16) and 255;val g=(c shr 8) and 255;val b=c and 255;y[j*w+i]=((66*r+129*g+25*b+128 shr 8)+16).coerceIn(0,255).toByte()}};for(j in 0 until h step 2)for(i in 0 until w step 2){var sr=0;var sg=0;var sb=0;repeat(2){dy->repeat(2){dx->val c=pixels[(j+dy)*src.width+i+dx];sr+=(c shr 16) and 255;sg+=(c shr 8) and 255;sb+=c and 255}};val idx=(j/2)*(w/2)+i/2;val r=sr/4;val g=sg/4;val b=sb/4;u[idx]=(((-38*r-74*g+112*b+128 shr 8)+128).coerceIn(0,255)).toByte();v[idx]=(((112*r-94*g-18*b+128 shr 8)+128).coerceIn(0,255)).toByte()};buffer.put(y);buffer.put(u);buffer.put(v)}
}
