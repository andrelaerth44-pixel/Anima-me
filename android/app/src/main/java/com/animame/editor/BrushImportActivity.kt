package com.animame.editor

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast

class BrushImportActivity : Activity() {
    companion object { private const val PICK = 7311 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FrameLayout(this))
        openPicker()
    }

    private fun openPicker() {
        startActivityForResult(android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            putExtra(android.content.Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/zip", "application/octet-stream"))
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }, PICK)
    }

    override fun onActivityResult(requestCode:Int, resultCode:Int, data:android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK) return
        if (resultCode != RESULT_OK || data?.data == null) { BrushImportBridge.clear(); finish(); return }
        BrushImportManager.handleResult(this, data.data!!) { result ->
            result.fold(
                onSuccess = { imported ->
                    BrushImportBridge.deliver(imported)
                    Toast.makeText(this, "Pincel importado: ${imported.preset.name}", Toast.LENGTH_SHORT).show()
                },
                onFailure = {
                    Toast.makeText(this, it.message ?: "Falha ao importar pincel.", Toast.LENGTH_LONG).show()
                    BrushImportBridge.clear()
                }
            )
            finish()
        }
    }

    override fun onDestroy() {
        if (isFinishing) BrushImportBridge.clear()
        super.onDestroy()
    }
}
