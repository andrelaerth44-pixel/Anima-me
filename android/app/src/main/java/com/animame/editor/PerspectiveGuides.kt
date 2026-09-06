package com.animame.editor

import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class RulerKind { HORIZONTAL, VERTICAL, GRID, FIELD_GUIDE, SAFE_AREA }
enum class PerspectiveMode { OFF, ONE_POINT, TWO_POINT, THREE_POINT }

data class GuideLine(val horizontal: Boolean, var position: Float, var visible: Boolean = true)

data class PerspectiveState(
    var mode: PerspectiveMode = PerspectiveMode.OFF,
    var horizon: Float = 0.5f,
    var vp1: PointF = PointF(0.5f, 0.5f),
    var vp2: PointF = PointF(0.5f, 0.5f),
    var vp3: PointF = PointF(0.5f, 0.1f),
    var snap: Boolean = true,
    var showGrid: Boolean = true,
    var subdivisions: Int = 12
)

/** OpenToonz-inspired ruler/guide state. Coordinates are normalized to the viewer. */
class PerspectiveGuideEngine {
    val state = PerspectiveState()
    val customGuides = mutableListOf<GuideLine>()
    var rulersVisible = true
    var guidesVisible = true
    var fieldGuideVisible = false
    var safeAreaVisible = false
    var gridVisible = false
    var gridSpacing = 0.05f

    fun resetPerspective() {
        state.mode = PerspectiveMode.OFF
        state.vp1.set(0.5f, 0.5f)
        state.vp2.set(0.5f, 0.5f)
        state.vp3.set(0.5f, 0.1f)
    }

    fun vanishingPoints(): List<PointF> = when (state.mode) {
        PerspectiveMode.ONE_POINT -> listOf(state.vp1)
        PerspectiveMode.TWO_POINT -> listOf(state.vp1, state.vp2)
        PerspectiveMode.THREE_POINT -> listOf(state.vp1, state.vp2, state.vp3)
        PerspectiveMode.OFF -> emptyList()
    }

    /** Snap a canvas point to the closest active perspective ray. */
    fun snap(point: PointF): PointF {
        if (!state.snap || state.mode == PerspectiveMode.OFF) return PointF(point.x, point.y)
        val points = vanishingPoints()
        if (points.isEmpty()) return PointF(point.x, point.y)
        var best = PointF(point.x, point.y)
        var bestError = Float.MAX_VALUE
        for (vp in points) {
            val dx = point.x - vp.x
            val dy = point.y - vp.y
            val length = sqrt(dx * dx + dy * dy).coerceAtLeast(0.0001f)
            val angle = atan2(dy, dx)
            val step = Math.PI.toFloat() / state.subdivisions.coerceAtLeast(4)
            val snappedAngle = kotlin.math.round(angle / step) * step
            val sx = vp.x + cos(snappedAngle) * length
            val sy = vp.y + sin(snappedAngle) * length
            val error = abs(sx - point.x) + abs(sy - point.y)
            if (error < bestError) {
                bestError = error
                best = PointF(sx, sy)
            }
        }
        return best
    }

    fun addHorizontalGuide(y: Float) { customGuides += GuideLine(true, y.coerceIn(0f, 1f)) }
    fun addVerticalGuide(x: Float) { customGuides += GuideLine(false, x.coerceIn(0f, 1f)) }
    fun removeGuide(index: Int) { if (index in customGuides.indices) customGuides.removeAt(index) }
}
