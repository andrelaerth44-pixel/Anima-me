package com.animame.editor

/** Maps the 1024 procedural library specs into the real BrushEngine settings model. */
object BrushLibraryBridge {
    data class Preset(
        val id: String,
        val name: String,
        val category: String,
        val session: String,
        val subcategory: String
    )

    fun presets(): List<Preset> = BrushLibraryV2.all.map {
        Preset(it.id, it.name, it.category, it.session, it.subcategory)
    }

    fun settings(spec: BrushSpec): BrushSettings {
        val material = when (spec.kernel) {
            BrushKernel.PENCIL -> BrushMaterial.PENCIL
            BrushKernel.CHARCOAL -> BrushMaterial.CHARCOAL
            BrushKernel.CHALK -> BrushMaterial.CHALK
            BrushKernel.WATERCOLOR -> BrushMaterial.WATERCOLOR
            BrushKernel.GOUACHE -> BrushMaterial.GOUACHE
            BrushKernel.OIL -> BrushMaterial.OIL
            BrushKernel.AIRBRUSH, BrushKernel.SOFT -> BrushMaterial.AIRBRUSH
            BrushKernel.INK, BrushKernel.CALLIGRAPHY -> BrushMaterial.INK
            BrushKernel.DRY -> BrushMaterial.PASTEL
            BrushKernel.SPRAY -> BrushMaterial.PARTICLE
            BrushKernel.STAMP -> BrushMaterial.STAMP
            else -> BrushMaterial.DIGITAL
        }
        val algorithm = when (spec.kernel) {
            BrushKernel.WATERCOLOR -> BrushAlgorithm.WATER
            BrushKernel.AIRBRUSH, BrushKernel.SOFT -> BrushAlgorithm.AIRBRUSH
            BrushKernel.RIBBON -> BrushAlgorithm.VECTOR
            BrushKernel.STAMP, BrushKernel.SPRAY -> BrushAlgorithm.PROCEDURAL
            else -> BrushAlgorithm.PROCEDURAL
        }
        val textureOpacity = if (spec.texture == BrushTexture.NONE) 0f else spec.grain.coerceIn(0f, 1f)
        val textureScale = when (spec.texture) {
            BrushTexture.PAPER, BrushTexture.CANVAS, BrushTexture.FABRIC -> 1.8f
            BrushTexture.GRAIN, BrushTexture.TOOTH, BrushTexture.ROUGH -> 1.25f
            BrushTexture.DOT, BrushTexture.SPECKLE -> 2.5f
            else -> 1f
        }
        val pressureDriven = when (spec.dynamics) {
            BrushDynamics.PRESSURE, BrushDynamics.PRESSURE_VELOCITY, BrushDynamics.PRESSURE_TILT -> true
            else -> false
        }
        val velocityDriven = when (spec.dynamics) {
            BrushDynamics.VELOCITY, BrushDynamics.PRESSURE_VELOCITY, BrushDynamics.VELOCITY_TILT -> true
            else -> false
        }
        return BrushSettings(
            id = spec.id,
            name = spec.name,
            category = spec.category,
            algorithm = algorithm,
            material = material,
            size = 12f * spec.sizeScale,
            opacity = spec.opacityScale,
            spacing = spec.spacing.coerceIn(0.005f, 4f),
            minSizeFactor = if (pressureDriven) 0.18f else 0.35f,
            pressureSizeFactor = if (pressureDriven) 0.95f else 0.55f,
            pressureOpacityFactor = if (pressureDriven) 0.9f else 0.55f,
            jitterPosition = spec.jitter,
            jitterThickness = spec.jitter * 0.7f,
            jitterSpacing = spec.jitter * 0.4f,
            rotationJitter = spec.rotation,
            initialAngle = spec.rotation,
            followRotation = spec.kernel == BrushKernel.RIBBON,
            scatterSize = spec.scatter,
            textureOpacity = textureOpacity,
            textureLowerLimit = (1f - spec.grain * 0.8f).coerceIn(0f, 1f),
            textureScale = textureScale,
            textureGrain = spec.grain,
            flow = spec.flow,
            fadeStart = spec.fadeStart,
            fadeEnd = spec.fadeEnd,
            speedSizeFactor = if (velocityDriven) (1f - spec.velocityResponse * 0.45f) else 1f,
            speedOpacityFactor = if (velocityDriven) (1f - spec.velocityResponse * 0.45f) else 1f,
            pressureExponent = spec.pressureExponent,
            waterColorMix = spec.mix,
            waterWetness = if (material == BrushMaterial.WATERCOLOR) 0.75f else 0f,
            antialias = true
        ).normalized()
    }
}
