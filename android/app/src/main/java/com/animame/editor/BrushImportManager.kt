package com.animame.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.BitMatrix
import com.google.zxing.common.HybridBinarizer
import java.io.File
import java.io.FileOutputStream
import java.util.EnumMap
import kotlin.math.max
import kotlin.math.roundToInt

object BrushImportManager {
    const val PICK_BRUSH = 7311
    private const val PREFIX = "ANIMAME-BRUSH-V1:"

    data class ImportResult(val preset: BrushPreset, val source: String, val warning: String? = null)

    fun openPicker(activity: Activity) {
        activity.startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/zip", "application/octet-stream"))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }, PICK_BRUSH)
    }

    fun handleResult(context: Context, uri: Uri, onDone: (Result<ImportResult>) -> Unit) {
        Thread {
            val result = runCatching { importUri(context, uri) }
            Handler(Looper.getMainLooper()).post { onDone(result) }
        }.start()
    }

    fun importUri(context: Context, uri: Uri): ImportResult {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Não foi possível ler o arquivo.")
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap != null) {
            decodeQr(bitmap)?.let { qr ->
                return importQr(context, qr.text, qr.rawBytes ?: qr.text.toByteArray(Charsets.UTF_8))
            }
            return importImageBrush(context, bitmap)
        }
        error("Formato de pincel não reconhecido. Use um QR de pincel, PNG/JPG ou um pacote Anima-me.")
    }

    private data class QrPayload(val text: String, val rawBytes: ByteArray?)

    private fun decodeQr(source: Bitmap): QrPayload? {
        val variants = listOf(source, rotate(source, 90f), rotate(source, 180f), rotate(source, 270f))
        for (bitmap in variants) {
            val result = runCatching {
                val scaled = if (bitmap.width < 700 || bitmap.height < 700)
                    Bitmap.createScaledBitmap(bitmap, max(700, bitmap.width * 2), max(700, bitmap.height * 2), true)
                else bitmap
                val pixels = IntArray(scaled.width * scaled.height)
                scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
                val sourceLum = RGBLuminanceSource(scaled.width, scaled.height, pixels)
                val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
                    put(DecodeHintType.TRY_HARDER, true)
                    put(DecodeHintType.POSSIBLE_FORMATS, listOf(BarcodeFormat.QR_CODE))
                    put(DecodeHintType.CHARACTER_SET, "ISO-8859-1")
                }
                MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(sourceLum)), hints)
            }.getOrNull()
            if (result != null) return QrPayload(result.text ?: "", result.rawBytes)
        }
        return null
    }

    private fun importQr(context: Context, text: String, raw: ByteArray): ImportResult {
        if (text.startsWith(PREFIX)) {
            val encoded = text.removePrefix(PREFIX)
            val json = String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT), Charsets.UTF_8)
            val preset = decodeAnimaMeJson(json)
            CustomBrushStore.add(context, preset)
            return ImportResult(preset, "Anima-me QR")
        }
        val ipbz = raw.startsWith(byteArrayOf('I'.code.toByte(), 'P'.code.toByte(), 'B'.code.toByte(), 'Z'.code.toByte()))
        if (ipbz || text.startsWith("IPBZ")) {
            val fingerprint = raw.fold(0x811c9dc5.toInt()) { h, b -> (h xor (b.toInt() and 0xff)) * 16777619 }
            val name = "IbisPaint QR ${Integer.toHexString(fingerprint).uppercase()}"
            val settings = BrushDefaults.forPreset("imported-ibis").copy(
                customName = name,
                textureStrength = .55f + ((raw.getOrNull(8)?.toInt()?.and(255) ?: 0) / 255f) * .35f,
                spacing = .06f + ((raw.getOrNull(9)?.toInt()?.and(255) ?: 0) / 255f) * .28f,
                pressureSize = (((raw.getOrNull(10)?.toInt()?.and(255) ?: 128) / 255f) * 2f - 1f),
                pressureOpacity = (((raw.getOrNull(11)?.toInt()?.and(255) ?: 128) / 255f) * 2f - 1f)
            )
            val preset = BrushPreset("ibis-${Integer.toHexString(fingerprint)}", name, "Imported", defaults = settings)
            CustomBrushStore.add(context, preset)
            return ImportResult(preset, "IbisPaint IPBZ QR", "O QR foi reconhecido como IPBZ. O formato binário proprietário do ibisPaint não é público; o Anima-me preserva a assinatura e cria uma ponte procedural segura, sem fingir compatibilidade byte-a-byte.")
        }
        error("QR detectado, mas não é um pacote de pincel Anima-me/IPBZ.")
    }

    private fun decodeAnimaMeJson(json: String): BrushPreset {
        val o = org.json.JSONObject(json)
        val id = o.optString("id", "imported-${System.currentTimeMillis()}")
        val name = o.optString("name", "Imported Brush")
        val family = o.optString("family", "Imported")
        val s = BrushSettings(size = o.optDouble("size", 16.0).toFloat(), opacity = o.optDouble("opacity", .8).toFloat(), flow = o.optDouble("flow", .8).toFloat(), hardness = o.optDouble("hardness", .7).toFloat(), feather = o.optDouble("feather", .1).toFloat(), spacing = o.optDouble("spacing", .12).toFloat(), pressureSize = o.optDouble("pressureSize", .3).toFloat(), pressureOpacity = o.optDouble("pressureOpacity", .15).toFloat(), pressureFlow = o.optDouble("pressureFlow", 0.0).toFloat(), textureStrength = o.optDouble("textureStrength", 0.0).toFloat(), textureScale = o.optDouble("textureScale", 1.0).toFloat(), brushType = o.optString("brushType", "stamp"), customName = name)
        return BrushPreset(id, name, family, defaults = s.normalized())
    }

    private fun importImageBrush(context: Context, source: Bitmap): ImportResult {
        val normalized = normalizeBrushBitmap(source)
        val dir = File(context.filesDir, "brushes/imported").apply { mkdirs() }
        val id = "img-${System.currentTimeMillis()}"
        val file = File(dir, "$id.png")
        FileOutputStream(file).use { normalized.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (normalized !== source) normalized.recycle()
        val settings = BrushSettings(size = 24f, opacity = .9f, flow = .85f, spacing = .35f, pressureSize = .35f, pressureOpacity = .2f, brushType = "stamp", brushPattern = "image:${file.absolutePath}", customName = "Image Brush")
        val preset = BrushPreset(id, "Imported Image Brush", "Imported", defaults = settings)
        CustomBrushStore.add(context, preset)
        return ImportResult(preset, "RoughAnimator-style image brush")
    }

    private fun normalizeBrushBitmap(source: Bitmap): Bitmap {
        val w = source.width.coerceAtMost(1024); val h = source.height.coerceAtMost(1024)
        val scaled = Bitmap.createScaledBitmap(source, w, h, true)
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val px = IntArray(w * h)
        scaled.getPixels(px, 0, w, 0, 0, w, h)
        for (i in px.indices) { val c = px[i]; val a = Color.alpha(c); val luminance = (Color.red(c) * .299f + Color.green(c) * .587f + Color.blue(c) * .114f).roundToInt(); val alpha = if (a < 250) a else (255 - luminance).coerceIn(0, 255); px[i] = Color.argb(alpha, 255, 255, 255) }
        out.setPixels(px, 0, w, 0, 0, w, h); if (scaled !== source) scaled.recycle(); return out
    }

    private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, android.graphics.Matrix().apply { postRotate(degrees) }, true)

    fun exportQr(context: Context, preset: BrushPreset): Bitmap {
        val s = preset.defaults.normalized()
        val json = org.json.JSONObject().apply { put("id", preset.id); put("name", preset.name); put("family", preset.family); put("size", s.size); put("opacity", s.opacity); put("flow", s.flow); put("hardness", s.hardness); put("feather", s.feather); put("spacing", s.spacing); put("pressureSize", s.pressureSize); put("pressureOpacity", s.pressureOpacity); put("pressureFlow", s.pressureFlow); put("textureStrength", s.textureStrength); put("textureScale", s.textureScale); put("brushType", s.brushType) }
        val payload = PREFIX + android.util.Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        return bitMatrixToBitmap(MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 900, 900))
    }

    private fun bitMatrixToBitmap(matrix: BitMatrix): Bitmap {
        val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
        return bitmap
    }
}

