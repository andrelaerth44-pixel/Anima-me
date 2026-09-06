package com.animame.editor

data class BrushPreset(
    val id: String,
    val name: String,
    val family: String,
    val engine: Engine,
    val assetFolder: String? = null
)

enum class Engine { TOONZ_RASTER, FULL_COLOR_MYPAINT, VECTOR, PROCEDURAL }

object BrushCatalog {
    val presets: List<BrushPreset> = listOf(
        BrushPreset("basic", "Basic Round", "Core", Engine.FULL_COLOR_MYPAINT),
        BrushPreset("pencil", "Pencil", "Core", Engine.TOONZ_RASTER),
        BrushPreset("ink", "Ink", "Core", Engine.TOONZ_RASTER),
        BrushPreset("paint", "Paint", "Core", Engine.FULL_COLOR_MYPAINT),
        BrushPreset("eraser", "Eraser", "Core", Engine.FULL_COLOR_MYPAINT),
        BrushPreset("water", "Water", "Effects", Engine.PROCEDURAL),
        BrushPreset("fire", "Fire", "Effects", Engine.PROCEDURAL),
        BrushPreset("light", "Light", "Effects", Engine.PROCEDURAL),
        BrushPreset("stars", "Stars", "Effects", Engine.PROCEDURAL),
        BrushPreset("chain", "Chain", "Effects", Engine.PROCEDURAL),
        BrushPreset("aotz-sketch", "AOTz Sketch", "AOTz", Engine.FULL_COLOR_MYPAINT, "aotz"),
        BrushPreset("aotz-ink", "AOTz Ink", "AOTz", Engine.FULL_COLOR_MYPAINT, "aotz"),
        BrushPreset("aotz-fill", "AOTz Fill", "AOTz", Engine.FULL_COLOR_MYPAINT, "aotz"),
        BrushPreset("aotz-clouds", "AOTz Clouds", "AOTz", Engine.PROCEDURAL, "aotz"),
        BrushPreset("aotz-water", "AOTz Water", "AOTz", Engine.PROCEDURAL, "aotz"),
        BrushPreset("aotz-grass", "AOTz Grass", "AOTz", Engine.PROCEDURAL, "aotz"),
        BrushPreset("aotz-leaves", "AOTz Leaves", "AOTz", Engine.PROCEDURAL, "aotz"),
        BrushPreset("aotz-fur", "AOTz Fur", "AOTz", Engine.PROCEDURAL, "aotz"),
        BrushPreset("aotz-eraser", "AOTz Eraser", "AOTz", Engine.FULL_COLOR_MYPAINT, "aotz"),
        BrushPreset("classic", "Classic", "Classic", Engine.FULL_COLOR_MYPAINT, "classic"),
        BrushPreset("experimental", "Experimental", "Experimental", Engine.FULL_COLOR_MYPAINT, "experimental"),
        BrushPreset("deevad", "Deevad", "Deevad", Engine.FULL_COLOR_MYPAINT, "deevad"),
        BrushPreset("ramon", "Ramon", "Ramon", Engine.FULL_COLOR_MYPAINT, "ramon"),
        BrushPreset("tanda", "Tanda", "Tanda", Engine.FULL_COLOR_MYPAINT, "tanda"),
        BrushPreset("kaerhon", "Kaerhon v1", "Kaerhon", Engine.FULL_COLOR_MYPAINT, "kaerhon_v1"),
        BrushPreset("mojo", "Mojo v1", "Mojo", Engine.FULL_COLOR_MYPAINT, "mojo_v1"),
        BrushPreset("slos", "SLOS MPB", "SLOS", Engine.FULL_COLOR_MYPAINT, "slos_mpb")
    )

    val families: List<String> = listOf("Core", "Effects", "AOTz", "Classic", "Experimental", "Deevad", "Ramon", "Tanda", "Kaerhon", "Mojo", "SLOS")
}