package com.animame.editor

/** Small package-local math helpers used by the procedural brush engine. */
private const val TWO_PI = 6.283185307179586f

fun sin(value: Float): Float = kotlin.math.sin(value.toDouble()).toFloat()
fun sqrt(value: Float): Float = kotlin.math.sqrt(value.coerceAtLeast(0f).toDouble()).toFloat()
fun max(a: Float, b: Float): Float = kotlin.math.max(a, b)
fun max(a: Int, b: Int): Int = kotlin.math.max(a, b)
