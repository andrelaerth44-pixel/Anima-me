package com.animame

/** Small shared state for the professional geometry/selection tools. */
object DrawingToolState {
    enum class LassoMode { NEW, ADD, SUBTRACT, INTERSECT }
    enum class ShapeMode { RECTANGLE, ELLIPSE, ARROW }

    @Volatile var lassoMode: LassoMode = LassoMode.NEW
    @Volatile var shapeMode: ShapeMode = ShapeMode.RECTANGLE

    fun nextLassoMode(): LassoMode {
        lassoMode = when (lassoMode) {
            LassoMode.NEW -> LassoMode.ADD
            LassoMode.ADD -> LassoMode.SUBTRACT
            LassoMode.SUBTRACT -> LassoMode.INTERSECT
            LassoMode.INTERSECT -> LassoMode.NEW
        }
        return lassoMode
    }

    fun nextShapeMode(): ShapeMode {
        shapeMode = when (shapeMode) {
            ShapeMode.RECTANGLE -> ShapeMode.ELLIPSE
            ShapeMode.ELLIPSE -> ShapeMode.ARROW
            ShapeMode.ARROW -> ShapeMode.RECTANGLE
        }
        return shapeMode
    }
}
