package com.animame

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

class ProjectHomeActivity : Activity() {
    private val prefsName = "anima_me_projects"
    private val projectsKey = "projects"
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::list.isInitialized) refreshProjects()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.rgb(18, 20, 23)); setPadding(dp(28), dp(20), dp(28), dp(20)) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply { text = "ANIMA-ME"; textSize = 28f; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0, dp(54), 1f))
        header.addView(button("Novo projeto", 140) { showNewProjectDialog() })
        root.addView(header)
        root.addView(TextView(this).apply { text = "Projetos"; textSize = 18f; setTextColor(Color.LTGRAY); setPadding(0, dp(16), 0, dp(8)) })
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refreshProjects()
    }

    private fun refreshProjects() {
        list.removeAllViews()
        val projects = loadProjects()
        if (projects.length() == 0) {
            list.addView(TextView(this).apply { text = "Nenhum projeto ainda.\n\nCrie um novo projeto para começar uma animação."; textSize = 17f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER; setPadding(dp(20), dp(60), dp(20), dp(60)) })
            return
        }
        for (i in 0 until projects.length()) {
            val project = projects.getJSONObject(i)
            val card = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(Color.rgb(30, 34, 39)); setPadding(dp(16), dp(12), dp(12), dp(12)) }
            card.addView(TextView(this).apply { text = "${project.optString("name", "Sem nome")}\n${project.optInt("width", 1280)} × ${project.optInt("height", 720)}  |  ${project.optInt("fps", 24)} FPS"; textSize = 16f; setTextColor(Color.WHITE) }, LinearLayout.LayoutParams(0, dp(70), 1f))
            card.addView(button("Abrir", 90) { openProject(project) })
            card.addView(button("Excluir", 90) { confirmDelete(project.optString("id")) })
            list.addView(card, LinearLayout.LayoutParams(-1, dp(94)).apply { setMargins(0, 0, 0, dp(8)) })
        }
    }

    private fun showNewProjectDialog() {
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(8), dp(24), 0) }
        val name = field("Nome do projeto", "Minha animação")
        val width = numberField("Largura", "1280")
        val height = numberField("Altura", "720")
        val fps = numberField("FPS", "24")
        val frames = numberField("Frames iniciais", "1")
        val transparent = CheckBox(this).apply { text = "Fundo transparente"; setTextColor(Color.WHITE); isChecked = false }
        content.addView(label("Nome")); content.addView(name)
        content.addView(label("Resolução"))
        val resolutionRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        resolutionRow.addView(width, LinearLayout.LayoutParams(0, dp(52), 1f))
        resolutionRow.addView(TextView(this).apply { text = " × "; textSize = 20f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER }, LinearLayout.LayoutParams(dp(38), dp(52)))
        resolutionRow.addView(height, LinearLayout.LayoutParams(0, dp(52), 1f))
        content.addView(resolutionRow)
        content.addView(label("Framerate")); content.addView(fps)
        content.addView(label("Frames iniciais")); content.addView(frames)
        content.addView(transparent)
        val dialog = AlertDialog.Builder(this).setTitle("Novo projeto").setView(ScrollView(this).apply { addView(content) }).setNegativeButton("Cancelar", null).setPositiveButton("OK", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val project = JSONObject()
                project.put("id", System.currentTimeMillis().toString())
                project.put("name", name.text.toString().trim().ifEmpty { "Minha animação" })
                project.put("width", width.text.toString().toIntOrNull()?.coerceIn(1, 16384) ?: 1280)
                project.put("height", height.text.toString().toIntOrNull()?.coerceIn(1, 16384) ?: 720)
                project.put("fps", fps.text.toString().toIntOrNull()?.coerceIn(1, 240) ?: 24)
                project.put("duration", frames.text.toString().toIntOrNull()?.coerceIn(1, 10000) ?: 1)
                project.put("transparent", transparent.isChecked)
                saveProject(project)
                dialog.dismiss()
                openProject(project)
            }
        }
        dialog.show()
    }

    private fun openProject(project: JSONObject) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra("project_id", project.optString("id")); putExtra("project_name", project.optString("name", "Minha animação")); putExtra("project_width", project.optInt("width", 1280)); putExtra("project_height", project.optInt("height", 720)); putExtra("project_fps", project.optInt("fps", 24)); putExtra("project_duration", project.optInt("duration", 1)); putExtra("project_transparent", project.optBoolean("transparent", false))
        })
    }

    private fun confirmDelete(id: String) {
        AlertDialog.Builder(this).setTitle("Excluir projeto").setMessage("Excluir este projeto da lista?").setNegativeButton("Cancelar", null).setPositiveButton("Excluir") { _, _ ->
            val old = loadProjects(); val next = JSONArray()
            for (i in 0 until old.length()) if (old.getJSONObject(i).optString("id") != id) next.put(old.getJSONObject(i))
            getSharedPreferences(prefsName, MODE_PRIVATE).edit().putString(projectsKey, next.toString()).apply(); refreshProjects()
        }.show()
    }

    private fun loadProjects(): JSONArray = try { JSONArray(getSharedPreferences(prefsName, MODE_PRIVATE).getString(projectsKey, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }

    private fun saveProject(project: JSONObject) {
        val old = loadProjects(); val next = JSONArray(); next.put(project)
        for (i in 0 until old.length()) next.put(old.getJSONObject(i))
        getSharedPreferences(prefsName, MODE_PRIVATE).edit().putString(projectsKey, next.toString()).apply()
    }

    private fun label(text: String) = TextView(this).apply { this.text = text; textSize = 13f; setTextColor(Color.LTGRAY); setPadding(0, dp(10), 0, dp(3)) }
    private fun field(hint: String, value: String) = EditText(this).apply { this.hint = hint; setText(value); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); inputType = InputType.TYPE_CLASS_TEXT; setSingleLine(true) }
    private fun numberField(hint: String, value: String) = EditText(this).apply { this.hint = hint; setText(value); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); inputType = InputType.TYPE_CLASS_NUMBER; setSingleLine(true) }
    private fun button(label: String, widthDp: Int, action: () -> Unit) = Button(this).apply { text = label; textSize = 11f; setOnClickListener { action() }; layoutParams = LinearLayout.LayoutParams(dp(widthDp), dp(52)).apply { setMargins(dp(4), 0, dp(4), 0) } }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
