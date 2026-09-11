package com.animame.editor

import android.graphics.Bitmap
import android.util.Base64
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.EnumMap
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * Native Anima-me brush QR format.
 *
 * It is deliberately independent from ibisPaint/IPBZ. A QR carries the complete
 * procedural BrushSettings needed by Anima-me, compressed and checksummed.
 */
object AnimaBrushQrCodec {
    const val PREFIX = "ANIMAME-BRUSH1:"
    private const val MAX_QR_BYTES = 2900

    data class Payload(val settings: BrushSettings, val rawCompressed: ByteArray, val checksum: String)

    fun encode(settings: BrushSettings): String {
        val normalized = settings.normalized()
        val json = toJson(normalized).toString().toByteArray(StandardCharsets.UTF_8)
        val compressed = deflate(json)
        require(compressed.size <= MAX_QR_BYTES) { "Pincel grande demais para um único QR (${compressed.size} bytes)" }
        val checksum = sha256(json).take(16)
        return PREFIX + checksum + ":" + Base64.encodeToString(compressed, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    fun decode(text: String): Payload {
        require(text.startsWith(PREFIX)) { "QR não pertence ao formato Anima-me" }
        val rest = text.removePrefix(PREFIX)
        val split = rest.indexOf(':')
        require(split > 0) { "QR de pincel inválido" }
        val expected = rest.substring(0, split)
        val compressed = Base64.decode(rest.substring(split + 1), Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val jsonBytes = inflate(compressed)
        val actual = sha256(jsonBytes).take(16)
        require(expected.equals(actual, true)) { "Checksum do pincel QR inválido" }
        return Payload(fromJson(JSONObject(String(jsonBytes, StandardCharsets.UTF_8))), compressed, actual)
    }

    fun qrBitmap(settings: BrushSettings, size: Int = 768): Bitmap {
        val payload = encode(settings)
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.MARGIN, 3)
            put(EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M)
        }
        val matrix: BitMatrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) for (x in 0 until size) bitmap.setPixel(x, y, if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        return bitmap
    }

    fun decodeBitmap(bitmap: Bitmap): Payload? {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return runCatching {
            val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
            val result = MultiFormatReader().decode(com.google.zxing.BinaryBitmap(HybridBinarizer(source)))
            decode(result.text)
        }.getOrNull()
    }

    private fun toJson(s: BrushSettings) = JSONObject().apply {
        put("format", "anima-me-brush"); put("version", 1)
        put("id", s.id); put("name", s.name); put("category", s.category)
        put("algorithm", s.algorithm.name); put("material", s.material.name)
        put("size", s.size); put("opacity", s.opacity); put("minSizeFactor", s.minSizeFactor); put("minOpacity", s.minOpacity)
        put("fadeOpacity", s.fadeOpacity); put("spacing", s.spacing)
        put("minThickness", s.minThickness); put("maxThickness", s.maxThickness); put("startThickness", s.startThickness); put("endThickness", s.endThickness)
        put("startOpacity", s.startOpacity); put("endOpacity", s.endOpacity)
        put("jitterPosition", s.jitterPosition); put("jitterThickness", s.jitterThickness); put("jitterOpacity", s.jitterOpacity); put("jitterSpacing", s.jitterSpacing)
        put("rotationJitter", s.rotationJitter); put("hueJitter", s.hueJitter); put("saturationJitter", s.saturationJitter); put("brightnessJitter", s.brightnessJitter); put("initialAngle", s.initialAngle)
        put("followRotation", s.followRotation); put("aspect", s.aspect); put("antialias", s.antialias)
        put("constantOpacity", s.constantOpacity); put("addOpacity", s.addOpacity); put("separateStroke", s.separateStroke); put("thinSpacing", s.thinSpacing)
        put("fadeStart", s.fadeStart); put("fadeEnd", s.fadeEnd)
        put("speedSizeFactor", s.speedSizeFactor); put("speedOpacityFactor", s.speedOpacityFactor); put("speedBlurFactor", s.speedBlurFactor)
        put("pressureSizeFactor", s.pressureSizeFactor); put("pressureOpacityFactor", s.pressureOpacityFactor); put("pressureBlurFactor", s.pressureBlurFactor); put("pressureExponent", s.pressureExponent)
        put("blur", s.blur); put("blurStart", s.blurStart); put("blurMiddle", s.blurMiddle); put("blurEnd", s.blurEnd)
        put("scatterSize", s.scatterSize); put("scatterDensity", s.scatterDensity); put("scatterDeviation", s.scatterDeviation)
        put("particleSizeAbsolute", s.particleSizeAbsolute); put("particleThickness", s.particleThickness)
        put("textureOpacity", s.textureOpacity); put("textureLowerLimit", s.textureLowerLimit); put("textureAbsoluteSize", s.textureAbsoluteSize); put("textureScale", s.textureScale); put("textureAngle", s.textureAngle); put("textureMoving", s.textureMoving); put("textureInvert", s.textureInvert); put("textureGrain", s.textureGrain)
        put("waterColorMix", s.waterColorMix); put("waterWetness", s.waterWetness); put("waterDragging", s.waterDragging); put("waterCorrection", s.waterCorrection)
        put("doubleShadowSize", s.doubleShadowSize); put("doubleShadowAngle", s.doubleShadowAngle); put("doubleShadowDistance", s.doubleShadowDistance)
        put("blendMode", s.blendMode); put("usesCurrentColor", s.usesCurrentColor); put("locked", s.locked); put("billboard", s.billboard); put("billboardPerspective", s.billboardPerspective); put("barrelRoll", s.barrelRoll); put("dependsOnCanvasSize", s.dependsOnCanvasSize)
        put("flow", s.flow); put("stabilizerConstant", s.stabilizerConstant); put("stabilizerFastStrokes", s.stabilizerFastStrokes); put("stabilizerSmoothing", s.stabilizerSmoothing); put("brushPrediction", s.brushPrediction); put("disablePrediction", s.disablePrediction); put("useLegacyStabilization", s.useLegacyStabilization); put("forceFade", s.forceFade); put("fadeStartTime", s.fadeStartTime); put("fadeEndTime", s.fadeEndTime)
    }

    private fun fromJson(o: JSONObject): BrushSettings {
        fun f(k: String, d: Double) = o.optDouble(k, d).toFloat()
        fun b(k: String, d: Boolean) = o.optBoolean(k, d)
        val algorithm = runCatching { BrushAlgorithm.valueOf(o.optString("algorithm", "MONO")) }.getOrDefault(BrushAlgorithm.MONO)
        val material = runCatching { BrushMaterial.valueOf(o.optString("material", "INK")) }.getOrDefault(BrushMaterial.INK)
        return BrushSettings(
            id=o.optString("id", "imported_qr"), name=o.optString("name", "Imported Brush"), category=o.optString("category", "Imported"), algorithm=algorithm, material=material,
            size=f("size",12.0), opacity=f("opacity",1.0), minSizeFactor=f("minSizeFactor",.25), minOpacity=f("minOpacity",.15), fadeOpacity=f("fadeOpacity",1.0), spacing=f("spacing",.12),
            minThickness=f("minThickness",.25), maxThickness=f("maxThickness",1.0), startThickness=f("startThickness",1.0), endThickness=f("endThickness",1.0), startOpacity=f("startOpacity",1.0), endOpacity=f("endOpacity",1.0),
            jitterPosition=f("jitterPosition",0.0), jitterThickness=f("jitterThickness",0.0), jitterOpacity=f("jitterOpacity",0.0), jitterSpacing=f("jitterSpacing",0.0), rotationJitter=f("rotationJitter",0.0), hueJitter=f("hueJitter",0.0), saturationJitter=f("saturationJitter",0.0), brightnessJitter=f("brightnessJitter",0.0), initialAngle=f("initialAngle",0.0), followRotation=b("followRotation",false), aspect=f("aspect",1.0), antialias=b("antialias",true),
            constantOpacity=b("constantOpacity",false), addOpacity=b("addOpacity",false), separateStroke=b("separateStroke",false), thinSpacing=b("thinSpacing",false), fadeStart=f("fadeStart",0.0), fadeEnd=f("fadeEnd",1.0),
            speedSizeFactor=f("speedSizeFactor",1.0), speedOpacityFactor=f("speedOpacityFactor",1.0), speedBlurFactor=f("speedBlurFactor",1.0), pressureSizeFactor=f("pressureSizeFactor",1.0), pressureOpacityFactor=f("pressureOpacityFactor",1.0), pressureBlurFactor=f("pressureBlurFactor",1.0), pressureExponent=f("pressureExponent",1.0),
            blur=f("blur",0.0), blurStart=f("blurStart",0.0), blurMiddle=f("blurMiddle",0.0), blurEnd=f("blurEnd",0.0), scatterSize=f("scatterSize",0.0), scatterDensity=f("scatterDensity",0.0), scatterDeviation=f("scatterDeviation",0.0), particleSizeAbsolute=b("particleSizeAbsolute",false), particleThickness=f("particleThickness",1.0),
            textureOpacity=f("textureOpacity",0.0), textureLowerLimit=f("textureLowerLimit",0.0), textureAbsoluteSize=b("textureAbsoluteSize",false), textureScale=f("textureScale",1.0), textureAngle=f("textureAngle",0.0), textureMoving=b("textureMoving",false), textureInvert=b("textureInvert",false), textureGrain=f("textureGrain",0.0),
            waterColorMix=f("waterColorMix",0.0), waterWetness=f("waterWetness",0.0), waterDragging=f("waterDragging",0.0), waterCorrection=f("waterCorrection",0.0), doubleShadowSize=f("doubleShadowSize",0.0), doubleShadowAngle=f("doubleShadowAngle",0.0), doubleShadowDistance=f("doubleShadowDistance",0.0), blendMode=o.optString("blendMode","NORMAL"), usesCurrentColor=b("usesCurrentColor",true), locked=b("locked",false), billboard=b("billboard",false), billboardPerspective=b("billboardPerspective",false), barrelRoll=b("barrelRoll",false), dependsOnCanvasSize=b("dependsOnCanvasSize",false),
            flow=f("flow",1.0), stabilizerConstant=f("stabilizerConstant",0.0), stabilizerFastStrokes=f("stabilizerFastStrokes",0.0), stabilizerSmoothing=f("stabilizerSmoothing",0.0), brushPrediction=f("brushPrediction",0.0), disablePrediction=b("disablePrediction",false), useLegacyStabilization=b("useLegacyStabilization",false), forceFade=b("forceFade",false), fadeStartTime=f("fadeStartTime",0.0), fadeEndTime=f("fadeEndTime",1.0)
        ).normalized()
    }

    private fun deflate(input: ByteArray): ByteArray { val d=Deflater(9,true); d.setInput(input); d.finish(); val out=ByteArray(input.size+256); val n=d.deflate(out); d.end(); return out.copyOf(n) }
    private fun inflate(input: ByteArray): ByteArray { val i=Inflater(true); i.setInput(input); val out=ByteArray(64*1024); val result=java.io.ByteArrayOutputStream(); while(!i.finished()){ val n=i.inflate(out); require(n>0 || i.needsDictionary() || i.needsInput()){ "QR comprimido inválido" }; if(n>0) result.write(out,0,n) }; i.end(); return result.toByteArray() }
    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
