package com.animame.editor

/** Composition camera animation. It is deliberately independent from the editor viewport. */
data class AnimatableCameraKey(
    val frame: Int,
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f
)

data class EvaluatedCamera(
    val x: Float = 0f,
    val y: Float = 0f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val opacity: Float = 1f
)

enum class CameraInterpolation { STEP, LINEAR, SMOOTH }

/**
 * Timeline camera track used for rendered composition movement.
 * Viewport zoom/pan/rotation must never be stored here: those are editor-only
 * gestures and are intentionally independent of camera keyframes.
 */
class AnimatableCameraTrack(
    keyframes: List<AnimatableCameraKey> = emptyList(),
    var interpolation: CameraInterpolation = CameraInterpolation.SMOOTH
) {
    private val frames = keyframes.sortedBy { it.frame }.toMutableList()

    fun all(): List<AnimatableCameraKey> = frames.toList()

    fun keyAt(frame: Int): AnimatableCameraKey? = frames.firstOrNull { it.frame == frame }

    fun set(k: AnimatableCameraKey) {
        val normalized = k.copy(
            frame = k.frame.coerceAtLeast(0),
            scale = k.scale.coerceAtLeast(0.0001f),
            opacity = k.opacity.coerceIn(0f, 1f)
        )
        val i = frames.indexOfFirst { it.frame == normalized.frame }
        if (i >= 0) frames[i] = normalized else frames.add(normalized)
        frames.sortBy { it.frame }
    }

    fun remove(frame: Int) { frames.removeAll { it.frame == frame } }

    fun clear() { frames.clear() }

    fun evaluate(frame: Float): EvaluatedCamera {
        if (frames.isEmpty()) return EvaluatedCamera()
        if (frame <= frames.first().frame) return frames.first().eval()
        if (frame >= frames.last().frame) return frames.last().eval()

        val hi = frames.indexOfFirst { it.frame >= frame }
        if (hi <= 0) return frames.first().eval()
        val a = frames[hi - 1]
        val b = frames[hi]
        val span = (b.frame - a.frame).coerceAtLeast(1)
        val raw = ((frame - a.frame) / span.toFloat()).coerceIn(0f, 1f)
        val t = when (interpolation) {
            CameraInterpolation.STEP -> 0f
            CameraInterpolation.LINEAR -> raw
            CameraInterpolation.SMOOTH -> raw * raw * (3f - 2f * raw)
        }
        return EvaluatedCamera(
            x = lerp(a.x, b.x, t),
            y = lerp(a.y, b.y, t),
            scale = lerp(a.scale, b.scale, t).coerceAtLeast(0.0001f),
            rotationDeg = lerpAngle(a.rotationDeg, b.rotationDeg, t),
            opacity = lerp(a.opacity, b.opacity, t).coerceIn(0f, 1f)
        )
    }

    private fun AnimatableCameraKey.eval() = EvaluatedCamera(x, y, scale, rotationDeg, opacity)
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var d = (b - a) % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return a + d * t
    }
}
