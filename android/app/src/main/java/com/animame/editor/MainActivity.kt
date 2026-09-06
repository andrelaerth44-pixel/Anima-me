package com.animame.editor

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle

class MainActivity : Activity() {
    private lateinit var editor: AnimationEditorViewV4
    companion object { const val PICK_VIDEO=4101; const val CREATE_MP4=4102; const val CREATE_GIF=4103; const val CREATE_SEQUENCE=4104 }
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;ProjectStore.initialize(applicationContext);editor=AnimationEditorViewV4(this);setContentView(editor)}
    fun pickVideo(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="video/*";addCategory(Intent.CATEGORY_OPENABLE)},PICK_VIDEO)}
    fun createExport(format:AnimationExportEngine.Format){val mime=when(format){AnimationExportEngine.Format.MP4->"video/mp4";AnimationExportEngine.Format.GIF->"image/gif";AnimationExportEngine.Format.PNG_SEQUENCE->"application/zip"};val name=when(format){AnimationExportEngine.Format.MP4->"animation.mp4";AnimationExportEngine.Format.GIF->"animation.gif";AnimationExportEngine.Format.PNG_SEQUENCE->"frames.zip"};val req=when(format){AnimationExportEngine.Format.MP4->CREATE_MP4;AnimationExportEngine.Format.GIF->CREATE_GIF;AnimationExportEngine.Format.PNG_SEQUENCE->CREATE_SEQUENCE};startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type=mime;putExtra(Intent.EXTRA_TITLE,name);addCategory(Intent.CATEGORY_OPENABLE)},req)}
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK)return;val uri=data?.data?:return;when(requestCode){PICK_VIDEO->editor.importVideo(uri);CREATE_MP4->editor.exportVideo(uri,AnimationExportEngine.Format.MP4);CREATE_GIF->editor.exportVideo(uri,AnimationExportEngine.Format.GIF);CREATE_SEQUENCE->editor.exportVideo(uri,AnimationExportEngine.Format.PNG_SEQUENCE)}}
    override fun onBackPressed(){super.onBackPressed()}
}
