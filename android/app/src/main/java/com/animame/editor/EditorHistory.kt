package com.animame.editor

/**
 * Bounded document history for destructive/editor mutations.
 * Snapshots are deep copies, so undo never shares mutable frame or stroke lists.
 */
class EditorHistory(
    private val document: AnimationDocument,
    private val capacity: Int = 80
) {
    private val undoStack = ArrayDeque<AnimationDocumentSnapshot>()
    private val redoStack = ArrayDeque<AnimationDocumentSnapshot>()

    fun checkpoint() {
        undoStack.addLast(document.snapshot())
        while (undoStack.size > capacity) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(): Boolean {
        val previous = undoStack.removeLastOrNull() ?: return false
        redoStack.addLast(document.snapshot())
        document.restore(previous)
        return true
    }

    fun redo(): Boolean {
        val next = redoStack.removeLastOrNull() ?: return false
        undoStack.addLast(document.snapshot())
        document.restore(next)
        return true
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
}
