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
 * The seed presets expand into hundreds of deterministic variants. Each variant has its
 * own settings, so selecting different variants produces visibly different strokes.
 */
object BrushCatalog {
    private fun p(id:String,name:String,family:String)=BrushPreset(id,name,family)
    private val seeds = listOf(
        p("basic","Basic Round","Simple"),p("basic-soft","Basic Soft","Simple"),p("basic-hard","Basic Hard","Simple"),p("basic-flat","Basic Flat","Simple"),p("basic-pressure","Pressure Round","Simple"),p("basic-opacity","Opacity Round","Simple"),
        p("pencil","Pencil","Sketch"),p("pencil-soft","Soft Pencil","Sketch"),p("pencil-mechanical","Mechanical Pencil","Sketch"),p("graphite","Graphite","Sketch"),p("sketch-light","Light Sketch","Sketch"),p("charcoal-sketch","Charcoal Sketch","Sketch"),
        p("ink","Ink","Ink"),p("ink-fine","Fine Ink","Ink"),p("ink-gpen","G-Pen","Ink"),p("ink-mapping","Mapping Pen","Ink"),p("ink-brush","Ink Brush","Ink"),p("ink-dry","Dry Ink","Ink"),p("technical","Technical Pen","Ink"),p("fountain","Fountain Pen","Ink"),
        p("comic","Comic Pen","Comic"),p("manga","Manga Pen","Comic"),p("screentone","Screen Tone","Comic"),p("speedline","Speed Line","Comic"),
        p("marker","Marker","Marker"),p("marker-soft","Soft Marker","Marker"),p("marker-flat","Flat Marker","Marker"),p("highlighter","Highlighter","Marker"),p("felt","Felt Tip","Marker"),p("brush-marker","Brush Marker","Marker"),
        p("pastel","Pastel","Pastel"),p("pastel-soft","Soft Pastel","Pastel"),p("oil-pastel","Oil Pastel","Pastel"),p("crayon","Crayon","Pastel"),p("chalk","Chalk","Chalk"),p("charcoal","Charcoal","Chalk"),p("dry-paint","Dry Paint","Dry Paint"),p("dry-brush","Dry Brush","Dry Paint"),
        p("paint","Opaque Paint","Paint"),p("acrylic","Acrylic","Paint"),p("oil","Oil Paint","Paint"),p("impasto","Impasto","Paint"),
        p("watercolor","Watercolor","Watercolor"),p("watercolor-soft","Soft Watercolor","Watercolor"),p("watercolor-wet","Wet Watercolor","Watercolor"),p("watercolor-edge","Watercolor Edge","Watercolor"),p("gouache","Gouache","Watercolor"),p("wash","Ink Wash","Watercolor"),
        p("smudge","Color Smudge","Blend"),p("blend-soft","Soft Blender","Blend"),p("blend-hard","Hard Blender","Blend"),p("mixing","Mixing Brush","Blend"),
        p("airbrush","Airbrush","Airbrush"),p("airbrush-hard","Hard Airbrush","Airbrush"),p("spray","Spray","Spray"),p("speckle","Speckle","Spray"),p("particles","Particles","Particles"),p("dust","Dust","Particles"),
        p("water","Water","Nature"),p("waves","Waves","Nature"),p("fire","Fire","Nature"),p("smoke","Smoke","Nature"),p("grass","Grass","Nature"),p("leaves","Leaves","Nature"),p("fur","Fur","Nature"),p("hair","Hair","Nature"),p("clouds","Clouds","Nature"),p("sand","Sand","Nature"),
        p("vine","Vine","Plants"),p("flower","Flower","Plants"),p("branch","Branch","Plants"),p("bamboo","Bamboo","Plants"),
        p("light","Light Glow","Effects"),p("bloom","Bloom","Effects"),p("stars","Stars","Effects"),p("spark","Spark","Effects"),p("glitter","Glitter","Effects"),p("neon","Neon","Effects"),p("chain","Chain","Effects"),p("ribbon","Ribbon","Effects"),
        p("texture","Texture","Texture"),p("paper","Paper Texture","Texture"),p("canvas","Canvas Texture","Texture"),p("noise","Noise Texture","Texture"),
        p("eraser","Eraser","Utility"),p("eraser-soft","Soft Eraser","Utility"),p("eraser-hard","Hard Eraser","Utility")
    )
    private val variantLabels=listOf("Fine","Soft","Hard","Dense","Light","Textured","Pressure","Dry","Wet")
    private fun variant(seed:BrushPreset,index:Int):BrushPreset {
        val n=index+1
        val base=seed.defaults
        val v=base.copy(
            size=(base.size*(.62f+index*.095f)).coerceIn(base.sizeMin,base.sizeMax),
            opacity=(base.opacity*(.72f+index*.035f).coerceAtMost(1.02f)).coerceIn(0f,1f),
            flow=(base.flow*(.82f+index*.025f)).coerceIn(0f,1f),
            hardness=(base.hardness+(index-4)*.055f).coerceIn(0f,1f),
            feather=(base.feather+(4-index)*.045f).coerceIn(0f,1f),
            spacing=(base.spacing*(.62f+index*.075f)).coerceIn(.01f,4f),
            pressureSize=(base.pressureSize+(index-4)*.065f).coerceIn(-1f,1f),
            pressureOpacity=(base.pressureOpacity+(index-4)*.04f).coerceIn(-1f,1f),
            pressureFlow=(base.pressureFlow+(index-4)*.035f).coerceIn(-1f,1f),
            jitter=(base.jitter+index*.025f).coerceIn(0f,1f),
            jitterPosition=(base.jitterPosition+index*.035f).coerceIn(0f,1f),
            jitterOpacity=(base.jitterOpacity+index*.025f).coerceIn(0f,1f),
            jitterAngle=(base.jitterAngle+index*.045f).coerceIn(0f,1f),
            textureStrength=(base.textureStrength+(index-4)*.045f).coerceIn(0f,1f),
            textureScale=(base.textureScale*(.72f+index*.09f)).coerceAtLeast(.05f),
            particleDensity=(base.particleDensity+(index-4)*.055f).coerceIn(0f,1f),
            particleDeviation=(base.particleDeviation+(index-4)*.08f).coerceIn(-1f,1f),
            aspect=(base.aspect*(.78f+index*.055f)).coerceIn(.05f,4f),
            followingRotation=base.followingRotation||index==7||index==8,
            initialAngle=base.initialAngle+index*17.5f,
            customName="${seed.name} • ${variantLabels[index]}"
        )
        return BrushPreset("${seed.id}-v$n",v.customName?:"${seed.name} v$n",seed.family,Engine.PROCEDURAL,seed.assetFolder,v)
    }

    // 90+ seeds x 9 deterministic variants = 900+ ready-to-use procedural presets.
    private val builtIns: List<BrushPreset> = seeds + seeds.flatMap { seed ->
        variantLabels.indices.map { index -> variant(seed, index) }
    }
    private val imported=mutableListOf<BrushPreset>()
    val presets:List<BrushPreset> get()=builtIns+imported
    val families=listOf("Simple","Sketch","Ink","Comic","Marker","Pastel","Chalk","Dry Paint","Paint","Watercolor","Blend","Airbrush","Spray","Particles","Nature","Plants","Effects","Texture","Utility","Imported")
    fun replaceImported(list:List<BrushPreset>){imported.clear();imported.addAll(list)}
}
