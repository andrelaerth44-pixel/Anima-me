package com.animame.editor

data class BrushPreset(val id:String,val name:String,val family:String,val engine:Engine=Engine.PROCEDURAL,val assetFolder:String?=null,val defaults:BrushSettings=BrushDefaults.forPreset(id))
enum class Engine { PROCEDURAL }

/** Original Anima-me brush library plus persistent user-imported presets. */
object BrushCatalog {
 private fun p(id:String,name:String,family:String)=BrushPreset(id,name,family)
 private val builtIns= listOf(
  p("basic","Basic Round","Simple"),p("basic-soft","Basic Soft","Simple"),p("basic-hard","Basic Hard","Simple"),p("basic-flat","Basic Flat","Simple"),p("basic-pressure","Pressure Round","Simple"),p("basic-opacity","Opacity Round","Simple"),
  p("pencil","Pencil","Sketch"),p("pencil-soft","Soft Pencil","Sketch"),p("pencil-mechanical","Mechanical Pencil","Sketch"),p("graphite","Graphite","Sketch"),p("sketch-light","Light Sketch","Sketch"),p("charcoal-sketch","Charcoal Sketch","Sketch"),
  p("ink","Ink","Ink"),p("ink-fine","Fine Ink","Ink"),p("ink-gpen","G-Pen","Ink"),p("ink-mapping","Mapping Pen","Ink"),p("ink-brush","Ink Brush","Ink"),p("ink-dry","Dry Ink","Ink"),
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
 private val imported=mutableListOf<BrushPreset>()
 val presets:List<BrushPreset> get()=builtIns+imported
 val families=listOf("Simple","Sketch","Ink","Comic","Marker","Pastel","Chalk","Dry Paint","Paint","Watercolor","Blend","Airbrush","Spray","Particles","Nature","Plants","Effects","Texture","Utility","Imported")
 fun replaceImported(list:List<BrushPreset>){ imported.clear(); imported.addAll(list) }
}
