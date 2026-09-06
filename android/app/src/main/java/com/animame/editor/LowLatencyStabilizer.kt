package com.animame.editor

import android.graphics.PointF
import kotlin.math.exp
import kotlin.math.hypot

/** Causal one-euro style filter tuned for direct finger/stylus following. It never uses future samples. */
class LowLatencyStabilizer(private val minCutoff:Float=3.8f, private val beta:Float=0.045f) {
    private var initialized=false
    private var x=0f; private var y=0f; private var vx=0f; private var vy=0f
    private var lastTime=0L

    fun reset(){initialized=false;lastTime=0L;vx=0f;vy=0f}

    fun update(point:PointF,timeMs:Long):PointF{
        if(!initialized){initialized=true;x=point.x;y=point.y;lastTime=timeMs;return PointF(x,y)}
        val dt=((timeMs-lastTime).coerceAtLeast(1L)/1000f).coerceIn(.001f,.05f)
        val rawVx=(point.x-x)/dt; val rawVy=(point.y-y)/dt
        val speed=hypot(rawVx,rawVy)
        val cutoff=minCutoff+beta*speed
        val a=1f-exp((-2f*Math.PI*cutoff*dt).toDouble()).toFloat()
        vx += (rawVx-vx)*a
        vy += (rawVy-vy)*a
        val gain=a.coerceIn(.12f,.98f)
        x += (point.x-x)*gain
        y += (point.y-y)*gain
        lastTime=timeMs
        return PointF(x,y)
    }
}
