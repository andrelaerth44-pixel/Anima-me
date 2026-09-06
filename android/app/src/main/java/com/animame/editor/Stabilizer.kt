package com.animame.editor

import android.graphics.Path
import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Real-time and after-stroke stabilizer for pen input. */
object Stabilizer {
    data class Sample(val point: PointF, val pressure: Float = 1f, val timeMs: Long = 0L, val tilt: Float = 0f, val orientation: Float = 0f)

    fun smooth(points: List<PointF>, strength: Float): Path = smoothSamples(points.map { Sample(it) }, strength, false)

    fun smoothSamples(samples: List<Sample>, strength: Float, realTime: Boolean = true): Path {
        val out = Path()
        if (samples.isEmpty()) return out
        if (samples.size == 1) { out.moveTo(samples[0].point.x, samples[0].point.y); return out }
        val s = strength.coerceIn(0f, 100f) / 100f
        val radius = 1 + (s * if(realTime) 5f else 11f).toInt()
        val filtered = ArrayList<PointF>(samples.size)
        for (i in samples.indices) {
            val a = max(0, i - radius); val b = if (realTime) i else min(samples.lastIndex, i + radius)
            var sx=0f; var sy=0f; var total=0f
            for (j in a..b) { val w=1f/(1f+abs(j-i).toFloat()); sx+=samples[j].point.x*w; sy+=samples[j].point.y*w; total+=w }
            filtered += PointF(sx/total, sy/total)
        }
        out.moveTo(filtered[0].x, filtered[0].y)
        if(filtered.size==2){out.lineTo(filtered[1].x,filtered[1].y);return out}
        for(i in 1 until filtered.lastIndex){val p=filtered[i];val n=filtered[i+1];out.quadTo(p.x,p.y,(p.x+n.x)*.5f,(p.y+n.y)*.5f)}
        out.quadTo(filtered[filtered.lastIndex-1].x,filtered[filtered.lastIndex-1].y,filtered.last().x,filtered.last().y);return out
    }
    fun adaptiveStrength(speedPxPerSecond: Float,minStrength: Float,maxStrength: Float): Float { val t=(1f-speedPxPerSecond.coerceAtLeast(0f)/2500f).coerceIn(0f,1f);return minStrength+(maxStrength-minStrength)*t }
    fun distance(a: PointF,b: PointF): Float = hypot(a.x-b.x,a.y-b.y)
}
