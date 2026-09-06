package com.animame.editor

import java.util.UUID

data class AnimationProject(val id:String=UUID.randomUUID().toString(),var name:String="Untitled",var fps:Int=24,var cameraWidth:Int=1280,var cameraHeight:Int=720,var margin:Int=0,var canvasWidth:Int=1280,var canvasHeight:Int=720)

object ProjectStore {
    val projects=mutableListOf<AnimationProject>()
    fun create(name:String,fps:Int,w:Int,h:Int,margin:Int=0):AnimationProject { val p=AnimationProject(name=name,fps=fps,cameraWidth=w,cameraHeight=h,margin=margin,canvasWidth=w+margin*2,canvasHeight=h+margin*2);projects.add(0,p);return p }
}