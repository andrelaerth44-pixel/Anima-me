package com.animame

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import com.animame.editor.AnimationDocument
import com.animame.editor.BrushPresets

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(EditorSurface())
    }

    private class EditorSurface : View {
        private val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(18, 20, 23) }
        private val panel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 34, 39) }
        private val canvasPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(245, 245, 245) }
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 28f }
        private val sub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; textSize = 18f }
        private val document = AnimationDocument()

        constructor() : super(null)
        constructor(context: android.content.Context) : super(context)

        override fun onDraw(c: Canvas) {
            super.onDraw(c)
            c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)
            val top = (height * 0.12f).coerceAtLeast(64f)
            val bottom = (height * 0.72f).coerceAtMost(height - 140f)
            c.drawRect(0f, 0f, width.toFloat(), top, panel)
            c.drawRect(0f, bottom, width.toFloat(), height.toFloat(), panel)

            val left = width * 0.10f
            val right = width * 0.90f
            c.drawRect(left, top + 24f, right, bottom - 24f, canvasPaint)

            c.drawText("ANIMA-ME", 28f, 42f, text)
            c.drawText("Frame ${document.currentFrame + 1}  •  ${document.fps} FPS", 28f, top - 18f, sub)
            c.drawText("Brushes: ${BrushPresets.all.size}", width - 230f, 42f, sub)
            c.drawText("Timeline  •  Layer 1", 28f, bottom + 44f, text)
            c.drawText("Brush / Eraser / Lasso / Fill / Onion Skin / QR", 28f, height - 28f, sub)
        }
    }
}
