package com.mitanshm.fitfindr.eval

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class EvalMetricsTest {

    @Test
    @DisplayName("jsonValidity counts strict and post-repair successes independently")
    fun `json validity counts independently`() {
        val validJson = """{"styleLabel":"smart casual","garments":[],"palette":[]}"""
        val trailingComma = """{"styleLabel":"smart casual","garments":[],"palette":[],}"""
        val unparseable = "not json at all"

        val result = EvalMetrics.jsonValidity(listOf(validJson, trailingComma, unparseable))

        assertEquals(3, result.totalCases)
        assertEquals(1, result.strictValidCount) // only validJson parses strictly
        assertEquals(2, result.validAfterRepairCount) // validJson + trailingComma parse after repair
        assertEquals(1.0 / 3, result.strictValidRate, 1e-9)
        assertEquals(2.0 / 3, result.postRepairValidRate, 1e-9)
    }

    @Test
    @DisplayName("jsonValidity of an empty list reports zero rates without dividing by zero")
    fun `json validity empty list`() {
        val result = EvalMetrics.jsonValidity(emptyList())
        assertEquals(0, result.totalCases)
        assertEquals(0.0, result.strictValidRate)
        assertEquals(0.0, result.postRepairValidRate)
    }

    @Test
    @DisplayName("garmentTypeScore is perfect when predicted exactly matches expected")
    fun `garment type score perfect match`() {
        val score = EvalMetrics.garmentTypeScore(listOf("jacket", "trousers"), listOf("jacket", "trousers"))
        assertEquals(1.0, score.precision)
        assertEquals(1.0, score.recall)
        assertEquals(1.0, score.f1)
    }

    @Test
    @DisplayName("garmentTypeScore is case-insensitive")
    fun `garment type score is case insensitive`() {
        val score = EvalMetrics.garmentTypeScore(listOf("JACKET"), listOf("jacket"))
        assertEquals(1.0, score.precision)
        assertEquals(1.0, score.recall)
    }

    @Test
    @DisplayName("garmentTypeScore penalizes an extra predicted garment (precision) and a missed one (recall)")
    fun `garment type score partial match`() {
        // predicted: jacket, shoes, hat (extra) ; expected: jacket, shoes, trousers (missed)
        val score = EvalMetrics.garmentTypeScore(listOf("jacket", "shoes", "hat"), listOf("jacket", "shoes", "trousers"))
        assertEquals(2.0 / 3, score.precision, 1e-9)
        assertEquals(2.0 / 3, score.recall, 1e-9)
    }

    @Test
    @DisplayName("garmentTypeScore treats duplicate predictions as separate true-positive opportunities, not free matches")
    fun `garment type score duplicate predictions`() {
        // predicted "shoes" twice, only one "shoes" expected -> one true positive, one false positive
        val score = EvalMetrics.garmentTypeScore(listOf("shoes", "shoes"), listOf("shoes"))
        assertEquals(0.5, score.precision)
        assertEquals(1.0, score.recall)
    }

    @Test
    @DisplayName("garmentTypeScore of two empty lists is a perfect (vacuous) match, not a divide-by-zero")
    fun `garment type score both empty`() {
        val score = EvalMetrics.garmentTypeScore(emptyList(), emptyList())
        assertEquals(1.0, score.precision)
        assertEquals(1.0, score.recall)
        assertEquals(1.0, score.f1)
    }

    @Test
    @DisplayName("colorNameMatchesPixelExtraction matches on an exact hex-to-name lookup")
    fun `color cross check matches`() {
        val hexToName = mapOf("#5B6B3B" to "olive green", "#2B2B2B" to "charcoal")
        assertEquals(
            true,
            EvalMetrics.colorNameMatchesPixelExtraction("olive green", listOf("#5B6B3B", "#2B2B2B"), hexToName),
        )
    }

    @Test
    @DisplayName("colorNameMatchesPixelExtraction is false when no swatch's known name matches")
    fun `color cross check no match`() {
        val hexToName = mapOf("#5B6B3B" to "olive green")
        assertEquals(
            false,
            EvalMetrics.colorNameMatchesPixelExtraction("charcoal", listOf("#5B6B3B"), hexToName),
        )
    }
}
