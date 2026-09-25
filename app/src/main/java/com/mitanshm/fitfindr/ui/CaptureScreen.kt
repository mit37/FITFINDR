package com.mitanshm.fitfindr.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Entry point: capture a photo with the camera, or pick one from the
 * gallery via the Android Photo Picker, then hand it off for inference.
 *
 * NOTE (honesty, see docs/PLAN.md): the CameraX preview binding itself is
 * intentionally NOT wired up to a live `PreviewView` in this milestone,
 * because there is no device/emulator in this development environment to
 * verify a camera binding against, and shipping an unverified camera
 * lifecycle binding would be worse than being explicit about the gap. The
 * gallery path (Android Photo Picker, `PickVisualMedia`) needs no camera
 * hardware and is wired for real. Wiring the live camera preview is
 * tracked for a session with real device access.
 */
@Composable
fun CaptureScreen(
    onOutfitCaptured: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var pickedImageUri by remember { mutableStateOf<String?>(null) }

    val pickMedia =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                pickedImageUri = uri.toString()
                onOutfitCaptured()
            }
        }

    Scaffold(
        topBar = { TopAppBar(title = { Text("FitFindr") }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text("Name your outfit from one photo. Nothing leaves your phone.")

            Button(onClick = {
                pickMedia.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                    ),
                )
            }) {
                Text("Pick from gallery")
            }

            Button(onClick = onOutfitCaptured) {
                // Camera capture: scaffolded, not wired to a live CameraX
                // PreviewView in this milestone — see the NOTE above.
                Text("Take photo (camera preview not wired in this build)")
            }

            Button(onClick = onOpenHistory) { Text("History") }
            Button(onClick = onOpenSettings) { Text("Settings") }
        }
    }
}
