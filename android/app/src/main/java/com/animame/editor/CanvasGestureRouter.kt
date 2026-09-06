package com.animame.editor

import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.hypot

/** Two-finger canvas navigation: pan + pinch zoom + rotation. */
class CanvasGestureRouter(private val editor: View) {
    private var active = false
    private var lastDistance = 0f
    private var lastAngle = 0f
    private var lastMidX = 0f
    private var lastMidY = 0f

    private val cameraX = editor.javaClass.declaredFields.firstOrNull { it.name == "cameraX" }
    private val cameraY = editor.javaClass.declaredFields.firstOrNull { it.name == "cameraY" }
    private val cameraScale = editor.javaClass.declaredFields.firstOrNull { it.name == "cameraScale" }
    private val cameraRotation = editor.javaClass.declaredFields.firstOrNull { it.name == "cameraRotation" }

    init { listOf(cameraX, cameraY, cameraScale, cameraRotation).forEach { it?.isAccessible = true } }

    fun handle(ev: MotionEvent): Boolean {
        if (ev.pointerCount < 2) {
            if (ev.actionMasked == MotionEvent.ACTION_UP || ev.actionMasked == MotionEvent.ACTION_CANCEL) reset()
            return false
        }
        val p0 = PointF(ev.getX(0), ev.getY(0)); val p1 = PointF(ev.getX(1), ev.getY(1))
        val midX = (p0.x + p1.x) * .5f; val midY = (p0.y + p1.y) * .5f
        val distance = hypot((p1.x-p0.x).toDouble(), (p1.y-p0.y).toDouble()).toFloat().coerceAtLeast(1f)
        val angle = Math.toDegrees(atan2((p1.y-p0.y).toDouble(), (p1.x-p0.x).toDouble())).toFloat()
        if (!active || ev.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            active=true; lastDistance=distance; lastAngle=angle; lastMidX=midX; lastMidY=midY; return true
        }
        val scale=(distance/lastDistance).coerceIn(.90f,1.10f)
        val rotation=normalize(angle-lastAngle)
        update(cameraScale){(it*scale).coerceIn(.05f,32f)}
        update(cameraRotation){it+rotation}
        update(cameraX){it+(midX-lastMidX)}
        update(cameraY){it+(midY-lastMidY)}
        lastDistance=distance; lastAngle=angle; lastMidX=midX; lastMidY=midY
        editor.invalidate(); return true
    }
    private fun update(field: java.lang.reflect.Field?, fn:(Float)->Float){
        if(field==null)return
        val n=field.get(editor) as? Number ?: return
        field.setFloat(editor,fn(n.toFloat()))
    }
    private fun normalize(v0:Float):Float{var v=v0;while(v>180)v-=360;while(v<-180)v+=360;return v}
    private fun reset(){active=false;lastDistance=0f;lastAngle=0f}
}
