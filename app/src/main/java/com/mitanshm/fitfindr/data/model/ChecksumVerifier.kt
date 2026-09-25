package com.mitanshm.fitfindr.data.model

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/**
 * SHA-256 checksum computation and verification, used to confirm a
 * downloaded model file is intact before it is ever loaded by
 * [com.mitanshm.fitfindr.data.inference.MediaPipeVlmEngine].
 *
 * Deliberately pure Kotlin/JVM (`java.security.MessageDigest` and
 * `java.io.File` are both part of the standard JDK, not the Android SDK) so
 * this can be unit-tested without any Android dependency or device — see
 * `ChecksumVerifierTest.kt` and the standalone JVM verification described in
 * `docs/PLAN.md`.
 */
object ChecksumVerifier {

    /** Streams [input] through SHA-256 in fixed-size chunks and returns the lowercase hex digest. */
    fun sha256(input: InputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(bufferSize)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest().toHexString()
    }

    fun sha256(file: File): String = file.inputStream().use { sha256(it) }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()

    /**
     * Case-insensitive comparison of a computed digest against an expected
     * one. Expected hashes are normally hardcoded as lowercase, but this
     * guards against a copy-pasted uppercase hash from a release page.
     */
    fun matches(
        computedHex: String,
        expectedHex: String,
    ): Boolean = computedHex.equals(expectedHex.trim(), ignoreCase = true)

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    private const val DEFAULT_BUFFER_SIZE = 8192
}
