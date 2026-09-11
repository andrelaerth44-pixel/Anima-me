package com.animame

import android.graphics.Bitmap
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import kotlin.math.abs

/** Small self-contained GIF89a encoder for imported image sequences. */
object GifSequenceEncoder {
    fun encode(frames: List<Bitmap>, out: OutputStream, delayMs: Int = 100) {
        require(frames.isNotEmpty()) { "No frames" }
        val w = frames.first().width
        val h = frames.first().height
        require(w in 1..4096 && h in 1..4096)
        out.write("GIF89a".toByteArray(StandardCharsets.US_ASCII))
        writeShort(out, w); writeShort(out, h)
        out.write(0xF7); out.write(0); out.write(0)
        val palette = palette(frames.first())
        out.write(palette)
        out.write(0x21); out.write(0xFF); out.write(11); out.write("NETSCAPE2.0".toByteArray(StandardCharsets.US_ASCII)); out.write(3); out.write(1); writeShort(out, 0); out.write(0)
        frames.forEachIndexed { index, source ->
            val bitmap = if (source.width == w && source.height == h) source else Bitmap.createScaledBitmap(source, w, h, true)
            out.write(0x21); out.write(0xF9); out.write(4); out.write(0); writeShort(out, (delayMs / 10).coerceAtLeast(1)); out.write(0); out.write(0)
            out.write(0x2C); writeShort(out, 0); writeShort(out, 0); writeShort(out, w); writeShort(out, h); out.write(0)
            val indices = quantize(bitmap, palette)
            val minCodeSize = 8
            out.write(minCodeSize)
            writeLzw(out, indices, minCodeSize)
            if (bitmap !== source) bitmap.recycle()
        }
        out.write(0x3B); out.flush()
    }

    private fun palette(bitmap: Bitmap): ByteArray {
        val counts = LinkedHashMap<Int, Int>()
        val step = maxOf(1, (bitmap.width * bitmap.height) / 60000)
        var y = 0
        while (y < bitmap.height) {
            var x = 0
            while (x < bitmap.width) {
                val c = bitmap.getPixel(x, y)
                val key = (c and 0x00FFFFFF) or 0xFF000000.toInt()
                counts[key] = (counts[key] ?: 0) + 1
                x += step
            }
            y += step
        }
        val colors = counts.entries.sortedByDescending { it.value }.take(256).map { it.key }.toMutableList()
        while (colors.size < 256) colors += 0xFF000000.toInt()
        return ByteArray(768).also { out -> colors.forEachIndexed { i, c -> out[i*3]=((c shr 16) and 255).toByte(); out[i*3+1]=((c shr 8) and 255).toByte(); out[i*3+2]=(c and 255).toByte() } }
    }

    private fun quantize(bitmap: Bitmap, palette: ByteArray): ByteArray {
        val out = ByteArray(bitmap.width * bitmap.height)
        for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
            val c = bitmap.getPixel(x, y); var best = 0; var bestD = Int.MAX_VALUE
            for (i in 0 until 256) {
                val dr=((c shr 16) and 255)-(palette[i*3].toInt() and 255)
                val dg=((c shr 8) and 255)-(palette[i*3+1].toInt() and 255)
                val db=(c and 255)-(palette[i*3+2].toInt() and 255)
                val d=dr*dr+dg*dg+db*db
                if(d<bestD){bestD=d;best=i;if(d==0)break}
            }
            out[y*bitmap.width+x]=best.toByte()
        }
        return out
    }

    private fun writeLzw(out: OutputStream, pixels: ByteArray, minCodeSize: Int) {
        val clear=1 shl minCodeSize; val end=clear+1; var codeSize=minCodeSize+1; var next=clear+2
        val dict=HashMap<Long,Int>(); val bytes=ArrayList<Byte>(); var bit=0; var acc=0
        fun emit(code:Int){acc=acc or (code shl bit);bit+=codeSize;while(bit>=8){bytes.add((acc and 255).toByte());acc=acc ushr 8;bit-=8}}
        emit(clear); var prefix=pixels[0].toInt() and 255
        for(i in 1 until pixels.size){val k=pixels[i].toInt() and 255;val key=(prefix.toLong() shl 8) or k.toLong();val found=dict[key]
            if(found!=null){prefix=found}
            else {emit(prefix);if(next<4096){dict[key]=next++;if(next==(1 shl codeSize) && codeSize<12)codeSize++}else{emit(clear);dict.clear();next=clear+2;codeSize=minCodeSize+1};prefix=k}}
        emit(prefix);emit(end);if(bit>0)bytes.add((acc and 255).toByte())
        var p=0;while(p<bytes.size){val n=minOf(255,bytes.size-p);out.write(n);out.write(bytes.subList(p,p+n).toByteArray());p+=n};out.write(0)
    }
    private fun writeShort(out:OutputStream,v:Int){out.write(v and 255);out.write((v ushr 8) and 255)}
}
