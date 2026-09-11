package com.animame.editor

import android.graphics.Bitmap
import android.graphics.Matrix
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import java.nio.charset.StandardCharsets
import java.util.EnumMap
import kotlin.math.max

/** Brush-panel QR pipeline. Native Anima-me format is preferred; IPBZ remains legacy-only. */
object BrushQrImporter {
    data class Result(val settings: BrushSettings, val codec: BrushQrCodec.ImportResult?, val rawPayload: ByteArray, val ipbz: Boolean)

    fun importBitmap(bitmap: Bitmap, suggestedName: String = "Imported Brush"): Result? {
        decodeCandidates(bitmap).forEach { qr ->
            val raw = qr.text.toByteArray(StandardCharsets.UTF_8)
            val parsed = importBytes(raw, qr.text, suggestedName)
            if (parsed != null) return parsed
        }
        return null
    }

    fun importBytes(data: ByteArray, text: String = String(data, StandardCharsets.UTF_8), suggestedName: String = "Imported Brush"): Result? {
        if (data.isEmpty()) return null

        // First-class native Anima-me QR: full BrushSettings + checksum.
        if (text.startsWith(AnimaBrushQrCodec.PREFIX)) {
            val native = runCatching { AnimaBrushQrCodec.decode(text) }.getOrNull() ?: return null
            return Result(native.settings, null, data.copyOf(), false)
        }

        // Legacy compatibility path for IPBZ. We never convert unknown proprietary fields;
        // this path only preserves the payload and creates a conservative placeholder brush.
        val codec = runCatching { BrushQrCodec.decode(data) }.getOrNull() ?: return null
        val ipbz = codec.magic.equals("IPBZ", true) || text.startsWith("IPBZ")
        if (!ipbz) return null
        val id = "imported_ipbz_${codec.version ?: 0}_${data.size}"
        val settings = BrushDefaults.forPreset(id).copy(id = id, name = suggestedName, category = "Imported / IPBZ")
        return Result(settings, codec, data.copyOf(), true)
    }

    private fun decodeCandidates(original: Bitmap): Sequence<Result> = sequence {
        val attempts = mutableListOf<Bitmap>()
        attempts += original
        listOf(90f, 180f, 270f).forEach { degrees ->
            attempts += Bitmap.createBitmap(original, 0, 0, original.width, original.height, Matrix().apply { postRotate(degrees) }, true)
        }
        val w = original.width
        val h = original.height
        if (w > 16 && h > 16) {
            val x = w / 8
            val y = h / 8
            attempts += Bitmap.createBitmap(original, x, y, max(1, w - x * 2), max(1, h - y * 2))
        }
        if (w > 32) attempts += Bitmap.createBitmap(original, 0, 0, w / 2, h)
        if (w > 32) attempts += Bitmap.createBitmap(original, w / 2, 0, w - w / 2, h)
        if (h > 32) attempts += Bitmap.createBitmap(original, 0, 0, w, h / 2)
        if (h > 32) attempts += Bitmap.createBitmap(original, 0, h / 2, w, h - h / 2)

        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.TRY_HARDER, true)
            put(DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
            put(DecodeHintType.CHARACTER_SET, "UTF-8")
        }
        val reader = MultiFormatReader()
        val seen = hashSetOf<String>()
        attempts.forEach { candidate ->
            try {
                val pixels = IntArray(candidate.width * candidate.height)
                candidate.getPixels(pixels, 0, candidate.width, 0, 0, candidate.width, candidate.height)
                val source = RGBLuminanceSource(candidate.width, candidate.height, pixels)
                val result = reader.decode(BinaryBitmap(HybridBinarizer(source)), hints)
                val key = result.text
                if (seen.add(key)) yield(result)
            } catch (_: Throwable) {
            } finally {
                if (candidate !== original) candidate.recycle()
                reader.reset()
            }
        }
    }
}
