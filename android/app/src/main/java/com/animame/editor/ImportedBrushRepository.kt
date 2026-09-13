package com.animame.editor

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persistent custom brushes imported through the brush-panel QR action. */
object ImportedBrushRepository {
    private const val PREFS = "animame_imported_brushes"
    private const val KEY = "items"
    private val items = linkedMapOf<String, BrushSettings>()
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        val raw = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return
        runCatching {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val settings = fromJson(array.getJSONObject(i))
                items[settings.id] = settings
            }
        }
    }

    fun all(): List<BrushSettings> = items.values.toList()
    fun find(id: String): BrushSettings? = items[id]

    fun add(context: Context, settings: BrushSettings) {
        initialize(context)
        val normalized = settings.normalized()
        items[normalized.id] = normalized
        persist(context)
    }

    private fun persist(context: Context) {
        val array = JSONArray()
        items.values.forEach { array.put(toJson(it)) }
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, array.toString()).apply()
    }

    private fun toJson(s: BrushSettings): JSONObject = JSONObject().apply {
        put("id", s.id); put("name", s.name); put("category", s.category)
        put("algorithm", s.algorithm.name); put("material", s.material.name)
        put("size", s.size); put("opacity", s.opacity); put("minSizeFactor", s.minSizeFactor); put("minOpacity", s.minOpacity)
        put("fadeOpacity", s.fadeOpacity); put("spacing", s.spacing)
        put("jitterPosition", s.jitterPosition); put("jitterThickness", s.jitterThickness); put("jitterOpacity", s.jitterOpacity); put("jitterSpacing", s.jitterSpacing)
        put("rotationJitter", s.rotationJitter); put("hueJitter", s.hueJitter); put("saturationJitter", s.saturationJitter); put("brightnessJitter", s.brightnessJitter); put("initialAngle", s.initialAngle)
        put("followRotation", s.followRotation); put("aspect", s.aspect); put("antialias", s.antialias)
        put("fadeStart", s.fadeStart); put("fadeEnd", s.fadeEnd)
        put("speedSizeFactor", s.speedSizeFactor); put("speedOpacityFactor", s.speedOpacityFactor); put("speedBlurFactor", s.speedBlurFactor)
        put("pressureSizeFactor", s.pressureSizeFactor); put("pressureOpacityFactor", s.pressureOpacityFactor); put("pressureBlurFactor", s.pressureBlurFactor); put("pressureExponent", s.pressureExponent)
        put("blur", s.blur); put("blurStart", s.blurStart); put("blurMiddle", s.blurMiddle); put("blurEnd", s.blurEnd)
        put("scatterSize", s.scatterSize); put("scatterDensity", s.scatterDensity); put("scatterDeviation", s.scatterDeviation)
        put("particleSizeAbsolute", s.particleSizeAbsolute); put("particleThickness", s.particleThickness)
        put("textureOpacity", s.textureOpacity); put("textureLowerLimit", s.textureLowerLimit); put("textureScale", s.textureScale); put("textureAngle", s.textureAngle); put("textureGrain", s.textureGrain)
        put("waterColorMix", s.waterColorMix); put("waterWetness", s.waterWetness); put("waterDragging", s.waterDragging); put("waterCorrection", s.waterCorrection)
        put("doubleShadowSize", s.doubleShadowSize); put("doubleShadowAngle", s.doubleShadowAngle); put("doubleShadowDistance", s.doubleShadowDistance)
        put("flow", s.flow)
    }

    private fun fromJson(o: JSONObject): BrushSettings {
        val algorithm = runCatching { BrushAlgorithm.valueOf(o.optString("algorithm", BrushAlgorithm.MONO.name)) }.getOrDefault(BrushAlgorithm.MONO)
        val material = runCatching { BrushMaterial.valueOf(o.optString("material", BrushMaterial.INK.name)) }.getOrDefault(BrushMaterial.INK)
        return BrushSettings(
            id = o.optString("id", "imported_${System.nanoTime()}"), name = o.optString("name", "Imported Brush"), category = o.optString("category", "Imported"),
            algorithm = algorithm, material = material, size = o.optDouble("size", 12.0).toFloat(), opacity = o.optDouble("opacity", 1.0).toFloat(),
            minSizeFactor = o.optDouble("minSizeFactor", .25).toFloat(), minOpacity = o.optDouble("minOpacity", .15).toFloat(), fadeOpacity = o.optDouble("fadeOpacity", 1.0).toFloat(), spacing = o.optDouble("spacing", .12).toFloat(),
            jitterPosition = o.optDouble("jitterPosition", 0.0).toFloat(), jitterThickness = o.optDouble("jitterThickness", 0.0).toFloat(), jitterOpacity = o.optDouble("jitterOpacity", 0.0).toFloat(), jitterSpacing = o.optDouble("jitterSpacing", 0.0).toFloat(),
            rotationJitter = o.optDouble("rotationJitter", 0.0).toFloat(), hueJitter = o.optDouble("hueJitter", 0.0).toFloat(), saturationJitter = o.optDouble("saturationJitter", 0.0).toFloat(), brightnessJitter = o.optDouble("brightnessJitter", 0.0).toFloat(), initialAngle = o.optDouble("initialAngle", 0.0).toFloat(),
            followRotation = o.optBoolean("followRotation", false), aspect = o.optDouble("aspect", 1.0).toFloat(), antialias = o.optBoolean("antialias", true),
            fadeStart = o.optDouble("fadeStart", 0.0).toFloat(), fadeEnd = o.optDouble("fadeEnd", 1.0).toFloat(),
            speedSizeFactor = o.optDouble("speedSizeFactor", 1.0).toFloat(), speedOpacityFactor = o.optDouble("speedOpacityFactor", 1.0).toFloat(), speedBlurFactor = o.optDouble("speedBlurFactor", 1.0).toFloat(),
            pressureSizeFactor = o.optDouble("pressureSizeFactor", 1.0).toFloat(), pressureOpacityFactor = o.optDouble("pressureOpacityFactor", 1.0).toFloat(), pressureBlurFactor = o.optDouble("pressureBlurFactor", 1.0).toFloat(), pressureExponent = o.optDouble("pressureExponent", 1.0).toFloat(),
            blur = o.optDouble("blur", 0.0).toFloat(), blurStart = o.optDouble("blurStart", 0.0).toFloat(), blurMiddle = o.optDouble("blurMiddle", 0.0).toFloat(), blurEnd = o.optDouble("blurEnd", 0.0).toFloat(),
            scatterSize = o.optDouble("scatterSize", 0.0).toFloat(), scatterDensity = o.optDouble("scatterDensity", 0.0).toFloat(), scatterDeviation = o.optDouble("scatterDeviation", 0.0).toFloat(),
            particleSizeAbsolute = o.optBoolean("particleSizeAbsolute", false), particleThickness = o.optDouble("particleThickness", 1.0).toFloat(),
            textureOpacity = o.optDouble("textureOpacity", 0.0).toFloat(), textureLowerLimit = o.optDouble("textureLowerLimit", 0.0).toFloat(), textureScale = o.optDouble("textureScale", 1.0).toFloat(), textureAngle = o.optDouble("textureAngle", 0.0).toFloat(), textureGrain = o.optDouble("textureGrain", 0.0).toFloat(),
            waterColorMix = o.optDouble("waterColorMix", 0.0).toFloat(), waterWetness = o.optDouble("waterWetness", 0.0).toFloat(), waterDragging = o.optDouble("waterDragging", 0.0).toFloat(), waterCorrection = o.optDouble("waterCorrection", 0.0).toFloat(),
            doubleShadowSize = o.optDouble("doubleShadowSize", 0.0).toFloat(), doubleShadowAngle = o.optDouble("doubleShadowAngle", 0.0).toFloat(), doubleShadowDistance = o.optDouble("doubleShadowDistance", 0.0).toFloat(),
            flow = o.optDouble("flow", 1.0).toFloat()
        ).normalized()
    }
}
