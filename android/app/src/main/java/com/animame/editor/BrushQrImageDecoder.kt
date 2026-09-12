package com.animame.editor

import android.graphics.Bitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * Image-side QR decoder for Anima-me brush import.
 * The QR image is decoded independently from the IPBZ parser so the parser can
 * continue preserving unknown binary records even when the QR image is noisy.
 */
object BrushQrImageDecoder {
    data class DecodeResult(
        val raw: ByteArray,
        val text: String?,
        val format: String
    )

    fun decode(bitmap: Bitmap): DecodeResult {
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, IntArray(bitmap.width * bitmap.height).also { pixels ->
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        })
        val reader = MultiFormatReader()
        val hints = mapOf(
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.POSSIBLE_FORMATS to listOf(com.google.zxing.BarcodeFormat.QR_CODE)
        )
        val result = try {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints)
        } catch (_: NotFoundException) {
            reader.reset()
            throw IllegalArgumentException("Nenhum QR foi encontrado na imagem")
        }
        val bytes = result.rawBytes ?: result.text?.toByteArray(Charsets.ISO_8859_1)
        ?: throw IllegalArgumentException("QR sem payload")
        return DecodeResult(bytes, result.text, result.barcodeFormat.name)
    }
}
