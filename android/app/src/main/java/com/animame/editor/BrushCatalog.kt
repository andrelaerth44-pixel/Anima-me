package com.animame.editor

data class BrushPreset(
    val id:String,
    val name:String,
    val family:String,
    val engine:Engine=Engine.PROCEDURAL,
    val assetFolder:String?=null,
    val defaults:BrushSettings=BrushDefaults.forPreset(id)
)

enum class Engine { PROCEDURAL }

/**
 * Original Anima-me procedural brush library.
 * Seeds are original implementations inspired by broad public brush concepts; no proprietary
 * ibisPaint brush assets are bundled. Each variant changes multiple render-driving parameters.
 */
object BrushCatalog {
    private fun p(id:String,name:String,family:String)=BrushPreset(id,name,family)
    private val seeds = listOf(
        p("basic","Basic Round","Simple"),p("basic-soft","Basic Soft","Simple"),p("basic-hard","Basic Hard","Simple"),p("basic-flat","Basic Flat","Simple"),p("basic-pressure","Pressure Round","Simple"),p("basic-opacity","Opacity Round","Simple"),
        p("pencil","Pencil","Sketch"),p("pencil-soft","Soft Pencil","Sketch"),p("pencil-mechanical","Mechanical Pencil","Sketch"),p("graphite","Graphite","Sketch"),p("sketch-light","Light Sketch","Sketch"),p("charcoal-sketch","Charcoal Sketch","Sketch"),
        p("ink","Ink","Ink"),p("ink-fine","Fine Ink","Ink"),p("ink-gpen","G-Pen","Ink"),p("ink-mapping","Mapping Pen","Ink"),p("ink-brush","Ink Brush","Ink"),p("ink-dry","Dry Ink","Ink"),p("technical","Technical Pen","Ink"),p("fountain","Fountain Pen","Ink"),
        p("comic","Comic Pen","Comic"),p("manga","Manga Pen","Comic"),p("screentone","Screen Tone","Comic"),p("speedline","Speed Line","Comic"),p("multi-line","Multi Line","Comic"),p("double-line","Double Line","Comic"),p("triple-line","Triple Line","Comic"),p("dot-row","Dot Row","Comic"),
        p("marker","Marker","Marker"),p("marker-soft","Soft Marker","Marker"),p("marker-flat","Flat Marker","Marker"),p("highlighter","Highlighter","Marker"),p("felt","Felt Tip","Marker"),p("brush-marker","Brush Marker","Marker"),
        p("pastel","Pastel","Pastel"),p("pastel-soft","Soft Pastel","Pastel"),p("oil-pastel","Oil Pastel","Pastel"),p("crayon","Crayon","Pastel"),p("chalk","Chalk","Chalk"),p("charcoal","Charcoal","Chalk"),p("dry-paint","Dry Paint","Dry Paint"),p("dry-brush","Dry Brush","Dry Paint"),
        p("paint","Opaque Paint","Paint"),p("acrylic","Acrylic","Paint"),p("oil","Oil Paint","Paint"),p("impasto","Impasto","Paint"),p("paint-texture","Textured Paint","Paint"),
        p("watercolor","Watercolor","Watercolor"),p("watercolor-soft","Soft Watercolor","Watercolor"),p("watercolor-wet","Wet Watercolor","Watercolor"),p("watercolor-edge","Watercolor Edge","Watercolor"),p("gouache","Gouache","Watercolor"),p("wash","Ink Wash","Watercolor"),p("shading-wash","Shading Wash","Watercolor"),
        p("smudge","Color Smudge","Blend"),p("blend-soft","Soft Blender","Blend"),p("blend-hard","Hard Blender","Blend"),p("mixing","Mixing Brush","Blend"),
        p("airbrush","Airbrush","Airbrush"),p("airbrush-hard","Hard Airbrush","Airbrush"),p("spray","Spray","Spray"),p("speckle","Speckle","Spray"),p("particles","Particles","Particles"),p("dust","Dust","Particles"),
        p("water","Water","Nature"),p("waves","Waves","Nature"),p("fire","Fire","Nature"),p("smoke","Smoke","Nature"),p("grass","Grass","Nature"),p("leaves","Leaves","Nature"),p("fur","Fur","Nature"),p("hair","Hair","Nature"),p("clouds","Clouds","Nature"),p("sand","Sand","Nature"),
        p("vine","Vine","Plants"),p("flower","Flower","Plants"),p("branch","Branch","Plants"),p("bamboo","Bamboo","Plants"),
        p("light","Light Glow","Effects"),p("bloom","Bloom","Effects"),p("stars","Stars","Effects"),p("spark","Spark","Effects"),p("glitter","Glitter","Effects"),p("neon","Neon","Effects"),p("chain","Chain","Effects"),p("ribbon","Ribbon","Effects"),
        p("light-diamond","Light Diamond","Effects"),p("tilt-mesh","Tilt Mesh","Effects"),p("vintage-tilt-mesh","Vintage Tilt Mesh","Texture"),p("pattern","Pattern Stamp","Texture"),p("texture-pattern","Texture Pattern","Texture"),p("canvas-grain","Canvas Grain","Texture"),
        p("texture","Texture","Texture"),p("paper","Paper Texture","Texture"),p("canvas","Canvas Texture","Texture"),p("noise","Noise Texture","Texture"),
        p("eraser","Eraser","Utility"),p("eraser-soft","Soft Eraser","Utility"),p("eraser-hard","Hard Eraser","Utility")
    )

