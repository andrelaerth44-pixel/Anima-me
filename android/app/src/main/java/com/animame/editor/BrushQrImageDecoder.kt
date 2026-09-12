package com.animame.editor

import android.graphics.Bitmap
import android.graphics.Matrix
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.ResultPoint
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.EnumMap

/** Image-side QR decoder that preserves ZXing raw bytes for binary IPBZ payloads. */
object BrushQrImageDecoder {
    data class DecodeResult(
        val raw: ByteArray,
        val text: String?,
        val format: String,
        val points: Array<ResultPoint>?
    )

    fun decode(bitmap: Bitmap): DecodeResult {
        val candidates = listOf(bitmap, rotate(bitmap, 90f), rotate(bitmap, 180f), rotate(bitmap, 270f))
        var last: Throwable? = null
        for (candidate in candidates) {
            try {
                decodeOnce(candidate)?.let { return it }
            } catch (t: Throwable) {
                last = t
            }
        }
        throw (last ?: NotFoundException.getNotFoundInstance())
    }

    private fun decodeOnce(bitmap: Bitmap): DecodeResult? {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
            put(DecodeHintType.TRY_HARDER, true)
            put(DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
            put(DecodeHintType.ALSO_INVERTED, true)
        }
        val reader = MultiFormatReader()
        reader.setHints(hints)
        val result = reader.decode(BinaryBitmap(HybridBinarizer(source)))
        val raw = result.rawBytes ?: result.text?.toByteArray(Charsets.ISO_8859_1) ?: ByteArray(0)
        if (raw.isEmpty()) return null
        return DecodeResult(raw, result.text, result.barcodeFormat.name, result.resultPoints)
    }

    private fun rotate(source: Bitmap, degrees: Float): Bitmap =
        Bitmap.createBitmap(
            source, 0, 0, source.width, source.height,
            Matrix().apply { postRotate(degrees) }, true
        )
}
