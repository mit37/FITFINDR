package com.mitanshm.fitfindr.data.db

import com.mitanshm.fitfindr.domain.Garment
import com.mitanshm.fitfindr.domain.OutfitResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OutfitMapperTest {

    private val sample =
        OutfitResult(
            styleLabel = "smart casual",
            garments =
                listOf(
                    Garment(
                        type = "jacket",
                        color = "olive green",
                        material = "cotton twill",
                        fit = "relaxed",
                        searchQueries = listOf("olive green jacket", "cotton twill relaxed jacket"),
                    ),
                    Garment(
                        type = "shoes",
                        color = "white",
                        material = "leather",
                        fit = "unknown",
                        searchQueries = emptyList(),
                    ),
                ),
            palette = listOf("#5B6B3B", "#2B2B2B", "#EDE6D6"),
            notes = "confident on all fields",
        )

    @Test
    @DisplayName("round-tripping a full OutfitResult through entities preserves every field")
    fun `round trip preserves fields`() {
        val (outfitEntity, garmentEntities) = OutfitMapper.toEntities(sample, createdAtEpochMs = 1_000L, outfitId = 42L)

        assertEquals(42L, outfitEntity.id)
        assertEquals("smart casual", outfitEntity.styleLabel)
        assertEquals("confident on all fields", outfitEntity.notes)
        assertEquals("#5B6B3B,#2B2B2B,#EDE6D6", outfitEntity.paletteCsv)
        assertEquals(1_000L, outfitEntity.createdAtEpochMs)
        assertEquals(2, garmentEntities.size)
        assertEquals(42L, garmentEntities[0].outfitId)

        val roundTripped = OutfitMapper.toDomain(outfitEntity, garmentEntities)
        assertEquals(sample, roundTripped)
    }

    @Test
    @DisplayName("an empty palette and a garment with no search queries round-trip to empty lists, not lists with one blank entry")
    fun `empty lists round trip cleanly`() {
        val outfitWithEmpties = sample.copy(palette = emptyList(), garments = listOf(sample.garments[1]))
        val (outfitEntity, garmentEntities) = OutfitMapper.toEntities(outfitWithEmpties, createdAtEpochMs = 0L)

        assertEquals("", outfitEntity.paletteCsv)
        assertEquals("", garmentEntities.single().searchQueriesCsv)

        val roundTripped = OutfitMapper.toDomain(outfitEntity, garmentEntities)
        assertEquals(emptyList<String>(), roundTripped.palette)
        assertEquals(emptyList<String>(), roundTripped.garments.single().searchQueries)
    }

    @Test
    @DisplayName("a null notes field round-trips to null, not the string 'null'")
    fun `null notes round trips to null`() {
        val (outfitEntity, _) = OutfitMapper.toEntities(sample.copy(notes = null), createdAtEpochMs = 0L)
        assertNull(outfitEntity.notes)
        assertNull(OutfitMapper.toDomain(outfitEntity, emptyList()).notes)
    }

    @Test
    @DisplayName("multi-word search queries survive the flatten/split round trip intact")
    fun `multi word search queries survive round trip`() {
        val (_, garmentEntities) = OutfitMapper.toEntities(sample, createdAtEpochMs = 0L)
        val jacketEntity = garmentEntities.first { it.type == "jacket" }
        val domain = OutfitMapper.toDomain(OutfitMapper.toEntities(sample, 0L).first, listOf(jacketEntity))
        assertEquals(listOf("olive green jacket", "cotton twill relaxed jacket"), domain.garments.single().searchQueries)
    }
}
