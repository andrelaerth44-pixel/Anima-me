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
    val pressureSizeFactor: Float = 1f, val pressureOpacityFactor: Float = 1f,
    val pressureBlurFactor: Float = 1f, val blur: Float = 0f, val blurStart: Float = 0f,
    val blurMiddle: Float = 0f, val blurEnd: Float = 0f, val scatterSize: Float = 0f,
    val scatterDensity: Float = 0f, val scatterDeviation: Float = 0f, val particleSizeAbsolute: Boolean = false,
    val particleThickness: Float = 1f, val textureOpacity: Float = 0f, val textureLowerLimit: Float = 0f,
    val textureAbsoluteSize: Boolean = false, val textureScale: Float = 1f, val textureAngle: Float = 0f,
    val textureMoving: Boolean = false, val textureInvert: Boolean = false,
    val waterColorMix: Float = 0f, val waterWetness: Float = 0f, val waterDragging: Float = 0f,
    val waterCorrection: Float = 0f, val doubleShadowSize: Float = 0f, val doubleShadowAngle: Float = 0f,
    val doubleShadowDistance: Float = 0f, val blendMode: String = "NORMAL", val usesCurrentColor: Boolean = true,
    val locked: Boolean = false, val billboard: Boolean = false, val billboardPerspective: Boolean = false,
    val barrelRoll: Boolean = false, val dependsOnCanvasSize: Boolean = false,
    // Raster-flow controls are independent from trajectory stabilization.
    val flow: Float = 1f,
    val textureGrain: Float = 0f,
    val pressureExponent: Float = 1f,
    // ibisPaint-style per-brush stroke correction controls.
    val stabilizerConstant: Float = 0f,
    val stabilizerFastStrokes: Float = 0f,
    val stabilizerSmoothing: Float = 0f,
    val brushPrediction: Float = 0f,
    val disablePrediction: Boolean = false,
    val useLegacyStabilization: Boolean = false,
    val forceFade: Boolean = false,
    val fadeStartTime: Float = 0f,
    val fadeEndTime: Float = 1f,
    // Stamp silhouette (stars/hearts/paws/...) for discrete decoration brushes.
    // Continuous brushes (pens, pencils, watercolor...) stay CIRCLE.
    val stampShape: StampShape = StampShape.CIRCLE,
    val stampFilled: Boolean = true
) {
    fun normalized() = copy(
        size = size.coerceIn(.25f, 4096f), opacity = opacity.coerceIn(0f,1f),
        spacing = spacing.coerceIn(.005f,4f), aspect = aspect.coerceIn(.05f,20f),
        minSizeFactor = minSizeFactor.coerceIn(0f,1f), minOpacity = minOpacity.coerceIn(0f,1f),
        fadeOpacity = fadeOpacity.coerceIn(0f,1f), flow = flow.coerceIn(0f,1f),
        textureOpacity = textureOpacity.coerceIn(0f,1f), textureLowerLimit = textureLowerLimit.coerceIn(0f,1f),
        textureScale = textureScale.coerceIn(.01f,100f), textureGrain = textureGrain.coerceIn(0f,1f),
        pressureExponent = pressureExponent.coerceIn(.1f,4f),
        stabilizerConstant = stabilizerConstant.coerceIn(0f,100f),
        stabilizerFastStrokes = stabilizerFastStrokes.coerceIn(0f,100f),
        stabilizerSmoothing = stabilizerSmoothing.coerceIn(0f,100f),
        brushPrediction = brushPrediction.coerceIn(0f,100f),
        fadeStartTime = fadeStartTime.coerceIn(0f,1f), fadeEndTime = fadeEndTime.coerceIn(0f,1f)
    )
}

