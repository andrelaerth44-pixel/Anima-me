package com.animame.editor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.lang.reflect.Field

/** Activity-facing actions kept outside the large editor surface. */
fun AnimationEditorViewV5.currentFps(): Int = readDocument().fps
fun AnimationEditorViewV5.currentWidth(): Int = readDocument().width
fun AnimationEditorViewV5.currentHeight(): Int = readDocument().height

fun AnimationEditorViewV5.setDocumentFps(value: Int) {
    val d = readDocument()
    d.fps = value.coerceIn(1, 120)
    d.normalize()
    syncProjectMetadata(d)
    invalidate()
}

/** Resize the project while preserving vector and raster artwork. */
fun AnimationEditorViewV5.resizeDocument(newWidth: Int, newHeight: Int) {
    val d = readDocument()
    val w = newWidth.coerceIn(16, 8192)
    val h = newHeight.coerceIn(16, 8192)
    val sx = w.toFloat() / d.width.coerceAtLeast(1)
    val sy = h.toFloat() / d.height.coerceAtLeast(1)
    d.layers.forEach { layer ->
        layer.frames.values.forEach { frame ->
            frame.strokes.forEach { stroke ->
                stroke.samples.replaceAll { sample -> sample.copy(x = sample.x * sx, y = sample.y * sy) }
            }
            frame.rasterPath?.let { resizeRaster(it, w, h) }
        }
    }
    d.width = w
    d.height = h
    d.normalize()
    syncProjectMetadata(d)
    invalidate()
}

/** Open a document already belonging to the selected project. */
fun AnimationEditorViewV5.openDocument(imported: AnimationDocument, existingProject: AnimationProject? = null) {
    val d = imported.apply { normalize() }
    val p = existingProject ?: AnimationProject(
        name = d.name.ifBlank { "Imported project" },
        fps = d.fps,
        cameraWidth = d.width,
        cameraHeight = d.height,
        canvasWidth = d.width,
        canvasHeight = d.height,
        lastFrame = d.currentFrame
    )
    if (ProjectStore.projects.none { it.id == p.id }) ProjectStore.projects.add(0, p)
    ProjectStore.touch(p)
    ProjectDocumentStore.save(context, p.id, d)
    setPrivate("document", d)
    setPrivate("project", p)
    setPrivate("projectName", p.name)
    setPrivate("fps", d.fps)
    setPrivate("cw", d.width)
    setPrivate("ch", d.height)
    setPrivate("selectedLayerId", d.activeLayer.id)
    setPrivate("cameraX", d.camera.x)
    setPrivate("cameraY", d.camera.y)
    setPrivate("cameraScale", d.camera.scale)
    setPrivate("cameraRotation", d.camera.rotation)
    setEnum("mode", "EDITOR")
    invalidate()
}

/** Current document used by explicit project export. */
fun AnimationEditorViewV5.documentForExport(): AnimationDocument = readDocument()

/** Attach audio to the currently open document and persist it. */
fun AnimationEditorViewV5.attachAudio(path: String) {
    val d = readDocument()
    d.audioPath = path
    d.audioStartFrame = d.currentFrame
    d.normalize()
    syncProjectMetadata(d)
    invalidate()
}

fun AnimationEditorViewV5.exportProjectPackage() {
    (context as? MainActivity)?.exportProject()
}

fun AnimationEditorViewV5.closeProjectToHome() {
    setEnum("mode", "HOME")
    invalidate()
}

/** Hide the in-editor hamburger menu before Activity-level routing handles an action. */
fun AnimationEditorViewV5.dismissMenu() {
    runCatching { setPrivate("menu", false) }
    invalidate()
}

private fun AnimationEditorViewV5.readDocument(): AnimationDocument {
    val field: Field = AnimationEditorViewV5::class.java.getDeclaredField("document")
    field.isAccessible = true
    return field.get(this) as AnimationDocument
}

private fun AnimationEditorViewV5.setPrivate(name: String, value: Any?) {
    val field = AnimationEditorViewV5::class.java.getDeclaredField(name)
    field.isAccessible = true
    field.set(this, value)
}

private fun AnimationEditorViewV5.setEnum(name: String, constant: String) {
    val field = AnimationEditorViewV5::class.java.getDeclaredField(name)
    field.isAccessible = true
    val value = field.type.enumConstants.firstOrNull { (it as Enum<*>).name == constant }
        ?: error("Unknown ${field.type.name} constant: $constant")
    field.set(this, value)
}

private fun AnimationEditorViewV5.syncProjectMetadata(d: AnimationDocument) {
    runCatching {
        val field = AnimationEditorViewV5::class.java.getDeclaredField("project")
        field.isAccessible = true
        val p = field.get(this) as? AnimationProject ?: return
        p.name = d.name
        p.fps = d.fps
        p.cameraWidth = d.width
        p.cameraHeight = d.height
        p.canvasWidth = d.width
        p.canvasHeight = d.height
        p.lastFrame = d.currentFrame
        ProjectStore.update(p)
        ProjectDocumentStore.save(context, p.id, d)
    }
}

private fun resizeRaster(path: String, width: Int, height: Int) {
    runCatching {
        val source = BitmapFactory.decodeFile(path) ?: return
        val scaled = Bitmap.createScaledBitmap(source, width, height, true)
        File(path).outputStream().use { out -> scaled.compress(Bitmap.CompressFormat.PNG, 100, out) }
        if (scaled !== source) scaled.recycle()
        source.recycle()
    }
}
