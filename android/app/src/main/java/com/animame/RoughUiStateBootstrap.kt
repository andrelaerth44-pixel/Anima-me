package com.animame

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.animame.editor.EditorToolEngine
import java.lang.reflect.Field

/** Keeps the tool buttons visually synchronized: active is white, inactive is gray. */
object RoughUiStateBootstrap {
    private const val TAG = "anima-me-rough-ui-state"
    private val toolLabels = setOf("Pincel", "Borracha", "Balde", "Laço", "Transform", "Conta-gotas", "Liquify")

    fun install(context: android.content.Context) {
        val activity = context as? Activity ?: return
        if (activity.javaClass.simpleName != "MainActivity") return
        val root = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (root.findViewWithTag<View>(TAG) != null) return
        val marker = View(activity).apply { tag = TAG; visibility = View.GONE }
        root.addView(marker, FrameLayout.LayoutParams(1, 1))
        root.post { bind(activity, root) }
    }

    private fun bind(activity: Activity, root: FrameLayout) {
        val editor = runCatching { readField(activity, "editor") }.getOrNull() ?: return
        val buttons = collect(root).filter { it.text?.toString() in toolLabels }
        if (buttons.isEmpty()) return
        buttons.forEach { button ->
            wrapClick(button) { update(buttons, editor) }
        }
        update(buttons, editor)
        root.postDelayed(object : Runnable {
            override fun run() {
                update(buttons, editor)
                root.postDelayed(this, 180L)
            }
        }, 180L)
    }

    private fun update(buttons: List<Button>, editor: Any) {
        val tool = runCatching { readField(editor, "tool") as EditorToolEngine.ToolType }
            .getOrDefault(EditorToolEngine.ToolType.BRUSH)
        val selected = when (tool) {
            EditorToolEngine.ToolType.BRUSH -> "Pincel"
            EditorToolEngine.ToolType.ERASER -> "Borracha"
            EditorToolEngine.ToolType.BUCKET -> "Balde"
            EditorToolEngine.ToolType.LASSO -> "Laço"
            EditorToolEngine.ToolType.TRANSFORM -> "Transform"
            EditorToolEngine.ToolType.EYEDROPPER -> "Conta-gotas"
            EditorToolEngine.ToolType.LIQUIFY -> "Liquify"
        }
        buttons.forEach { button ->
            val active = button.text?.toString() == selected
            button.setTextColor(if (active) Color.BLACK else Color.LTGRAY)
            button.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (active) Color.WHITE else Color.rgb(62, 67, 74)
            )
        }
    }

    private fun wrapClick(button: Button, refresh: () -> Unit) {
        val old = readOnClick(button) ?: return
        if (old is WrappedClick) return
        button.setOnClickListener(WrappedClick(old, refresh))
    }

    private class WrappedClick(private val delegate: View.OnClickListener, private val refresh: () -> Unit) : View.OnClickListener {
        override fun onClick(v: View) {
            delegate.onClick(v)
            refresh()
        }
    }

    private fun readOnClick(view: View): View.OnClickListener? = runCatching {
        val infoField = View::class.java.getDeclaredField("mListenerInfo")
        infoField.isAccessible = true
        val info = infoField.get(view) ?: return@runCatching null
        val listenerClass = Class.forName("android.view.View\$ListenerInfo")
        val clickField = listenerClass.getDeclaredField("mOnClickListener")
        clickField.isAccessible = true
        clickField.get(info) as? View.OnClickListener
    }.getOrNull()

    private fun readField(target: Any, name: String): Any? = runCatching {
        var type: Class<*>? = target.javaClass
        while (type != null) {
            try {
                val f = type.getDeclaredField(name)
                f.isAccessible = true
                return@runCatching f.get(target)
            } catch (_: NoSuchFieldException) { type = type.superclass }
        }
        null
    }.getOrNull()

    private fun collect(root: View): List<Button> {
        val out = mutableListOf<Button>()
        fun walk(v: View) {
            if (v is Button) out += v
            if (v is android.view.ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))
        }
        walk(root)
        return out
    }
}
