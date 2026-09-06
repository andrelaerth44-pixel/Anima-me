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
    invalidate()
}

/**
 * Resize the project while preserving the artwork. Vector strokes are scaled
 * in document space and raster frames are resized to the new canvas.
 */
fun AnimationEditorViewV5.resizeDocument(newWidth: Int, newHeight: Int) {
    val d = readDocument()
    val w = newWidth.coerceIn(16, 8192)
    val h = newHeight.coerceIn(16, 8192)
    val sx = w.toFloat() / d.width.coerceAtLeast(1)
    val sy = h.toFloat() / d.height.coerceAtLeast(1)

    d.layers.forEach { layer ->
        layer.frames.values.forEach { frame ->
            frame.strokes.forEach { stroke ->
                stroke.samples.replaceAll { sample ->
                    sample.copy(x = sample.x * sx, y = sample.y * sy)
                }
            }
            frame.rasterPath?.let { path ->
                resizeRaster(path, w, h)
            }
        }
    }
    d.width = w
    d.height = h
    d.normalize()
    invalidate()
}

/** Open the real project-export picker instead of leaving the old callback inert. */
fun AnimationEditorViewV5.exportProjectPackage() {
    (context as? MainActivity)?.exportProject()
}

/** Return to the project browser without terminating the Android activity. */
fun AnimationEditorViewV5.closeProjectToHome() {
    runCatching {
        val modeField = AnimationEditorViewV5::class.java.getDeclaredField("mode")
        modeField.isAccessible = true
        val modeType = modeField.type
        val home = java.lang.Enum.valueOf(modeType.asSubclass(Enum::class.java), "HOME")
        modeField.set(this, home)
        invalidate()
    }
}

private fun AnimationEditorViewV5.readDocument(): AnimationDocument {
    val field: Field = AnimationEditorViewV5::class.java.getDeclaredField("document")
    field.isAccessible = true
    return field.get(this) as AnimationDocument
}

private fun resizeRaster(path: String, width: Int, height: Int) {
    runCatching {
        val source = BitmapFactory.decodeFile(path) ?: return
        val scaled = Bitmap.createScaledBitmap(source, width, height, true)
        File(path).outputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (scaled !== source) scaled.recycle()
        source.recycle()
    }
}
