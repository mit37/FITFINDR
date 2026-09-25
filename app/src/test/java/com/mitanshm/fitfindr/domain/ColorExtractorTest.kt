package com.mitanshm.fitfindr.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ColorExtractorTest {

    private val red = 0xFF0000
    private val blue = 0x0000FF
    private val green = 0x00FF00

    @Test
    @DisplayName("empty input returns an empty list")
    fun `empty input`() {
        val clusters = ColorExtractor.extract(IntArray(0), k = 3)
        assertTrue(clusters.isEmpty())
    }

    @Test
    @DisplayName("a single repeated color returns exactly one cluster covering 100% of pixels")
    fun `all same pixel`() {
        val pixels = IntArray(50) { red }
        val clusters = ColorExtractor.extract(pixels, k = 3)

        assertEquals(1, clusters.size)
        assertEquals(red, clusters[0].rgb)
        assertEquals(1.0, clusters[0].share, 1e-9)
    }

    @Test
    @DisplayName("two well-separated clusters of equal size are found with roughly equal share")
    fun `two clear clusters equal size`() {
        val pixels = IntArray(200)
        for (i in 0 until 100) pixels[i] = red
        for (i in 100 until 200) pixels[i] = blue

        val clusters = ColorExtractor.extract(pixels, k = 2)

        assertEquals(2, clusters.size)
        val rgbs = clusters.map { it.rgb }.toSet()
        assertEquals(setOf(red, blue), rgbs)
        clusters.forEach { assertEquals(0.5, it.share, 1e-9) }
    }

    @Test
    @DisplayName("an unbalanced two-cluster input reports the majority cluster first, by share")
    fun `unbalanced clusters sorted by share descending`() {
        val pixels = IntArray(100)
        for (i in 0 until 80) pixels[i] = red
        for (i in 80 until 100) pixels[i] = blue

        val clusters = ColorExtractor.extract(pixels, k = 2)

        assertEquals(2, clusters.size)
        assertEquals(red, clusters[0].rgb)
        assertEquals(0.8, clusters[0].share, 1e-9)
        assertEquals(blue, clusters[1].rgb)
        assertEquals(0.2, clusters[1].share, 1e-9)
    }

    @Test
    @DisplayName("requesting more clusters than distinct colors never invents empty clusters")
    fun `k larger than distinct colors`() {
        val pixels = IntArray(30) { red }
        val clusters = ColorExtractor.extract(pixels, k = 5)

        assertEquals(1, clusters.size)
    }

    @Test
    @DisplayName("three well-separated clusters are all recovered")
    fun `three clear clusters`() {
        val pixels = IntArray(300)
        for (i in 0 until 100) pixels[i] = red
        for (i in 100 until 200) pixels[i] = green
        for (i in 200 until 300) pixels[i] = blue

        val clusters = ColorExtractor.extract(pixels, k = 3)

        assertEquals(3, clusters.size)
        assertEquals(setOf(red, green, blue), clusters.map { it.rgb }.toSet())
        clusters.forEach { assertEquals(1.0 / 3.0, it.share, 1e-6) }
    }

    @Test
    @DisplayName("extraction is deterministic for a fixed seed across repeated runs")
    fun `deterministic for fixed seed`() {
        val pixels = IntArray(150)
        for (i in 0 until 50) pixels[i] = red
        for (i in 50 until 100) pixels[i] = green
        for (i in 100 until 150) pixels[i] = blue

        val first = ColorExtractor.extract(pixels, k = 3, seed = 7L)
        val second = ColorExtractor.extract(pixels, k = 3, seed = 7L)

        assertEquals(first, second)
    }

    @Test
    @DisplayName("k must be positive")
    fun `rejects non-positive k`() {
        val pixels = intArrayOf(red)
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            ColorExtractor.extract(pixels, k = 0)
        }
    }

    @Test
    @DisplayName("toHex formats a packed RGB int as an uppercase hex string")
    fun `toHex formatting`() {
        assertEquals("#FF0000", ColorExtractor.toHex(red))
        assertEquals("#00FF00", ColorExtractor.toHex(green))
        assertEquals("#0000FF", ColorExtractor.toHex(blue))
        assertEquals("#000000", ColorExtractor.toHex(0))
    }

    @Test
    @DisplayName("a single pixel input returns one cluster with 100% share")
    fun `single pixel input`() {
        val clusters = ColorExtractor.extract(intArrayOf(green), k = 3)
        assertEquals(1, clusters.size)
        assertEquals(green, clusters[0].rgb)
        assertEquals(1.0, clusters[0].share, 1e-9)
    }
}
