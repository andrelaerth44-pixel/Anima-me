package com.animame.editor

import android.graphics.PointF
import java.util.UUID

data class StrokeSample(val x: Float, val y: Float, val pressure: Float = 1f, val timeMs: Long = 0L)

data class StrokeData(
    val id: String = UUID.randomUUID().toString(),
    val brushId: String = "basic",
    val color: Int = 0xFF000000.toInt(),
    val size: Float = 12f,
    val opacity: Float = 1f,
    val settings: BrushSettings = BrushDefaults.forPreset(brushId).copy(size = size, opacity = opacity).normalized(),
    val samples: MutableList<StrokeSample> = mutableListOf()
) {
    fun resolvedBrushSettings(): BrushSettings = settings.copy(size = size, opacity = opacity).normalized()
}

data class DrawingFrame(val id: String = UUID.randomUUID().toString(), val strokes: MutableList<StrokeData> = mutableListOf(), var exposure: Int = 1)

data class AnimationLayer(
    val id: String = UUID.randomUUID().toString(),
    var name: String = "Layer",
    var visible: Boolean = true,
    var locked: Boolean = false,
    var opacity: Float = 1f,
    var blendMode: String = "NORMAL",
    var isBackground: Boolean = false,
    val frames: MutableMap<Int, DrawingFrame> = linkedMapOf()
) {
    fun frameAt(frame: Int): DrawingFrame? = frames[frame]
    fun ensureFrame(frame: Int): DrawingFrame = frames.getOrPut(frame) { DrawingFrame() }
    fun previousFrames(frame: Int, count: Int): List<Pair<Int, DrawingFrame>> =
        frames.keys.filter { it < frame }.sortedDescending().take(count).mapNotNull { f -> frames[f]?.let { f to it } }
    fun nextFrames(frame: Int, count: Int): List<Pair<Int, DrawingFrame>> =
        frames.keys.filter { it > frame }.sorted().take(count).mapNotNull { f -> frames[f]?.let { f to it } }
}

data class OnionSkinSettings(var enabled: Boolean = true, var previousCount: Int = 2, var nextCount: Int = 2, var opacity: Int = 50, var tintPrevious: Boolean = true, var tintNext: Boolean = true)
data class AnimationCamera(var x: Float = 0f, var y: Float = 0f, var scale: Float = 1f, var rotation: Float = 0f)
data class CameraKeyframe(val frame: Int, val camera: AnimationCamera)
data class TimelineSelection(var layerId: String? = null, var startFrame: Int = 0, var endFrame: Int = 0)

