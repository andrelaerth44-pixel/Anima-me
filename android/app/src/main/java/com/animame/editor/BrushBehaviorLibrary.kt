package com.animame.editor

import kotlin.math.abs

/**
 * Deterministic behavior resolver for every named brush preset.
 * The catalog remains data-only; this layer gives each preset a reproducible
 * combination of kernel, material, spacing, texture, dynamics, jitter and flow.
 */
object BrushBehaviorLibrary {
    fun settings(id: String): BrushSettings {
        val preset = BrushCatalog.all().firstOrNull { it.id == id }
            ?: return BrushDefaults.forPreset(id)
        return settings(preset.id, preset.name, preset.category)
    }

    fun settings(id: String, name: String, category: String): BrushSettings {
        val key = (id + "|" + name + "|" + category).hashCode().toLong()
        val h = abs(key).toInt()
        val lower = name.lowercase()
        val material = when {
            "watercolor" in lower || "water" in lower || category.equals("Watercolor", true) -> BrushMaterial.WATERCOLOR
            "gouache" in lower -> BrushMaterial.GOUACHE
            "acrylic" in lower -> BrushMaterial.ACRYLIC
            "oil" in lower || category.equals("Paint", true) -> BrushMaterial.OIL
            "pencil" in lower || "mechanical" in lower -> BrushMaterial.PENCIL
            "charcoal" in lower -> BrushMaterial.CHARCOAL
            "chalk" in lower -> BrushMaterial.CHALK
            "pastel" in lower || category.equals("Pastel", true) -> BrushMaterial.PASTEL
            "marker" in lower -> BrushMaterial.MARKER
            "airbrush" in lower -> BrushMaterial.AIRBRUSH
            "glitter" in lower || "sequin" in lower -> BrushMaterial.GLITTER
            "hair" in lower || category.equals("Hair", true) || "braid" in lower -> BrushMaterial.HAIR
            "grass" in lower || "leaf" in lower || "flower" in lower || category.equals("Plants", true) -> BrushMaterial.PLANT
            "particle" in lower || "spray" in lower || "sputter" in lower -> BrushMaterial.PARTICLE
            "stamp" in lower || category.equals("Stamps", true) || category.equals("Materials", true) -> BrushMaterial.STAMP
            else -> BrushMaterial.INK
        }
        val algorithm = when {
            material == BrushMaterial.WATERCOLOR -> BrushAlgorithm.WATER
            material == BrushMaterial.AIRBRUSH -> BrushAlgorithm.AIRBRUSH
            category.equals("Vector", true) || "ribbon" in lower -> BrushAlgorithm.VECTOR
            material == BrushMaterial.PARTICLE || material == BrushMaterial.STAMP -> BrushAlgorithm.PROCEDURAL
            else -> BrushAlgorithm.MONO
        }
        val texture = ((h ushr 8) % 100) / 100f
        val jitter = ((h ushr 16) % 55) / 100f
        val rotation = ((h ushr 24) % 360).toFloat()
        val pressure = .55f + ((h % 40) / 100f)
        val spacing = when {
            material == BrushMaterial.AIRBRUSH -> .025f + ((h % 8) / 1000f)
            material == BrushMaterial.WATERCOLOR -> .045f + ((h % 18) / 1000f)
            material == BrushMaterial.PARTICLE || material == BrushMaterial.STAMP -> .18f + ((h % 35) / 100f)
            else -> .045f + ((h % 35) / 1000f)
        }
        val flow = when {
            material == BrushMaterial.WATERCOLOR -> .55f + ((h % 35) / 100f)
            material == BrushMaterial.AIRBRUSH -> .22f + ((h % 25) / 100f)
            material == BrushMaterial.INK -> .78f + ((h % 22) / 100f)
            else -> .65f + ((h % 30) / 100f)
        }.coerceIn(.05f, 1f)
        return BrushSettings(
            id = id,
            name = name,
            category = category,
            algorithm = algorithm,
            material = material,
            size = 12f * (.65f + ((h ushr 3) % 150) / 100f),
            opacity = (.62f + ((h ushr 11) % 38) / 100f).coerceIn(.1f, 1f),
            minSizeFactor = (.16f + ((h ushr 5) % 25) / 100f).coerceIn(.05f, .7f),
            minOpacity = (.12f + ((h ushr 9) % 30) / 100f).coerceIn(.02f, .8f),
            spacing = spacing,
            pressureSizeFactor = pressure,
            pressureOpacityFactor = .45f + ((h ushr 13) % 55) / 100f,
            pressureExponent = .7f + ((h ushr 17) % 80) / 100f,
            speedSizeFactor = .72f + ((h ushr 19) % 48) / 100f,
            speedOpacityFactor = .7f + ((h ushr 21) % 45) / 100f,
            jitterPosition = if (material == BrushMaterial.PARTICLE || material == BrushMaterial.STAMP) jitter else jitter * .22f,
            jitterThickness = jitter * .55f,
            jitterOpacity = jitter * .35f,
            jitterSpacing = jitter * .45f,
            rotationJitter = if (material == BrushMaterial.STAMP || material == BrushMaterial.HAIR) rotation else rotation * .12f,
            initialAngle = rotation,
            followRotation = category.equals("Vector", true) || "braid" in lower || "hair" in lower,
            aspect = .65f + ((h ushr 23) % 90) / 100f,
            antialias = true,
            flow = flow,
            fadeOpacity = if ("fade" in lower || "soft" in lower) .18f else .85f,
            fadeStart = if ("fade" in lower) .15f else 0f,
            fadeEnd = if ("fade" in lower) .82f else 1f,
            textureOpacity = if (material == BrushMaterial.DIGITAL || material == BrushMaterial.INK) texture * .45f else texture,
            textureLowerLimit = .15f + texture * .55f,
            textureScale = 1f + ((h ushr 27) % 24) / 10f,
            textureGrain = texture,
            scatterSize = if (material == BrushMaterial.PARTICLE || material == BrushMaterial.STAMP) .25f + jitter else jitter * .08f,
            waterColorMix = if (material == BrushMaterial.WATERCOLOR) .35f + texture * .55f else 0f,
            waterWetness = if (material == BrushMaterial.WATERCOLOR) .55f + jitter * .4f else 0f,
            waterDragging = if (material == BrushMaterial.WATERCOLOR) .15f + texture * .5f else 0f,
            blendMode = if ("shadow" in lower) "MULTIPLY" else "NORMAL"
        ).normalized()
    }
}
