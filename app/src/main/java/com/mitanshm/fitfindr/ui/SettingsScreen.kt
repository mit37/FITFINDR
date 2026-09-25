package com.mitanshm.fitfindr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mitanshm.fitfindr.data.model.ModelConfig
import com.mitanshm.fitfindr.data.model.ModelDownloadState

/**
 * Real model-management UI (milestone 4/5): shows current download state,
 * lets the user start/cancel a download or delete the model, and displays
 * the active inference accelerator. Replaces the milestone-2 placeholder.
 *
 * HONEST GAP (see docs/PLAN.md): the accelerator display is currently
 * always "not applicable" because `InferenceModule` binds `FakeVlmEngine`,
 * not `MediaPipeVlmEngine` -- see `SettingsViewModel.activeAcceleratorLabel`
 * and `InferenceModule`'s doc comment for why, and what switches it. The
 * download UI itself is real, wired to the real `ModelDownloader`
 * (WorkManager), but has never been exercised against a real network or
 * device -- see `docs/PLAN.md`, "Cloud-instance constraints".
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val downloadState by viewModel.downloadState.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Model", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(ModelConfig.MODEL_FILE_NAME)

            ModelDownloadStatus(downloadState, viewModel.isModelReady())

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.startDownload() },
                    enabled = downloadState !is ModelDownloadState.InProgress && downloadState !is ModelDownloadState.Queued,
                ) { Text("Download model") }

                Button(
                    onClick = { viewModel.cancelDownload() },
                    enabled = downloadState is ModelDownloadState.InProgress || downloadState is ModelDownloadState.Queued,
                ) { Text("Cancel") }

                Button(onClick = { viewModel.deleteModel() }) { Text("Delete model") }
            }

            HorizontalDivider()

            Text("Active accelerator", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Text(viewModel.activeAcceleratorLabel())

            HorizontalDivider()

            Button(onClick = onBack) { Text("Back") }
        }
    }
}

@Composable
private fun ModelDownloadStatus(
    state: ModelDownloadState,
    isReady: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        when (state) {
            is ModelDownloadState.NotStarted -> Text(if (isReady) "Downloaded and checksum-verified." else "Not downloaded.")
            is ModelDownloadState.Queued -> Text("Queued…")
            is ModelDownloadState.InProgress -> {
                val fraction = if (state.totalBytes > 0) (state.downloadedBytes.toFloat() / state.totalBytes).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                Text("${formatMb(state.downloadedBytes)} / ${formatMb(state.totalBytes)} MB")
            }
            is ModelDownloadState.Complete -> Text("Download complete.")
            is ModelDownloadState.Failed -> Text("Download failed: ${state.message}")
        }
    }
}

private fun formatMb(bytes: Long): String = "%.1f".format(bytes / (1024.0 * 1024.0))
