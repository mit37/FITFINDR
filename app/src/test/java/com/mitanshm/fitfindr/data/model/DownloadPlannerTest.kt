package com.mitanshm.fitfindr.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DownloadPlannerTest {

    @Test
    @DisplayName("no existing partial file starts a fresh download for the full size")
    fun `no partial file starts fresh`() {
        val action = DownloadPlanner.plan(existingPartialBytes = 0, expectedTotalBytes = 1_000)
        assertEquals(DownloadAction.StartFresh(remainingBytes = 1_000), action)
    }

    @Test
    @DisplayName("a partial file smaller than expected resumes from its offset")
    fun `partial file resumes from offset`() {
        val action = DownloadPlanner.plan(existingPartialBytes = 300, expectedTotalBytes = 1_000)
        assertEquals(DownloadAction.Resume(fromByteOffset = 300, remainingBytes = 700), action)
    }

    @Test
    @DisplayName("a partial file equal to the expected size is treated as stale and restarted")
    fun `partial file equal to expected size restarts`() {
        val action = DownloadPlanner.plan(existingPartialBytes = 1_000, expectedTotalBytes = 1_000)
        assertTrue(action is DownloadAction.RestartFromScratch)
    }

    @Test
    @DisplayName("a partial file larger than expected is treated as stale and restarted")
    fun `partial file larger than expected restarts`() {
        val action = DownloadPlanner.plan(existingPartialBytes = 1_500, expectedTotalBytes = 1_000)
        assertTrue(action is DownloadAction.RestartFromScratch)
    }

    @Test
    @DisplayName("expectedTotalBytes must be positive")
    fun `expected total bytes must be positive`() {
        assertThrows(IllegalArgumentException::class.java) {
            DownloadPlanner.plan(existingPartialBytes = 0, expectedTotalBytes = 0)
        }
    }

    @Test
    @DisplayName("existingPartialBytes must not be negative")
    fun `existing partial bytes must not be negative`() {
        assertThrows(IllegalArgumentException::class.java) {
            DownloadPlanner.plan(existingPartialBytes = -1, expectedTotalBytes = 1_000)
        }
    }
}
