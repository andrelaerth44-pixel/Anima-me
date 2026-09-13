package com.animame.editor

object BrushLibraryBridge {
    data class Preset(val id: String, val name: String, val category: String, val session: String, val subcategory: String)

    fun presets(): List<Preset> = BrushLibraryV2.all.map { spec ->
        Preset(spec.id, spec.name, spec.category, spec.session, spec.subcategory)
    }

    fun settings(spec: BrushSpec): BrushSettings {
        val material: BrushMaterial = when (spec.kernel) {
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
        val algorithm: BrushAlgorithm = when (spec.kernel) {
            BrushKernel.WATERCOLOR -> BrushAlgorithm.WATER
            BrushKernel.AIRBRUSH, BrushKernel.SOFT -> BrushAlgorithm.AIRBRUSH
            BrushKernel.RIBBON -> BrushAlgorithm.VECTOR
            BrushKernel.STAMP, BrushKernel.SPRAY -> BrushAlgorithm.PROCEDURAL
            else -> BrushAlgorithm.PROCEDURAL
        }
        val textureOpacity: Float = if (spec.texture == BrushTexture.NONE) 0f else spec.grain.coerceIn(0f, 1f)
        val textureScale: Float = when (spec.texture) {
            BrushTexture.PAPER, BrushTexture.CANVAS, BrushTexture.FABRIC -> 1.8f
            BrushTexture.GRAIN, BrushTexture.TOOTH, BrushTexture.ROUGH -> 1.25f
            BrushTexture.DOT, BrushTexture.SPECKLE -> 2.5f
            else -> 1f
        }
        val pressureSize: Float = when (spec.dynamics) {
            BrushDynamics.PRESSURE, BrushDynamics.PRESSURE_VELOCITY, BrushDynamics.PRESSURE_TILT -> .95f
            else -> .55f
        }
        val pressureOpacity: Float = when (spec.dynamics) {
            BrushDynamics.PRESSURE_TILT, BrushDynamics.PRESSURE_VELOCITY -> .9f
            else -> .55f
        }
        val speedSize: Float = (1f - (spec.dynamics.ordinal / 12f)).coerceIn(.35f, 1f)
        val speedOpacity: Float = (1f - spec.velocityResponse * .45f).coerceIn(.1f, 1f)

        return BrushSettings(
            id = spec.id,
            name = spec.name,
            category = spec.category,
            algorithm = algorithm,
            material = material,
            size = 12f * spec.sizeScale,
            opacity = spec.opacityScale.coerceIn(0f, 1f),
            spacing = spec.spacing.coerceIn(.005f, 4f),
            minSizeFactor = .18f,
            pressureSizeFactor = pressureSize,
            pressureOpacityFactor = pressureOpacity,
            jitterPosition = spec.jitter,
            jitterThickness = spec.jitter * .7f,
            jitterSpacing = spec.jitter * .4f,
            rotationJitter = spec.rotation,
            initialAngle = spec.rotation,
            followRotation = spec.kernel == BrushKernel.RIBBON,
            scatterSize = spec.scatter,
            textureOpacity = textureOpacity,
            textureLowerLimit = (1f - spec.grain * .8f).coerceIn(0f, 1f),
            textureScale = textureScale,
            textureGrain = spec.grain.coerceIn(0f, 1f),
            flow = spec.flow.coerceIn(0f, 1f),
            fadeStart = spec.fadeStart.coerceIn(0f, 1f),
            fadeEnd = spec.fadeEnd.coerceIn(0f, 1f),
            speedSizeFactor = speedSize,
            speedOpacityFactor = speedOpacity,
            pressureExponent = spec.pressureExponent,
            waterColorMix = spec.mix.coerceIn(0f, 1f),
            waterWetness = if (material == BrushMaterial.WATERCOLOR) .75f else 0f,
            antialias = true
        ).normalized()
    }
}
