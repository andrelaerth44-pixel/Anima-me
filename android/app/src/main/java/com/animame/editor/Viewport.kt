package com.animame.editor

import android.graphics.Matrix
import android.view.MotionEvent
import kotlin.math.atan2
import kotlin.math.hypot

class Viewport {
    var scale=1f; var rotation=0f; var tx=0f; var ty=0f
    private var lastDistance=0f; private var lastAngle=0f; private var lastMidX=0f; private var lastMidY=0f
    fun reset(){scale=1f;rotation=0f;tx=0f;ty=0f}
    fun matrix(canvasW:Float,canvasH:Float):Matrix=Matrix().apply{postTranslate(tx,ty);postScale(scale,scale,canvasW/2f,canvasH/2f);postRotate(rotation,canvasW/2f,canvasH/2f)}
    fun gesture(e:MotionEvent):Boolean{
        if(e.pointerCount<2)return false
        val x1=e.getX(0);val y1=e.getY(0);val x2=e.getX(1);val y2=e.getY(1)
        val mx=(x1+x2)/2f;val my=(y1+y2)/2f;val d=hypot(x2-x1,y2-y1);val a=Math.toDegrees(atan2((y2-y1).toDouble(),(x2-x1).toDouble())).toFloat()
        when(e.actionMasked){MotionEvent.ACTION_POINTER_DOWN->{lastDistance=d;lastAngle=a;lastMidX=mx;lastMidY=my};MotionEvent.ACTION_MOVE->{if(lastDistance>0){scale=(scale*(d/lastDistance)).coerceIn(.2f,8f);rotation+=a-lastAngle;tx+=mx-lastMidX;ty+=my-lastMidY};lastDistance=d;lastAngle=a;lastMidX=mx;lastMidY=my};MotionEvent.ACTION_POINTER_UP->{lastDistance=0f}}
        return true
    }
}