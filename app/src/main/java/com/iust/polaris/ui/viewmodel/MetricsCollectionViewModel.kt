package com.iust.polaris.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iust.polaris.data.local.NetworkMetric
import com.iust.polaris.data.repository.NetworkMetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Data class to hold the entire state of our screen (remains the same)
data class MetricsUiState(
    val isCollecting: Boolean = false,
    val collectionStatusText: String = "Ready to start collection.",
    val collectedMetrics: List<NetworkMetric> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel // Annotation to enable Hilt injection for this ViewModel
class MetricsCollectionViewModel @Inject constructor( // Inject the repository
    private val networkMetricsRepository: NetworkMetricsRepository
) : ViewModel() {

    // Private mutable state for collection status and other UI elements not directly from DB
    private val _isCollecting = MutableStateFlow(false)
    val isCollecting: StateFlow<Boolean> = _isCollecting.asStateFlow()

    private val _collectionStatusText = MutableStateFlow("Ready to start collection.")
    val collectionStatusText: StateFlow<String> = _collectionStatusText.asStateFlow()

    // Observe metrics from the repository
    private val _metricsFromDb: StateFlow<List<NetworkMetric>> =
        networkMetricsRepository.getAllMetrics()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000), // Keep flow active for 5s after last subscriber
                initialValue = emptyList()
            )

    // Combine all state sources into a single UiState flow
    val uiState: StateFlow<MetricsUiState> = combine(
        _isCollecting,
        _collectionStatusText,
        _metricsFromDb
    ) { isCollecting, statusText, metricsList ->
        MetricsUiState(
            isCollecting = isCollecting,
            collectionStatusText = statusText,
            collectedMetrics = metricsList,
            isLoading = false // Set to false once metrics are loaded (even if empty)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MetricsUiState() // Initial state with isLoading = true
    )


    fun onToggleCollection() {
        viewModelScope.launch {
            val currentlyCollecting = _isCollecting.value
            if (currentlyCollecting) {
                // Logic to stop collection will go here
                _isCollecting.value = false
                _collectionStatusText.value = "Collection stopped."
            } else {
                // Logic to start collection will go here
                _isCollecting.value = true
                _collectionStatusText.value = "Collecting network data..."
                insertDummyMetric()
            }
        }
    }

    private fun insertDummyMetric() {
        viewModelScope.launch {
            val dummyMetric = NetworkMetric(
                timestamp = System.currentTimeMillis(),
                networkType = "LTE_Hilt", // Updated for clarity
                signalStrength = -85,
                latitude = 35.7000,
                longitude = 51.3300,
                cellId = "12345_Hilt",
                isUploaded = false,
                plmnId = "43211",
                lac = 1001,
                tac = 2002,
                rac = null,
                arfcn = 1500,
                frequencyBand = "Band 3",
                actualFrequencyMhz = 1800.5,
                rsrp = -90,
                rsrq = -12,
                rscp = null,
                ecno = null,
                rxlev = null
            )
            networkMetricsRepository.insertMetric(dummyMetric)
        }
    }

    fun clearAllCollectedMetrics() {
        viewModelScope.launch {
            networkMetricsRepository.clearAllMetrics()
        }
    }
}
