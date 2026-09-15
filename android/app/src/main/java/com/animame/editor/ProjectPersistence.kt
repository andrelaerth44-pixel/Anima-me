package com.animame.editor

import android.content.Context
import java.io.File

/**
 * Local project persistence for Anima-me.
 * Stores the complete AnimationDocument JSON outside the UI layer so projects
 * can survive Activity recreation and later be wired into the project hub.
 */
object ProjectPersistence {
    private const val DIRECTORY = "projects"
    private const val EXTENSION = ".animame.json"

    private fun directory(context: Context): File = File(context.filesDir, DIRECTORY).apply { mkdirs() }

    private fun file(context: Context, projectId: String): File {
        val safeId = projectId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(directory(context), safeId + EXTENSION)
    }

    fun exists(context: Context, projectId: String): Boolean = file(context, projectId).isFile

    fun save(context: Context, projectId: String, document: AnimationDocument) {
        require(projectId.isNotBlank()) { "projectId não pode estar vazio" }
        val target = file(context, projectId)
        val temporary = File(target.parentFile, target.name + ".tmp")
        temporary.writeText(AnimationDocumentJson.encode(document), Charsets.UTF_8)
        if (target.exists() && !target.delete()) {
            temporary.delete()
            error("Não foi possível substituir o projeto salvo")
        }
        if (!temporary.renameTo(target)) {
            temporary.delete()
            error("Não foi possível finalizar o salvamento do projeto")
        }
    }

    fun load(context: Context, projectId: String): AnimationDocument? {
        val source = file(context, projectId)
        if (!source.isFile) return null
        return runCatching {
            AnimationDocumentJson.decode(source.readText(Charsets.UTF_8))
        }.getOrNull()
    }

    fun delete(context: Context, projectId: String): Boolean = file(context, projectId).delete()

    fun clearAll(context: Context) {
        directory(context).listFiles()?.forEach { it.delete() }
    }
}
