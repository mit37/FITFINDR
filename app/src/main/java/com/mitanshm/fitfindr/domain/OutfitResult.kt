package com.mitanshm.fitfindr.domain

/**
 * The structured description of an outfit produced by a [com.mitanshm.fitfindr.data.inference.VlmEngine]
 * and parsed by [ResultParser].
 *
 * Any field the model could not confidently determine is set to the literal
 * string [UNKNOWN] rather than guessed, per the prompt contract in
 * `assets/prompts/describe_v1.txt`.
 */
data class OutfitResult(
    val styleLabel: String,
    val garments: List<Garment>,
    val palette: List<String>,
    val notes: String? = null,
) {
    companion object {
        const val UNKNOWN = "unknown"
    }
}

data class Garment(
    val type: String,
    val color: String,
    val material: String,
    val fit: String,
    val searchQueries: List<String>,
)
