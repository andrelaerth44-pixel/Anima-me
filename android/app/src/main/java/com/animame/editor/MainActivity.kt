package com.animame.editor

import android.app.Activity
import android.os.Bundle
import android.content.pm.ActivityInfo

class MainActivity : Activity() {
    private lateinit var editor: AnimationEditorViewV4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        editor = AnimationEditorViewV4(this)
        setContentView(editor)
    }

    override fun onBackPressed() {
        if (::editor.isInitialized) {
            // Keep the editor state alive; the system can still finish the activity normally.
            super.onBackPressed()
        } else {
            super.onBackPressed()
        }
    }
}