object BrushDefaults {
    fun forPreset(id: String): BrushSettings = when {
        id == "eraser" -> BrushSettings(id = id, name = "Eraser", category = "Eraser", algorithm = BrushAlgorithm.ERASER, material = BrushMaterial.DIGITAL, flow = 1f)
        // Catalog presets carry opaque "canvas_N" ids: look up their real name/category
        // so they get genuinely different behavior instead of all falling through below.
        id.startsWith("canvas_") -> {
            val preset = BrushCatalog.all().firstOrNull { it.id == id }
            if (preset != null) forNamedBrush(preset.id, preset.name, preset.category) else forNamedBrush(id, id, "Simple")
        }
        id.contains("water", true) -> BrushSettings(id=id, name=id, category="Watercolor", algorithm=BrushAlgorithm.WATER, material=BrushMaterial.WATERCOLOR, spacing=.08f, waterColorMix=.65f, waterWetness=.8f, waterDragging=.35f, flow=.72f, textureGrain=.18f)
        id.contains("oil", true) -> BrushSettings(id=id, name=id, category="Paint", material=BrushMaterial.OIL, spacing=.09f, textureOpacity=.2f, flow=.9f, textureGrain=.25f)
        id.contains("pencil", true) -> BrushSettings(id=id, name=id, category="Sketch", material=BrushMaterial.PENCIL, spacing=.07f, minSizeFactor=.35f, pressureSizeFactor=.9f, textureOpacity=.55f, textureGrain=.7f, flow=.82f)
        id.contains("airbrush", true) -> BrushSettings(id=id, name=id, category="Airbrush", algorithm=BrushAlgorithm.AIRBRUSH, material=BrushMaterial.AIRBRUSH, spacing=.03f, opacity=.2f, blur=.65f, flow=.65f)
        id.contains("eraser", true) -> BrushSettings(id=id, name=id, category="Eraser", algorithm=BrushAlgorithm.ERASER, material=BrushMaterial.DIGITAL, flow=1f)
        else -> forNamedBrush(id, id, "Simple")
    }

