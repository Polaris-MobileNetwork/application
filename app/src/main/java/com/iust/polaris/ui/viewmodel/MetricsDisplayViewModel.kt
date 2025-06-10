package com.iust.polaris.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iust.polaris.data.local.NetworkMetric
import com.iust.polaris.data.repository.NetworkMetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MetricsDisplayState(
    val items: List<NetworkMetric> = emptyList(),
    val isLoading: Boolean = false,
    val isPaginating: Boolean = false,
    val endReached: Boolean = false
)

@HiltViewModel
class MetricsDisplayViewModel @Inject constructor(
    private val networkMetricsRepository: NetworkMetricsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MetricsDisplayState())
    val uiState = _uiState.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20

    init {
        loadNextItems(isInitialLoad = true)

        viewModelScope.launch {
            networkMetricsRepository.getAllMetricsFlow()
                .drop(1)
                .onEach {
                    refresh()
                }
                .collect {}
        }
    }

    fun loadNextItems(isInitialLoad: Boolean = false) {
        if (_uiState.value.isPaginating || (_uiState.value.endReached && !isInitialLoad)) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                if (isInitialLoad) it.copy(isLoading = true) else it.copy(isPaginating = true)
            }

            try {
                val newItems = networkMetricsRepository.getMetricsPaged(
                    page = currentPage,
                    pageSize = pageSize
                )
                _uiState.update { currentState ->
                    currentState.copy(
                        items = if (isInitialLoad) newItems else currentState.items + newItems,
                        isLoading = false,
                        isPaginating = false,
                        endReached = newItems.size < pageSize
                    )
                }
                currentPage++
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isPaginating = false) }
            }
        }
    }

    private fun refresh() {
        currentPage = 0
        _uiState.update { it.copy(items = emptyList(), endReached = false) }
        loadNextItems(isInitialLoad = true)
    }
}
