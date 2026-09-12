package com.animame.editor

/**
 * Undo/redo state for the animation document.
 *
 * Snapshots are deep copies so frame/layer edits and stroke mutations cannot
 * accidentally modify an older history entry.
 */
class DocumentHistory(private val maxEntries: Int = 80) {
    private val undoStack = ArrayDeque<AnimationDocumentSnapshot>()
    private val redoStack = ArrayDeque<AnimationDocumentSnapshot>()

    fun checkpoint(document: AnimationDocument) {
        undoStack.addLast(AnimationDocumentSnapshot.capture(document))
        while (undoStack.size > maxEntries.coerceAtLeast(1)) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(document: AnimationDocument): Boolean {
        if (undoStack.isEmpty()) return false
        val snapshot = undoStack.removeLast()
        redoStack.addLast(AnimationDocumentSnapshot.capture(document))
        snapshot.restoreInto(document)
        return true
    }

    fun redo(document: AnimationDocument): Boolean {
        if (redoStack.isEmpty()) return false
        val snapshot = redoStack.removeLast()
        undoStack.addLast(AnimationDocumentSnapshot.capture(document))
        snapshot.restoreInto(document)
        return true
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}

data class AnimationDocumentSnapshot(
    val name: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val duration: Int,
    val currentFrame: Int,
    val playbackStart: Int,
    val playbackEnd: Int,
    val backgroundColor: Int,
    val transparentBackground: Boolean,
    val layers: List<AnimationLayerSnapshot>,
    val cameraKeys: List<CameraKeyframe>,
    val onion: OnionSkinSettings,
    val camera: AnimationCamera,
    val selectedLayerId: String?
) {
    fun restoreInto(document: AnimationDocument) {
        document.name = name
        document.width = width
        document.height = height
        document.fps = fps
        document.duration = duration
        document.currentFrame = currentFrame
        document.playbackStart = playbackStart
        document.playbackEnd = playbackEnd
        document.backgroundColor = backgroundColor
        document.transparentBackground = transparentBackground
        document.layers.clear()
        document.layers.addAll(layers.map { it.toLayer() })
        document.cameraKeys.clear()
        document.cameraKeys.addAll(cameraKeys.map { it.copy(camera = it.camera.copy()) })
        document.onion.enabled = onion.enabled
        document.onion.previousCount = onion.previousCount
        document.onion.nextCount = onion.nextCount
        document.onion.opacity = onion.opacity
        document.onion.tintPrevious = onion.tintPrevious
        document.onion.tintNext = onion.tintNext
        document.camera.x = camera.x
        document.camera.y = camera.y
        document.camera.scale = camera.scale
        document.camera.rotation = camera.rotation
        document.selectedLayerId = selectedLayerId
        document.normalize()
    }

    companion object {
        fun capture(document: AnimationDocument): AnimationDocumentSnapshot =
            AnimationDocumentSnapshot(
                name = document.name,
                width = document.width,
                height = document.height,
                fps = document.fps,
                duration = document.duration,
                currentFrame = document.currentFrame,
                playbackStart = document.playbackStart,
                playbackEnd = document.playbackEnd,
                backgroundColor = document.backgroundColor,
                transparentBackground = document.transparentBackground,
                layers = document.layers.map { AnimationLayerSnapshot.capture(it) },
                cameraKeys = document.cameraKeys.map { it.copy(camera = it.camera.copy()) },
                onion = document.onion.copy(),
                camera = document.camera.copy(),
                selectedLayerId = document.selectedLayerId
            )
    }
}

data class AnimationLayerSnapshot(
    val id: String,
    val name: String,
    val visible: Boolean,
    val locked: Boolean,
    val opacity: Float,
    val blendMode: String,
    val isBackground: Boolean,
    val frames: Map<Int, DrawingFrameSnapshot>
) {
    fun toLayer(): AnimationLayer = AnimationLayer(
        id = id,
        name = name,
        visible = visible,
        locked = locked,
        opacity = opacity,
        blendMode = blendMode,
        isBackground = isBackground,
        frames = linkedMapOf<Int, DrawingFrame>().apply {
            this@AnimationLayerSnapshot.frames.forEach { (frame, snapshot) -> put(frame, snapshot.toFrame()) }
        }
    )

    companion object {
        fun capture(layer: AnimationLayer) = AnimationLayerSnapshot(
            id = layer.id,
            name = layer.name,
            visible = layer.visible,
            locked = layer.locked,
            opacity = layer.opacity,
            blendMode = layer.blendMode,
            isBackground = layer.isBackground,
            frames = layer.frames.mapValues { (_, frame) -> DrawingFrameSnapshot.capture(frame) }
        )
    }
}

data class DrawingFrameSnapshot(
    val id: String,
    val exposure: Int,
    val strokes: List<StrokeSnapshot>
) {
    fun toFrame(): DrawingFrame = DrawingFrame(
        id = id,
        exposure = exposure,
        strokes = strokes.map { it.toStroke() }.toMutableList()
    )

    companion object {
        fun capture(frame: DrawingFrame) = DrawingFrameSnapshot(
            id = frame.id,
            exposure = frame.exposure,
            strokes = frame.strokes.map { StrokeSnapshot.capture(it) }
        )
    }
}

data class StrokeSnapshot(
    val id: String,
    val brushId: String,
    val color: Int,
    val size: Float,
    val opacity: Float,
    val settings: BrushSettings,
    val samples: List<StrokeSample>
) {
    fun toStroke(): StrokeData = StrokeData(
        id = id,
        brushId = brushId,
        color = color,
        size = size,
        opacity = opacity,
        settings = settings.copy(),
        samples = samples.map { it.copy() }.toMutableList()
    )

    companion object {
        fun capture(stroke: StrokeData) = StrokeSnapshot(
            id = stroke.id,
            brushId = stroke.brushId,
            color = stroke.color,
            size = stroke.size,
            opacity = stroke.opacity,
            settings = stroke.settings.copy(),
            samples = stroke.samples.map { it.copy() }
        )
    }
}
