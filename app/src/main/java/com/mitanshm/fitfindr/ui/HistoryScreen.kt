package com.mitanshm.fitfindr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mitanshm.fitfindr.data.db.OutfitHistoryItem
import java.text.DateFormat
import java.util.Date

/**
 * Room-backed outfit history: lists every saved [com.mitanshm.fitfindr.domain.OutfitResult]
 * (newest first), lets the user tap one to expand it, delete it, or share
 * it as an image via [ShareImage]. Replaces the milestone-2 placeholder.
 *
 * Like the rest of the Compose UI in this repo, this has been written
 * against the real Room/Compose APIs and reviewed by eye, but not run on a
 * device or emulator -- see docs/PLAN.md, "Cloud-instance constraints".
 */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val items by viewModel.history.collectAsState()
    val context = LocalContext.current

    Scaffold(topBar = { TopAppBar(title = { Text("History") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (items.isEmpty()) {
                Text("No saved outfits yet. Describe one to see it here.")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items, key = { it.id }) { item ->
                        HistoryRow(
                            item = item,
                            onDelete = { viewModel.delete(item.id) },
                            onShare = { ShareImage.share(context, ShareImage.render(item.result)) },
                        )
                    }
                }
            }
            Button(onClick = onBack) { Text("Back") }
        }
    }
}

@Composable
private fun HistoryRow(
    item: OutfitHistoryItem,
    onDelete: () -> Unit,
    onShare: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${item.result.styleLabel} — ${DateFormat.getDateTimeInstance().format(Date(item.createdAtEpochMs))}")

            if (expanded) {
                item.result.garments.forEach { garment ->
                    Text("${garment.type} — ${garment.color} (${garment.material}, ${garment.fit})")
                }
                Text("Palette: ${item.result.palette.joinToString(", ")}")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Collapse" else "Expand") }
                TextButton(onClick = onShare) { Text("Share") }
                TextButton(onClick = onDelete) { Text("Delete") }
            }
        }
    }
}
