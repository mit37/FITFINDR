package com.mitanshm.fitfindr.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Parses the JSON text returned by a [com.mitanshm.fitfindr.data.inference.VlmEngine]
 * into an [OutfitResult].
 *
 * Real model output is not always perfectly clean JSON: it may be wrapped in
 * a markdown code fence, contain a trailing comma, or use single quotes.
 * [parse] first attempts a strict parse. If that fails, it attempts exactly
 * one repair pass (see [repair]) and retries. If that also fails, it returns
 * a [Result.failure].
 *
 * Any field present but blank, or missing entirely, is mapped to
 * [OutfitResult.UNKNOWN] rather than guessed — this mirrors the "if unsure,
 * write unknown" instruction given to the model in
 * `assets/prompts/describe_v1.txt`, and also covers the case where the
 * model itself failed to fill a field.
 */
object ResultParser {

    private val strictJson = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    fun parse(rawText: String): Result<OutfitResult> {
        strictDecode(rawText)?.let { return Result.success(it.toDomain()) }

        val repaired = repair(rawText)
        strictDecode(repaired)?.let { return Result.success(it.toDomain()) }

        return Result.failure(
            ResultParseException("Could not parse model output as OutfitResult JSON, even after repair."),
        )
    }

    private fun strictDecode(text: String): OutfitResultDto? =
        try {
            strictJson.decodeFromString(OutfitResultDto.serializer(), text)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }

    /**
     * One best-effort repair pass over near-miss JSON. Each step is
     * intentionally conservative and order-sensitive:
     *  1. Extract the substring between the first `{` and the last `}`,
     *     which strips markdown code fences (``` or ```json) and any
     *     leading/trailing prose the model added around the JSON.
     *  2. Remove trailing commas before a closing `}` or `]`.
     *  3. Convert single-quoted string literals to double-quoted ones.
     */
    internal fun repair(text: String): String {
        var result = extractBraces(text)
        result = removeTrailingCommas(result)
        result = singleQuotesToDouble(result)
        return result
    }

    private fun extractBraces(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else text
    }

    private val trailingCommaRegex = Regex(""",(\s*[}\]])""")

    private fun removeTrailingCommas(text: String): String = trailingCommaRegex.replace(text) { it.groupValues[1] }

    // Converts 'single quoted' tokens to "double quoted" tokens. This is a
    // deliberately narrow heuristic (no escaped-quote handling) sufficient
    // for the near-miss cases a small on-device model actually produces;
    // it is not a general JSON5 parser.
    private val singleQuotedRegex = Regex("'([^'\"]*)'")

    private fun singleQuotesToDouble(text: String): String = singleQuotedRegex.replace(text) { "\"${it.groupValues[1]}\"" }
}

class ResultParseException(message: String) : Exception(message)

@Serializable
internal data class OutfitResultDto(
    val styleLabel: String? = null,
    val garments: List<GarmentDto>? = null,
    val palette: List<String>? = null,
    val notes: String? = null,
) {
    fun toDomain(): OutfitResult =
        OutfitResult(
            styleLabel = styleLabel.orUnknown(),
            garments = garments?.map { it.toDomain() } ?: emptyList(),
            palette = palette ?: emptyList(),
            notes = notes,
        )
}

@Serializable
internal data class GarmentDto(
    val type: String? = null,
    val color: String? = null,
    val material: String? = null,
    val fit: String? = null,
    val searchQueries: List<String>? = null,
) {
    fun toDomain(): Garment =
        Garment(
            type = type.orUnknown(),
            color = color.orUnknown(),
            material = material.orUnknown(),
            fit = fit.orUnknown(),
            searchQueries = searchQueries ?: emptyList(),
        )
}

private fun String?.orUnknown(): String = if (this.isNullOrBlank()) OutfitResult.UNKNOWN else this
