package com.animame.editor

enum class BrushAlgorithm { MONO, WATER, DOUBLE, COLOR, PROCEDURAL, VECTOR, AIRBRUSH, ERASER, SMUDGE }
enum class BrushMaterial { INK, PENCIL, CHARCOAL, PASTEL, WATERCOLOR, GOUACHE, ACRYLIC, OIL, MARKER, AIRBRUSH, CHALK, CRAYON, DIGITAL, GLITTER, HAIR, PLANT, PARTICLE, STAMP }

data class BrushSettings(
    val id: String = "basic", val name: String = "Basic", val category: String = "Simple",
    val algorithm: BrushAlgorithm = BrushAlgorithm.MONO, val material: BrushMaterial = BrushMaterial.INK,
    val size: Float = 12f, val opacity: Float = 1f, val minSizeFactor: Float = .25f,
    val minOpacity: Float = .15f, val fadeOpacity: Float = 1f, val spacing: Float = .12f,
    val minThickness: Float = .25f, val maxThickness: Float = 1f, val startThickness: Float = 1f,
    val endThickness: Float = 1f, val startOpacity: Float = 1f, val endOpacity: Float = 1f,
    val jitterPosition: Float = 0f, val jitterThickness: Float = 0f, val jitterOpacity: Float = 0f,
    val jitterSpacing: Float = 0f, val rotationJitter: Float = 0f, val hueJitter: Float = 0f,
    val saturationJitter: Float = 0f, val brightnessJitter: Float = 0f, val initialAngle: Float = 0f,
    val followRotation: Boolean = false, val aspect: Float = 1f, val antialias: Boolean = true,
    val constantOpacity: Boolean = false, val addOpacity: Boolean = false, val separateStroke: Boolean = false,
    val thinSpacing: Boolean = false, val fadeStart: Float = 0f, val fadeEnd: Float = 1f,
    val speedSizeFactor: Float = 1f, val speedOpacityFactor: Float = 1f, val speedBlurFactor: Float = 1f,
    val pressureSizeFactor: Float = 1f, val pressureOpacityFactor: Float = 1f, val pressureBlurFactor: Float = 1f,
    val blur: Float = 0f, val blurStart: Float = 0f, val blurMiddle: Float = 0f, val blurEnd: Float = 0f,
    val scatterSize: Float = 0f, val scatterDensity: Float = 0f, val scatterDeviation: Float = 0f,
    val particleSizeAbsolute: Boolean = false, val particleThickness: Float = 1f,
    val textureOpacity: Float = 0f, val textureLowerLimit: Float = 0f, val textureAbsoluteSize: Boolean = false,
    val textureScale: Float = 1f, val textureAngle: Float = 0f, val textureMoving: Boolean = false,
    val textureInvert: Boolean = false, val waterColorMix: Float = 0f, val waterWetness: Float = 0f,
    val waterDragging: Float = 0f, val waterCorrection: Float = 0f, val doubleShadowSize: Float = 0f,
    val doubleShadowAngle: Float = 0f, val doubleShadowDistance: Float = 0f, val blendMode: String = "NORMAL",
    val usesCurrentColor: Boolean = true, val locked: Boolean = false, val billboard: Boolean = false,
    val billboardPerspective: Boolean = false, val barrelRoll: Boolean = false, val dependsOnCanvasSize: Boolean = false
) {
    fun normalized() = copy(
        size = size.coerceIn(.25f, 4096f), opacity = opacity.coerceIn(0f, 1f),
        spacing = spacing.coerceIn(.005f, 4f), aspect = aspect.coerceIn(.05f, 20f),
        minSizeFactor = minSizeFactor.coerceIn(0f, 1f)
    )
}

object BrushDefaults {
    fun forPreset(id: String): BrushSettings = profile(id, id, "Simple")

    fun forCatalog(id: String, name: String, category: String): BrushSettings = profile(id, name, category)

