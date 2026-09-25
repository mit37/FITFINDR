package com.mitanshm.fitfindr.eval

/**
 * Parses `eval/labels.csv` (schema documented in `eval/README.md`). Pure
 * Kotlin, no Android import, so it is unit-tested directly (see
 * `LabelsCsvTest.kt`) without needing a device to read the real file.
 */
object LabelsCsv {
    const val HEADER = "photo_id,filename,expected_garment_types,expected_colors,expected_style_label,device_class,notes"

    fun parse(csvText: String): List<LabelRow> {
        val lines = csvText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return emptyList()
        val dataLines = if (lines.first() == HEADER) lines.drop(1) else lines
        return dataLines.map { line -> parseRow(line) }
    }

    private fun parseRow(line: String): LabelRow {
        val cols = line.split(",")
        require(cols.size >= EXPECTED_COLUMN_COUNT) { "Malformed labels.csv row (expected $EXPECTED_COLUMN_COUNT columns): $line" }
        return LabelRow(
            photoId = cols[0],
            filename = cols[1],
            expectedGarmentTypes = cols[2].splitSemicolons(),
            expectedColors = cols[3].splitSemicolons(),
            expectedStyleLabel = cols[4],
            deviceClass = cols[5],
            notes = cols.getOrElse(6) { "" },
        )
    }

    private fun String.splitSemicolons(): List<String> = if (isBlank()) emptyList() else split(";").map { it.trim() }

    private const val EXPECTED_COLUMN_COUNT = 6
}

data class LabelRow(
    val photoId: String,
    val filename: String,
    val expectedGarmentTypes: List<String>,
    val expectedColors: List<String>,
    val expectedStyleLabel: String,
    val deviceClass: String,
    val notes: String,
)
