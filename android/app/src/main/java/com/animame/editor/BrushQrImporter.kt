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
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.EnumMap
import kotlin.math.max

/** Brush-panel QR pipeline: image detection, rotation/crop retries, validation and parsing. */
object BrushQrImporter {
    data class Result(val settings: BrushSettings, val codec: BrushQrCodec.ImportResult, val rawPayload: ByteArray, val ipbz: Boolean)

    fun importBitmap(bitmap: Bitmap, suggestedName: String = "Imported Brush"): Result? {
        decodeCandidates(bitmap).forEach { qr ->
            val raw = qr.rawBytes ?: qr.text.toByteArray(StandardCharsets.ISO_8859_1)
            val parsed = importBytes(raw, qr.text, suggestedName)
            if (parsed != null) return parsed
        }
        return null
    }

    fun importBytes(data: ByteArray, text: String = String(data, StandardCharsets.ISO_8859_1), suggestedName: String = "Imported Brush"): Result? {
        if (data.isEmpty()) return null
        val codec = runCatching { BrushQrCodec.decode(data) }.getOrNull() ?: return null
        val ipbz = text.startsWith("IPBZ") || codec.magic.equals("IPBZ", true) || data.size >= 4 && data.copyOfRange(0, 4).contentEquals(byteArrayOf('I'.code.toByte(), 'P'.code.toByte(), 'B'.code.toByte(), 'Z'.code.toByte()))
        val jsonText = sequenceOf(text, codec.payload?.toString(StandardCharsets.UTF_8) ?: "")
            .firstOrNull { it.trimStart().startsWith("{") || it.trimStart().startsWith("ANIMABRUSH2:") }
        val settings = if (jsonText != null) parseAnimaBrush(jsonText, data, suggestedName) else {
            // Preserve IPBZ bytes exactly. The settings fallback is deliberately conservative;
            // no proprietary field is guessed or silently reinterpreted.
            BrushDefaults.forPreset("imported_${fingerprint(data)}").copy(
                id = "imported_${fingerprint(data)}",
                name = suggestedName,
                category = "Imported / IPBZ"
            )
        }
        return Result(settings, codec, data.copyOf(), ipbz)
    }

    private fun parseAnimaBrush(text: String, raw: ByteArray, suggestedName: String): BrushSettings = runCatching {
        val normalized = text.trim().removePrefix("ANIMABRUSH2:").trim()
        val o = JSONObject(normalized)
        BrushSettings(
            id = o.optString("id", "imported_${fingerprint(raw)}"),
            name = o.optString("name", suggestedName),
            category = o.optString("category", "Imported"),
            algorithm = enumOr(o.optString("algorithm"), BrushAlgorithm.MONO),
            material = enumOr(o.optString("material"), BrushMaterial.INK),
            size = o.optDouble("size", 12.0).toFloat(),
            opacity = o.optDouble("opacity", 1.0).toFloat(),
            minSizeFactor = o.optDouble("minSizeFactor", .25).toFloat(),
            minOpacity = o.optDouble("minOpacity", .15).toFloat(),
            fadeOpacity = o.optDouble("fadeOpacity", 1.0).toFloat(),
            spacing = o.optDouble("spacing", .12).toFloat(),
            jitterPosition = o.optDouble("jitterPosition", 0.0).toFloat(),
            jitterThickness = o.optDouble("jitterThickness", 0.0).toFloat(),
            jitterOpacity = o.optDouble("jitterOpacity", 0.0).toFloat(),
            jitterSpacing = o.optDouble("jitterSpacing", 0.0).toFloat(),
            rotationJitter = o.optDouble("rotationJitter", 0.0).toFloat(),
            hueJitter = o.optDouble("hueJitter", 0.0).toFloat(),
            saturationJitter = o.optDouble("saturationJitter", 0.0).toFloat(),
            brightnessJitter = o.optDouble("brightnessJitter", 0.0).toFloat(),
            initialAngle = o.optDouble("initialAngle", 0.0).toFloat(),
            followRotation = o.optBoolean("followRotation", false),
            aspect = o.optDouble("aspect", 1.0).toFloat(),
            antialias = o.optBoolean("antialias", true),
            fadeStart = o.optDouble("fadeStart", 0.0).toFloat(),
            fadeEnd = o.optDouble("fadeEnd", 1.0).toFloat(),
            speedSizeFactor = o.optDouble("speedSizeFactor", 1.0).toFloat(),
            speedOpacityFactor = o.optDouble("speedOpacityFactor", 1.0).toFloat(),
            pressureSizeFactor = o.optDouble("pressureSizeFactor", 1.0).toFloat(),
            pressureOpacityFactor = o.optDouble("pressureOpacityFactor", 1.0).toFloat(),
            pressureExponent = o.optDouble("pressureExponent", 1.0).toFloat(),
            blur = o.optDouble("blur", 0.0).toFloat(),
            scatterSize = o.optDouble("scatterSize", 0.0).toFloat(),
            scatterDensity = o.optDouble("scatterDensity", 0.0).toFloat(),
            scatterDeviation = o.optDouble("scatterDeviation", 0.0).toFloat(),
            textureOpacity = o.optDouble("textureOpacity", 0.0).toFloat(),
            textureLowerLimit = o.optDouble("textureLowerLimit", 0.0).toFloat(),
            textureScale = o.optDouble("textureScale", 1.0).toFloat(),
            textureGrain = o.optDouble("textureGrain", 0.0).toFloat(),
            waterColorMix = o.optDouble("waterColorMix", 0.0).toFloat(),
            waterWetness = o.optDouble("waterWetness", 0.0).toFloat(),
            waterDragging = o.optDouble("waterDragging", 0.0).toFloat(),
            waterCorrection = o.optDouble("waterCorrection", 0.0).toFloat(),
            flow = o.optDouble("flow", 1.0).toFloat()
        ).normalized()
    }.getOrElse { BrushDefaults.forPreset("imported_${fingerprint(raw)}").copy(id = "imported_${fingerprint(raw)}", name = suggestedName, category = "Imported") }

    private inline fun <reified T : Enum<T>> enumOr(value: String, fallback: T): T = runCatching { enumValueOf<T>(value.uppercase()) }.getOrDefault(fallback)

    private fun fingerprint(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).take(6).joinToString("") { "%02x".format(it) }

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
            put(DecodeHintType.CHARACTER_SET, "ISO-8859-1")
        }
        val reader = MultiFormatReader()
        val seen = hashSetOf<String>()
        attempts.forEach { candidate ->
            try {
                val pixels = IntArray(candidate.width * candidate.height)
                candidate.getPixels(pixels, 0, candidate.width, 0, 0, candidate.width, candidate.height)
                val source = RGBLuminanceSource(candidate.width, candidate.height, pixels)
                val result = reader.decode(BinaryBitmap(HybridBinarizer(source)), hints)
                val key = result.text + ":" + (result.rawBytes?.size ?: 0)
                if (seen.add(key)) yield(result)
            } catch (_: Throwable) {
            } finally {
                if (candidate !== original) candidate.recycle()
                reader.reset()
            }
        }
    }
}
