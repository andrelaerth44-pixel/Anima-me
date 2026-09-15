package com.animame.editor

/**
 * Document-level undo/redo using immutable deep snapshots.
 * A bounded stack prevents history from retaining document memory indefinitely.
 */
class HistoryController(private val maxEntries: Int = 100) {
    data class Entry(
        val before: AnimationDocumentSnapshot,
        val after: AnimationDocumentSnapshot,
        val label: String
    )

    private val undoStack = ArrayDeque<Entry>()
    private val redoStack = ArrayDeque<Entry>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size

    fun record(before: AnimationDocumentSnapshot, after: AnimationDocumentSnapshot, label: String) {
        if (before == after) return
        undoStack.addLast(Entry(before, after, label))
        while (undoStack.size > maxEntries.coerceAtLeast(1)) undoStack.removeFirst()
        redoStack.clear()
    }

    fun undo(document: AnimationDocument): Boolean {
        val entry = undoStack.removeLastOrNull() ?: return false
        document.restore(entry.before)
        redoStack.addLast(entry)
        trimRedo()
        return true
    }

    fun redo(document: AnimationDocument): Boolean {
        val entry = redoStack.removeLastOrNull() ?: return false
        document.restore(entry.after)
        undoStack.addLast(entry)
        trimUndo()
        return true
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun setLimit(limit: Int) {
        val bounded = limit.coerceIn(10, 500)
        while (undoStack.size > bounded) undoStack.removeFirst()
        while (redoStack.size > bounded) redoStack.removeFirst()
    }

    private fun trimUndo() {
        while (undoStack.size > maxEntries.coerceAtLeast(1)) undoStack.removeFirst()
    }

    private fun trimRedo() {
        while (redoStack.size > maxEntries.coerceAtLeast(1)) redoStack.removeFirst()
    }
}
