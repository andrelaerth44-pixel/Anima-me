package com.animame.editor

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ProjectPackage {
 fun importImageSequence(context:Context,uris:List<Uri>,width:Int,height:Int):List<String>{val dir=File(context.filesDir,"imports/images_${System.currentTimeMillis()}").apply{mkdirs()};val out=ArrayList<String>();uris.forEachIndexed{i,u->context.contentResolver.openInputStream(u)?.use{input->val f=File(dir,"frame_%06d.png".format(i));val bmp=BitmapFactory.decodeStream(input)?:return@forEachIndexed;f.outputStream().use{bmp.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bmp.recycle();out+=f.absolutePath}};return out}
 fun copyAudio(context:Context,uri:Uri):String?{val dir=File(context.filesDir,"audio").apply{mkdirs()};val f=File(dir,"audio_${System.currentTimeMillis()}.bin");return runCatching{context.contentResolver.openInputStream(uri)?.use{input->f.outputStream().use{input.copyTo(it)}};f.absolutePath}.getOrNull()}
 fun export(context:Context,output:Uri,document:AnimationDocument){val root=File(context.cacheDir,"package_${System.currentTimeMillis()}").apply{mkdirs()};try{val assets=File(root,"assets").apply{mkdirs()};val json=DocumentCodec.encode(document);val remapped=json.replace("${context.filesDir.absolutePath}/","assets/");File(root,"document.json").writeText(remapped);val paths=Regex("assets/[^\\\"} ,]+(?:\\\\[^\\\"} ,]+)*").findAll(remapped).map{it.value}.toSet();paths.forEach{rel->val source=File(context.filesDir,rel.removePrefix("assets/"));if(source.exists()&&source.isFile){val target=File(root,rel);target.parentFile?.mkdirs();source.copyTo(target,overwrite=true)}};context.contentResolver.openOutputStream(output)?.use{os->ZipOutputStream(os).use{zip->root.walkTopDown().filter{it.isFile}.forEach{file->val rel=root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar,'/');zip.putNextEntry(ZipEntry(rel));file.inputStream().use{it.copyTo(zip)};zip.closeEntry()}}}}finally{root.deleteRecursively()}}
 fun importPackage(context:Context,uri:Uri):AnimationDocument?{val root=File(context.cacheDir,"unpackage_${System.currentTimeMillis()}").apply{mkdirs()};return try{context.contentResolver.openInputStream(uri)?.use{input->ZipInputStream(input).use{zip->while(true){val e=zip.nextEntry?:break;val target=File(root,e.name);if(!target.canonicalPath.startsWith(root.canonicalPath+File.separator))continue;target.parentFile?.mkdirs();target.outputStream().use{zip.copyTo(it)};zip.closeEntry()}}};val jsonFile=File(root,"document.json");if(!jsonFile.exists())return null;val json=jsonFile.readText();val fixed=json.replace("assets/",context.filesDir.absolutePath+"/");DocumentCodec.decode(fixed)}catch(_:Throwable){null}finally{root.deleteRecursively()}}
}
