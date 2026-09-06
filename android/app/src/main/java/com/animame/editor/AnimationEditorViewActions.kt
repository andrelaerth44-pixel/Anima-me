package com.animame.editor

import android.net.Uri
import java.lang.reflect.Field

/** Small bridge for menu actions that are owned by MainActivity. */
fun AnimationEditorViewV5.currentFps(): Int = readDocument().fps
fun AnimationEditorViewV5.currentWidth(): Int = readDocument().width
fun AnimationEditorViewV5.currentHeight(): Int = readDocument().height

fun AnimationEditorViewV5.setDocumentFps(value: Int) {
    val d = readDocument()
    d.fps = value.coerceIn(1, 120)
    invalidate()
}

fun AnimationEditorViewV5.resizeDocument(newWidth: Int, newHeight: Int) {
    val d = readDocument()
    d.width = newWidth
    d.height = newHeight
    invalidate()
}

fun AnimationEditorViewV5.exportProjectPackage() {
    // Kept as a no-op bridge for the existing activity callback; project export
    // is intentionally started only from an explicit destination action.
}

private fun AnimationEditorViewV5.readDocument(): AnimationDocument {
    val field: Field = AnimationEditorViewV5::class.java.getDeclaredField("document")
    field.isAccessible = true
    return field.get(this) as AnimationDocument
}
