package com.animame.editor

/** Single adapter for the legacy document camera fields and the new animatable camera track. */
class AnimationCameraController(private val document: AnimationDocument) {
    var interpolation: CameraInterpolation = CameraInterpolation.SMOOTH

    fun track(): AnimatableCameraTrack = AnimatableCameraTrack(
        document.cameraKeys.map { key ->
            AnimatableCameraKey(key.frame, key.camera.x, key.camera.y, key.camera.scale, key.camera.rotation)
        }, interpolation
    )

    fun setKey(frame: Int, camera: AnimationCamera) {
        val normalized = CameraKeyframe(frame.coerceAtLeast(0), camera.copy(scale = camera.scale.coerceAtLeast(0.0001f)))
        document.cameraKeys.removeAll { it.frame == normalized.frame }
        document.cameraKeys.add(normalized)
        document.cameraKeys.sortBy { it.frame }
        document.camera.x = normalized.camera.x
        document.camera.y = normalized.camera.y
        document.camera.scale = normalized.camera.scale
        document.camera.rotation = normalized.camera.rotation
    }

    fun removeKey(frame: Int) { document.cameraKeys.removeAll { it.frame == frame } }
    fun clear() { document.cameraKeys.clear() }
    fun evaluate(frame: Float): EvaluatedCamera = track().evaluate(frame)
}
