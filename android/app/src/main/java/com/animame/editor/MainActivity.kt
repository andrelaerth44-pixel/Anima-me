package com.animame.editor

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.widget.EditText
import android.widget.LinearLayout

class MainActivity : Activity() {
    private lateinit var editor: AnimationEditorViewV4
    private lateinit var home: ProjectHomeView
    private var inEditor = false
    private var menuOpen = false
    private var canvasGestures: CanvasGestureRouter? = null

    companion object {
        const val PICK_VIDEO = 4101
        const val CREATE_MP4 = 4102
        const val CREATE_GIF = 4103
        const val CREATE_SEQUENCE = 4104
        const val PICK_IMAGES = 4105
        const val PICK_AUDIO = 4106
        const val PICK_PROJECT = 4107
        const val CREATE_PROJECT = 4108
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        ProjectStore.initialize(applicationContext)
        editor = AnimationEditorViewV4(this)
        canvasGestures = CanvasGestureRouter(editor)
        editor.setOnTouchListener { _, event ->
            // Two fingers always belong to viewport navigation. Returning true here
            // prevents the drawing engine from creating accidental samples while
            // pinch/rotate/pan is in progress. One-finger events continue normally
            // through AnimationEditorViewV5's tool/UI routing.
            if (event.pointerCount >= 2) canvasGestures?.handle(event) == true else false
        }
        home = ProjectHomeView(this) { document, project -> openEditor(document, project) }
        showHome()
    }

    private fun showHome() {
        inEditor = false
        menuOpen = false
        setContentView(home)
        home.invalidate()
    }

    private fun openEditor(document: AnimationDocument, project: AnimationProject) {
        inEditor = true
        menuOpen = false
        editor.openDocument(document, project)
        ProjectStore.touch(project)
        setContentView(editor)
        editor.invalidate()
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (inEditor && ev.actionMasked == MotionEvent.ACTION_UP) {
            val x = ev.x
            val y = ev.y
            if (!menuOpen && x < 60f && y < 60f) {
                menuOpen = true
            } else if (menuOpen && x < 300f) {
                when (y) {
                    in 78f..146f -> { editor.dismissMenu(); showHome(); return true }
                    in 147f..214f -> { editor.dismissMenu(); showExportDialog(); menuOpen=false; return true }
                    in 215f..280f -> { editor.dismissMenu(); pickAudio(); menuOpen=false; return true }
                    in 281f..346f -> { editor.dismissMenu(); pickImages(); menuOpen=false; return true }
                    in 347f..412f -> { editor.dismissMenu(); pickVideo(); menuOpen=false; return true }
                    in 413f..488f -> { editor.dismissMenu(); pickProject(); menuOpen=false; return true }
                    in 489f..553f -> { editor.dismissMenu(); changeFramerate(); menuOpen=false; return true }
                    in 554f..630f -> { editor.dismissMenu(); resizeCanvas(); menuOpen=false; return true }
                    else -> { editor.dismissMenu(); menuOpen=false; return true }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    fun pickVideo() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "video/*"; addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }, PICK_VIDEO)
    }

    private fun pickImages() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"; putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }, PICK_IMAGES)
    }

