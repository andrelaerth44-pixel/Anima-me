package com.animame.editor

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

object BrushQrCodec {
    private val magic=byteArrayOf('I'.code.toByte(),'P'.code.toByte(),'B'.code.toByte(),'Z'.code.toByte())
    fun encode(s:BrushSettings):ByteArray{val raw=listOf(s.id,s.name,s.category.name,s.algorithm.name,s.size,s.opacity,s.spacing,s.aspect,s.angle,s.jitterPosition,s.jitterSize,s.jitterOpacity,s.scatter,s.particleDensity,s.particleSize,s.blur,s.texture,s.wetness,s.colorMix,s.dragging,s.pressureSize,s.pressureOpacity,s.speedSize,s.speedOpacity,s.pattern).joinToString("\u001f").toByteArray();val d=Deflater(9,true);d.setInput(raw);d.finish();val buf=ByteArray(4096);val out=ByteArrayOutputStream();while(!d.finished()){val n=d.deflate(buf);out.write(buf,0,n)};d.end();return magic+byteArrayOf(1)+out.toByteArray()}
    fun decode(bytes:ByteArray):BrushSettings?{if(bytes.size<6||!bytes.copyOfRange(0,4).contentEquals(magic))return null;val i=Inflater(true);i.setInput(bytes,5,bytes.size-5);val out=ByteArrayOutputStream();val buf=ByteArray(4096);return try{while(!i.finished()){val n=i.inflate(buf);if(n==0&&i.needsInput())break;out.write(buf,0,n)};i.end();parse(out.toByteArray().toString(Charsets.UTF_8))}catch(_:Throwable){i.end();null}}
    private fun parse(v:String):BrushSettings?{val a=v.split("\u001f");if(a.size<25)return null;val b=BrushDefaults.forPreset(a[0]);fun f(i:Int,d:Float)=a[i].toFloatOrNull()?:d;return b.copy(name=a[1],category=runCatching{BrushCategory.valueOf(a[2])}.getOrDefault(b.category),algorithm=runCatching{BrushAlgorithm.valueOf(a[3])}.getOrDefault(b.algorithm),size=f(4,b.size),opacity=f(5,b.opacity),spacing=f(6,b.spacing),aspect=f(7,b.aspect),angle=f(8,b.angle),jitterPosition=f(9,b.jitterPosition),jitterSize=f(10,b.jitterSize),jitterOpacity=f(11,b.jitterOpacity),scatter=f(12,b.scatter),particleDensity=f(13,b.particleDensity),particleSize=f(14,b.particleSize),blur=f(15,b.blur),texture=f(16,b.texture),wetness=f(17,b.wetness),colorMix=f(18,b.colorMix),dragging=f(19,b.dragging),pressureSize=f(20,b.pressureSize),pressureOpacity=f(21,b.pressureOpacity),speedSize=f(22,b.speedSize),speedOpacity=f(23,b.speedOpacity),pattern=a[24])}
    fun qrBitmap(payload:ByteArray,size:Int=768):Bitmap{val bits=QRCodeWriter().encode(Base64.getEncoder().encodeToString(payload),BarcodeFormat.QR_CODE,size,size);val b=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);for(y in 0 until size)for(x in 0 until size)b.setPixel(x,y,if(bits[x,y])Color.BLACK else Color.WHITE);return b}
    fun readQr(bitmap:Bitmap):ByteArray?{val w=bitmap.width;val h=bitmap.height;val px=IntArray(w*h);bitmap.getPixels(px,0,w,0,0,w,h);val src=RGBLuminanceSource(w,h,px);return try{val r=MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(src)));Base64.getDecoder().decode(r.text)}catch(_:Throwable){null}}
}
