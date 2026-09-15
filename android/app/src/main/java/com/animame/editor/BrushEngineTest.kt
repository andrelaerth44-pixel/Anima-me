package com.animame.editor

/** Tiny compile-time smoke helper retained for local tests. */
object BrushEngineTest { fun run(): Boolean = BrushEngine.stamps(listOf(StrokeSample(0f,0f)), BrushSettings()).isNotEmpty() }
