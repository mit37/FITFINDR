package com.mitanshm.fitfindr.domain

import com.mitanshm.fitfindr.data.inference.VlmEngine
import javax.inject.Inject

/**
 * Runs a captured photo through the configured [VlmEngine] and parses its
 * raw text response into an [OutfitResult] via [ResultParser].
 */
class DescribeOutfitUseCase
    @Inject
    constructor(
        private val vlmEngine: VlmEngine,
        private val resultParser: ResultParserContract = ResultParser.asContract(),
    ) {
        suspend operator fun invoke(imageBytes: ByteArray): Result<OutfitResult> {
            val rawText = vlmEngine.describe(imageBytes)
            return resultParser.parse(rawText)
        }
    }

/**
 * Thin interface over [ResultParser.parse] so [DescribeOutfitUseCase] can be
 * unit-tested with a fake parser if ever needed, without dragging in
 * kotlinx.serialization mocking.
 */
fun interface ResultParserContract {
    fun parse(rawText: String): Result<OutfitResult>
}

private fun ResultParser.asContract(): ResultParserContract = ResultParserContract { text -> this.parse(text) }