object CustomBrushStore {
    private const val PREFS = "animame_custom_brushes"
    private const val KEY = "records"
    fun initialize(context: Context) { BrushCatalog.replaceImported(load(context)) }
    fun add(context: Context, preset: BrushPreset) { val existing = load(context).filterNot { it.id == preset.id } + preset; save(context, existing); BrushCatalog.replaceImported(existing) }
    private fun save(context: Context, list: List<BrushPreset>) {
        val array = org.json.JSONArray()
        list.forEach { p -> val s = p.defaults; array.put(org.json.JSONObject().apply { put("id", p.id); put("name", p.name); put("family", p.family); put("size", s.size); put("opacity", s.opacity); put("flow", s.flow); put("hardness", s.hardness); put("feather", s.feather); put("spacing", s.spacing); put("pressureSize", s.pressureSize); put("pressureOpacity", s.pressureOpacity); put("pressureFlow", s.pressureFlow); put("textureStrength", s.textureStrength); put("textureScale", s.textureScale); put("brushType", s.brushType); put("brushPattern", s.brushPattern) }) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
    fun load(context: Context): List<BrushPreset> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]") ?: "[]"
        val array = runCatching { org.json.JSONArray(raw) }.getOrElse { org.json.JSONArray() }
        return buildList { for (i in 0 until array.length()) { val o = array.optJSONObject(i) ?: continue; val s = BrushSettings(size = o.optDouble("size", 16.0).toFloat(), opacity = o.optDouble("opacity", .8).toFloat(), flow = o.optDouble("flow", .8).toFloat(), hardness = o.optDouble("hardness", .7).toFloat(), feather = o.optDouble("feather", .1).toFloat(), spacing = o.optDouble("spacing", .12).toFloat(), pressureSize = o.optDouble("pressureSize", .3).toFloat(), pressureOpacity = o.optDouble("pressureOpacity", .2).toFloat(), pressureFlow = o.optDouble("pressureFlow", 0.0).toFloat(), textureStrength = o.optDouble("textureStrength", 0.0).toFloat(), textureScale = o.optDouble("textureScale", 1.0).toFloat(), brushType = o.optString("brushType", "stamp"), brushPattern = o.optString("brushPattern", "round"), customName = o.optString("name", "Imported Brush")); add(BrushPreset(o.optString("id"), o.optString("name", "Imported Brush"), o.optString("family", "Imported"), defaults = s)) } }
    }
}
