package com.mitanshm.fitfindr.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * PLACEHOLDER, not a real implementation. Real model download/delete
 * management and the active-accelerator (CPU/GPU/NPU) display are milestone
 * 4/5 work (see PRD.md, docs/PLAN.md) and do not exist yet -- this screen
 * exists only so navigation has somewhere to go. Do not treat this as
 * milestone 4/5 being done.
 */
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Coming in milestones 4-5: model download/delete management and active accelerator display.")
            Button(onClick = onBack) { Text("Back") }
        }
    }
}
