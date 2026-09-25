package com.mitanshm.fitfindr.eval

import com.mitanshm.fitfindr.data.inference.FakeVlmEngine
import com.mitanshm.fitfindr.domain.ColorExtractor
import com.mitanshm.fitfindr.domain.ResultParser
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Runs the eval harness's own logic ([EvalMetrics], [ResultParser],
 * [ColorExtractor]) against [FakeVlmEngine] and the existing golden JSON
 * fixtures, entirely on the JVM (no Android SDK, device, or network
 * needed). This is a genuine, reproducible run -- `./gradlew test` (or the
 * standalone JVM verification in `docs/PLAN.md`) actually executes it --
 * but it is a self-check of the harness *mechanism*, NOT a measurement of
 * real model performance: `FakeVlmEngine` always returns the same fixed
 * fixture string instantly, so nothing here reflects Gemma 3n E4B's real
 * accuracy, latency, or memory use on a phone. The numbers this test
 * prints are exactly what `eval/results.json`'s `"harness_self_check"`
 * section reports, labeled as such.
 */
class EvalHarnessSelfCheckTest {

    @Test
    @DisplayName("json validity self-check over the existing golden fixtures matches eval/results.json's harness_self_check numbers")
    fun `json validity self check`() {
        val rawTexts =
            listOf(
                fixture("valid.json"),
                fixture("trailing_comma.json"),
                fixture("markdown_fenced.json"),
                fixture("single_quoted.json"),
                fixture("missing_fields.json"),
                fixture("unrecoverable.txt"),
            )

        val result = EvalMetrics.jsonValidity(rawTexts)

        // 6 cases. valid.json and missing_fields.json are both syntactically
        // valid JSON on the first try (missing_fields.json is just missing
        // optional keys, which is legal JSON) -> 2/6 strict. All but the
        // deliberately unrecoverable fixture parse after the repair pass -> 5/6.
        assertEquals(6, result.totalCases)
        assertEquals(2, result.strictValidCount)
        assertEquals(5, result.validAfterRepairCount)
    }

    @Test
    @DisplayName("FakeVlmEngine's fixture output parses successfully and self-matches on garment types")
    fun `fake engine fixture self consistency`() =
        runBlocking {
            val rawResponse = FakeVlmEngine().describe(ByteArray(0))
            val parsed = ResultParser.parse(rawResponse).getOrThrow()

            val fixtureGarmentTypes = listOf("jacket", "trousers", "shoes")
            val score = EvalMetrics.garmentTypeScore(parsed.garments.map { it.type }, fixtureGarmentTypes)

            assertEquals(1.0, score.precision)
            assertEquals(1.0, score.recall)
        }

    @Test
    @DisplayName("end-to-end (fake engine describe + parse) latency self-check completes well under a second per call")
    fun `end to end self check latency`() =
        runBlocking {
            val engine = FakeVlmEngine()
            val iterations = 200
            val startNanos = System.nanoTime()
            repeat(iterations) {
                val raw = engine.describe(ByteArray(0))
                ResultParser.parse(raw).getOrThrow()
            }
            val elapsedMs = (System.nanoTime() - startNanos) / NANOS_PER_MILLI
            val averageMsPerCall = elapsedMs.toDouble() / iterations

            // This bound (500ms) is generous on purpose -- the point is only to
            // prove FakeVlmEngine+ResultParser complete fast and deterministically
            // in this environment, not to benchmark them tightly. It is NOT a
            // proxy for real on-device model latency; see the class doc.
            assertTrue(averageMsPerCall < SELF_CHECK_LATENCY_BOUND_MS, "average ms/call was $averageMsPerCall")
        }

    @Test
    @DisplayName("ColorExtractor self-check: extracting the fixture engine's own palette hexes' RGB is deterministic")
    fun `color extractor self check`() {
        val pixels = intArrayOf(0x5B6B3B, 0x5B6B3B, 0x5B6B3B, 0x2B2B2B, 0x2B2B2B, 0xEDE6D6)
        val clusters = ColorExtractor.extract(pixels, k = 3, seed = 42)

        assertEquals(3, clusters.size)
        assertEquals("#5B6B3B", ColorExtractor.toHex(clusters.first().rgb))
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader.getResourceAsStream("fixtures/$name")) {
            "Missing test fixture: $name"
        }.bufferedReader().readText()

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
        const val SELF_CHECK_LATENCY_BOUND_MS = 500.0
    }
}
