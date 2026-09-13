package com.animame

import android.content.Context
import com.animame.editor.AnimationDocument
import com.animame.editor.AnimationDocumentJson

/** Persists the real animation document separately from the project-list metadata. */
object ProjectDocumentStore {
    private const val PREFS = "anima_me_project_documents"
    private const val PREFIX = "document_"

    fun load(context: Context, projectId: String): AnimationDocument? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(PREFIX + projectId, null) ?: return null
        return runCatching { AnimationDocumentJson.decode(raw) }.getOrNull()
    }

    fun save(context: Context, projectId: String, document: AnimationDocument) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(PREFIX + projectId, AnimationDocumentJson.encode(document))
            .apply()
    }

    fun delete(context: Context, projectId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(PREFIX + projectId)
            .apply()
    }
}