    private val variantLabels=listOf("Fine","Soft","Hard","Dense","Light","Textured","Pressure","Dry","Wet","Dynamic","Scatter","Tilt")

    private fun variant(seed:BrushPreset,index:Int):BrushPreset {
        val n=index+1
        val base=seed.defaults
        val phase=(index*31f)%360f
        val v=base.copy(
            size=(base.size*(.55f+index*.082f)).coerceIn(base.sizeMin,base.sizeMax),
            opacity=(base.opacity*(.68f+index*.031f).coerceAtMost(1.02f)).coerceIn(0f,1f),
            flow=(base.flow*(.72f+index*.027f)).coerceIn(0f,1f),
            hardness=(base.hardness+(index-5.5f)*.048f).coerceIn(0f,1f),
            feather=(base.feather+(5.5f-index)*.038f).coerceIn(0f,1f),
            spacing=(base.spacing*(.48f+index*.072f)).coerceIn(.01f,4f),
            startThickness=(base.startThickness+(index-5.5f)*.055f).coerceIn(0f,2f),
            endThickness=(base.endThickness+(5.5f-index)*.045f).coerceIn(0f,2f),
            startOpacity=(base.startOpacity-(5.5f-index)*.035f).coerceIn(0f,1f),
            endOpacity=(base.endOpacity+(index-5.5f)*.032f).coerceIn(0f,1f),
            blurDegree=(base.blurDegree+index*.018f).coerceIn(0f,1f),
            fade=(base.fade+index*.014f).coerceIn(0f,1f),
            pressureSize=(base.pressureSize+(index-5.5f)*.062f).coerceIn(-1f,1f),
            pressureOpacity=(base.pressureOpacity+(index-5.5f)*.042f).coerceIn(-1f,1f),
            pressureFlow=(base.pressureFlow+(index-5.5f)*.035f).coerceIn(-1f,1f),
            pressureBlurring=(base.pressureBlurring+(index-5.5f)*.03f).coerceIn(-1f,1f),
            speedThickness=(base.speedThickness+(index-5.5f)*.045f).coerceIn(-1f,1f),
            speedOpacity=(base.speedOpacity+(5.5f-index)*.032f).coerceIn(-1f,1f),
            speedBlurring=(base.speedBlurring+index*.022f).coerceIn(-1f,1f),
            tiltSize=(base.tiltSize+(index-5.5f)*.05f).coerceIn(-1f,1f),
            tiltOpacity=(base.tiltOpacity+(index-5.5f)*.04f).coerceIn(-1f,1f),
            jitter=(base.jitter+index*.021f).coerceIn(0f,1f),
            jitterPosition=(base.jitterPosition+index*.032f).coerceIn(0f,1f),
            jitterThickness=(base.jitterThickness+index*.027f).coerceIn(0f,1f),
            jitterOpacity=(base.jitterOpacity+index*.022f).coerceIn(0f,1f),
            jitterSpacing=(base.jitterSpacing+index*.025f).coerceIn(0f,1f),
            jitterAngle=(base.jitterAngle+index*.041f).coerceIn(0f,1f),
            scatter=base.scatter || index>=10,
            particleSize=(base.particleSize*(.68f+index*.075f)).coerceIn(.1f,512f),
            particleDensity=(base.particleDensity+(index-5.5f)*.052f).coerceIn(0f,1f),
            particleDeviation=(base.particleDeviation+(index-5.5f)*.075f).coerceIn(-1f,1f),
            aspect=(base.aspect*(.68f+index*.058f)).coerceIn(.05f,4f),
            followingRotation=base.followingRotation||index==6||index==11,
            initialAngle=base.initialAngle+phase,
            textureStrength=(base.textureStrength+(index-5.5f)*.052f).coerceIn(0f,1f),
            textureScale=(base.textureScale*(.58f+index*.082f)).coerceAtLeast(.05f),
            textureRotation=(base.textureRotation+phase*.73f)%360f,
            textureOffset=(base.textureOffset+index*.137f)%1f,
            roundness=(base.roundness+(index-5.5f)*.038f).coerceIn(.05f,1f),
            customName="${seed.name} • ${variantLabels[index]}"
        )
        return BrushPreset("${seed.id}-v$n",v.customName?:"${seed.name} v$n",seed.family,Engine.PROCEDURAL,seed.assetFolder,v.normalized())
    }

    // 100+ original seeds x 12 visual variants = 1200+ presets.
    private val builtIns: List<BrushPreset> = seeds + seeds.flatMap { seed ->
        variantLabels.indices.map { index -> variant(seed, index) }
    }
    private val imported=mutableListOf<BrushPreset>()
    val presets:List<BrushPreset> get()=builtIns+imported
    val families=listOf("Simple","Sketch","Ink","Comic","Marker","Pastel","Chalk","Dry Paint","Paint","Watercolor","Blend","Airbrush","Spray","Particles","Nature","Plants","Effects","Texture","Utility","Imported")
    fun replaceImported(list:List<BrushPreset>){imported.clear();imported.addAll(list)}
}
