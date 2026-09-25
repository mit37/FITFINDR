package com.mitanshm.fitfindr.eval

import com.mitanshm.fitfindr.domain.ResultParser

/**
 * Pure metric computations for the eval harness (see `eval/README.md`).
 * No Android import, so this logic is shared unchanged between:
 *  - `app/src/androidTest/.../eval/EvalHarnessInstrumentedTest.kt`, which
 *    supplies real photos, wall-clock latency and RAM readings from a real
 *    device (unexercised in this environment -- no device here);
 *  - `app/src/test/.../eval/EvalHarnessSelfCheckTest.kt`, a genuine,
 *    runnable-today self-check against `FakeVlmEngine` fixture data, whose
 *    output is recorded in `eval/results.json` as a self-check, clearly
 *    NOT representative of real model performance (see docs/PLAN.md).
 */
object EvalMetrics {

    data class ParseValidity(
        val totalCases: Int,
        val strictValidCount: Int,
        val validAfterRepairCount: Int,
    ) {
        val strictValidRate: Double get() = rate(strictValidCount)
        val postRepairValidRate: Double get() = rate(validAfterRepairCount)

        private fun rate(count: Int): Double = if (totalCases == 0) 0.0 else count.toDouble() / totalCases
    }

    /**
     * Parses each of [rawTexts] once strictly (no repair) and once with the
     * repair pass allowed, and tallies how many succeed each way. This is
     * the JSON-validity-rate-pre/post-repair metric from the PRD's
     * evaluation plan.
     */
    fun jsonValidity(rawTexts: List<String>): ParseValidity {
        var strictValid = 0
        var validAfterRepair = 0
        rawTexts.forEach { raw ->
            if (ResultParser.parseStrictOnly(raw).isSuccess) strictValid++
            if (ResultParser.parse(raw).isSuccess) validAfterRepair++
        }
        return ParseValidity(rawTexts.size, strictValid, validAfterRepair)
    }

    data class GarmentTypeScore(val precision: Double, val recall: Double, val f1: Double)

    /**
     * Multiset precision/recall of predicted garment-`type` strings against
     * a labeled expected set (case-insensitive, order-independent, and
     * duplicate-count-aware -- e.g. predicting "shoes" twice when only one
     * is expected counts one true positive and one false positive).
     */
    fun garmentTypeScore(
        predicted: List<String>,
        expected: List<String>,
    ): GarmentTypeScore {
        val predictedLower = predicted.map { it.lowercase() }
        val expectedLower = expected.map { it.lowercase() }
        if (predictedLower.isEmpty() && expectedLower.isEmpty()) return GarmentTypeScore(1.0, 1.0, 1.0)

        val remainingExpected = expectedLower.toMutableList()
        val truePositives = predictedLower.count { remainingExpected.remove(it) }

        val precision = if (predictedLower.isEmpty()) 0.0 else truePositives.toDouble() / predictedLower.size
        val recall = if (expectedLower.isEmpty()) 0.0 else truePositives.toDouble() / expectedLower.size
        val f1 = if (precision + recall == 0.0) 0.0 else 2 * precision * recall / (precision + recall)
        return GarmentTypeScore(precision, recall, f1)
    }

    /**
     * Whether a model-reported color name and a pixel-extracted palette hex
     * "cross-check" as consistent -- true if [modelColorName] is a
     * substring match against a curated hex->name table for any swatch in
     * [pixelPalette]. Intentionally simple (a real system would use a color
     * name space / nearest-neighbor lookup); adequate to prove the
     * cross-check *mechanism* works, which is all this eval harness can
     * honestly claim without real labeled photos.
     */
    fun colorNameMatchesPixelExtraction(
        modelColorName: String,
        pixelPalette: List<String>,
        hexToName: Map<String, String>,
    ): Boolean {
        val normalizedModelName = modelColorName.trim().lowercase()
        return pixelPalette.any { hex ->
            hexToName[hex.uppercase()]?.lowercase()?.let { name -> name == normalizedModelName || name.contains(normalizedModelName) } == true
        }
    }
}
