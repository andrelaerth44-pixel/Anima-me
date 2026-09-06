package com.animame.editor

import android.content.Context
import java.io.File

/** Stores the complete document separately from the lightweight project browser index. */
object ProjectDocumentStore {
    fun save(context:Context,projectId:String,document:AnimationDocument){val dir=File(context.filesDir,"projects").apply{mkdirs()};File(dir,"$projectId.animame.json").writeText(DocumentCodec.encode(document))}
    fun load(context:Context,projectId:String):AnimationDocument?=runCatching{val f=File(context.filesDir,"projects/$projectId.animame.json");if(f.exists())DocumentCodec.decode(f.readText()) else null}.getOrNull()
    fun delete(context:Context,projectId:String){File(context.filesDir,"projects/$projectId.animame.json").delete()}
}
