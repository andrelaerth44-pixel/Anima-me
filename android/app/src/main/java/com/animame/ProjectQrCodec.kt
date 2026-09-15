package com.animame

import android.graphics.Bitmap
import com.animame.editor.AnimationDocument
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.math.min

object ProjectQrCodec {
    private const val PREFIX = "ANIMAME1:"

    fun payload(document: AnimationDocument, mediaCount: Int = 0): String {
        val json = JSONObject().apply {
            put("app", "Anima-me")
            put("version", 1)
            put("name", document.name)
            put("width", document.width)
            put("height", document.height)
            put("fps", document.fps)
            put("duration", document.duration)
            put("currentFrame", document.currentFrame)
            put("layers", document.layers.size)
            put("mediaFrames", mediaCount)
        }.toString()
        val compressed = Deflater(Deflater.BEST_SPEED).run {
            setInput(json.toByteArray(StandardCharsets.UTF_8)); finish()
            val out = ByteArray(4096); val n = deflate(out); end(); out.copyOf(n)
        }
        val encoded = android.util.Base64.encodeToString(compressed, android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
        return PREFIX + encoded
    }

    fun qrBitmap(payload: String, size: Int = 640): Bitmap {
        require(payload.toByteArray(StandardCharsets.UTF_8).size <= 2950) { "QR payload is too large" }
        val matrix: BitMatrix = QRCodeWriter().encode(payload, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) for (x in 0 until size) bitmap.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        return bitmap
    }

    fun decode(bitmap: Bitmap): String? {
        val scaled = if (bitmap.width > 1600 || bitmap.height > 1600) {
            Bitmap.createScaledBitmap(bitmap, min(1600, bitmap.width), min(1600, bitmap.height), true)
        } else bitmap
        return try {
            val pixels = IntArray(scaled.width * scaled.height)
            scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
            val source = RGBLuminanceSource(scaled.width, scaled.height, pixels)
            MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(source))).text
        } catch (_: Throwable) { null }
        finally { if (scaled !== bitmap) scaled.recycle() }
    }

    fun unpack(payload: String): JSONObject? {
        if (!payload.startsWith(PREFIX)) return null
        return try {
            val raw = android.util.Base64.decode(payload.removePrefix(PREFIX), android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
            val inflater = Inflater(); inflater.setInput(raw)
            val out = ByteArray(8192); val n = inflater.inflate(out); inflater.end()
            JSONObject(String(out, 0, n, StandardCharsets.UTF_8))
        } catch (_: Throwable) { null }
    }
}
