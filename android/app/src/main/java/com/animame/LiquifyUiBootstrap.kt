package com.animame

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import com.animame.editor.AnimationDocument
import com.animame.editor.LiquifyEngine
import com.animame.editor.LiquifyHistory
import com.animame.editor.StrokeSample
import java.lang.reflect.Field

/** Installs Liquify on the existing canvas without changing the animation model. */
object LiquifyUiBootstrap {
    private const val TAG = "anima-me-liquify"
    private const val BUTTON_TAG = "anima-me-liquify-button"

    fun install(context: android.content.Context) {
        val activity = context as? Activity ?: return
        if (activity.javaClass.simpleName != "MainActivity") return
        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (content.findViewWithTag<View>(TAG) != null) return
        val editor = readField(activity, "editor") ?: return

        val overlay = LiquifyOverlay(activity, editor)
        overlay.tag = TAG
        overlay.visibility = View.GONE
        content.addView(overlay, FrameLayout.LayoutParams(-1, -1).apply {
            topMargin = dp(activity, 66)
            bottomMargin = dp(activity, 58)
        })

        val button = Button(activity).apply {
            tag = BUTTON_TAG
            text = "Liquify"
            textSize = 10f
            setOnClickListener { showPanel(activity, overlay) }
        }
        content.addView(button, FrameLayout.LayoutParams(dp(activity, 78), dp(activity, 54)).apply {
            leftMargin = dp(activity, 650)
            topMargin = dp(activity, 6)
        })
    }

    private fun showPanel(activity: Activity, overlay: LiquifyOverlay) {
        val box = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 8, 18, 4)
        }
        val radius = EditText(activity).apply {
            hint = "Raio (px)"
            setText(overlay.settings.radius.toInt().toString())
        }
        val strength = EditText(activity).apply {
            hint = "Força 0–100%"
            setText((overlay.settings.strength * 100).toInt().toString())
        }
        box.addView(radius)
        box.addView(strength)

        var selected = overlay.settings.mode
        LiquifyEngine.Mode.values().forEach { mode ->
            box.addView(Button(activity).apply {
                text = modeLabel(mode)
                setOnClickListener {
                    selected = mode
                    overlay.setMode(mode)
                }
            })
        }

        val controls = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        controls.addView(Button(activity).apply {
            text = "Ativar"
            setOnClickListener {
                val r = radius.text.toString().toFloatOrNull()?.coerceIn(4f, 1000f) ?: overlay.settings.radius
                val s = ((strength.text.toString().toFloatOrNull() ?: overlay.settings.strength * 100f)
                    .coerceIn(0f, 100f)) / 100f
                overlay.settings = LiquifyEngine.Settings(r, s, selected)
                overlay.visibility = View.VISIBLE
                overlay.bringToFront()
                Toast.makeText(activity, "Liquify ativo — arraste sobre o desenho", Toast.LENGTH_SHORT).show()
            }
        })
        controls.addView(Button(activity).apply {
            text = "Desativar"
            setOnClickListener { overlay.visibility = View.GONE }
        })
        controls.addView(Button(activity).apply {
            text = "Desfazer"
            setOnClickListener {
                if (overlay.undo()) Toast.makeText(activity, "Liquify desfeito", Toast.LENGTH_SHORT).show()
            }
        })
        box.addView(controls)

        AlertDialog.Builder(activity)
            .setTitle("Liquify")
            .setView(box)
            .setNegativeButton("Fechar", null)
            .show()
    }

    private fun modeLabel(mode: LiquifyEngine.Mode) = when (mode) {
        LiquifyEngine.Mode.PUSH -> "Empurrar"
        LiquifyEngine.Mode.PULL -> "Puxar"
        LiquifyEngine.Mode.TWIRL_CW -> "Girar ↻"
        LiquifyEngine.Mode.TWIRL_CCW -> "Girar ↺"
        LiquifyEngine.Mode.PINCH -> "Contrair"
        LiquifyEngine.Mode.BLOAT -> "Expandir"
        LiquifyEngine.Mode.RECONSTRUCT -> "Reconstruir"
    }

    private class LiquifyOverlay(private val activity: Activity, private val editor: Any) : View(activity) {
        var settings = LiquifyEngine.Settings()
        private val history = LiquifyHistory()
        private val path = mutableListOf<StrokeSample>()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.WHITE
        }
        private var cursorX = -1f
        private var cursorY = -1f

        fun setMode(mode: LiquifyEngine.Mode) { settings = settings.copy(mode = mode) }

        override fun onDraw(canvas: Canvas) {
            if (cursorX < 0f || cursorY < 0f) return
            canvas.drawCircle(cursorX, cursorY, settings.radius.coerceAtMost(240f), paint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    path.clear()
                    addPathPoint(event)
                    cursorX = event.x
                    cursorY = event.y
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    addPathPoint(event)
                    cursorX = event.x
                    cursorY = event.y
                    invalidate()
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (event.actionMasked == MotionEvent.ACTION_UP && path.size > 1) applyGesture()
                    path.clear()
                    invalidate()
                    return true
                }
            }
            return true
        }

        fun undo(): Boolean {
            val frame = currentFrame() ?: return false
            val ok = history.undo(frame.strokes)
            refreshEditor()
            return ok
        }

        private fun applyGesture() {
            val frame = currentFrame() ?: return
            if (frame.strokes.isEmpty()) return
            history.push(frame.strokes)
            frame.strokes.forEach { stroke ->
                val changed = LiquifyEngine.applyStroke(stroke.samples, path, settings)
                stroke.samples.clear()
                stroke.samples.addAll(changed)
            }
            refreshEditor()
        }

        private fun currentFrame(): com.animame.editor.DrawingFrame? {
            val document = readField(editor, "document") as? AnimationDocument ?: return null
            return document.activeLayer.frameAt(document.currentFrame)
        }

        private fun addPathPoint(event: MotionEvent) {
            val p = toCanvasPoint(event.x, event.y)
            path += StrokeSample(p.first, p.second, 1f, event.eventTime)
        }

        private fun toCanvasPoint(x: Float, y: Float): Pair<Float, Float> {
            val editorWidth = (readField(editor, "width") as? Int)?.toFloat() ?: width.toFloat()
            val editorHeight = (readField(editor, "height") as? Int)?.toFloat() ?: (height + dp(activity, 124)).toFloat()
            val screenY = y + dp(activity, 66).toFloat()
            val left = editorWidth * .07f
            val right = editorWidth * .93f
            val top = 66f
            val bottom = editorHeight - 58f
            val centerX = (left + right) * .5f
            val centerY = (top + bottom) * .5f
            val zoom = readFloat(editor, "zoom", 1f)
            val panX = readFloat(editor, "panX", 0f)
            val panY = readFloat(editor, "panY", 0f)
            return Pair(
                (x - centerX - panX) / zoom + centerX,
                (screenY - centerY - panY) / zoom + centerY
            )
        }

        private fun refreshEditor() {
            runCatching {
                editor.javaClass.getMethod("invalidate").invoke(editor)
                editor.javaClass.getMethod("refreshTimeline").invoke(editor)
            }
        }
    }

    private fun readField(target: Any, name: String): Any? = runCatching {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            runCatching {
                val field: Field = type!!.getDeclaredField(name)
                field.isAccessible = true
                return field.get(target)
            }
            type = type.superclass
        }
        null
    }.getOrNull()

    private fun readFloat(target: Any, name: String, fallback: Float): Float =
        runCatching { (readField(target, name) as Number).toFloat() }.getOrDefault(fallback)

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
