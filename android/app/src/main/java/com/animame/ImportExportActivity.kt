package com.animame

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.media.MediaMetadataRetriever
import java.io.BufferedOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ImportExportActivity : Activity() {
    private val PICK_IMAGES=10;private val PICK_VIDEO=11;private val PICK_QR=12;private val SAVE_GIF=20;private val SAVE_MP4=21;private val SAVE_PNG=22;private val SAVE_IMAGE=23
    private lateinit var status:TextView

    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);requestedOrientation=android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;buildUi()}
    private fun buildUi(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(30,20,30,20);setBackgroundColor(ThemeColorStore.NAVY_950)}
        root.addView(TextView(this).apply{text="Importação / Exportação";textSize=27f;setTextColor(ThemeColorStore.TEXT)})
        root.addView(TextView(this).apply{text="QR • imagens • sequência • vídeo • GIF • MP4";textSize=16f;setTextColor(ThemeColorStore.MUTED);setPadding(0,6,0,18)})
        status=TextView(this).apply{textSize=15f;setTextColor(ThemeColorStore.TEXT);setPadding(0,0,0,14)};root.addView(status);refresh()
        fun b(label:String,action:()->Unit)=Button(this).apply{text=label;setTextColor(ThemeColorStore.TEXT);setBackgroundColor(ThemeColorStore.NAVY_800);setOnClickListener{action()}}
        root.addView(b("Importar imagens / sequência"){pickImages()});root.addView(b("Importar vídeo → sequência de frames"){pickVideo()});root.addView(b("Importar QR de uma imagem"){pickQr()})
        root.addView(b("Exportar imagem PNG + QR"){ifReady{save(SAVE_IMAGE,"anima-me-frame.png","image/png")}});root.addView(b("Exportar sequência PNG (ZIP)"){ifReady{save(SAVE_PNG,"anima-me-sequence.zip","application/zip")}})
        root.addView(b("Exportar GIF"){ifReady{save(SAVE_GIF,"anima-me.gif","image/gif")}});root.addView(b("Exportar vídeo MP4 (H.264)"){ifReady{save(SAVE_MP4,"anima-me.mp4","video/mp4")}})
        root.addView(b("Limpar sequência"){MediaSequenceStore.clear();refresh()});root.addView(b("Voltar"){finish()});setContentView(root)
    }
    private fun refresh(){status.text="Frames importados: ${MediaSequenceStore.count}"}
    private fun ifReady(action:()->Unit){if(MediaSequenceStore.count==0)Toast.makeText(this,"Importa primeiro uma imagem ou sequência.",Toast.LENGTH_SHORT).show()else action()}
    private fun pickImages(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="image/*";putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);addCategory(Intent.CATEGORY_OPENABLE)},PICK_IMAGES)}
    private fun pickVideo(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="video/*";addCategory(Intent.CATEGORY_OPENABLE)},PICK_VIDEO)}
    private fun pickQr(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE)},PICK_QR)}
    private fun save(code:Int,name:String,mime:String){startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply{type=mime;putExtra(Intent.EXTRA_TITLE,name);addCategory(Intent.CATEGORY_OPENABLE)},code)}

    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(resultCode!=RESULT_OK)return
        when(requestCode){PICK_IMAGES->importImages(data);PICK_VIDEO->importVideo(data?.data);PICK_QR->importQr(data?.data);SAVE_IMAGE->exportImage(data?.data);SAVE_PNG->exportPngZip(data?.data);SAVE_GIF->exportGif(data?.data);SAVE_MP4->exportMp4(data?.data)}
    }
    private fun importImages(data:Intent){val uris=mutableListOf<Uri>();data.data?.let{uris+=it};data.clipData?.let{clip->for(i in 0 until clip.itemCount)uris+=clip.getItemAt(i).uri};val ordered=uris.distinct().sortedBy{displayName(it)};val decoded=ordered.mapNotNull{decode(it)};if(decoded.isNotEmpty()){MediaSequenceStore.replace(decoded);refresh();Toast.makeText(this,"${decoded.size} frame(s) importado(s)",Toast.LENGTH_SHORT).show()}}
    private fun importVideo(uri:Uri?){if(uri==null)return;val retriever=MediaMetadataRetriever();try{retriever.setDataSource(this,uri);val duration=(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?:0L);val frames=mutableListOf<Bitmap>();val fps=24;val step=1_000_000L/fps;var t=0L;while(t<duration*1000L&&frames.size<240){retriever.getFrameAtTime(t,MediaMetadataRetriever.OPTION_CLOSEST)?.let{frames+=scale(it,1280)};t+=step};if(frames.isNotEmpty()){MediaSequenceStore.replace(frames);refresh();Toast.makeText(this,"Vídeo importado: ${frames.size} frames a ${fps} FPS",Toast.LENGTH_LONG).show()}else Toast.makeText(this,"Não foi possível extrair frames.",Toast.LENGTH_SHORT).show()}catch(e:Throwable){Toast.makeText(this,"Falha ao importar vídeo: ${e.message}",Toast.LENGTH_LONG).show()}finally{retriever.release()}}
    private fun importQr(uri:Uri?){if(uri==null)return;val b=decode(uri)?:return;val payload=ProjectQrCodec.decode(b);b.recycle();if(payload==null){Toast.makeText(this,"QR não reconhecido.",Toast.LENGTH_SHORT).show();return};val obj=ProjectQrCodec.unpack(payload);Toast.makeText(this,if(obj!=null)"QR Anima-me: ${obj.optString("name","Projeto")} • ${obj.optInt("frames",obj.optInt("mediaFrames",0))} frames" else "QR lido: ${payload.take(40)}…",Toast.LENGTH_LONG).show()}

    private fun exportImage(uri:Uri?){if(uri==null)return;val source=MediaSequenceStore.all().first();val bitmap=Bitmap.createBitmap(source);val payload=ProjectQrCodec.payload(com.animame.editor.AnimationDocument(duration=MediaSequenceStore.count),MediaSequenceStore.count);val qr=try{ProjectQrCodec.qrBitmap(payload,360)}catch(_:Throwable){null};qr?.let{val c=Canvas(bitmap);val p=Paint(Paint.ANTI_ALIAS_FLAG);c.drawBitmap(it,(bitmap.width-it.width-24).toFloat(),(bitmap.height-it.height-24).toFloat(),p);it.recycle()};contentResolver.openOutputStream(uri)?.use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle();Toast.makeText(this,"PNG exportado com QR.",Toast.LENGTH_SHORT).show()}
    private fun exportPngZip(uri:Uri?){if(uri==null)return;contentResolver.openOutputStream(uri)?.use{zos->ZipOutputStream(BufferedOutputStream(zos)).use{zip->MediaSequenceStore.all().forEachIndexed{index,b->zip.putNextEntry(ZipEntry("frame_${(index+1).toString().padStart(5,'0')}.png"));b.compress(Bitmap.CompressFormat.PNG,100,zip);zip.closeEntry()};zip.putNextEntry(ZipEntry("project.json"));val json=ProjectQrCodec.payload(com.animame.editor.AnimationDocument(duration=MediaSequenceStore.count),MediaSequenceStore.count);zip.write(json.toByteArray());zip.closeEntry()}};Toast.makeText(this,"Sequência PNG exportada.",Toast.LENGTH_SHORT).show()}
    private fun exportGif(uri:Uri?){if(uri==null)return;contentResolver.openOutputStream(uri)?.use{GifSequenceEncoder.encode(MediaSequenceStore.all(),it,42)};Toast.makeText(this,"GIF exportado.",Toast.LENGTH_SHORT).show()}
    private fun exportMp4(uri:Uri?){if(uri==null)return;val temp=java.io.File(cacheDir,"animame-export.mp4");try{VideoSequenceEncoder.encode(MediaSequenceStore.all(),temp,24);contentResolver.openOutputStream(uri)?.use{temp.inputStream().use{src->src.copyTo(it)}};Toast.makeText(this,"MP4 H.264 exportado.",Toast.LENGTH_SHORT).show()}catch(e:Throwable){Toast.makeText(this,"MP4 não suportado neste dispositivo: ${e.message}",Toast.LENGTH_LONG).show()}finally{temp.delete()}}

    private fun decode(uri:Uri):Bitmap?=try{contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it)}?.let{scale(it,1280)}}catch(_:Throwable){null}
    private fun scale(b:Bitmap,max:Int):Bitmap{val m=maxOf(b.width,b.height);if(m<=max)return b;val s=max.toFloat()/m;val n=Bitmap.createScaledBitmap(b,(b.width*s).toInt().coerceAtLeast(1),(b.height*s).toInt().coerceAtLeast(1),true);if(n!==b)b.recycle();return n}
    private fun displayName(uri:Uri):String{contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use{if(it.moveToFirst())return it.getString(0)}};return uri.toString()}
}
