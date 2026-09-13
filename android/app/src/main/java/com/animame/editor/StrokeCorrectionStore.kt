package com.animame.editor

/** Runtime defaults used by the editor when a brush has no custom correction values. */
object StrokeCorrectionStore {
    @Volatile var constant: Float = 0f
    @Volatile var fastStrokes: Float = 0f
    @Volatile var smoothing: Float = 18f
    @Volatile var prediction: Float = 0f
    @Volatile var useLegacy: Boolean = false
    @Volatile var forceFade: Boolean = false
    @Volatile var fadeStart: Float = 0f
    @Volatile var fadeEnd: Float = 1f

    fun settings(): StrokeStabilizer.Settings = StrokeStabilizer.Settings(
        constant = constant,
        fastStrokes = fastStrokes,
        smoothing = smoothing,
        prediction = prediction,
        mode = if (constant > 0f || fastStrokes > 0f) StrokeStabilizer.Mode.REAL_TIME else StrokeStabilizer.Mode.AFTER,
        legacy = useLegacy,
        forceFade = forceFade,
        fadeStart = fadeStart,
        fadeEnd = fadeEnd
    )
}
