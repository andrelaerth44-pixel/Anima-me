package com.animame.editor

import android.app.Activity
import android.os.Bundle
import android.content.pm.ActivityInfo

class MainActivity : Activity() {
    private lateinit var editor: AnimationEditorViewV4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        // Always restore the persistent project browser first. Opening a project is an explicit user action.
        ProjectStore.initialize(applicationContext)
        editor = AnimationEditorViewV4(this)
        setContentView(editor)
    }

    override fun onBackPressed() {
        if (::editor.isInitialized) super.onBackPressed() else super.onBackPressed()
    }
}
