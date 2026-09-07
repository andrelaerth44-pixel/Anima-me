package com.animame.editor

/** Legacy helper retained for compatibility; live smoothing is handled by StrokeProcessingEngine. */
object StrokeSmoothing {
    fun smooth(stroke: StrokeData): StrokeData {
        if (stroke.samples.size < 3) return stroke
        val s=stroke.samples; val out=ArrayList<StrokeSample>(s.size); out+=s.first()
        for(i in 1 until s.lastIndex){val a=s[i-1];val b=s[i];val c=s[i+1];out+=b.copy(x=(a.x+b.x*2f+c.x)/4f,y=(a.y+b.y*2f+c.y)/4f)}
        out+=s.last(); return stroke.copy(samples=out.toMutableList())
    }
}