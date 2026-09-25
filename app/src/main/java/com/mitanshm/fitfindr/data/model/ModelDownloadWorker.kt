package com.mitanshm.fitfindr.data.model

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * WorkManager `CoroutineWorker` that performs the actual model download:
 * HTTP GET with a `Range` header for resume support, a storage-space
 * precheck before writing anything, and a SHA-256 checksum verification
 * pass once the file is fully written.
 *
 * UNVERIFIED IN THIS ENVIRONMENT (honesty, see docs/PLAN.md): there is no
 * Android SDK, emulator, device, or reachable model-hosting endpoint in
 * this container, so this class has never actually been run — it is
 * implemented against the documented `HttpURLConnection`/WorkManager APIs
 * and reviewed by eye, not exercised. The pure decision logic it calls out
 * to ([DownloadPlanner], [StoragePrecheck], [ChecksumVerifier]) IS unit
 * tested and independently verified (see those files' tests); only the
 * glue code in this class (network I/O, file I/O, WorkManager plumbing) is
 * unverified.
 */
@HiltWorker
class ModelDownloadWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
    ) : CoroutineWorker(appContext, workerParams) {

        override suspend fun doWork(): Result {
            val modelFile = File(applicationContext.filesDir, ModelConfig.MODEL_FILE_NAME)
            val partialFile = File(applicationContext.filesDir, ModelConfig.PARTIAL_FILE_NAME)

            if (modelFile.exists() && ChecksumVerifier.sha256(modelFile) == ModelConfig.EXPECTED_SHA256) {
                return Result.success(buildProgressData(ModelConfig.APPROX_SIZE_BYTES, ModelConfig.APPROX_SIZE_BYTES))
            }

            val action = DownloadPlanner.plan(partialFile.takeIf { it.exists() }?.length() ?: 0L, ModelConfig.APPROX_SIZE_BYTES)
            if (action is DownloadAction.RestartFromScratch) {
                partialFile.delete()
            }

            val availableBytes = applicationContext.filesDir.usableSpace
            val remaining =
                when (action) {
                    is DownloadAction.StartFresh -> action.remainingBytes
                    is DownloadAction.Resume -> action.remainingBytes
                    is DownloadAction.RestartFromScratch -> ModelConfig.APPROX_SIZE_BYTES
                }
            if (!StoragePrecheck.hasSufficientSpace(availableBytes, remaining)) {
                return Result.failure(buildErrorData("Not enough free storage for the model download."))
            }

            return try {
                downloadWithResume(partialFile, action)
                if (ChecksumVerifier.sha256(partialFile) != ModelConfig.EXPECTED_SHA256) {
                    partialFile.delete()
                    return Result.failure(buildErrorData("Checksum mismatch after download; deleted corrupt file."))
                }
                partialFile.renameTo(modelFile)
                Result.success(buildProgressData(ModelConfig.APPROX_SIZE_BYTES, ModelConfig.APPROX_SIZE_BYTES))
            } catch (e: java.io.IOException) {
                // Leave the .partial file in place so the next enqueue can resume from it.
                Result.retry().also { setProgressAsync(buildErrorData(e.message ?: "I/O error during download")) }
            }
        }

        private fun downloadWithResume(
            partialFile: File,
            action: DownloadAction,
        ) {
            val startOffset = (action as? DownloadAction.Resume)?.fromByteOffset ?: 0L
            val connection = URL(ModelConfig.DOWNLOAD_URL).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            if (startOffset > 0) {
                connection.setRequestProperty("Range", "bytes=$startOffset-")
            }
            connection.connect()

            // A server that ignores Range and returns 200 (not 206) cannot be
            // resumed from an offset -- restart the partial file rather than
            // corrupt it by appending onto the wrong position.
            val append = startOffset > 0 && connection.responseCode == HTTP_PARTIAL_CONTENT
            if (startOffset > 0 && !append) {
                partialFile.delete()
            }

            connection.inputStream.use { input ->
                java.io.FileOutputStream(partialFile, append).use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    var totalWritten = if (append) startOffset else 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        totalWritten += read
                        setProgressAsync(buildProgressData(totalWritten, ModelConfig.APPROX_SIZE_BYTES))
                    }
                }
            }
            connection.disconnect()
        }

        private fun buildProgressData(
            downloadedBytes: Long,
            totalBytes: Long,
        ) = androidx.work.Data.Builder()
            .putLong(KEY_DOWNLOADED_BYTES, downloadedBytes)
            .putLong(KEY_TOTAL_BYTES, totalBytes)
            .build()

        private fun buildErrorData(message: String) = androidx.work.Data.Builder().putString(KEY_ERROR, message).build()

        companion object {
            const val KEY_DOWNLOADED_BYTES = "downloadedBytes"
            const val KEY_TOTAL_BYTES = "totalBytes"
            const val KEY_ERROR = "error"
            const val WORK_NAME = "model_download"
            private const val HTTP_PARTIAL_CONTENT = 206
            private const val CONNECT_TIMEOUT_MS = 15_000
            private const val READ_TIMEOUT_MS = 15_000
            private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
        }
    }
