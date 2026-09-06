package com.animame.editor

/** Timeline utilities: exposure, insertion, deletion and camera interpolation. */
object TimelineEngine {
    fun exposureEnd(start:Int, exposure:Int, duration:Int):Int = (start + exposure.coerceAtLeast(1) - 1).coerceAtMost(duration-1)
    fun frameFromTime(seconds:Float,fps:Int,duration:Int):Int=(seconds.coerceAtLeast(0f)*fps.coerceAtLeast(1)).toInt().coerceIn(0,duration-1)
    fun timeFromFrame(frame:Int,fps:Int):Float=frame.coerceAtLeast(0)/fps.coerceAtLeast(1).toFloat()

    fun <T> insert(map:MutableMap<Int,T>,at:Int):MutableMap<Int,T>{
        val out=linkedMapOf<Int,T>();map.entries.sortedByDescending{it.key}.forEach{(f,v)->out[if(f>=at)f+1 else f]=v};return out
    }
    fun <T> remove(map:MutableMap<Int,T>,at:Int):MutableMap<Int,T>{
        val out=linkedMapOf<Int,T>();map.entries.sortedBy{it.key}.forEach{(f,v)->if(f!=at)out[if(f>at)f-1 else f]=v};return out
    }
}

object CameraInterpolation {
    fun linear(a:AnimationCamera,b:AnimationCamera,t:Float)=AnimationCamera(
        x=a.x+(b.x-a.x)*t.coerceIn(0f,1f),y=a.y+(b.y-a.y)*t.coerceIn(0f,1f),
        scale=a.scale+(b.scale-a.scale)*t.coerceIn(0f,1f),rotation=a.rotation+shortest(a.rotation,b.rotation)*t.coerceIn(0f,1f)
    )
    private fun shortest(a:Float,b:Float):Float{var d=(b-a)%360f;if(d>180)d-=360f;if(d<-180)d+=360f;return d}
}