    private fun pickAudio() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "audio/*"; addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }, PICK_AUDIO)
    }

    private fun pickProject() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "application/zip"; addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }, PICK_PROJECT)
    }

    private fun showExportDialog() {
        val options = arrayOf("MP4 video", "GIF", "PNG image sequence")
        AlertDialog.Builder(this)
            .setTitle("Export video")
            .setItems(options) { _, which ->
                when(which) {
                    0 -> createExport(AnimationExportEngine.Format.MP4)
                    1 -> createExport(AnimationExportEngine.Format.GIF)
                    2 -> createExport(AnimationExportEngine.Format.PNG_SEQUENCE)
                }
            }.show()
    }

    fun createExport(format: AnimationExportEngine.Format) {
        val mime = when (format) {
            AnimationExportEngine.Format.MP4 -> "video/mp4"
            AnimationExportEngine.Format.GIF -> "image/gif"
            AnimationExportEngine.Format.PNG_SEQUENCE -> "application/zip"
        }
        val name = when (format) {
            AnimationExportEngine.Format.MP4 -> "animation.mp4"
            AnimationExportEngine.Format.GIF -> "animation.gif"
            AnimationExportEngine.Format.PNG_SEQUENCE -> "frames.zip"
        }
        val req = when (format) {
            AnimationExportEngine.Format.MP4 -> CREATE_MP4
            AnimationExportEngine.Format.GIF -> CREATE_GIF
            AnimationExportEngine.Format.PNG_SEQUENCE -> CREATE_SEQUENCE
        }
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type=mime; putExtra(Intent.EXTRA_TITLE,name); addCategory(Intent.CATEGORY_OPENABLE)
        }, req)
    }

    fun exportProject() {
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type="application/zip"; putExtra(Intent.EXTRA_TITLE,"project.animame"); addCategory(Intent.CATEGORY_OPENABLE)
        }, CREATE_PROJECT)
    }

    private fun changeFramerate() {
        val input=EditText(this).apply { hint="FPS"; setText(editor.currentFps().toString()); inputType=2 }
        AlertDialog.Builder(this).setTitle("Change framerate").setView(input)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Apply") { _,_ -> input.text.toString().toIntOrNull()?.let(editor::setDocumentFps) }.show()
    }

    private fun resizeCanvas() {
        val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(40,0,40,0) }
        val w=EditText(this).apply { hint="Width"; setText(editor.currentWidth().toString()); inputType=2 }
        val h=EditText(this).apply { hint="Height"; setText(editor.currentHeight().toString()); inputType=2 }
        box.addView(w); box.addView(h)
        AlertDialog.Builder(this).setTitle("Resize or crop/expand").setView(box)
            .setNegativeButton("Cancel",null)
            .setPositiveButton("Apply") { _,_ ->
                val nw=w.text.toString().toIntOrNull(); val nh=h.text.toString().toIntOrNull()
                if(nw!=null&&nh!=null) editor.resizeDocument(nw,nh)
            }.show()
    }

    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK)return
        when(requestCode) {
            PICK_VIDEO -> data?.data?.let(editor::importVideo)
            CREATE_MP4 -> data?.data?.let { editor.exportVideo(it,AnimationExportEngine.Format.MP4) }
            CREATE_GIF -> data?.data?.let { editor.exportVideo(it,AnimationExportEngine.Format.GIF) }
            CREATE_SEQUENCE -> data?.data?.let { editor.exportVideo(it,AnimationExportEngine.Format.PNG_SEQUENCE) }
            PICK_IMAGES -> {
                val uris=ArrayList<Uri>(); data?.clipData?.let{c->for(i in 0 until c.itemCount)uris+=c.getItemAt(i).uri}; data?.data?.let{if(uris.isEmpty())uris+=it}
                if(uris.isNotEmpty())createImageProject(uris)
            }
            PICK_AUDIO -> data?.data?.let { ProjectPackage.copyAudio(this,it)?.let(editor::attachAudio) }
            PICK_PROJECT -> data?.data?.let { ProjectPackage.importPackage(this,it)?.let { d ->
                val p=ProjectStore.create(d.name,d.fps,d.width,d.height); ProjectDocumentStore.save(this,p.id,d); openEditor(d,p)
            } }
            CREATE_PROJECT -> data?.data?.let { ProjectPackage.export(this,it,editor.documentForExport()) }
        }
    }

    private fun createImageProject(uris:List<Uri>) {
        val paths=ProjectPackage.importImageSequence(this,uris); if(paths.isEmpty())return
        val first=android.graphics.BitmapFactory.decodeFile(paths.first()); val w=first?.width?:1280; val h=first?.height?:720; first?.recycle()
        val p=ProjectStore.create("Image sequence",24,w,h); val d=AnimationDocument(p.name,w,h,24,paths.size); val layer=d.activeLayer.apply{name="Image sequence"}
        paths.forEachIndexed{i,path->layer.frames[i]=DrawingFrame(rasterPath=path)}; d.normalize(); ProjectDocumentStore.save(this,p.id,d); ProjectStore.touch(p); openEditor(d,p)
    }

    override fun onBackPressed(){if(inEditor)showHome()else super.onBackPressed()}
}
