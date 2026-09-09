package com.animame.editor

import android.app.Activity
import android.content.Context

object BrushImportBridge {
    private var callback: ((BrushImportManager.ImportResult) -> Unit)? = null
    fun launch(context: Context, onImported: (BrushImportManager.ImportResult) -> Unit) {
        callback = onImported
        context.startActivity(android.content.Intent(context, BrushImportActivity::class.java))
    }
    internal fun deliver(result: BrushImportManager.ImportResult) { callback?.invoke(result); callback = null }
    internal fun clear() { callback = null }
}
