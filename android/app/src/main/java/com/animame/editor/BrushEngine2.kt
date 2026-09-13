package com.animame.editor

import kotlin.math.*
import kotlin.random.Random

object BrushEngine2 {
    fun stampCount(samples: List<StrokeSample>, settings: BrushSettings): Int = BrushEngine.stamps(samples, settings).size
}
