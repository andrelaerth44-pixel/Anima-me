package com.animame.editor

/** Persistent editor preferences. These settings control interaction and UI,
 * not animation-camera transforms. */
data class EditorSettings(
    var allowPinchZoom: Boolean = true,
    var allowPinchRotation: Boolean = true,
    var snapViewRotation: Boolean = false,
    var snapViewZoom: Boolean = false,
    var interpolationOnZoom: Boolean = true,
    var penOnlyDrawing: Boolean = false,
    var showPenHoverCursor: Boolean = true,
    var interfaceScale: Int = 100,
    var leftHandedInterface: Boolean = false,
    var onionSkinOpacity: Int = 50,
    var onionPreviousTint: Boolean = true,
    var onionFollowingTint: Boolean = true
) {
    fun normalize() {
        interfaceScale = interfaceScale.coerceIn(75, 150)
        onionSkinOpacity = onionSkinOpacity.coerceIn(0, 100)
    }
}

object EditorSettingsStore {
    val current = EditorSettings()

    fun snapZoom(value: Float): Float {
        if (!current.snapViewZoom) return value
        val steps = floatArrayOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f, 4f, 8f)
        return steps.minByOrNull { kotlin.math.abs(it - value) } ?: value
    }

    fun snapRotation(value: Float): Float {
        if (!current.snapViewRotation) return value
        return kotlin.math.round(value / 15f) * 15f
    }
}
