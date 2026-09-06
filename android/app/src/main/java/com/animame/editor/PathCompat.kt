package com.animame.editor

import android.graphics.Path
import android.graphics.RectF

val Path.bounds: RectF
    get() = RectF().also { computeBounds(it, true) }
