package com.animame.editor

import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle

class MainActivity:Activity(){
 private lateinit var editor:AnimationEditorViewV4
 private var pendingImages=false
 companion object{const val PICK_VIDEO=4101;const val CREATE_MP4=4102;const val CREATE_GIF=4103;const val CREATE_SEQUENCE=4104;const val PICK_IMAGES=4105;const val PICK_AUDIO=4106;const val PICK_PROJECT=4107;const val CREATE_PROJECT=4108}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;ProjectStore.initialize(applicationContext);editor=AnimationEditorViewV4(this);setContentView(editor)}
 fun pickVideo(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="video/*";addCategory(Intent.CATEGORY_OPENABLE);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},PICK_VIDEO)}
 fun pickImages(){pendingImages=true;startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="image/*";putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);addCategory(Intent.CATEGORY_OPENABLE);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},PICK_IMAGES)}
 fun pickAudio(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="audio/*";addCategory(Intent.CATEGORY_OPENABLE);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},PICK_AUDIO)}
 fun pickProject(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="application/zip";addCategory(Intent.CATEGORY_OPENABLE);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},PICK_PROJECT)}
 fun createExport(format:AnimationExportEngine.Format){val mime=when(format){AnimationExportEngine.Format.MP4->"video/mp4";AnimationExportEngine.Format.GIF->"image/gif";AnimationExportEngine.Format.PNG_SEQUENCE->"application/zip"};val name=when(format){AnimationExportEngine.Format.MP4->"animation.mp4";AnimationExportEngine.Format.GIF->"animation.gif";AnimationExportEngine.Format.PNG_SEQUENCE->"frames.zip"};val req=when(format){AnimationExportEngine.Format.MP4->CREATE_MP4;AnimationExportEngine.Format.GIF->CREATE_GIF;AnimationExportEngine.Format.PNG_SEQUENCE->CREATE_SEQUENCE};startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type=mime;putExtra(Intent.EXTRA_TITLE,name);addCategory(Intent.CATEGORY_OPENABLE)},req)}
 fun exportProject(){startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type="application/zip";putExtra(Intent.EXTRA_TITLE,"project.animame");addCategory(Intent.CATEGORY_OPENABLE)},CREATE_PROJECT)}
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK)return;when(requestCode){PICK_VIDEO->data?.data?.let{editor.importVideo(it)};CREATE_MP4->data?.data?.let{editor.exportVideo(it,AnimationExportEngine.Format.MP4)};CREATE_GIF->data?.data?.let{editor.exportVideo(it,AnimationExportEngine.Format.GIF)};CREATE_SEQUENCE->data?.data?.let{editor.exportVideo(it,AnimationExportEngine.Format.PNG_SEQUENCE)};PICK_IMAGES->{val uris=ArrayList<Uri>();data?.clipData?.let{c->for(i in 0 until c.itemCount)uris+=c.getItemAt(i).uri};data?.data?.let{if(uris.isEmpty())uris+=it};if(uris.isNotEmpty())createImageProject(uris)};PICK_AUDIO->data?.data?.let{importAudioProject(it)};PICK_PROJECT->data?.data?.let{importProject(it)};CREATE_PROJECT->{/* Project package export is exposed by the editor workflow. */}}}
 private fun createImageProject(uris:List<Uri>){val paths=ProjectPackage.importImageSequence(this,uris);if(paths.isEmpty())return;val w=android.graphics.BitmapFactory.decodeFile(paths.first())?.width?:1280;val h=android.graphics.BitmapFactory.decodeFile(paths.first())?.height?:720;val p=ProjectStore.create("Image sequence",24,w,h);val d=AnimationDocument(p.name,w,h,24,paths.size);val l=d.activeLayer.apply{name="Image sequence"};paths.forEachIndexed{i,path->l.frames[i]=DrawingFrame(rasterPath=path)};ProjectDocumentStore.save(this,p.id,d);ProjectStore.touch(p)}
 private fun importAudioProject(uri:Uri){val path=ProjectPackage.copyAudio(this,uri)?:return;val p=ProjectStore.create("Audio project",24,1280,720);val d=AnimationDocument(p.name,1280,720,24,1,audioPath=path);ProjectDocumentStore.save(this,p.id,d);ProjectStore.touch(p)}
 private fun importProject(uri:Uri){val d=ProjectPackage.importPackage(this,uri)?:return;val p=ProjectStore.create(d.name,d.fps,d.width,d.height);ProjectDocumentStore.save(this,p.id,d);ProjectStore.touch(p)}
 override fun onBackPressed(){super.onBackPressed()}
}
