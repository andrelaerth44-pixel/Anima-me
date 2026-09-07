package com.animame.editor

/** Post-stroke smoothing. Never applied to the live preview. */
object StrokeSmoothing {
    fun smooth(stroke: StrokeData): StrokeData {
        val s = stroke.samples
        if (s.size < 3) return stroke
        val out = ArrayList<Stabilizer.Sample>(s.size)
        out += s.first()
        for (i in 1 until s.lastIndex) {
            val a = s[i - 1]
            val b = s[i]
            val c = s[i + 1]
            out += Stabilizer.Sample(
                x = (a.x + b.x * 2f + c.x) / 4f,
                y = (a.y + b.y * 2f + c.y) / 4f,
                pressure = b.pressure,
                timeMs = b.timeMs,
                tilt = b.tilt,
                orientation = b.orientation
            )
        }
        out += s.last()
        return stroke.copy(samples = out)
    }
}
