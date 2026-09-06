package com.animame.editor

import android.view.MotionEvent

/** Stylus tilt in radians, normalized to the useful 0..PI/2 range for brush dynamics. */
val MotionEvent.tilt: Float
    get() = getAxisValue(MotionEvent.AXIS_TILT).coerceIn(0f, Math.PI.toFloat() / 2f)
