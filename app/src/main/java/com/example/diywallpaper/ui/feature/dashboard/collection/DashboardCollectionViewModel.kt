package com.example.diywallpaper.ui.feature.dashboard.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.diywallpaper.core.result.AppResult
import com.example.diywallpaper.domain.usecase.design.ObserveUserDesignsUseCase
import com.example.diywallpaper.domain.usecase.design.DeleteDesignUseCase
import com.example.diywallpaper.domain.usecase.design.RenameDesignUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class DashboardCollectionViewModel @Inject constructor(
    private val observeUserDesignsUseCase: ObserveUserDesignsUseCase,
    private val renameDesignUseCase: RenameDesignUseCase,
    private val deleteDesignUseCase: DeleteDesignUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardCollectionUiState())
    val uiState: StateFlow<DashboardCollectionUiState> = _uiState.asStateFlow()

    init {
        observeDesigns()
    }

    fun onFilterSelected(filter: CollectionFilter) {
        _uiState.update { state ->
            state.copy(selectedFilter = filter)
        }
    }

    fun onDesignSelected(designId: String) {
        _uiState.update { state ->
            state.copy(pendingOpenDesignId = designId)
        }
    }

    fun consumePendingOpenDesign() {
        _uiState.update { state ->
            state.copy(pendingOpenDesignId = null)
        }
    }

    fun renameDesign(designId: String, title: String) {
        viewModelScope.launch {
            when (val result = renameDesignUseCase(designId, title.trim())) {
                is AppResult.Success -> {
                    _uiState.update { state -> state.copy(errorMessage = null) }
                }
                is AppResult.Error -> {
                    _uiState.update { state ->
                        state.copy(errorMessage = result.error.toString().substringBefore("("))
                    }
                }
            }
        }
    }

    fun deleteDesign(designId: String) {
        viewModelScope.launch {
            when (val result = deleteDesignUseCase(designId)) {
                is AppResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            errorMessage = null,
                            pendingOpenDesignId = state.pendingOpenDesignId.takeUnless { it == designId }
                        )
                    }
                }
                is AppResult.Error -> {
                    _uiState.update { state ->
                        state.copy(errorMessage = result.error.toString().substringBefore("("))
                    }
                }
            }
        }
    }

    private fun observeDesigns() {
        viewModelScope.launch {
            observeUserDesignsUseCase().collect { designs ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        designCount = designs.size,
                        designs = designs,
                        errorMessage = null
                    )
                }
            }
        }
    }
}
