package com.mitanshm.fitfindr.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ResultParserTest {

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader.getResourceAsStream("fixtures/$name")) {
            "Missing test fixture: $name"
        }.bufferedReader().readText()

    @Test
    @DisplayName("valid JSON parses on the strict (first) attempt")
    fun `parses valid json strictly`() {
        val result = ResultParser.parse(fixture("valid.json"))

        assertTrue(result.isSuccess)
        val outfit = result.getOrThrow()
        assertEquals("smart casual", outfit.styleLabel)
        assertEquals(2, outfit.garments.size)
        assertEquals("jacket", outfit.garments[0].type)
        assertEquals("olive green", outfit.garments[0].color)
        assertEquals("cotton twill", outfit.garments[0].material)
        assertEquals("relaxed", outfit.garments[0].fit)
        assertEquals(listOf("olive green cotton twill jacket relaxed fit"), outfit.garments[0].searchQueries)
        assertEquals(listOf("#5B6B3B", "#2B2B2B", "#EDE6D6"), outfit.palette)
        assertEquals("confident on all fields", outfit.notes)
    }

    @Test
    @DisplayName("trailing commas are repaired and parsed")
    fun `repairs trailing commas`() {
        val result = ResultParser.parse(fixture("trailing_comma.json"))

        assertTrue(result.isSuccess)
        val outfit = result.getOrThrow()
        assertEquals("streetwear", outfit.styleLabel)
        assertEquals(1, outfit.garments.size)
        assertEquals("hoodie", outfit.garments[0].type)
        assertEquals("oversized", outfit.garments[0].fit)
    }

    @Test
    @DisplayName("markdown code fences and surrounding prose are stripped and repaired")
    fun `repairs markdown fenced json`() {
        val result = ResultParser.parse(fixture("markdown_fenced.json"))

        assertTrue(result.isSuccess)
        val outfit = result.getOrThrow()
        assertEquals("business formal", outfit.styleLabel)
        assertEquals("blazer", outfit.garments.single().type)
        assertEquals("navy", outfit.garments.single().color)
    }

    @Test
    @DisplayName("single-quoted JSON is repaired to double-quoted and parsed")
    fun `repairs single quoted json`() {
        val result = ResultParser.parse(fixture("single_quoted.json"))

        assertTrue(result.isSuccess)
        val outfit = result.getOrThrow()
        assertEquals("streetwear", outfit.styleLabel)
        assertEquals("sneakers", outfit.garments.single().type)
        assertEquals("white", outfit.garments.single().color)
        assertEquals("leather", outfit.garments.single().material)
    }

    @Test
    @DisplayName("missing or blank fields default to the literal string 'unknown'")
    fun `missing fields default to unknown`() {
        val result = ResultParser.parse(fixture("missing_fields.json"))

        assertTrue(result.isSuccess)
        val outfit = result.getOrThrow()
        assertEquals(OutfitResult.UNKNOWN, outfit.styleLabel)
        val garment = outfit.garments.single()
        assertEquals("scarf", garment.type)
        assertEquals(OutfitResult.UNKNOWN, garment.color) // blank string -> unknown
        assertEquals(OutfitResult.UNKNOWN, garment.material) // absent -> unknown
        assertEquals(OutfitResult.UNKNOWN, garment.fit) // absent -> unknown
        assertTrue(garment.searchQueries.isEmpty())
        assertTrue(outfit.palette.isEmpty())
    }

    @Test
    @DisplayName("text with no JSON object at all fails to parse, even after repair")
    fun `unrecoverable text fails`() {
        val result = ResultParser.parse(fixture("unrecoverable.txt"))

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull() is ResultParseException)
    }

    @Test
    @DisplayName("an empty string fails to parse")
    fun `empty string fails`() {
        val result = ResultParser.parse("")

        assertFalse(result.isSuccess)
    }

    @Test
    @DisplayName("garments and palette default to empty lists when absent, not unknown")
    fun `absent lists default to empty not unknown`() {
        val result = ResultParser.parse("""{"styleLabel": "minimal"}""")

        assertTrue(result.isSuccess)
        val outfit = result.getOrThrow()
        assertEquals("minimal", outfit.styleLabel)
        assertTrue(outfit.garments.isEmpty())
        assertTrue(outfit.palette.isEmpty())
    }

    @Test
    @DisplayName("unknown extra keys in the model output are ignored, not fatal")
    fun `ignores unknown keys`() {
        val result =
            ResultParser.parse(
                """
                {
                  "styleLabel": "casual",
                  "confidenceScoreTheModelMadeUp": 0.97,
                  "garments": []
                }
                """.trimIndent(),
            )

        assertTrue(result.isSuccess)
        assertEquals("casual", result.getOrThrow().styleLabel)
    }
}
