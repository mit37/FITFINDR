package com.mitanshm.fitfindr.eval

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class LabelsCsvTest {

    @Test
    @DisplayName("a header-only CSV (the committed eval/labels.csv, no photos supplied yet) parses to an empty list")
    fun `header only csv parses empty`() {
        assertTrue(LabelsCsv.parse(LabelsCsv.HEADER + "\n").isEmpty())
        assertTrue(LabelsCsv.parse("").isEmpty())
    }

    @Test
    @DisplayName("a data row parses into a LabelRow with semicolon-separated lists split correctly")
    fun `parses a data row`() {
        val csv =
            "${LabelsCsv.HEADER}\n" +
                "1,outfit1.jpg,jacket;trousers;shoes,olive green;charcoal;white,smart casual,pixel8_2024,backlit"

        val rows = LabelsCsv.parse(csv)

        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals("1", row.photoId)
        assertEquals("outfit1.jpg", row.filename)
        assertEquals(listOf("jacket", "trousers", "shoes"), row.expectedGarmentTypes)
        assertEquals(listOf("olive green", "charcoal", "white"), row.expectedColors)
        assertEquals("smart casual", row.expectedStyleLabel)
        assertEquals("pixel8_2024", row.deviceClass)
        assertEquals("backlit", row.notes)
    }

    @Test
    @DisplayName("notes column is optional and defaults to an empty string")
    fun `notes column optional`() {
        val csv = "1,outfit1.jpg,jacket,olive green,smart casual,pixel8_2024"
        assertEquals("", LabelsCsv.parse(csv).single().notes)
    }

    @Test
    @DisplayName("a row with too few columns throws rather than silently dropping data")
    fun `malformed row throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            LabelsCsv.parse("1,outfit1.jpg,jacket")
        }
    }
}
