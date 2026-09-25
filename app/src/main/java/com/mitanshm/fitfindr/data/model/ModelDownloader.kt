package com.mitanshm.fitfindr.data.model

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Public API for managing the on-device model file: start/resume a
 * download via WorkManager, observe its progress, check whether a valid
 * (checksum-verified) copy is already present, and delete it.
 *
 * The download itself is delegated to [ModelDownloadWorker]; this class is
 * the thin, injectable facade the UI (`SettingsScreen`) and DI graph talk
 * to. Like [ModelDownloadWorker], this has not been exercised against a
 * real network or device in this environment — see `docs/PLAN.md`.
 */
@Singleton
class ModelDownloader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val workManager get() = WorkManager.getInstance(context)

        private val modelFile get() = File(context.filesDir, ModelConfig.MODEL_FILE_NAME)

        /** True if a model file is present on disk AND its checksum matches [ModelConfig.EXPECTED_SHA256]. */
        fun isModelReady(): Boolean = modelFile.exists() && ChecksumVerifier.sha256(modelFile) == ModelConfig.EXPECTED_SHA256

        fun enqueueDownload() {
            val request =
                OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.UNMETERED)
                            .setRequiresStorageNotLow(true)
                            .build(),
                    )
                    .build()
            workManager.enqueueUniqueWork(ModelDownloadWorker.WORK_NAME, ExistingWorkPolicy.KEEP, request)
        }

        fun cancelDownload() {
            workManager.cancelUniqueWork(ModelDownloadWorker.WORK_NAME)
        }

        /** Deletes any downloaded model and partial-download file. Safe to call even if nothing was downloaded. */
        fun deleteModel(): Boolean {
            val partial = File(context.filesDir, ModelConfig.PARTIAL_FILE_NAME)
            val deletedPartial = !partial.exists() || partial.delete()
            val deletedModel = !modelFile.exists() || modelFile.delete()
            return deletedPartial && deletedModel
        }

        /** Emits the current download status as WorkManager reports it. */
        fun observeDownloadState(): Flow<ModelDownloadState> =
            workManager.getWorkInfosForUniqueWorkFlow(ModelDownloadWorker.WORK_NAME).map { infos ->
                val info = infos.firstOrNull() ?: return@map ModelDownloadState.NotStarted
                when (info.state) {
                    WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> ModelDownloadState.Queued
                    WorkInfo.State.RUNNING ->
                        ModelDownloadState.InProgress(
                            downloadedBytes = info.progress.getLong(ModelDownloadWorker.KEY_DOWNLOADED_BYTES, 0L),
                            totalBytes = info.progress.getLong(ModelDownloadWorker.KEY_TOTAL_BYTES, ModelConfig.APPROX_SIZE_BYTES),
                        )
                    WorkInfo.State.SUCCEEDED -> ModelDownloadState.Complete
                    WorkInfo.State.FAILED ->
                        ModelDownloadState.Failed(
                            info.outputData.getString(ModelDownloadWorker.KEY_ERROR) ?: "Download failed",
                        )
                    WorkInfo.State.CANCELLED -> ModelDownloadState.NotStarted
                }
            }
    }

sealed interface ModelDownloadState {
    data object NotStarted : ModelDownloadState

    data object Queued : ModelDownloadState

    data class InProgress(val downloadedBytes: Long, val totalBytes: Long) : ModelDownloadState

    data object Complete : ModelDownloadState

    data class Failed(val message: String) : ModelDownloadState
}
