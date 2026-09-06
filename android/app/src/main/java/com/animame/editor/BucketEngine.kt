package com.animame.editor

import android.graphics.Bitmap
import android.graphics.Color
import java.util.ArrayDeque
import kotlin.math.abs

/** Flood fill with anti-alias-aware tolerance, expansion and small-gap closing. */
object BucketEngine {
    data class Settings(
        val strength: Int = 24,
        val expansion: Int = 2,
        val gapRecognition: Boolean = true,
        val gapRadius: Int = 3,
        val underLine: Boolean = true
    )

    fun fill(bitmap: Bitmap, sx: Int, sy: Int, fillColor: Int, settings: Settings = Settings()): Boolean {
        if (sx !in 0 until bitmap.width || sy !in 0 until bitmap.height) return false
        val w = bitmap.width; val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        val target = pixels[sy * w + sx]
        if (same(target, fillColor, 2)) return false

        // Build a barrier mask from non-background ink and dilate it to close tiny gaps.
        val barrier = BooleanArray(w * h)
        for (i in pixels.indices) barrier[i] = inkLike(pixels[i], settings.strength)
        if (settings.gapRecognition && settings.gapRadius > 0) dilate(barrier, w, h, settings.gapRadius)

        val visited = BooleanArray(w * h)
        val q = ArrayDeque<Int>()
        val start = sy * w + sx
        if (barrier[start]) return false
        q.add(start); visited[start] = true
        val region = ArrayList<Int>()
        while (q.isNotEmpty()) {
            val p = q.removeFirst(); region.add(p)
            val x = p % w; val y = p / w
            if (x > 0) visit(p - 1, pixels, target, settings.strength, barrier, visited, q)
            if (x + 1 < w) visit(p + 1, pixels, target, settings.strength, barrier, visited, q)
            if (y > 0) visit(p - w, pixels, target, settings.strength, barrier, visited, q)
            if (y + 1 < h) visit(p + w, pixels, target, settings.strength, barrier, visited, q)
        }
        if (region.isEmpty()) return false

        for (p in region) pixels[p] = fillColor
        if (settings.expansion != 0) {
            val expanded = region.toHashSet()
            val r = abs(settings.expansion).coerceAtMost(8)
            for (p in region) {
                val x = p % w; val y = p / w
                for (dy in -r..r) for (dx in -r..r) {
                    val xx = x + dx; val yy = y + dy
                    if (xx in 0 until w && yy in 0 until h && dx * dx + dy * dy <= r * r) expanded.add(yy * w + xx)
                }
            }
            for (p in expanded) {
                if (settings.underLine || !barrier[p]) pixels[p] = fillColor
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        return true
    }

    private fun visit(p: Int, pixels: IntArray, target: Int, tolerance: Int, barrier: BooleanArray, visited: BooleanArray, q: ArrayDeque<Int>) {
        if (visited[p] || barrier[p]) return
        if (!similar(pixels[p], target, tolerance)) return
        visited[p] = true; q.add(p)
    }

    private fun similar(a: Int, b: Int, t: Int): Boolean =
        abs(Color.red(a) - Color.red(b)) <= t && abs(Color.green(a) - Color.green(b)) <= t && abs(Color.blue(a) - Color.blue(b)) <= t && abs(Color.alpha(a) - Color.alpha(b)) <= t

    private fun same(a: Int, b: Int, t: Int) = similar(a, b, t)

    private fun inkLike(c: Int, threshold: Int): Boolean =
        Color.alpha(c) > 20 && (Color.red(c) < 255 - threshold || Color.green(c) < 255 - threshold || Color.blue(c) < 255 - threshold)

    private fun dilate(mask: BooleanArray, w: Int, h: Int, radius: Int) {
        val src = mask.clone()
        for (y in 0 until h) for (x in 0 until w) {
            var hit = false
            loop@ for (dy in -radius..radius) for (dx in -radius..radius) {
                if (dx * dx + dy * dy > radius * radius) continue
                val xx = x + dx; val yy = y + dy
                if (xx in 0 until w && yy in 0 until h && src[yy * w + xx]) { hit = true; break@loop }
            }
            if (hit) mask[y * w + x] = true
        }
    }
}
