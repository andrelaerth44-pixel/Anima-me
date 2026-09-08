package com.animame.editor

data class BrushPreset(
    val id: String,
    val name: String,
    val family: String,
    val engine: Engine,
    val assetFolder: String? = null,
    val defaults: BrushSettings = BrushDefaults.forPreset(id)
)

enum class Engine { TOONZ_RASTER, FULL_COLOR_MYPAINT, VECTOR, PROCEDURAL }

/** Original Anima-me brush library: organized by practical digital-painting families. */
object BrushCatalog {
    private fun p(id:String,name:String,family:String,engine:Engine=Engine.FULL_COLOR_MYPAINT)=BrushPreset(id,name,family,engine)
    val presets: List<BrushPreset> = listOf(
        p("basic","Basic Round","Simple"), p("basic-soft","Basic Soft","Simple"), p("basic-hard","Basic Hard","Simple"),
        p("basic-flat","Basic Flat","Simple"), p("basic-pressure","Pressure Round","Simple"), p("basic-opacity","Opacity Round","Simple"),
        p("pencil","Pencil","Sketch",Engine.TOONZ_RASTER), p("pencil-soft","Soft Pencil","Sketch",Engine.TOONZ_RASTER),
        p("pencil-mechanical","Mechanical Pencil","Sketch",Engine.TOONZ_RASTER), p("graphite","Graphite","Sketch",Engine.TOONZ_RASTER),
        p("sketch-light","Light Sketch","Sketch",Engine.TOONZ_RASTER), p("charcoal-sketch","Charcoal","Sketch",Engine.PROCEDURAL),
        p("ink","Ink","Ink",Engine.TOONZ_RASTER), p("ink-fine","Fine Ink","Ink",Engine.TOONZ_RASTER),
        p("ink-gpen","G-Pen","Ink",Engine.TOONZ_RASTER), p("ink-mapping","Mapping Pen","Ink",Engine.TOONZ_RASTER),
        p("ink-brush","Ink Brush","Ink",Engine.TOONZ_RASTER), p("ink-dry","Dry Ink","Ink",Engine.PROCEDURAL),
        p("comic","Comic Pen","Comic",Engine.TOONZ_RASTER), p("manga","Manga Pen","Comic",Engine.TOONZ_RASTER),
        p("screentone","Screen Tone","Comic",Engine.PROCEDURAL), p("speedline","Speed Line","Comic",Engine.PROCEDURAL),
        p("marker","Marker","Marker",Engine.PROCEDURAL), p("marker-soft","Soft Marker","Marker",Engine.PROCEDURAL),
        p("marker-flat","Flat Marker","Marker",Engine.PROCEDURAL), p("highlighter","Highlighter","Marker",Engine.PROCEDURAL),
        p("felt","Felt Tip","Marker",Engine.PROCEDURAL), p("brush-marker","Brush Marker","Marker",Engine.PROCEDURAL),
        p("pastel","Pastel","Pastel",Engine.PROCEDURAL), p("pastel-soft","Soft Pastel","Pastel",Engine.PROCEDURAL),
        p("oil-pastel","Oil Pastel","Pastel",Engine.PROCEDURAL), p("crayon","Crayon","Pastel",Engine.PROCEDURAL),
        p("chalk","Chalk","Chalk",Engine.PROCEDURAL), p("charcoal","Charcoal","Chalk",Engine.PROCEDURAL),
        p("dry-paint","Dry Paint","Dry Paint",Engine.PROCEDURAL), p("dry-brush","Dry Brush","Dry Paint",Engine.PROCEDURAL),
        p("paint","Opaque Paint","Paint"), p("acrylic","Acrylic","Paint",Engine.PROCEDURAL),
        p("oil","Oil Paint","Paint",Engine.PROCEDURAL), p("impasto","Impasto","Paint",Engine.PROCEDURAL),
        p("watercolor","Watercolor","Watercolor",Engine.PROCEDURAL), p("watercolor-soft","Soft Watercolor","Watercolor",Engine.PROCEDURAL),
        p("watercolor-wet","Wet Watercolor","Watercolor",Engine.PROCEDURAL), p("watercolor-edge","Watercolor Edge","Watercolor",Engine.PROCEDURAL),
        p("gouache","Gouache","Watercolor",Engine.PROCEDURAL), p("wash","Ink Wash","Watercolor",Engine.PROCEDURAL),
        p("smudge","Color Smudge","Blend",Engine.PROCEDURAL), p("blend-soft","Soft Blender","Blend",Engine.PROCEDURAL),
        p("blend-hard","Hard Blender","Blend",Engine.PROCEDURAL), p("mixing","Mixing Brush","Blend",Engine.PROCEDURAL),
        p("airbrush","Airbrush","Airbrush",Engine.PROCEDURAL), p("airbrush-hard","Hard Airbrush","Airbrush",Engine.PROCEDURAL),
        p("spray","Spray","Spray",Engine.PROCEDURAL), p("speckle","Speckle","Spray",Engine.PROCEDURAL),
        p("particles","Particles","Particles",Engine.PROCEDURAL), p("dust","Dust","Particles",Engine.PROCEDURAL),
        p("water","Water","Nature",Engine.PROCEDURAL), p("waves","Waves","Nature",Engine.PROCEDURAL),
        p("fire","Fire","Nature",Engine.PROCEDURAL), p("smoke","Smoke","Nature",Engine.PROCEDURAL),
        p("grass","Grass","Nature",Engine.PROCEDURAL), p("leaves","Leaves","Nature",Engine.PROCEDURAL),
        p("fur","Fur","Nature",Engine.PROCEDURAL), p("hair","Hair","Nature",Engine.PROCEDURAL),
        p("clouds","Clouds","Nature",Engine.PROCEDURAL), p("sand","Sand","Nature",Engine.PROCEDURAL),
        p("vine","Vine","Plants",Engine.PROCEDURAL), p("flower","Flower","Plants",Engine.PROCEDURAL),
        p("branch","Branch","Plants",Engine.PROCEDURAL), p("bamboo","Bamboo","Plants",Engine.PROCEDURAL),
        p("light","Light Glow","Effects",Engine.PROCEDURAL), p("bloom","Bloom","Effects",Engine.PROCEDURAL),
        p("stars","Stars","Effects",Engine.PROCEDURAL), p("spark","Spark","Effects",Engine.PROCEDURAL),
        p("glitter","Glitter","Effects",Engine.PROCEDURAL), p("neon","Neon","Effects",Engine.PROCEDURAL),
        p("chain","Chain","Effects",Engine.PROCEDURAL), p("ribbon","Ribbon","Effects",Engine.PROCEDURAL),
        p("texture","Texture","Texture",Engine.PROCEDURAL), p("paper","Paper Texture","Texture",Engine.PROCEDURAL),
        p("canvas","Canvas Texture","Texture",Engine.PROCEDURAL), p("noise","Noise Texture","Texture",Engine.PROCEDURAL),
        p("eraser","Eraser","Utility"), p("eraser-soft","Soft Eraser","Utility"), p("eraser-hard","Hard Eraser","Utility"),
        p("aotz-sketch","AOTz Sketch","Legacy Packs"), p("aotz-ink","AOTz Ink","Legacy Packs"),
        p("aotz-fill","AOTz Fill","Legacy Packs"), p("aotz-clouds","AOTz Clouds","Legacy Packs",Engine.PROCEDURAL),
        p("aotz-water","AOTz Water","Legacy Packs",Engine.PROCEDURAL), p("aotz-grass","AOTz Grass","Legacy Packs",Engine.PROCEDURAL),
        p("aotz-leaves","AOTz Leaves","Legacy Packs",Engine.PROCEDURAL), p("aotz-fur","AOTz Fur","Legacy Packs",Engine.PROCEDURAL),
        p("aotz-eraser","AOTz Eraser","Legacy Packs")
    )
    val families: List<String> = listOf(
        "Simple","Sketch","Ink","Comic","Marker","Pastel","Chalk","Dry Paint","Paint","Watercolor",
        "Blend","Airbrush","Spray","Particles","Nature","Plants","Effects","Texture","Utility","Legacy Packs"
    )
}
