package com.animame.editor

import android.content.Context
import java.io.File

/** Crash-safe project checkpoint manager. */
object AutosaveManager {
    private const val FILE="anima-me.autosave"
    fun checkpoint(context:Context,payload:String){
        val dir=File(context.filesDir,"recovery");if(!dir.exists())dir.mkdirs()
        val tmp=File(dir,"$FILE.tmp");val dst=File(dir,FILE)
        tmp.writeText(payload,Charsets.UTF_8)
        if(dst.exists())dst.delete()
        tmp.renameTo(dst)
    }
    fun hasRecovery(context:Context)=File(File(context.filesDir,"recovery"),FILE).exists()
    fun read(context:Context):String?=runCatching{File(File(context.filesDir,"recovery"),FILE).readText(Charsets.UTF_8)}.getOrNull()
    fun clear(context:Context){File(File(context.filesDir,"recovery"),FILE).delete()}
}
