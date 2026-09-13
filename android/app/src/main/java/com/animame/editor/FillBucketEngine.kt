package com.animame.editor

import kotlin.math.abs

/**
 * Standalone flood-fill core for the raster side of the Anima-me editor.
 * It is deliberately independent from Android UI so the same algorithm can
 * later be used by the canvas, import pipeline and export pipeline.
 */
object FillBucketEngine {
    enum class Connectivity { FOUR, EIGHT }

    data class Result(val pixelsChanged: Int, val bounds: Bounds?)
    data class Bounds(val left: Int, val top: Int, val right: Int, val bottom: Int)

    fun fill(
        pixels: IntArray,
        width: Int,
        height: Int,
        startX: Int,
        startY: Int,
        replacementColor: Int,
        tolerance: Int = 0,
        connectivity: Connectivity = Connectivity.FOUR
    ): Result {
        require(width > 0 && height > 0)
        require(pixels.size >= width * height)
        if (startX !in 0 until width || startY !in 0 until height) return Result(0, null)

        val start = pixels[startY * width + startX]
        if (colorsClose(start, replacementColor, tolerance)) return Result(0, null)

        val visited = BooleanArray(width * height)
        val queue = IntArray(width * height)
        var head = 0
        var tail = 0
        queue[tail++] = startY * width + startX
        visited[startY * width + startX] = true

        var changed = 0
        var minX = startX
        var minY = startY
        var maxX = startX
        var maxY = startY

        while (head < tail) {
            val index = queue[head++]
            val x = index % width
            val y = index / width
            if (!colorsClose(pixels[index], start, tolerance)) continue

            pixels[index] = replacementColor
            changed++
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y

            enqueue(x - 1, y)
            enqueue(x + 1, y)
            enqueue(x, y - 1)
            enqueue(x, y + 1)
            if (connectivity == Connectivity.EIGHT) {
                enqueue(x - 1, y - 1)
                enqueue(x + 1, y - 1)
                enqueue(x - 1, y + 1)
                enqueue(x + 1, y + 1)
            }
        }

        return Result(changed, if (changed == 0) null else Bounds(minX, minY, maxX, maxY))

        fun enqueue(x: Int, y: Int) {
            if (x !in 0 until width || y !in 0 until height) return
            val index = y * width + x
            if (!visited[index]) {
                visited[index] = true
                queue[tail++] = index
            }
        }
    }

    private fun colorsClose(a: Int, b: Int, tolerance: Int): Boolean {
        val t = tolerance.coerceIn(0, 255)
        return abs(((a ushr 16) and 0xff) - ((b ushr 16) and 0xff)) <= t &&
            abs(((a ushr 8) and 0xff) - ((b ushr 8) and 0xff)) <= t &&
            abs((a and 0xff) - (b and 0xff)) <= t &&
            abs(((a ushr 24) and 0xff) - ((b ushr 24) and 0xff)) <= t
    }
}
