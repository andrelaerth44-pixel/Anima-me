package com.animame.editor

import java.util.WeakHashMap

/** One bounded history controller per live document. */
object HistoryRegistry {
    private val histories = WeakHashMap<AnimationDocument, HistoryController>()

    @Synchronized
    fun forDocument(document: AnimationDocument): HistoryController =
        histories.getOrPut(document) { HistoryController(100) }
}
