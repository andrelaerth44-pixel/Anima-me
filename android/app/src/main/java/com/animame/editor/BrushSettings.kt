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
    val fadeEndTime: Float = 1f
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
        id.contains("water", true) -> BrushSettings(id=id, name=id, category="Watercolor", algorithm=BrushAlgorithm.WATER, material=BrushMaterial.WATERCOLOR, spacing=.08f, waterColorMix=.65f, waterWetness=.8f, waterDragging=.35f, flow=.72f, textureGrain=.18f)
        id.contains("oil", true) -> BrushSettings(id=id, name=id, category="Paint", material=BrushMaterial.OIL, spacing=.09f, textureOpacity=.2f, flow=.9f, textureGrain=.25f)
        id.contains("pencil", true) -> BrushSettings(id=id, name=id, category="Sketch", material=BrushMaterial.PENCIL, spacing=.07f, minSizeFactor=.35f, pressureSizeFactor=.9f, textureOpacity=.55f, textureGrain=.7f, flow=.82f)
        id.contains("airbrush", true) -> BrushSettings(id=id, name=id, category="Airbrush", algorithm=BrushAlgorithm.AIRBRUSH, material=BrushMaterial.AIRBRUSH, spacing=.03f, opacity=.2f, blur=.65f, flow=.65f)
        id.contains("eraser", true) -> BrushSettings(id=id, name=id, category="Eraser", algorithm=BrushAlgorithm.ERASER, material=BrushMaterial.DIGITAL, flow=1f)
        else -> BrushSettings(id=id, name=id)
    }
}