data class AnimationDocument(
    var name: String = "Untitled",
    var width: Int = 1280,
    var height: Int = 720,
    var fps: Int = 24,
    var duration: Int = 1,
    var currentFrame: Int = 0,
    var playbackStart: Int = 0,
    var playbackEnd: Int = 0,
    var backgroundColor: Int = 0xFFFFFFFF.toInt(),
    var transparentBackground: Boolean = false,
    val layers: MutableList<AnimationLayer> = mutableListOf(),
    val cameraKeys: MutableList<CameraKeyframe> = mutableListOf(),
    val onion: OnionSkinSettings = OnionSkinSettings(),
    val camera: AnimationCamera = AnimationCamera(),
    var selectedLayerId: String? = null
) {
    init {
        playbackEnd = duration.coerceAtLeast(1) - 1
        if (layers.isEmpty()) {
            val background = AnimationLayer(name = "Background", locked = true, isBackground = true)
            val layer = AnimationLayer(name = "Layer 1")
            layers += layer
            layers += background
        }
        selectedLayerId = selectedLayerId ?: layers.firstOrNull { !it.isBackground }?.id ?: layers.firstOrNull()?.id
    }

    val activeLayer: AnimationLayer
        get() = layers.firstOrNull { it.id == selectedLayerId && !it.isBackground }
            ?: layers.firstOrNull { !it.isBackground }?.also { selectedLayerId = it.id }
            ?: AnimationLayer(name = "Layer 1").also { layers.add(0, it); selectedLayerId = it.id }

    fun selectLayer(layerId: String): Boolean {
        if (layers.none { it.id == layerId && !it.isBackground }) return false
        selectedLayerId = layerId
        return true
    }

    fun normalize() {
        fps = fps.coerceIn(1, 240)
        width = width.coerceIn(1, 16384)
        height = height.coerceIn(1, 16384)
        duration = duration.coerceAtLeast(1)
        currentFrame = currentFrame.coerceIn(0, duration - 1)
        playbackStart = playbackStart.coerceIn(0, duration - 1)
        playbackEnd = playbackEnd.coerceIn(playbackStart, duration - 1)
        onion.previousCount = onion.previousCount.coerceIn(0, 12)
        onion.nextCount = onion.nextCount.coerceIn(0, 12)
        onion.opacity = onion.opacity.coerceIn(0, 100)
        if (layers.none { it.id == selectedLayerId && !it.isBackground }) selectedLayerId = layers.firstOrNull { !it.isBackground }?.id
        if (layers.none { it.isBackground }) layers.add(AnimationLayer(name = "Background", locked = true, isBackground = true))
    }

    fun exposureAt(frame: Int, layerId: String? = selectedLayerId): Int =
        layers.firstOrNull { it.id == layerId }?.frameAt(frame)?.exposure?.coerceAtLeast(1) ?: 1

    fun setExposure(frame: Int, exposure: Int, layerId: String? = selectedLayerId) {
        val layer = layers.firstOrNull { it.id == layerId } ?: return
        layer.ensureFrame(frame).exposure = exposure.coerceIn(1, 120)
    }

    fun setPlaybackRange(start: Int, end: Int) {
        val a = start.coerceIn(0, duration - 1)
        val b = end.coerceIn(0, duration - 1)
        playbackStart = minOf(a, b)
        playbackEnd = maxOf(a, b)
        currentFrame = currentFrame.coerceIn(playbackStart, playbackEnd)
    }

    fun advancePlaybackFrame(): Int {
        if (duration <= 1) return currentFrame
        currentFrame = if (currentFrame >= playbackEnd) playbackStart else currentFrame + 1
        return currentFrame
    }

    fun insertFrame(at: Int) {
        val index = at.coerceIn(0, duration)
        layers.forEach { layer ->
            val shifted = layer.frames.entries.sortedByDescending { it.key }.associate { (f, drawing) -> if (f >= index) f + 1 to drawing else f to drawing }
            layer.frames.clear()
            layer.frames.putAll(shifted)
        }
        duration += 1
        playbackEnd = duration - 1
        currentFrame = index.coerceAtMost(duration - 1)
    }

    fun duplicateFrame(frame: Int) {
        layers.forEach { layer ->
            if (layer.isBackground) return@forEach
            val src = layer.frameAt(frame) ?: return@forEach
            val copy = DrawingFrame(exposure = src.exposure)
            src.strokes.forEach { s -> copy.strokes += s.copy(samples = s.samples.map { it.copy() }.toMutableList()) }
            layer.frames[frame + 1] = copy
        }
        duration = maxOf(duration, frame + 2)
        playbackEnd = duration - 1
    }

    fun deleteFrame(frame: Int) {
        if (duration <= 1) return
        layers.forEach { layer ->
            layer.frames.remove(frame)
            val shifted = layer.frames.entries.sortedBy { it.key }.map { (f, d) -> if (f > frame) f - 1 to d else f to d }
            layer.frames.clear()
            layer.frames.putAll(shifted)
        }
        duration -= 1
        playbackEnd = duration - 1
        currentFrame = currentFrame.coerceIn(0, duration - 1)
        normalize()
    }

    fun addLayer(name: String = "Layer ${layers.count { !it.isBackground } + 1}"): AnimationLayer =
        AnimationLayer(name = name).also {
            val backgroundIndex = layers.indexOfFirst { it.isBackground }
            if (backgroundIndex >= 0) layers.add(backgroundIndex, it) else layers.add(it)
            selectedLayerId = it.id
        }

    fun deleteLayer(layerId: String) {
        val target = layers.firstOrNull { it.id == layerId } ?: return
        if (target.isBackground || layers.count { !it.isBackground } <= 1) return
        val wasSelected = selectedLayerId == layerId
        layers.removeAll { it.id == layerId }
        if (wasSelected) selectedLayerId = layers.firstOrNull { !it.isBackground }?.id
    }

    fun moveLayer(layerId: String, delta: Int) {
        val i = layers.indexOfFirst { it.id == layerId }
        if (i < 0 || layers[i].isBackground) return
        val backgroundIndex = layers.indexOfFirst { it.isBackground }
        val ni = (i + delta).coerceIn(0, maxOf(0, backgroundIndex - 1))
        if (i != ni) layers.add(ni, layers.removeAt(i))
    }
}

fun StrokeData.points(): List<PointF> = samples.map { PointF(it.x, it.y) }
