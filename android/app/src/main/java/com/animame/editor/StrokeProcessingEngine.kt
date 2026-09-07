package com.animame.editor

import kotlin.math.hypot

object StrokeProcessingEngine {
    fun process(input: List<Stabilizer.Sample>, smoothing: Boolean, stabilizerPercent: Float, realTime: Boolean = true): List<Stabilizer.Sample> {
        if (input.isEmpty()) return emptyList()
        val stabilized = if (stabilizerPercent > 0f) stabilize(input, stabilizerPercent, realTime) else input.map { it.copy(point = it.point.copy()) }
        return if (smoothing) smooth(stabilized) else stabilized
    }

    fun stabilize(input: List<Stabilizer.Sample>, percent: Float, realTime: Boolean): List<Stabilizer.Sample> {
        if (input.size < 2 || percent <= 0f) return input.map { it.copy(point = it.point.copy()) }
        val strength = (percent / 100f).coerceIn(0f, 1f)
        val out = ArrayList<Stabilizer.Sample>(input.size)
        var x = input.first().point.x; var y = input.first().point.y
        out += input.first().copy(point = input.first().point.copy())
        for (i in 1 until input.size) {
            val raw = input[i]; val prev = input[i - 1]
            val speed = hypot((raw.point.x - prev.point.x).toDouble(), (raw.point.y - prev.point.y).toDouble()).toFloat()
            val fastBoost = if (realTime) (speed / 24f).coerceIn(0f, 1f) * 0.35f else 0f
            val correction = (strength * 0.72f + fastBoost * strength).coerceIn(0f, 0.94f)
            val follow = 1f - correction
            x += (raw.point.x - x) * follow; y += (raw.point.y - y) * follow
            out += raw.copy(point = raw.point.copy(x = x, y = y))
        }
        out[out.lastIndex] = input.last().copy(point = input.last().point.copy())
        return out
    }

    fun smooth(input: List<Stabilizer.Sample>): List<Stabilizer.Sample> {
        if (input.size < 3) return input.map { it.copy(point = it.point.copy()) }
        val out = ArrayList<Stabilizer.Sample>(input.size)
        out += input.first().copy(point = input.first().point.copy())
        for (i in 1 until input.lastIndex) {
            val a=input[i-1]; val b=input[i]; val c=input[i+1]
            val ab=hypot((b.point.x-a.point.x).toDouble(),(b.point.y-a.point.y).toDouble()).toFloat()
            val bc=hypot((c.point.x-b.point.x).toDouble(),(c.point.y-b.point.y).toDouble()).toFloat()
            val total=(ab+bc).coerceAtLeast(.001f); val wa=.25f+.25f*(bc/total); val wb=.5f; val wc=.25f+.25f*(ab/total)
            out += b.copy(point=b.point.copy(x=a.point.x*wa+b.point.x*wb+c.point.x*wc,y=a.point.y*wa+b.point.y*wb+c.point.y*wc))
        }
        out += input.last().copy(point=input.last().point.copy()); return out
    }

    fun resample(input: List<Stabilizer.Sample>, spacingPx: Float): List<Stabilizer.Sample> {
        if(input.size<2||spacingPx<=0f)return input.map{it.copy(point=it.point.copy())}
        val out=ArrayList<Stabilizer.Sample>();out+=input.first().copy(point=input.first().point.copy());var carry=0f
        for(i in 1 until input.size){val a=input[i-1];val b=input[i];val dx=b.point.x-a.point.x;val dy=b.point.y-a.point.y;val d=hypot(dx.toDouble(),dy.toDouble()).toFloat();if(d<=0f)continue;var traveled=spacingPx-carry;while(traveled<d){val t=traveled/d;out+=a.copy(point=a.point.copy(x=a.point.x+dx*t,y=a.point.y+dy*t,));traveled+=spacingPx};carry=(d-(traveled-spacingPx)).coerceIn(0f,spacingPx)}
        out+=input.last().copy(point=input.last().point.copy());return out
    }
}