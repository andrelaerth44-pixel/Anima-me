package com.animame.editor

data class CameraKeyframe(val frame:Int,val x:Float=0f,val y:Float=0f,val scale:Float=1f,val rotationDeg:Float=0f,val opacity:Float=1f)
data class CameraState(val x:Float=0f,val y:Float=0f,val scale:Float=1f,val rotationDeg:Float=0f,val opacity:Float=1f)
enum class CameraInterpolation { STEP, LINEAR, SMOOTH }

/** Deterministic composition camera for animation playback/export. */
class CameraTrack(keyframes:List<CameraKeyframe> = emptyList(), val interpolation:CameraInterpolation=CameraInterpolation.SMOOTH) {
    private val frames=keyframes.sortedBy{it.frame}.toMutableList()
    fun all():List<CameraKeyframe> = frames.toList()
    fun set(k:CameraKeyframe){ val i=frames.indexOfFirst{it.frame==k.frame}; if(i>=0) frames[i]=k else frames.add(k); frames.sortBy{it.frame} }
    fun remove(frame:Int){ frames.removeAll{it.frame==frame} }
    fun evaluate(frame:Float):CameraState {
        if(frames.isEmpty()) return CameraState()
        if(frame<=frames.first().frame) return frames.first().state()
        if(frame>=frames.last().frame) return frames.last().state()
        val hi=frames.indexOfFirst{it.frame>=frame}; val a=frames[hi-1]; val b=frames[hi]
        val raw=((frame-a.frame)/(b.frame-a.frame)).coerceIn(0f,1f)
        val t=when(interpolation){CameraInterpolation.STEP->0f;CameraInterpolation.LINEAR->raw;CameraInterpolation.SMOOTH->raw*raw*(3f-2f*raw)}
        return CameraState(lerp(a.x,b.x,t),lerp(a.y,b.y,t),lerp(a.scale,b.scale,t).coerceAtLeast(.0001f),lerpAngle(a.rotationDeg,b.rotationDeg,t),lerp(a.opacity,b.opacity,t).coerceIn(0f,1f))
    }
    private fun CameraKeyframe.state()=CameraState(x,y,scale,rotationDeg,opacity)
    private fun lerp(a:Float,b:Float,t:Float)=a+(b-a)*t
    private fun lerpAngle(a:Float,b:Float,t:Float):Float{var d=(b-a)%360f;if(d>180)d-=360f;if(d< -180)d+=360f;return a+d*t}
}
