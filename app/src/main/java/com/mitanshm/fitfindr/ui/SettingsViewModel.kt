package com.mitanshm.fitfindr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitanshm.fitfindr.data.model.ModelDownloadState
import com.mitanshm.fitfindr.data.model.ModelDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val modelDownloader: ModelDownloader,
    ) : ViewModel() {
        val downloadState: StateFlow<ModelDownloadState> =
            modelDownloader.observeDownloadState()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ModelDownloadState.NotStarted)

        fun isModelReady(): Boolean = modelDownloader.isModelReady()

        fun startDownload() = modelDownloader.enqueueDownload()

        fun cancelDownload() = modelDownloader.cancelDownload()

        fun deleteModel() {
            viewModelScope.launch { modelDownloader.deleteModel() }
        }

        /**
         * The active inference accelerator, per the PRD's requirement that
         * Settings shows this. Honest gap (see docs/PLAN.md and
         * `SettingsScreen`'s doc comment): `MediaPipeVlmEngine` is not the
         * bound `VlmEngine` (`InferenceModule` still binds `FakeVlmEngine`,
         * see that file's doc comment), so there is no real accelerator to
         * report yet. This always returns the fixed string below rather
         * than fabricating a CPU/GPU/NPU value nobody has observed.
         */
        fun activeAcceleratorLabel(): String = "Not applicable — running against FakeVlmEngine (see docs/PLAN.md)"

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
