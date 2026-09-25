package com.mitanshm.fitfindr.data.model

/**
 * Pure decision logic for "is there enough free storage to download the
 * model file". Kept free of any Android/`java.io.File` API (it operates on
 * plain `Long` byte counts) so the decision itself is unit-testable without
 * a filesystem, an Android context, or a device — see
 * `StoragePrecheckTest.kt`.
 *
 * The actual free-space measurement (`StatFs` / `File.usableSpace`) lives in
 * the Android-only [AndroidModelStorage] and is handed to this object as a
 * plain number.
 */
object StoragePrecheck {

    /**
     * Extra headroom required on top of the download's remaining byte
     * count, so the device isn't left with zero free space (which causes
     * unrelated OS/app misbehavior) right after a download completes.
     */
    const val DEFAULT_MARGIN_BYTES: Long = 200L * 1024 * 1024 // 200 MiB

    /**
     * @param availableBytes free space on the target volume right now.
     * @param remainingDownloadBytes bytes still to be written for this
     *   download (i.e. `expectedTotalBytes - alreadyDownloadedBytes`, so a
     *   resumed download only needs to account for what's left).
     * @param marginBytes extra headroom to require beyond the raw byte
     *   count that will be written.
     */
    fun hasSufficientSpace(
        availableBytes: Long,
        remainingDownloadBytes: Long,
        marginBytes: Long = DEFAULT_MARGIN_BYTES,
    ): Boolean {
        require(availableBytes >= 0) { "availableBytes must not be negative: $availableBytes" }
        require(remainingDownloadBytes >= 0) { "remainingDownloadBytes must not be negative: $remainingDownloadBytes" }
        require(marginBytes >= 0) { "marginBytes must not be negative: $marginBytes" }
        return availableBytes >= remainingDownloadBytes + marginBytes
    }

    /** How many more bytes are needed before [hasSufficientSpace] would return true, or 0 if it already does. */
    fun shortfallBytes(
        availableBytes: Long,
        remainingDownloadBytes: Long,
        marginBytes: Long = DEFAULT_MARGIN_BYTES,
    ): Long = (remainingDownloadBytes + marginBytes - availableBytes).coerceAtLeast(0L)
}
