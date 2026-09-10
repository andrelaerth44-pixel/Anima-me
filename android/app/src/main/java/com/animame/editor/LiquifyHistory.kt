package com.animame.editor

/** Bounded undo history for geometry-editing tools such as Liquify. */
class LiquifyHistory(private val capacity: Int = 12) {
    data class StrokeSnapshot(val id: String, val samples: List<StrokeSample>)

    private val undoStack = ArrayDeque<List<StrokeSnapshot>>()

    fun push(strokes: List<StrokeData>) {
        undoStack.addLast(
            strokes.map { stroke ->
                StrokeSnapshot(stroke.id, stroke.samples.map { sample -> sample.copy() })
            }
        )
        while (undoStack.size > capacity.coerceAtLeast(1)) undoStack.removeFirst()
    }

    fun undo(strokes: List<StrokeData>): Boolean {
        val snapshot = undoStack.removeLastOrNull() ?: return false
        snapshot.forEach { saved ->
            strokes.firstOrNull { it.id == saved.id }?.let { stroke ->
                stroke.samples.clear()
                stroke.samples.addAll(saved.samples.map { it.copy() })
            }
        }
        return true
    }

    fun clear() = undoStack.clear()

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()
}
