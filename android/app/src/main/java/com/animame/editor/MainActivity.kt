package com.animame.editor

import android.app.Activity
import android.os.Bundle
import android.content.pm.ActivityInfo

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(AnimationEditorView(this))
    }
}
