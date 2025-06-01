package com.iust.polaris.ui.viewmodel

import android.app.Application // Application context is needed to start services
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iust.polaris.data.local.NetworkMetric
import com.iust.polaris.data.repository.NetworkMetricsRepository
import com.iust.polaris.service.NetworkMetricService // Import your service
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MetricsUiState(
    val isCollecting: Boolean = false,
    val collectionStatusText: String = "Ready to start collection.",
    val collectedMetrics: List<NetworkMetric> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class MetricsCollectionViewModel @Inject constructor(
    private val application: Application, // Inject Application context
    private val networkMetricsRepository: NetworkMetricsRepository
) : ViewModel() {

    private val _isCollecting = MutableStateFlow(false)
    // No need for public isCollecting if uiState.isCollecting is used by UI
    // val isCollecting: StateFlow<Boolean> = _isCollecting.asStateFlow()

    private val _collectionStatusText = MutableStateFlow("Ready to start collection.")
    // No need for public collectionStatusText if uiState.collectionStatusText is used
    // val collectionStatusText: StateFlow<String> = _collectionStatusText.asStateFlow()

    private val _metricsFromDb: StateFlow<List<NetworkMetric>> =
        networkMetricsRepository.getAllMetrics()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val uiState: StateFlow<MetricsUiState> = combine(
        _isCollecting,
        _collectionStatusText,
        _metricsFromDb
    ) { isCollecting, statusText, metricsList ->
        MetricsUiState(
            isCollecting = isCollecting,
            collectionStatusText = statusText,
            collectedMetrics = metricsList,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MetricsUiState()
    )


    fun onToggleCollection() {
        // We don't need viewModelScope.launch here if start/stopService are not suspend functions
        val currentlyCollecting = _isCollecting.value
        if (currentlyCollecting) {
            NetworkMetricService.stopService(application) // Use application context
            _isCollecting.value = false
            _collectionStatusText.value = "Stopping collection..." // User feedback
        } else {
            NetworkMetricService.startService(application) // Use application context
            _isCollecting.value = true
            _collectionStatusText.value = "Starting collection..." // User feedback
        }
    }

    // Dummy metric insertion is now handled by the service (or will be)
    // We can remove insertDummyMetric() from here if the service handles all insertions.
    // For now, let's keep it to ensure the DB part is testable from ViewModel if service is not fully ready.
    private fun insertDummyMetricForTesting() {
        viewModelScope.launch {
            val dummyMetric = NetworkMetric(
                timestamp = System.currentTimeMillis(),
                networkType = "LTE_Hilt_VM_Test",
                signalStrength = -77,
                latitude = 35.7003,
                longitude = 51.3303,
                cellId = "Hilt_VM_Test_002",
                isUploaded = false,
                plmnId = "43211", lac = 1001, tac = 2002, rac = null, arfcn = 1500,
                frequencyBand = "Band 3", actualFrequencyMhz = 1800.5,
                rsrp = -90, rsrq = -12, rscp = null, ecno = null, rxlev = null
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
