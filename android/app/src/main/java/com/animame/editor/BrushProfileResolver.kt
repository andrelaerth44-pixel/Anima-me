package com.animame.editor

/**
 * Converts catalog labels into original Anima-me brush profiles.
 * Name/category driven so the large catalog does not collapse into one generic renderer.
 */
object BrushProfileResolver {
    fun resolve(input: BrushSettings): BrushSettings {
        val n = input.name.lowercase()
        val c = input.category.lowercase()
        var s = input

        fun patch(material: BrushMaterial, algorithm: BrushAlgorithm = s.algorithm) {
            s = s.copy(material = material, algorithm = algorithm)
        }

        when {
            c.contains("eraser") || n.contains("eraser") -> {
                patch(BrushMaterial.DIGITAL, BrushAlgorithm.ERASER)
                s = s.copy(opacity = 1f, minOpacity = 1f, pressureSizeFactor = 1f, pressureOpacityFactor = 0f)
            }
            n.contains("watercolor") || c.contains("watercolor") -> {
                patch(BrushMaterial.WATERCOLOR, BrushAlgorithm.WATER)
                s = s.copy(spacing = .045f, minSizeFactor = .45f, minOpacity = .12f, waterColorMix = .55f, waterWetness = .72f, waterDragging = .38f, waterCorrection = .2f, textureOpacity = if (n.contains("texture")) .42f else .16f, fadeOpacity = if (n.contains("fade")) .3f else .72f, fadeStart = .05f, fadeEnd = .9f, jitterOpacity = .08f)
            }
            n.contains("gouache") -> {
                patch(BrushMaterial.GOUACHE, BrushAlgorithm.WATER)
                s = s.copy(spacing = .055f, waterColorMix = .28f, waterWetness = .28f, pressureSizeFactor = .85f, textureOpacity = .3f, minOpacity = .35f)
            }
            n.contains("acrylic") -> {
                patch(BrushMaterial.ACRYLIC, BrushAlgorithm.COLOR)
                s = s.copy(spacing = .06f, textureOpacity = .38f, pressureSizeFactor = .75f, pressureOpacityFactor = .65f, minOpacity = .45f)
            }
            n.contains("oil") || (c.contains("paint") && n.contains("brush")) -> {
                patch(BrushMaterial.OIL, BrushAlgorithm.DOUBLE)
                s = s.copy(spacing = .055f, textureOpacity = .34f, pressureSizeFactor = .72f, pressureOpacityFactor = .65f, minOpacity = .42f, doubleShadowSize = .18f, doubleShadowDistance = .08f)
            }
            n.contains("airbrush") -> {
                patch(BrushMaterial.AIRBRUSH, BrushAlgorithm.AIRBRUSH)
                s = s.copy(spacing = .018f, opacity = minOf(s.opacity, .38f), minOpacity = .04f, blur = .72f, blurStart = .35f, blurMiddle = .72f, blurEnd = .9f, pressureOpacityFactor = .75f)
            }
            n.contains("pencil") || c.contains("sketch") || n.contains("mechanical") -> {
                patch(if (n.contains("charcoal")) BrushMaterial.CHARCOAL else BrushMaterial.PENCIL)
                s = s.copy(spacing = .075f, minSizeFactor = .28f, minOpacity = .18f, pressureSizeFactor = .92f, pressureOpacityFactor = .88f, textureOpacity = .58f, jitterThickness = .12f, jitterPosition = .025f, speedSizeFactor = .88f)
            }
            n.contains("charcoal") -> {
                patch(BrushMaterial.CHARCOAL)
                s = s.copy(spacing = .065f, minSizeFactor = .3f, minOpacity = .12f, pressureSizeFactor = .9f, textureOpacity = .72f, jitterPosition = .045f, jitterOpacity = .18f, scatterSize = .12f, scatterDensity = .35f)
            }
            n.contains("pastel") -> {
                patch(BrushMaterial.PASTEL)
                s = s.copy(spacing = .07f, minSizeFactor = .42f, minOpacity = .3f, pressureSizeFactor = .8f, textureOpacity = .65f, jitterPosition = .025f)
            }
            n.contains("crayon") -> {
                patch(BrushMaterial.CRAYON)
                s = s.copy(spacing = .085f, minSizeFactor = .5f, minOpacity = .35f, textureOpacity = .8f, jitterThickness = .16f)
            }
            n.contains("chalk") -> {
                patch(BrushMaterial.CHALK)
                s = s.copy(spacing = .075f, minSizeFactor = .38f, minOpacity = .22f, textureOpacity = .9f, jitterPosition = .035f, scatterSize = .06f)
            }
            n.contains("marker") -> {
                patch(BrushMaterial.MARKER)
                s = s.copy(spacing = .035f, minSizeFactor = .6f, minOpacity = .55f, pressureSizeFactor = .55f, pressureOpacityFactor = .5f, fadeOpacity = .82f, fadeStart = .02f, fadeEnd = .96f)
            }
            n.contains("pointillism") || c.contains("pointillism") -> {
                patch(BrushMaterial.PARTICLE, BrushAlgorithm.PROCEDURAL)
                s = s.copy(spacing = .22f, minSizeFactor = .35f, minOpacity = .18f, scatterSize = .7f, scatterDensity = .8f, particleThickness = .8f, jitterPosition = .22f, jitterSpacing = .45f)
            }
            n.contains("glitter") || n.contains("sequin") || (c.contains("effects") && n.contains("spark")) -> {
                patch(BrushMaterial.GLITTER, BrushAlgorithm.PROCEDURAL)
                s = s.copy(spacing = .12f, minSizeFactor = .35f, scatterSize = .8f, scatterDensity = .9f, particleThickness = .7f, jitterPosition = .35f, rotationJitter = 3.14f, jitterSpacing = .55f)
            }
            n.contains("hair") || n.contains("fur") || n.contains("braid") || c.contains("hair") -> {
                patch(BrushMaterial.HAIR, BrushAlgorithm.PROCEDURAL)
                s = s.copy(spacing = .045f, minSizeFactor = .18f, pressureSizeFactor = .9f, pressureOpacityFactor = .7f, jitterPosition = .06f, jitterThickness = .14f, followRotation = true, rotationJitter = .12f)
            }
            n.contains("grass") || n.contains("leaf") || n.contains("flower") || n.contains("tree") || n.contains("trunk") || c.contains("plants") -> {
                patch(BrushMaterial.PLANT, BrushAlgorithm.PROCEDURAL)
                s = s.copy(spacing = .16f, minSizeFactor = .35f, scatterSize = .3f, scatterDensity = .45f, rotationJitter = 1.2f, followRotation = true, jitterPosition = .12f, jitterSpacing = .25f)
            }
            n.contains("chain") || n.contains("lace") || n.contains("bead") || n.contains("stitch") || n.contains("knitting") -> {
                patch(BrushMaterial.PARTICLE, BrushAlgorithm.PROCEDURAL)
                s = s.copy(spacing = .28f, minSizeFactor = .5f, scatterSize = .04f, scatterDensity = .9f, followRotation = true, rotationJitter = .08f, particleThickness = 1.15f)
            }
            n.contains("outline") || c.contains("outlines") -> {
                patch(BrushMaterial.INK, BrushAlgorithm.MONO)
                s = s.copy(spacing = .025f, minSizeFactor = .55f, minOpacity = .72f, pressureSizeFactor = .58f, pressureOpacityFactor = .45f, jitterPosition = 0f, jitterThickness = 0f)
            }
            n.contains("calligraphy") || n.contains("dip pen") || n.contains("fountain") || n.contains("falcon") -> {
                patch(BrushMaterial.INK, BrushAlgorithm.MONO)
                s = s.copy(spacing = .03f, minSizeFactor = .2f, minOpacity = .6f, pressureSizeFactor = .95f, pressureOpacityFactor = .75f, followRotation = true, rotationJitter = .02f, aspect = .72f)
            }
            n.contains("blur") || n.contains("blurring") -> {
                patch(BrushMaterial.DIGITAL, BrushAlgorithm.SMUDGE)
                s = s.copy(spacing = .03f, opacity = minOf(s.opacity, .45f), minOpacity = .05f, blur = .8f, blurStart = .4f, blurMiddle = .75f, blurEnd = .9f)
            }
            n.contains("vector") -> {
                patch(BrushMaterial.DIGITAL, BrushAlgorithm.VECTOR)
                s = s.copy(spacing = .02f, minSizeFactor = .85f, minOpacity = .9f, pressureSizeFactor = .35f, pressureOpacityFactor = .25f, antialias = true)
            }
            n.contains("stamp") || c.contains("stamps") -> {
                patch(BrushMaterial.STAMP, BrushAlgorithm.PROCEDURAL)
                s = s.copy(spacing = .65f, minSizeFactor = 1f, minOpacity = 1f, pressureSizeFactor = .25f, pressureOpacityFactor = .15f, jitterSpacing = .15f)
            }
            n.contains("digital") || c.contains("digital") -> {
                patch(BrushMaterial.DIGITAL, BrushAlgorithm.MONO)
                s = s.copy(spacing = .02f, minSizeFactor = .7f, minOpacity = .75f, pressureSizeFactor = .55f, pressureOpacityFactor = .45f)
            }
            n.contains("ink") || n.contains("pen") || c.contains("ink") -> {
                patch(BrushMaterial.INK, BrushAlgorithm.MONO)
                s = s.copy(spacing = .028f, minSizeFactor = .25f, minOpacity = .55f, pressureSizeFactor = .88f, pressureOpacityFactor = .68f)
            }
            c.contains("brushes") -> {
                patch(BrushMaterial.INK, BrushAlgorithm.MONO)
                s = s.copy(spacing = .06f, minSizeFactor = .32f, minOpacity = .3f, pressureSizeFactor = .75f, pressureOpacityFactor = .6f)
            }
        }

        if (n.contains("soft")) s = s.copy(blur = maxOf(s.blur, .12f), minOpacity = minOf(s.minOpacity, .28f))
        if (n.contains("hard")) s = s.copy(blur = minOf(s.blur, .02f), minOpacity = maxOf(s.minOpacity, .6f))
        if (n.contains("rough") || n.contains("coarse")) s = s.copy(jitterPosition = maxOf(s.jitterPosition, .04f), jitterThickness = maxOf(s.jitterThickness, .12f), textureOpacity = maxOf(s.textureOpacity, .5f))
        if (n.contains("fade")) s = s.copy(fadeStart = .08f, fadeEnd = .9f, fadeOpacity = .2f)
        if (n.contains("bleed")) s = s.copy(waterWetness = maxOf(s.waterWetness, .85f), waterDragging = maxOf(s.waterDragging, .55f), jitterOpacity = maxOf(s.jitterOpacity, .08f))
        if (n.contains("texture")) s = s.copy(textureOpacity = maxOf(s.textureOpacity, .65f))
        if (n.contains("particle") || n.contains("splash") || n.contains("shower")) s = s.copy(scatterSize = maxOf(s.scatterSize, .45f), scatterDensity = maxOf(s.scatterDensity, .55f), jitterPosition = maxOf(s.jitterPosition, .12f))
        if (n.contains("color")) s = s.copy(hueJitter = maxOf(s.hueJitter, .06f), saturationJitter = maxOf(s.saturationJitter, .08f))

        return s.copy(name = input.name, category = input.category, id = input.id)
    }
}
