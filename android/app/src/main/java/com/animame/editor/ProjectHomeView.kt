package com.animame.editor

import android.app.Dialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*

/** Project browser and compact New Project flow based strictly on the supplied reference screens. */
class ProjectHomeView(
    context: Context,
    private val onOpen: (AnimationDocument, AnimationProject) -> Unit
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var sortByName = false

    init { setBackgroundColor(Color.rgb(52, 54, 57)) }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(52, 54, 57)
        c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        // Header / project browser.
        paint.color = Color.rgb(43, 45, 48)
        c.drawRect(0f, 0f, width.toFloat(), 52f, paint)
        text(c, "ANIMA-ME", 20f, 34f, Color.WHITE, 18f, true)
        text(c, "Projects", 126f, 33f, Color.LTGRAY, 11f)
        round(c, width - 174f, 8f, width - 94f, 44f, Color.rgb(73, 76, 80), 5f)
        text(c, "▣+", width - 152f, 32f, Color.WHITE, 17f)
        round(c, width - 90f, 8f, width - 8f, 44f, if (sortByName) Color.rgb(245, 239, 220) else Color.rgb(73, 76, 80), 5f)
        text(c, if (sortByName) "Name" else "Date", width - 77f, 31f, if (sortByName) Color.DKGRAY else Color.WHITE, 10f)

        val projects = if (sortByName) ProjectStore.projects.sortedBy { it.name.lowercase() } else ProjectStore.projects.toList()
        if (projects.isEmpty()) {
            text(c, "No projects", width / 2f - 35f, height / 2f - 10f, Color.LTGRAY, 14f)
            text(c, "Create a project to start drawing", width / 2f - 102f, height / 2f + 14f, Color.GRAY, 10f)
        } else {
            var y = 66f
            projects.forEach { p ->
                projectCard(c, p, y)
                y += 78f
            }
        }

        // New project action remains fixed at the lower-right, as in the reference workflow.
        round(c, width / 2f + 8f, height - 58f, width - 8f, height - 8f, Color.rgb(91, 91, 91), 5f)
        text(c, "+", width * .58f, height - 27f, Color.WHITE, 18f, true)
        text(c, "New project", width * .62f, height - 28f, Color.WHITE, 13f)
    }

    private fun projectCard(c: Canvas, p: AnimationProject, y: Float) {
        round(c, 8f, y, width / 2f - 8f, y + 68f, Color.rgb(68, 71, 76), 3f)
        // Paper/camera thumbnail.
        paint.color = Color.rgb(238, 238, 238)
        c.drawRect(17f, y + 16f, 63f, y + 48f, paint)
        paint.color = Color.rgb(210, 213, 216)
        c.drawRect(22f, y + 20f, 58f, y + 44f, paint)
        text(c, p.name, 74f, y + 29f, Color.WHITE, 13f)
        text(c, "${p.fps} fps  •  ${p.cameraWidth} × ${p.cameraHeight}", 74f, y + 49f, Color.LTGRAY, 9f)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action != MotionEvent.ACTION_UP) return true
        val x = e.x
        val y = e.y
        if (y < 52f && x > width - 94f) {
            sortByName = !sortByName
            invalidate()
            return true
        }
        if (y > height - 72f && x > width / 2f) {
            showCreateDialog()
            return true
        }
        val projects = if (sortByName) ProjectStore.projects.sortedBy { it.name.lowercase() } else ProjectStore.projects.toList()
        val index = ((y - 66f) / 78f).toInt()
        if (index in projects.indices && x < width / 2f) {
            val p = projects[index]
            val d = ProjectDocumentStore.load(context, p.id)
                ?: AnimationDocument(p.name, p.canvasWidth, p.canvasHeight, p.fps, 1)
            ProjectStore.touch(p)
            onOpen(d, p)
        }
        return true
    }

    private fun showCreateDialog() {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(26, 18, 26, 14)
            background = panelBackground(Color.rgb(43, 44, 47), 6)
        }
        val title = TextView(context).apply {
            text = "New project"
            setTextColor(Color.WHITE)
            textSize = 18f
            setPadding(0, 0, 0, 12)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, -2))

        val name = textField("New project name", "Untitled")
        root.addView(label("New project name")); root.addView(name)

        val fps = numberField("Frames per second", "24")
        val cameraW = numberField("Camera width", "1280")
        val cameraH = numberField("Camera height", "720")
        val marginW = numberField("+ Margins width", "0")
        val marginH = numberField("+ Margins height", "0")

        root.addView(label("Frames per second")); root.addView(fps)
        root.addView(label("Camera size"))
        val cameraRow = row(); cameraRow.addView(cameraW, weightParams()); cameraRow.addView(cameraH, weightParams())
        root.addView(cameraRow)
        root.addView(label("+ Margins"))
        val marginRow = row(); marginRow.addView(marginW, weightParams()); marginRow.addView(marginH, weightParams())
        root.addView(marginRow)

        val canvasLabel = TextView(context).apply {
            setTextColor(Color.WHITE); textSize = 12f; setPadding(0, 10, 0, 6)
            text = "= Canvas size: 1280 × 720"
        }
        root.addView(canvasLabel)
        val watcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) = Unit
            override fun onTextChanged(s: CharSequence?, st: Int, before: Int, count: Int) {
                val w = cameraW.text.toString().toIntOrNull() ?: 0
                val h = cameraH.text.toString().toIntOrNull() ?: 0
                val mw = marginW.text.toString().toIntOrNull() ?: 0
                val mh = marginH.text.toString().toIntOrNull() ?: 0
                canvasLabel.text = "= Canvas size: ${w + mw * 2} × ${h + mh * 2}"
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        }
        cameraW.addTextChangedListener(watcher); cameraH.addTextChangedListener(watcher)
        marginW.addTextChangedListener(watcher); marginH.addTextChangedListener(watcher)

        val buttons = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.END; setPadding(0, 8, 0, 0) }
        val cancel = Button(context).apply { text = "Cancel"; setTextColor(Color.WHITE); setBackgroundColor(Color.TRANSPARENT) }
        val okay = Button(context).apply { text = "Okay"; setTextColor(Color.WHITE); setBackgroundColor(Color.TRANSPARENT) }
        buttons.addView(cancel, LinearLayout.LayoutParams(-2, 48)); buttons.addView(okay, LinearLayout.LayoutParams(-2, 48))
        root.addView(buttons)

        cancel.setOnClickListener { dialog.dismiss() }
        okay.setOnClickListener {
            val n = name.text.toString().trim().ifBlank { "Untitled" }
            val f = fps.text.toString().toIntOrNull()?.coerceIn(1, 240) ?: 24
            val ww = cameraW.text.toString().toIntOrNull()?.coerceIn(1, 16384) ?: 1280
            val hh = cameraH.text.toString().toIntOrNull()?.coerceIn(1, 16384) ?: 720
            val mx = marginW.text.toString().toIntOrNull()?.coerceIn(0, 8192) ?: 0
            val my = marginH.text.toString().toIntOrNull()?.coerceIn(0, 8192) ?: 0
            val canvasW = (ww + mx * 2).coerceIn(1, 16384)
            val canvasH = (hh + my * 2).coerceIn(1, 16384)
            val p = ProjectStore.create(n, f, ww, hh, maxOf(mx, my))
            p.canvasWidth = canvasW; p.canvasHeight = canvasH; ProjectStore.update(p)
            val d = ProjectDocumentStore.load(context, p.id) ?: AnimationDocument(n, canvasW, canvasH, f, 1)
            d.name = n; d.width = canvasW; d.height = canvasH; d.fps = f; d.duration = 1; d.currentFrame = 0; d.normalize()
            ProjectDocumentStore.save(context, p.id, d); ProjectStore.touch(p)
            dialog.dismiss(); onOpen(d, p)
        }

        dialog.setContentView(root)
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.setLayout(dp(520), WindowManager.LayoutParams.WRAP_CONTENT)
        }
        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(dp(520), WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun label(s: String) = TextView(context).apply {
        text = s; setTextColor(Color.LTGRAY); textSize = 11f; setPadding(0, 7, 0, 2)
    }
    private fun textField(hint: String, value: String) = EditText(context).apply {
        setText(value); setHint(hint); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); textSize = 13f; singleLine = true
        background = panelBackground(Color.rgb(57, 59, 63), 3)
        setPadding(10, 0, 10, 0)
    }
    private fun numberField(hint: String, value: String) = textField(hint, value).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER }
    private fun row() = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun weightParams() = LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginEnd = dp(6) }
    private fun panelBackground(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
    private fun round(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int, rad: Float) { paint.color = color; paint.style = Paint.Style.FILL; c.drawRoundRect(l, t, r, b, rad, rad, paint) }
    private fun text(c: Canvas, s: String, x: Float, y: Float, color: Int, size: Float, bold: Boolean = false) { paint.color = color; paint.textSize = size; paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT; paint.style = Paint.Style.FILL; c.drawText(s, x, y, paint) }
}
