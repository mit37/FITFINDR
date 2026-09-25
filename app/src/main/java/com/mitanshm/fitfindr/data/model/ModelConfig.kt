package com.mitanshm.fitfindr.data.model

/**
 * Metadata for the on-device model FitFindr downloads at runtime.
 *
 * IMPORTANT (honesty): the model is never bundled with or committed to this
 * repository (see `STANDARDS.md` rule 7 and the README's "What this does
 * not do" section). [expectedSha256] and [approxSizeBytes] below are
 * placeholders, NOT verified real values — there is no Android SDK/device
 * in this development environment capable of actually downloading and
 * running Gemma 3n E4B, so nobody has computed the real checksum here. They
 * must be replaced with the real published checksum and size for whichever
 * MediaPipe-compatible `.task` bundle is chosen before this is used against
 * a real model in production. Shipping with a placeholder checksum is safe
 * (it will simply always fail [ChecksumVerifier.matches], so a corrupt or
 * substituted file is never treated as good) but is called out here so it
 * is never mistaken for a real, verified value.
 */
object ModelConfig {
    /** TODO(release): replace with the real hosted URL for the chosen Gemma 3n E4B `.task` bundle. */
    const val DOWNLOAD_URL: String = "https://example-model-host.invalid/gemma-3n-e4b-int4.task"

    /** TODO(release): replace with the real published SHA-256 of that exact file. Placeholder, not verified. */
    const val EXPECTED_SHA256: String = "0000000000000000000000000000000000000000000000000000000000000000"

    /** TODO(release): replace with the real file size in bytes. Placeholder, used only to size-check progress UI. */
    const val APPROX_SIZE_BYTES: Long = 4_400_000_000L

    const val MODEL_FILE_NAME: String = "gemma-3n-e4b-int4.task"
    const val PARTIAL_FILE_NAME: String = "$MODEL_FILE_NAME.partial"
}
