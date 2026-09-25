package com.mitanshm.fitfindr.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class StoragePrecheckTest {

    @Test
    @DisplayName("enough space including margin returns true")
    fun `sufficient space`() {
        assertTrue(StoragePrecheck.hasSufficientSpace(availableBytes = 1_000, remainingDownloadBytes = 500, marginBytes = 100))
    }

    @Test
    @DisplayName("exactly remaining + margin is still sufficient (boundary is inclusive)")
    fun `exact boundary is sufficient`() {
        assertTrue(StoragePrecheck.hasSufficientSpace(availableBytes = 600, remainingDownloadBytes = 500, marginBytes = 100))
    }

    @Test
    @DisplayName("one byte short of remaining + margin is insufficient")
    fun `one byte short is insufficient`() {
        assertFalse(StoragePrecheck.hasSufficientSpace(availableBytes = 599, remainingDownloadBytes = 500, marginBytes = 100))
    }

    @Test
    @DisplayName("default margin is applied when not specified")
    fun `default margin applied`() {
        val justUnderDefaultMargin = StoragePrecheck.DEFAULT_MARGIN_BYTES - 1
        assertFalse(StoragePrecheck.hasSufficientSpace(availableBytes = justUnderDefaultMargin, remainingDownloadBytes = 0))
        assertTrue(StoragePrecheck.hasSufficientSpace(availableBytes = StoragePrecheck.DEFAULT_MARGIN_BYTES, remainingDownloadBytes = 0))
    }

    @Test
    @DisplayName("shortfallBytes reports 0 when there is enough space")
    fun `shortfall is zero when sufficient`() {
        assertEquals(0L, StoragePrecheck.shortfallBytes(availableBytes = 1_000, remainingDownloadBytes = 500, marginBytes = 100))
    }

    @Test
    @DisplayName("shortfallBytes reports the exact number of bytes still needed")
    fun `shortfall reports exact gap`() {
        assertEquals(50L, StoragePrecheck.shortfallBytes(availableBytes = 550, remainingDownloadBytes = 500, marginBytes = 100))
    }

    @Test
    @DisplayName("negative inputs are rejected")
    fun `negative inputs rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            StoragePrecheck.hasSufficientSpace(availableBytes = -1, remainingDownloadBytes = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            StoragePrecheck.hasSufficientSpace(availableBytes = 0, remainingDownloadBytes = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            StoragePrecheck.hasSufficientSpace(availableBytes = 0, remainingDownloadBytes = 0, marginBytes = -1)
        }
    }
}
