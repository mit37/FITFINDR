package com.mitanshm.fitfindr.data.model

/**
 * Pure decision logic for how to (re)start a resumable download, given how
 * many bytes already exist on disk from a previous, possibly-interrupted
 * attempt, and how large the final file is expected to be.
 *
 * No Android/network/filesystem import — this only reasons about `Long`
 * byte counts and returns a [DownloadAction] for the caller (the real
 * `HttpURLConnection`/WorkManager code in [ModelDownloadWorker]) to act on.
 * Unit-tested directly, and via the standalone JVM verification described
 * in `docs/PLAN.md`.
 */
object DownloadPlanner {

    /**
     * @param existingPartialBytes bytes already present in the `.partial`
     *   file on disk (0 if none exists yet).
     * @param expectedTotalBytes the full expected size of the final file.
     */
    fun plan(
        existingPartialBytes: Long,
        expectedTotalBytes: Long,
    ): DownloadAction {
        require(existingPartialBytes >= 0) { "existingPartialBytes must not be negative" }
        require(expectedTotalBytes > 0) { "expectedTotalBytes must be positive" }

        return when {
            existingPartialBytes <= 0L -> DownloadAction.StartFresh(remainingBytes = expectedTotalBytes)
            existingPartialBytes >= expectedTotalBytes ->
                // A same-or-larger partial file than expected is treated as
                // corrupt/stale (e.g. server changed the asset), not "done" --
                // resuming from a byte offset past the real end would produce
                // garbage. Restart clean rather than guess.
                DownloadAction.RestartFromScratch(reason = "existing partial file is >= expected size; likely stale")
            else ->
                DownloadAction.Resume(
                    fromByteOffset = existingPartialBytes,
                    remainingBytes = expectedTotalBytes - existingPartialBytes,
                )
        }
    }
}

sealed interface DownloadAction {
    data class StartFresh(val remainingBytes: Long) : DownloadAction

    data class Resume(val fromByteOffset: Long, val remainingBytes: Long) : DownloadAction

    data class RestartFromScratch(val reason: String) : DownloadAction
}
