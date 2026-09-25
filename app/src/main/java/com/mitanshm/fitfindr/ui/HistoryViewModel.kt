package com.mitanshm.fitfindr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitanshm.fitfindr.data.db.OutfitHistoryItem
import com.mitanshm.fitfindr.data.db.OutfitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val repository: OutfitRepository,
    ) : ViewModel() {
        val history: StateFlow<List<OutfitHistoryItem>> =
            repository.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        fun delete(outfitId: Long) {
            viewModelScope.launch { repository.delete(outfitId) }
        }

        private companion object {
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
