package com.mitanshm.fitfindr.domain

import kotlin.math.min
import kotlin.random.Random

/**
 * Pure-Kotlin k-means dominant-color extraction. Deliberately has zero
 * Android dependency (no `android.graphics.Bitmap`) so it is directly
 * unit-testable on the JVM without Robolectric or a device — per the PRD's
 * explicit requirement. An Android-side adapter (not in this file) is
 * responsible for turning a `Bitmap` region into the `IntArray` of packed
 * `0xRRGGBB` pixels this class consumes.
 */
object ColorExtractor {

    /**
     * A cluster's centroid color, packed as `0xRRGGBB`, and the fraction of
     * input pixels assigned to it (0.0..1.0), sorted by [share] descending.
     */
    data class ColorCluster(val rgb: Int, val share: Double)

    /**
     * Runs k-means over [pixels] (each an `Int` packed as `0xRRGGBB`, alpha
     * ignored) and returns up to [k] clusters sorted by descending share.
     *
     * - Empty input returns an empty list.
     * - If there are fewer distinct colors than [k], returns one cluster per
     *   distinct color (never invents empty clusters).
     * - Deterministic for a given [seed]: centroid initialization uses a
     *   seeded [Random], and pixels are always assigned to the
     *   lowest-index centroid on distance ties, so results are reproducible
     *   across runs and platforms.
     */
    fun extract(
        pixels: IntArray,
        k: Int = 3,
        maxIterations: Int = 20,
        seed: Long = 42L,
    ): List<ColorCluster> {
        require(k > 0) { "k must be positive" }
        if (pixels.isEmpty()) return emptyList()

        val distinct = pixels.toHashSet()
        val effectiveK = min(k, distinct.size)

        val random = Random(seed)
        var centroids = initCentroids(pixels, effectiveK, random)

        var assignments = IntArray(pixels.size)
        var iteration = 0
        while (iteration < maxIterations) {
            iteration++
            var changed = false
            for (i in pixels.indices) {
                val nearest = nearestCentroid(pixels[i], centroids)
                if (assignments[i] != nearest) {
                    assignments[i] = nearest
                    changed = true
                }
            }
            centroids = recomputeCentroids(pixels, assignments, centroids)
            if (!changed) break
        }

        val counts = IntArray(centroids.size)
        for (a in assignments) counts[a]++

        return centroids.indices
            .filter { counts[it] > 0 }
            .map { i -> ColorCluster(rgb = centroids[i], share = counts[i].toDouble() / pixels.size) }
            .sortedByDescending { it.share }
    }

    private fun initCentroids(
        pixels: IntArray,
        k: Int,
        random: Random,
    ): IntArray {
        val distinct = pixels.distinct()
        return if (distinct.size <= k) {
            distinct.toIntArray()
        } else {
            distinct.shuffled(random).take(k).toIntArray()
        }
    }

    private fun nearestCentroid(
        pixel: Int,
        centroids: IntArray,
    ): Int {
        var bestIndex = 0
        var bestDist = Int.MAX_VALUE
        for (i in centroids.indices) {
            val d = distanceSquared(pixel, centroids[i])
            if (d < bestDist) {
                bestDist = d
                bestIndex = i
            }
        }
        return bestIndex
    }

    private fun distanceSquared(
        a: Int,
        b: Int,
    ): Int {
        val dr = ((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)
        val dg = ((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)
        val db = (a and 0xFF) - (b and 0xFF)
        return dr * dr + dg * dg + db * db
    }

    private fun recomputeCentroids(
        pixels: IntArray,
        assignments: IntArray,
        previous: IntArray,
    ): IntArray {
        val sumR = LongArray(previous.size)
        val sumG = LongArray(previous.size)
        val sumB = LongArray(previous.size)
        val count = LongArray(previous.size)

        for (i in pixels.indices) {
            val cluster = assignments[i]
            val p = pixels[i]
            sumR[cluster] += (p shr 16) and 0xFF
            sumG[cluster] += (p shr 8) and 0xFF
            sumB[cluster] += p and 0xFF
            count[cluster]++
        }

        return IntArray(previous.size) { i ->
            if (count[i] == 0L) {
                previous[i]
            } else {
                val r = (sumR[i] / count[i]).toInt()
                val g = (sumG[i] / count[i]).toInt()
                val b = (sumB[i] / count[i]).toInt()
                (r shl 16) or (g shl 8) or b
            }
        }
    }

    /** Formats a packed `0xRRGGBB` int as `#RRGGBB`. */
    fun toHex(rgb: Int): String = "#%06X".format(rgb and 0xFFFFFF)
}
