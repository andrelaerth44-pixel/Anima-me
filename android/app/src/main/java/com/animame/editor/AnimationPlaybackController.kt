package com.animame.editor

import android.view.Choreographer

/** FPS-locked playback driven by display vsync and elapsed nanoseconds. */
class AnimationPlaybackController(
    private val document: AnimationDocument,
    private val onFrameChanged: (Int) -> Unit = {}
) : Choreographer.FrameCallback {
    private val choreographer = Choreographer.getInstance()
    private var running = false
    private var lastNanos = 0L
    private var accumulatorNanos = 0L

    val isPlaying: Boolean get() = running

    fun play() {
        if (running) return
        document.normalize()
        running = true
        lastNanos = System.nanoTime()
        accumulatorNanos = 0L
        choreographer.postFrameCallback(this)
    }

    fun pause() {
        running = false
        choreographer.removeFrameCallback(this)
        lastNanos = 0L
        accumulatorNanos = 0L
    }

    fun toggle() {
        if (running) pause() else play()
    }

    fun reset() {
        pause()
        document.currentFrame = document.playbackStart.coerceIn(0, document.duration - 1)
        onFrameChanged(document.currentFrame)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        val elapsed = if (lastNanos == 0L) 0L else (frameTimeNanos - lastNanos).coerceAtLeast(0L)
        lastNanos = frameTimeNanos
        accumulatorNanos += elapsed
        val frameDuration = 1_000_000_000L / document.fps.coerceIn(1, 240)
        var changed = false
        while (accumulatorNanos >= frameDuration) {
            accumulatorNanos -= frameDuration
            val next = if (document.currentFrame >= document.playbackEnd) {
                document.playbackStart
            } else {
                document.currentFrame + 1
            }
            document.currentFrame = next.coerceIn(0, document.duration - 1)
            changed = true
        }
        if (changed) onFrameChanged(document.currentFrame)
        choreographer.postFrameCallback(this)
    }

    fun onionFrames(): Pair<List<Pair<Int, DrawingFrame>>, List<Pair<Int, DrawingFrame>>> {
        val layer = document.activeLayer
        return layer.previousFrames(document.currentFrame, document.onion.previousCount) to
            layer.nextFrames(document.currentFrame, document.onion.nextCount)
    }

    fun release() = pause()
}
