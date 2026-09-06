package com.animame.editor

import kotlin.math.hypot

fun strokeLength(samples:List<StrokeSample>):Float{var total=0f;for(i in 1 until samples.size)total+=hypot(samples[i].x-samples[i-1].x,samples[i].y-samples[i-1].y);return total}
