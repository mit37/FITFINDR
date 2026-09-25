package com.mitanshm.fitfindr.data.db

import com.mitanshm.fitfindr.domain.Garment
import com.mitanshm.fitfindr.domain.OutfitResult

/**
 * Pure conversion functions between [OutfitResult]/[Garment] (domain) and
 * [OutfitEntity]/[GarmentEntity] (Room rows). Deliberately has no
 * `androidx.room` or `android.*` import itself -- it only references the
 * entity *classes*, not their Room annotations -- so this logic is
 * unit-testable, and independently verified, without Room or the Android
 * SDK on the classpath at all (see `OutfitMapperTest.kt` and the standalone
 * JVM verification in `docs/PLAN.md`, which uses plain, unannotated mirror
 * copies of these two data classes since the real ones require Room, which
 * is only resolvable through Google's Maven repo that this cloud instance
 * cannot reach).
 */
object OutfitMapper {

    /** Separator used to flatten a garment's search-query list into one Room column. Chosen to be
     * vanishingly unlikely to appear inside a real search-query phrase. */
    const val QUERY_SEPARATOR = "\u001F" // ASCII unit separator

    fun toEntities(
        result: OutfitResult,
        createdAtEpochMs: Long,
        outfitId: Long = 0,
    ): Pair<OutfitEntity, List<GarmentEntity>> {
        val outfitEntity =
            OutfitEntity(
                id = outfitId,
                styleLabel = result.styleLabel,
                notes = result.notes,
                paletteCsv = result.palette.joinToString(","),
                createdAtEpochMs = createdAtEpochMs,
            )
        val garmentEntities =
            result.garments.map { garment ->
                GarmentEntity(
                    outfitId = outfitId,
                    type = garment.type,
                    color = garment.color,
                    material = garment.material,
                    fit = garment.fit,
                    searchQueriesCsv = garment.searchQueries.joinToString(QUERY_SEPARATOR),
                )
            }
        return outfitEntity to garmentEntities
    }

    fun toDomain(
        outfit: OutfitEntity,
        garments: List<GarmentEntity>,
    ): OutfitResult =
        OutfitResult(
            styleLabel = outfit.styleLabel,
            garments =
                garments.map { g ->
                    Garment(
                        type = g.type,
                        color = g.color,
                        material = g.material,
                        fit = g.fit,
                        searchQueries = if (g.searchQueriesCsv.isEmpty()) emptyList() else g.searchQueriesCsv.split(QUERY_SEPARATOR),
                    )
                },
            palette = if (outfit.paletteCsv.isEmpty()) emptyList() else outfit.paletteCsv.split(","),
            notes = outfit.notes,
        )
}
