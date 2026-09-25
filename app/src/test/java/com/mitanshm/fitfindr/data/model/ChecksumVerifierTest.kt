package com.mitanshm.fitfindr.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.nio.file.Path
import kotlin.io.path.writeBytes

class ChecksumVerifierTest {

    // Known value: SHA-256 of the empty byte string (computed independently with
    // `python3 -c "import hashlib; print(hashlib.sha256(b'').hexdigest())"`).
    private val emptyInputSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

    // Known value: SHA-256 of the ASCII bytes "abc".
    private val abcSha256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

    @Test
    @DisplayName("sha256 of an empty input matches the well-known empty-string digest")
    fun `empty input digest`() {
        assertEquals(emptyInputSha256, ChecksumVerifier.sha256(ByteArray(0)))
        assertEquals(emptyInputSha256, ChecksumVerifier.sha256(ByteArrayInputStream(ByteArray(0))))
    }

    @Test
    @DisplayName("sha256 of a known input ('abc') matches its well-known digest")
    fun `known input digest`() {
        assertEquals(abcSha256, ChecksumVerifier.sha256("abc".toByteArray()))
    }

    @Test
    @DisplayName("sha256(File) matches sha256(ByteArray) for the same content")
    fun `file and bytes agree`(
        @TempDir tempDir: Path,
    ) {
        val bytes = "the quick brown fox jumps over the lazy dog".toByteArray()
        val file = tempDir.resolve("sample.bin")
        file.writeBytes(bytes)

        assertEquals(ChecksumVerifier.sha256(bytes), ChecksumVerifier.sha256(file.toFile()))
    }

    @Test
    @DisplayName("matches() is case-insensitive and trims whitespace on the expected side")
    fun `matches is case insensitive and trims`() {
        assertTrue(ChecksumVerifier.matches(abcSha256, abcSha256.uppercase()))
        assertTrue(ChecksumVerifier.matches(abcSha256, "  $abcSha256  "))
    }

    @Test
    @DisplayName("matches() rejects a different digest")
    fun `matches rejects mismatch`() {
        assertFalse(ChecksumVerifier.matches(abcSha256, emptyInputSha256))
    }

    @Test
    @DisplayName("chunked streaming with a tiny buffer produces the same digest as the default buffer")
    fun `small buffer size does not change the digest`() {
        val bytes = ByteArray(10_000) { (it % 251).toByte() }
        val withDefaultBuffer = ChecksumVerifier.sha256(ByteArrayInputStream(bytes))
        val withTinyBuffer = ChecksumVerifier.sha256(ByteArrayInputStream(bytes), bufferSize = 7)
        assertEquals(withDefaultBuffer, withTinyBuffer)
    }
}
