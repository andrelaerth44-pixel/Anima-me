package com.animame.editor

import android.graphics.PointF
import kotlin.math.exp
import kotlin.math.hypot

/** Causal one-euro style filter: no future samples, low latency while drawing. */
class LowLatencyStabilizer(private val minCutoff:Float=1.2f, private val beta:Float=0.015f) {
    private var initialized=false
    private var x=0f; private var y=0f; private var vx=0f; private var vy=0f
    private var lastTime=0L
    fun reset(){initialized=false;lastTime=0L}
    fun update(point:PointF,timeMs:Long):PointF{
        if(!initialized){initialized=true;x=point.x;y=point.y;lastTime=timeMs;return PointF(x,y)}
        val dt=((timeMs-lastTime).coerceAtLeast(1L)/1000f).coerceIn(.001f,.1f)
        val rawVx=(point.x-x)/dt; val rawVy=(point.y-y)/dt
        val speed=hypot(rawVx,rawVy)
        val cutoff=minCutoff+beta*speed
        val a=1f-exp((-2f*Math.PI*cutoff*dt).toDouble()).toFloat()
        vx += (rawVx-vx)*a; vy += (rawVy-vy)*a
        x += (point.x-x)*(a.coerceIn(.05f,.95f)); y += (point.y-y)*(a.coerceIn(.05f,.95f))
        lastTime=timeMs
        return PointF(x,y)
    }
}
