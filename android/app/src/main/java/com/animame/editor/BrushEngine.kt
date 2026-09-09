package com.animame.editor

import kotlin.math.*
import kotlin.random.Random

object BrushEngine {
    data class Stamp(val x: Float, val y: Float, val size: Float, val alpha: Float, val angle: Float, val colorShift: Float)
    fun stamps(samples: List<StrokeSample>, settings: BrushSettings, seed: Long = 0L): List<Stamp> {
        if (samples.isEmpty()) return emptyList()
        val s = settings.normalized(); val rng = Random(seed xor s.id.hashCode().toLong()); val out = ArrayList<Stamp>()
        var prev: StrokeSample? = null; var distance = Float.POSITIVE_INFINITY
        samples.forEach { p ->
            val q=prev; val dx=if(q==null)0f else p.x-q.x; val dy=if(q==null)0f else p.y-q.y
            val speed=if(q==null)0f else hypot(dx,dy)/max(1L,p.timeMs-q.timeMs).toFloat(); val sn=(speed/2f).coerceIn(0f,1f)
            distance += hypot(dx,dy); val pressure=p.pressure.coerceIn(0f,1f)
            val size=s.size*lerp(s.minSizeFactor,1f,pressure)*lerp(1f,s.speedSizeFactor,sn)
            val alpha=s.opacity*lerp(s.minOpacity,1f,pressure)*lerp(1f,s.speedOpacityFactor,sn)*s.fadeOpacity
            val jitter=s.jitterPosition*size; val jx=(rng.nextFloat()*2f-1f)*jitter; val jy=(rng.nextFloat()*2f-1f)*jitter
            if(prev==null || distance >= max(.5f,s.spacing*size)) { out += Stamp(p.x+jx,p.y+jy,size.coerceIn(.25f,4096f),alpha.coerceIn(0f,1f),s.initialAngle+(if(s.followRotation) atan2(dy,dx) else 0f)+s.rotationJitter*(rng.nextFloat()*2f-1f),s.hueJitter*(rng.nextFloat()*2f-1f)); distance=0f }
            prev=p
        }
        return out
    }
    fun smooth(samples: List<StrokeSample>, strength: Float): List<StrokeSample> { val k=strength.coerceIn(0f,1f); if(samples.size<3||k==0f)return samples; return samples.mapIndexed{ i,p->if(i==0||i==samples.lastIndex)p else {val a=samples[i-1];val b=samples[i+1];StrokeSample(lerp(p.x,(a.x+b.x)/2,k),lerp(p.y,(a.y+b.y)/2,k),p.pressure,p.timeMs)}} }
    private fun lerp(a:Float,b:Float,t:Float)=a+(b-a)*t
}
