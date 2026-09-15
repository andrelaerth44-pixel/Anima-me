package com.animame.editor

import org.json.JSONArray
import org.json.JSONObject

/**
 * Stable, dependency-free project codec used by the editor autosave/export layer.
 * The codec intentionally keeps the document model separate from Android UI state.
 */
object AnimationDocumentJson {
    private const val VERSION = 1

    fun encode(document: AnimationDocument): String = JSONObject().apply {
        put("version", VERSION)
        put("name", document.name)
        put("width", document.width)
        put("height", document.height)
        put("fps", document.fps)
        put("duration", document.duration)
        put("currentFrame", document.currentFrame)
        put("playbackStart", document.playbackStart)
        put("playbackEnd", document.playbackEnd)
        put("backgroundColor", document.backgroundColor)
        put("transparentBackground", document.transparentBackground)
        put("selectedLayerId", document.selectedLayerId)
        put("camera", JSONObject().apply {
            put("x", document.camera.x)
            put("y", document.camera.y)
            put("scale", document.camera.scale)
            put("rotation", document.camera.rotation)
        })
        put("onion", JSONObject().apply {
            put("enabled", document.onion.enabled)
            put("previousCount", document.onion.previousCount)
            put("nextCount", document.onion.nextCount)
            put("opacity", document.onion.opacity)
            put("tintPrevious", document.onion.tintPrevious)
            put("tintNext", document.onion.tintNext)
        })
        put("cameraKeys", JSONArray().apply {
            document.cameraKeys.forEach { key ->
                put(JSONObject().apply {
                    put("frame", key.frame)
                    put("x", key.camera.x)
                    put("y", key.camera.y)
                    put("scale", key.camera.scale)
                    put("rotation", key.camera.rotation)
                })
            }
        })
        put("layers", JSONArray().apply {
            document.layers.forEach { layer ->
                put(JSONObject().apply {
                    put("id", layer.id)
                    put("name", layer.name)
                    put("visible", layer.visible)
                    put("locked", layer.locked)
                    put("opacity", layer.opacity)
                    put("blendMode", layer.blendMode)
                    put("isBackground", layer.isBackground)
                    put("frames", JSONArray().apply {
                        layer.frames.forEach { (index, frame) ->
                            put(JSONObject().apply {
                                put("index", index)
                                put("id", frame.id)
                                put("exposure", frame.exposure)
                                put("strokes", JSONArray().apply {
                                    frame.strokes.forEach { stroke ->
                                        put(JSONObject().apply {
                                            put("id", stroke.id)
                                            put("brushId", stroke.brushId)
                                            put("color", stroke.color)
                                            put("size", stroke.size)
                                            put("opacity", stroke.opacity)
                                            put("samples", JSONArray().apply {
                                                stroke.samples.forEach { sample ->
                                                    put(JSONObject().apply {
                                                        put("x", sample.x)
                                                        put("y", sample.y)
                                                        put("pressure", sample.pressure)
                                                        put("timeMs", sample.timeMs)
                                                        put("tilt", sample.tilt)
                                                        put("orientation", sample.orientation)
                                                    })
                                                }
                                            })
                                        })
                                    }
                                })
                            })
                        }
                    })
                })
            }
        })
    }.toString()

