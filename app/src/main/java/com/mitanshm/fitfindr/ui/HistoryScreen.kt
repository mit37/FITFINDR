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
 * PLACEHOLDER, not a real implementation. The real Room-backed history list
 * and share-as-image feature are milestone 6 (see PRD.md, docs/PLAN.md) and
 * do not exist yet -- this screen exists only so navigation has somewhere
 * to go. Do not treat this as milestone 6 being done.
 */
@Composable
fun HistoryScreen(onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("History") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Coming in milestone 6: Room-backed outfit history + share-as-image.")
            Button(onClick = onBack) { Text("Back") }
        }
    }
}