    private fun profile(id: String, name: String, category: String): BrushSettings {
        val n = name.lowercase()
        val c = category.lowercase()
        return when {
            c == "eraser" || n.contains("eraser") -> BrushSettings(id, name, category, BrushAlgorithm.ERASER, BrushMaterial.DIGITAL, opacity = 1f, spacing = .04f, constantOpacity = true)
            c == "watercolor" || n.contains("watercolor") -> BrushSettings(id, name, category, BrushAlgorithm.WATER, BrushMaterial.WATERCOLOR, spacing = .065f, minSizeFactor = .28f, minOpacity = .08f, pressureSizeFactor = .9f, pressureOpacityFactor = .8f, waterColorMix = if (n.contains("opaque")) .25f else .72f, waterWetness = if (n.contains("dry")) .25f else .88f, waterDragging = .42f, waterCorrection = .2f, textureOpacity = if (n.contains("texture")) .35f else .08f, fadeEnd = if (n.contains("fade")) .72f else 1f)
            c == "paint" || n.contains("gouache") -> BrushSettings(id, name, category, BrushAlgorithm.COLOR, BrushMaterial.GOUACHE, spacing = .08f, pressureSizeFactor = .85f, pressureOpacityFactor = .65f, textureOpacity = .22f, textureScale = 1.15f)
            n.contains("acrylic") -> BrushSettings(id, name, category, BrushAlgorithm.COLOR, BrushMaterial.ACRYLIC, spacing = .075f, pressureSizeFactor = .8f, textureOpacity = .32f, textureScale = 1.35f, minOpacity = .35f)
            n.contains("oil") -> BrushSettings(id, name, category, BrushAlgorithm.COLOR, BrushMaterial.OIL, spacing = .085f, pressureSizeFactor = .78f, pressureOpacityFactor = .7f, textureOpacity = .42f, textureScale = 1.5f, jitterThickness = .04f)
            c == "markers" || n.contains("marker") -> BrushSettings(id, name, category, BrushAlgorithm.MONO, BrushMaterial.MARKER, spacing = .055f, minSizeFactor = .5f, minOpacity = .3f, pressureSizeFactor = .45f, pressureOpacityFactor = .35f, fadeOpacity = if (n.contains("fade")) .65f else 1f)
            c == "pastel" || n.contains("pastel") || n.contains("crayon") || n.contains("chalk") -> BrushSettings(id, name, category, BrushAlgorithm.PROCEDURAL, if (n.contains("chalk")) BrushMaterial.CHALK else if (n.contains("crayon")) BrushMaterial.CRAYON else BrushMaterial.PASTEL, spacing = .07f, minSizeFactor = .55f, minOpacity = .18f, pressureSizeFactor = .55f, pressureOpacityFactor = .75f, textureOpacity = .58f, textureScale = 1.7f, jitterPosition = .025f)
            c == "sketch" || n.contains("pencil") || n.contains("charcoal") -> BrushSettings(id, name, category, BrushAlgorithm.PROCEDURAL, if (n.contains("charcoal")) BrushMaterial.CHARCOAL else BrushMaterial.PENCIL, spacing = .055f, minSizeFactor = .22f, minOpacity = .1f, pressureSizeFactor = .92f, pressureOpacityFactor = .88f, textureOpacity = .62f, textureScale = 1.8f, jitterThickness = .035f)
            c == "airbrush" || n.contains("airbrush") -> BrushSettings(id, name, category, BrushAlgorithm.AIRBRUSH, BrushMaterial.AIRBRUSH, spacing = .025f, opacity = .22f, minOpacity = .02f, pressureSizeFactor = .5f, pressureOpacityFactor = .8f, blur = .72f, blurStart = .35f, blurMiddle = .72f, blurEnd = .9f, scatterSize = .18f, scatterDensity = .2f)
            c == "hair" || n.contains("hair") || n.contains("braid") -> BrushSettings(id, name, category, BrushAlgorithm.PROCEDURAL, BrushMaterial.HAIR, spacing = .035f, minSizeFactor = .18f, minOpacity = .18f, pressureSizeFactor = .75f, followRotation = true, jitterPosition = .035f, jitterThickness = .12f, textureOpacity = .25f)
            c == "plants" || n.contains("grass") || n.contains("leaf") || n.contains("flower") || n.contains("tree") -> BrushSettings(id, name, category, BrushAlgorithm.PROCEDURAL, BrushMaterial.PLANT, spacing = .22f, minSizeFactor = .55f, scatterSize = .35f, scatterDensity = .65f, scatterDeviation = .6f, rotationJitter = 1f, followRotation = true, particleThickness = .75f)
            c == "stamps" || c == "materials" || c == "chains" -> BrushSettings(id, name, category, BrushAlgorithm.PROCEDURAL, BrushMaterial.STAMP, spacing = .45f, minSizeFactor = .8f, separateStroke = true, rotationJitter = .25f, scatterSize = .12f, textureOpacity = .18f)
            c == "pointillism" || n.contains("dot") || n.contains("glitter") || n.contains("sequin") -> BrushSettings(id, name, category, BrushAlgorithm.PROCEDURAL, if (n.contains("glitter") || n.contains("sequin")) BrushMaterial.GLITTER else BrushMaterial.PARTICLE, spacing = .18f, opacity = .72f, minOpacity = .08f, scatterSize = .45f, scatterDensity = .9f, scatterDeviation = .8f, particleSizeAbsolute = true, particleThickness = .65f, hueJitter = if (n.contains("glitter")) .08f else 0f, brightnessJitter = if (n.contains("glitter")) .12f else 0f)
            c == "vector" -> BrushSettings(id, name, category, BrushAlgorithm.VECTOR, BrushMaterial.DIGITAL, spacing = .035f, minSizeFactor = .55f, pressureSizeFactor = .45f, constantOpacity = true)
            c == "outlines" || n.contains("outline") -> BrushSettings(id, name, category, BrushAlgorithm.MONO, BrushMaterial.INK, spacing = .045f, minSizeFactor = .45f, minOpacity = .3f, pressureSizeFactor = .35f, pressureOpacityFactor = .25f, fadeEnd = if (n.contains("fade")) .8f else 1f)
            c == "digital" || n.contains("digital") || n.contains("texture pen") -> BrushSettings(id, name, category, BrushAlgorithm.MONO, BrushMaterial.DIGITAL, spacing = .045f, minSizeFactor = .42f, pressureSizeFactor = .6f, pressureOpacityFactor = .45f, constantOpacity = n.contains("uniform"))
            c == "effects" || c == "light" -> BrushSettings(id, name, category, BrushAlgorithm.PROCEDURAL, if (c == "light") BrushMaterial.GLITTER else BrushMaterial.PARTICLE, spacing = .14f, opacity = .72f, minOpacity = .05f, blur = if (n.contains("blur")) .55f else .08f, scatterSize = .32f, scatterDensity = .55f, scatterDeviation = .7f, brightnessJitter = if (c == "light") .14f else .04f, hueJitter = if (n.contains("rainbow")) .16f else 0f)
            c == "ink" || n.contains("pen") || n.contains("calligraphy") -> BrushSettings(id, name, category, if (n.contains("calligraphy")) BrushAlgorithm.VECTOR else BrushAlgorithm.MONO, BrushMaterial.INK, spacing = .045f, minSizeFactor = .2f, minOpacity = .12f, pressureSizeFactor = .9f, pressureOpacityFactor = .72f, followRotation = n.contains("calligraphy"), aspect = if (n.contains("ruling")) .35f else 1f, fadeEnd = if (n.contains("fade")) .75f else 1f)
            else -> BrushSettings(id, name, category, BrushAlgorithm.MONO, BrushMaterial.INK, spacing = .09f, pressureSizeFactor = .8f, pressureOpacityFactor = .65f)
        }
    }
}