    fun decode(raw: String): AnimationDocument {
        val root = JSONObject(raw)
        require(root.optInt("version", VERSION) in 1..VERSION) { "Versão de projeto não suportada" }

        val document = AnimationDocument(
            name = root.optString("name", "Untitled"),
            width = root.optInt("width", 1280),
            height = root.optInt("height", 720),
            fps = root.optInt("fps", 24),
            duration = root.optInt("duration", 1).coerceAtLeast(1),
            currentFrame = root.optInt("currentFrame", 0),
            playbackStart = root.optInt("playbackStart", 0),
            playbackEnd = root.optInt("playbackEnd", 0),
            backgroundColor = root.optInt("backgroundColor", 0xFFFFFFFF.toInt()),
            transparentBackground = root.optBoolean("transparentBackground", false),
            selectedLayerId = root.optString("selectedLayerId").ifBlank { null }
        )

        document.layers.clear()
        val layers = root.optJSONArray("layers") ?: JSONArray()
        for (i in 0 until layers.length()) {
            val item = layers.getJSONObject(i)
            val layer = AnimationLayer(
                id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                name = item.optString("name", "Layer"),
                visible = item.optBoolean("visible", true),
                locked = item.optBoolean("locked", false),
                opacity = item.optDouble("opacity", 1.0).toFloat(),
                blendMode = item.optString("blendMode", "NORMAL"),
                isBackground = item.optBoolean("isBackground", false)
            )
            val frames = item.optJSONArray("frames") ?: JSONArray()
            for (j in 0 until frames.length()) {
                val frameJson = frames.getJSONObject(j)
                val frameIndex = frameJson.optInt("index", 0)
                val frame = DrawingFrame(
                    id = frameJson.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                    exposure = frameJson.optInt("exposure", 1).coerceIn(1, 120)
                )
                val strokes = frameJson.optJSONArray("strokes") ?: JSONArray()
                for (k in 0 until strokes.length()) {
                    val strokeJson = strokes.getJSONObject(k)
                    val brushId = strokeJson.optString("brushId", "basic")
                    val size = strokeJson.optDouble("size", 12.0).toFloat()
                    val opacity = strokeJson.optDouble("opacity", 1.0).toFloat()
                    val samples = mutableListOf<StrokeSample>()
                    val sampleArray = strokeJson.optJSONArray("samples") ?: JSONArray()
                    for (s in 0 until sampleArray.length()) {
                        val p = sampleArray.getJSONObject(s)
                        samples += StrokeSample(
                            x = p.optDouble("x", 0.0).toFloat(),
                            y = p.optDouble("y", 0.0).toFloat(),
                            pressure = p.optDouble("pressure", 1.0).toFloat(),
                            timeMs = p.optLong("timeMs", 0L),
                            tilt = p.optDouble("tilt", 0.0).toFloat(),
                            orientation = p.optDouble("orientation", 0.0).toFloat()
                        )
                    }
                    frame.strokes += StrokeData(
                        id = strokeJson.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
                        brushId = brushId,
                        color = strokeJson.optInt("color", 0xFF000000.toInt()),
                        size = size,
                        opacity = opacity,
                        settings = BrushDefaults.forPreset(brushId).copy(size = size, opacity = opacity).normalized(),
                        samples = samples
                    )
                }
                layer.frames[frameIndex] = frame
            }
            document.layers += layer
        }

        val camera = root.optJSONObject("camera")
        if (camera != null) {
            document.camera.x = camera.optDouble("x", 0.0).toFloat()
            document.camera.y = camera.optDouble("y", 0.0).toFloat()
            document.camera.scale = camera.optDouble("scale", 1.0).toFloat()
            document.camera.rotation = camera.optDouble("rotation", 0.0).toFloat()
        }

        val onion = root.optJSONObject("onion")
        if (onion != null) {
            document.onion.enabled = onion.optBoolean("enabled", true)
            document.onion.previousCount = onion.optInt("previousCount", 2)
            document.onion.nextCount = onion.optInt("nextCount", 2)
            document.onion.opacity = onion.optInt("opacity", 50)
            document.onion.tintPrevious = onion.optBoolean("tintPrevious", true)
            document.onion.tintNext = onion.optBoolean("tintNext", true)
        }

        document.cameraKeys.clear()
        val keys = root.optJSONArray("cameraKeys") ?: JSONArray()
        for (i in 0 until keys.length()) {
            val item = keys.getJSONObject(i)
            document.cameraKeys += CameraKeyframe(
                item.optInt("frame", 0),
                AnimationCamera(
                    item.optDouble("x", 0.0).toFloat(),
                    item.optDouble("y", 0.0).toFloat(),
                    item.optDouble("scale", 1.0).toFloat(),
                    item.optDouble("rotation", 0.0).toFloat()
                )
            )
        }

        document.normalize()
        return document
    }
}
