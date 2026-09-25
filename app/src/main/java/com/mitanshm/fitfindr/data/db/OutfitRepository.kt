package com.mitanshm.fitfindr.data.db

import com.mitanshm.fitfindr.domain.OutfitResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Saved-outfit history: wraps [OutfitDao] and [OutfitMapper] behind a
 * domain-typed API so callers (ViewModels) never see [OutfitEntity] /
 * [GarmentEntity] directly.
 *
 * The mapping logic this delegates to ([OutfitMapper]) is unit-tested
 * without Room. This class itself (which depends on the real [OutfitDao]
 * interface, and therefore Room) has not been exercised in this
 * environment -- see `docs/PLAN.md`.
 */
@Singleton
class OutfitRepository
    @Inject
    constructor(
        private val dao: OutfitDao,
    ) {
        suspend fun save(
            result: OutfitResult,
            createdAtEpochMs: Long = System.currentTimeMillis(),
        ): Long {
            val (outfitEntity, garmentEntitiesWithoutId) = OutfitMapper.toEntities(result, createdAtEpochMs)
            val outfitId = dao.insertOutfit(outfitEntity)
            if (garmentEntitiesWithoutId.isNotEmpty()) {
                dao.insertGarments(garmentEntitiesWithoutId.map { it.copy(outfitId = outfitId) })
            }
            return outfitId
        }

        fun observeHistory(): Flow<List<OutfitHistoryItem>> =
            dao.observeAllWithGarments().map { rows ->
                rows.map { row ->
                    OutfitHistoryItem(
                        id = row.outfit.id,
                        createdAtEpochMs = row.outfit.createdAtEpochMs,
                        result = OutfitMapper.toDomain(row.outfit, row.garments),
                    )
                }
            }

        suspend fun get(outfitId: Long): OutfitResult? =
            dao.getWithGarments(outfitId)?.let { OutfitMapper.toDomain(it.outfit, it.garments) }

        suspend fun delete(outfitId: Long) = dao.deleteOutfit(outfitId)
    }

data class OutfitHistoryItem(
    val id: Long,
    val createdAtEpochMs: Long,
    val result: OutfitResult,
)