    /**
     * Derives real, physically-different stroke behavior from a brush's
     * category plus keywords in its name, instead of every preset silently
     * falling back to the same generic ink pen. This is what gives each of
     * the catalog's brushes its own character: material, texture, jitter,
     * scatter, water behavior, and — for discrete decoration brushes — an
     * actual stamp silhouette instead of a plain circle.
     *
     * A hash of the exact name adds small deterministic variation so that
     * siblings within one family (seven grass species, eight chain widths...)
     * are not numerically identical to each other either.
     */
    fun forNamedBrush(id: String, name: String, category: String): BrushSettings {
        val n = name.lowercase()
        val cat = category.lowercase()
        val h = name.hashCode()
        fun seed(salt: Int): Float = (((h xor (salt * 0x9E3779B9.toInt())) and 0xFFFF).toFloat() / 0xFFFF.toFloat())

        var algorithm = BrushAlgorithm.MONO
        var material = BrushMaterial.INK
        var size = 12f
        var opacity = 1f
        var spacing = .12f
        var minSizeFactor = .3f
        var minOpacity = .2f
        var pressureExponent = 1f
        var pressureSizeFactor = 1f
        var pressureOpacityFactor = 1f
        var jitterPosition = 0f
        var jitterThickness = 0f
        var jitterSpacing = 0f
        var rotationJitter = 0f
        var hueJitter = 0f
        var brightnessJitter = 0f
        var followRotation = false
        var aspect = 1f
        var scatterSize = 0f
        var textureOpacity = 0f
        var textureGrain = 0f
        var textureScale = 1f
        var flow = 1f
        var fadeOpacity = 1f
        var blur = 0f
        var waterColorMix = 0f
        var waterWetness = 0f
        var waterDragging = 0f
        var thinSpacing = false
        var blendMode = "NORMAL"
        var stampShape = StampShape.CIRCLE
        var stampFilled = true

        when {
            cat.contains("airbrush") -> { algorithm = BrushAlgorithm.AIRBRUSH; material = BrushMaterial.AIRBRUSH; spacing = .028f; opacity = .32f; blur = .55f; flow = .6f; minOpacity = .05f }
            cat.contains("watercolor") -> { algorithm = BrushAlgorithm.WATER; material = BrushMaterial.WATERCOLOR; spacing = .09f; waterColorMix = .6f; waterWetness = .75f; waterDragging = .3f; flow = .7f; textureGrain = .2f }
            cat.contains("sketch") -> { material = BrushMaterial.PENCIL; spacing = .065f; minSizeFactor = .35f; pressureSizeFactor = .9f; textureOpacity = .5f; textureGrain = .65f; flow = .85f }
            cat.contains("paint") -> { material = BrushMaterial.OIL; spacing = .09f; textureOpacity = .22f; textureGrain = .3f; flow = .9f }
            cat.contains("pastel") -> { material = BrushMaterial.CHALK; spacing = .05f; textureOpacity = .6f; textureGrain = .8f; minOpacity = .3f; flow = .75f }
            cat.contains("marker") -> { material = BrushMaterial.MARKER; spacing = .1f; minOpacity = .85f; pressureOpacityFactor = .2f; flow = 1f }
            cat.contains("digital") -> { material = BrushMaterial.DIGITAL; spacing = .1f; flow = 1f }
            cat.contains("vector") -> { algorithm = BrushAlgorithm.VECTOR; material = BrushMaterial.DIGITAL; spacing = .06f; pressureSizeFactor = .4f; flow = 1f }
            cat.contains("light") -> { material = BrushMaterial.DIGITAL; blendMode = "ADD"; brightnessJitter = .15f; flow = .9f; spacing = .08f }
            cat.contains("stamp") -> { algorithm = BrushAlgorithm.PROCEDURAL; material = BrushMaterial.STAMP; spacing = 1.1f; scatterSize = .12f; rotationJitter = .5f }
            cat.contains("plant") -> { algorithm = BrushAlgorithm.PROCEDURAL; material = BrushMaterial.PLANT; spacing = .55f; scatterSize = .2f; rotationJitter = .35f; aspect = 1.6f; followRotation = true; stampShape = StampShape.LEAF }
            cat.contains("chain") -> { algorithm = BrushAlgorithm.PROCEDURAL; material = BrushMaterial.STAMP; spacing = .85f; jitterSpacing = .04f; rotationJitter = .1f; followRotation = true }
            cat.contains("hair") -> { algorithm = BrushAlgorithm.PROCEDURAL; material = BrushMaterial.HAIR; spacing = .06f; jitterThickness = .25f; scatterSize = .08f; textureGrain = .4f }
            cat.contains("material") -> { algorithm = BrushAlgorithm.PROCEDURAL; material = BrushMaterial.STAMP; spacing = .5f; scatterSize = .1f; jitterSpacing = .05f }
            cat.contains("outline") -> { spacing = .05f; minSizeFactor = .6f; pressureSizeFactor = .3f; flow = 1f }
            cat.contains("pointillism") -> { algorithm = BrushAlgorithm.PROCEDURAL; material = BrushMaterial.PARTICLE; spacing = 1.4f; scatterSize = .35f; size = 6f }
            cat.contains("effect") -> { algorithm = BrushAlgorithm.PROCEDURAL; material = BrushMaterial.PARTICLE; spacing = .4f; scatterSize = .18f; hueJitter = .05f }
            cat.contains("eraser") -> { algorithm = BrushAlgorithm.ERASER; material = BrushMaterial.DIGITAL; flow = 1f }
            cat.contains("brushes") -> { material = BrushMaterial.INK; spacing = .11f; pressureSizeFactor = .85f }
            else -> { material = BrushMaterial.INK; spacing = .1f; flow = .95f }
        }

        if ("soft" in n) { minOpacity += .15f; pressureExponent -= .15f; textureGrain *= .7f }
        if ("hard" in n) { pressureExponent += .35f; minSizeFactor += .15f; textureGrain *= .6f }
        if ("bleed" in n) { waterWetness += .2f; waterDragging += .15f; textureOpacity += .15f; material = BrushMaterial.WATERCOLOR; algorithm = BrushAlgorithm.WATER }
        if ("fade" in n) { fadeOpacity = .25f }
        if ("rough" in n) { jitterThickness += .18f; jitterPosition += .01f; textureGrain += .25f }
        if ("real" in n) { jitterThickness += .06f; pressureSizeFactor += .1f }
        if ("particle" in n) { material = BrushMaterial.PARTICLE; scatterSize += .25f; spacing += .3f }
        if ("outline" in n) { size *= .6f; minSizeFactor += .3f; stampFilled = false }
        if ("blur" in n) { blur += .5f; textureOpacity *= .4f; size *= 1.15f }
        if ("glitter" in n || "sequin" in n || "prism" in n || "kira" in n || "sparkle" in n) { material = BrushMaterial.GLITTER; hueJitter += .25f; brightnessJitter += .3f; scatterSize += .2f; jitterThickness += .15f; blendMode = "ADD" }
        if ("snow" in n) { stampShape = StampShape.SNOWFLAKE; material = BrushMaterial.PARTICLE; scatterSize += .15f }
        if ("grass" in n || "fern" in n || "clover" in n || "sedge" in n) { material = BrushMaterial.PLANT; stampShape = StampShape.LEAF; aspect = 1.4f + seed(1) * .6f; rotationJitter += .3f; followRotation = true; scatterSize += .12f }
        else if ("leaf" in n || "leaves" in n || "tree" in n) { material = BrushMaterial.PLANT; stampShape = StampShape.LEAF; rotationJitter += .2f; followRotation = true }
        if ("petal" in n || "blossom" in n || "flower" in n || "rose" in n || "peony" in n) { stampShape = StampShape.HEART; material = BrushMaterial.PLANT; scatterSize += .1f }
        if ("heart" in n) stampShape = StampShape.HEART
        if ("star" in n && "start" !in n) stampShape = StampShape.STAR
        if ("diamond" in n) stampShape = StampShape.DIAMOND
        if ("hexagon" in n || "hexagram" in n) stampShape = StampShape.HEXAGON
        if ("pentagon" in n || "pentagram" in n) stampShape = StampShape.PENTAGON
        if ("triangle" in n) stampShape = StampShape.TRIANGLE
        if ("square" in n || "cube" in n) stampShape = StampShape.SQUARE
        if ("drop" in n) stampShape = StampShape.DROP
        if ("footprint" in n || "paw" in n) { stampShape = StampShape.PAW; material = BrushMaterial.STAMP }
        if ("cross" in n && "cross hatching" !in n) stampShape = StampShape.CROSS
        if ("chain" in n || "braid" in n || "knitting" in n || "stitch" in n || "lace" in n || "net" in n) { algorithm = BrushAlgorithm.PROCEDURAL; material = BrushMaterial.STAMP; jitterSpacing = .02f; followRotation = true }
        if ("crown" in n || "note" in n) { algorithm = BrushAlgorithm.PROCEDURAL; material = BrushMaterial.STAMP; spacing = spacing.coerceAtLeast(.9f) }
        if ("dot" in n || "stripe" in n || "grid" in n || "houndstooth" in n || "hatching" in n) { thinSpacing = true; jitterThickness *= .3f; jitterPosition = 0f }
        if ("charcoal" in n) { material = BrushMaterial.CHARCOAL; textureGrain += .3f; minOpacity -= .05f }
        if ("chalk" in n || "pastel" in n || "crayon" in n) { material = BrushMaterial.CHALK; textureGrain += .25f }
        if ("gouache" in n) material = BrushMaterial.GOUACHE
        if ("acrylic" in n) material = BrushMaterial.ACRYLIC
        if ("impressionism" in n) { material = BrushMaterial.OIL; textureGrain += .3f; jitterThickness += .15f }
        if ("fur" in n || "gloss" in n) { material = BrushMaterial.HAIR; jitterThickness += .2f }

        spacing = (spacing * (0.9f + seed(2) * .25f)).coerceIn(.005f, 4f)
        textureScale = 1f + seed(3) * 3f
        rotationJitter = (rotationJitter + seed(4) * .1f).coerceIn(0f, 1f)
        size *= 0.85f + seed(5) * .5f

        return BrushSettings(
            id = id, name = name, category = category, algorithm = algorithm, material = material,
            size = size, opacity = opacity, minSizeFactor = minSizeFactor.coerceIn(0f, 1f), minOpacity = minOpacity.coerceIn(0f, 1f),
            spacing = spacing, pressureExponent = pressureExponent.coerceIn(.1f, 4f),
            pressureSizeFactor = pressureSizeFactor.coerceIn(0f, 1.5f), pressureOpacityFactor = pressureOpacityFactor.coerceIn(0f, 1.5f),
            jitterPosition = jitterPosition, jitterThickness = jitterThickness.coerceIn(0f, 1f), jitterSpacing = jitterSpacing,
            rotationJitter = rotationJitter, hueJitter = hueJitter.coerceIn(0f, 1f),
            brightnessJitter = brightnessJitter, followRotation = followRotation, aspect = aspect.coerceIn(.05f, 20f),
            scatterSize = scatterSize.coerceIn(0f, 1f), textureOpacity = textureOpacity.coerceIn(0f, 1f),
            textureGrain = textureGrain.coerceIn(0f, 1f), textureScale = textureScale.coerceIn(.01f, 100f),
            flow = flow.coerceIn(0f, 1f), fadeOpacity = fadeOpacity.coerceIn(0f, 1f),
            blur = blur.coerceIn(0f, 1f), waterColorMix = waterColorMix.coerceIn(0f, 1f), waterWetness = waterWetness.coerceIn(0f, 1f),
            waterDragging = waterDragging.coerceIn(0f, 1f), thinSpacing = thinSpacing, blendMode = blendMode,
            stampShape = stampShape, stampFilled = stampFilled
        )
    }
}
