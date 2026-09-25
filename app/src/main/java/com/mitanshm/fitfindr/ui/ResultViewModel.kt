package com.mitanshm.fitfindr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mitanshm.fitfindr.data.db.OutfitRepository
import com.mitanshm.fitfindr.domain.DescribeOutfitUseCase
import com.mitanshm.fitfindr.domain.OutfitResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ResultUiState {
    data object Loading : ResultUiState

    data class Success(val outfit: OutfitResult) : ResultUiState

    data class Error(val message: String) : ResultUiState
}

@HiltViewModel
class ResultViewModel
    @Inject
    constructor(
        private val describeOutfit: DescribeOutfitUseCase,
        private val outfitRepository: OutfitRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
        val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

        init {
            describe()
        }

        fun describe() {
            viewModelScope.launch {
                _uiState.value = ResultUiState.Loading
                // No real capture pipeline is wired yet (see CaptureScreen's
                // NOTE) -- FakeVlmEngine ignores its input entirely, so an
                // empty placeholder byte array is sufficient here and is not
                // a shortcut around anything real.
                val result = describeOutfit(ByteArray(0))
                _uiState.value =
                    result.fold(
                        onSuccess = { outfit ->
                            // Persist every successful describe to history. Failures are
                            // intentionally not saved -- there is nothing structured to show later.
                            outfitRepository.save(outfit)
                            ResultUiState.Success(outfit)
                        },
                        onFailure = { ResultUiState.Error(it.message ?: "Unknown error") },
                    )
            }
        }
    }
