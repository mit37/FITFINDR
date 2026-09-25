package com.mitanshm.fitfindr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mitanshm.fitfindr.domain.Garment
import com.mitanshm.fitfindr.domain.OutfitResult

/**
 * Renders an [OutfitResult] produced by [ResultViewModel] (backed, in this
 * milestone, by `FakeVlmEngine`'s fixture -- see docs/PLAN.md). This is real
 * Compose UI code, but it has only been checked by reading, not by running
 * the app or a Compose UI test, because this environment has no Android
 * SDK/device (see docs/PLAN.md, "Cloud-instance constraints").
 */
@Composable
fun ResultScreen(
    onDone: () -> Unit,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your outfit") }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when (val s = state) {
                is ResultUiState.Loading -> Text("Describing outfit on-device…")
                is ResultUiState.Error -> Text("Could not describe this outfit: ${s.message}")
                is ResultUiState.Success -> {
                    OutfitResultView(s.outfit)
                    Button(onClick = { ShareImage.share(context, ShareImage.render(s.outfit)) }) {
                        Text("Share as image")
                    }
                }
            }
            Button(onClick = onDone) { Text("Done") }
        }
    }
}

@Composable
private fun OutfitResultView(outfit: OutfitResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Style: ${outfit.styleLabel}")

        Text("Palette:")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            outfit.palette.forEach { swatch -> Text(swatch) }
        }

        Text("Garments:")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(outfit.garments) { garment -> GarmentCard(garment) }
        }
    }
}

@Composable
private fun GarmentCard(garment: Garment) {
    Card(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("${garment.type} — ${garment.color}")
            Text("Material: ${garment.material}, fit: ${garment.fit}")
            garment.searchQueries.forEach { query -> Text("Search: \"$query\"") }
        }
    }
}
