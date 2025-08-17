package com.iust.polaris.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iust.polaris.common.ServiceStateHolder
import com.iust.polaris.common.ServiceStatus
import com.iust.polaris.data.local.NetworkMetric
import com.iust.polaris.data.repository.NetworkMetricsRepository
import com.iust.polaris.service.NetworkMetricService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MainUiState(
    // Service & Monitor Tab State
    val serviceStatus: ServiceStatus = ServiceStatus.STOPPED,
    val collectionDuration: String = "00:00:00",
    val lastSyncStatus: String = "Never synced",
    val isSyncing: Boolean = false,
    val unsyncedMetricsCount: Int = 0,

    // Metrics Display Tab State
    val allMetrics: List<NetworkMetric> = emptyList(),
    val isLoadingMetrics: Boolean = true
) {
    // Helper properties for the UI
    val isServiceInitializing: Boolean get() = serviceStatus == ServiceStatus.INITIALIZING
    val serviceStatusText: String get() = when (serviceStatus) {
        ServiceStatus.STOPPED -> "Service is stopped."
        ServiceStatus.INITIALIZING -> "Starting..."
        ServiceStatus.COLLECTING -> "Collecting data..."
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val serviceStateHolder: ServiceStateHolder,
    private val networkMetricsRepository: NetworkMetricsRepository
) : ViewModel() {

    private val _isSyncing = MutableStateFlow(false)
    private val _lastSyncStatus = MutableStateFlow("Never synced")

    private val _snackbarEventChannel = Channel<String>()
    val snackbarEvents = _snackbarEventChannel.receiveAsFlow()
    val uiState: StateFlow<MainUiState> = combine(
        // --- FIX: Pass the flows as a list to the combine operator ---
        listOf(
            serviceStateHolder.serviceStatus,
            serviceStateHolder.durationFormatted,
            _lastSyncStatus,
            _isSyncing,
            networkMetricsRepository.getUnsyncedMetricsCount(),
            networkMetricsRepository.getAllMetricsFlow()
        )
    ) { values ->
        // Destructure the array of values with type casting
        val status = values[0] as ServiceStatus
        val duration = values[1] as String
        val syncStatus = values[2] as String
        val isSyncing = values[3] as Boolean
        val unsyncedCount = values[4] as Int
        val allMetrics = values[5] as List<NetworkMetric>

        // Create the UI state from the combined values
        MainUiState(
            serviceStatus = status,
            collectionDuration = duration,
            lastSyncStatus = syncStatus,
            isSyncing = isSyncing,
            unsyncedMetricsCount = unsyncedCount,
            allMetrics = allMetrics,
            isLoadingMetrics = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    fun onToggleCollection() {
        if (uiState.value.isServiceInitializing) return
        if (uiState.value.serviceStatus == ServiceStatus.COLLECTING) {
            NetworkMetricService.stopService(application)
        } else {
            NetworkMetricService.startService(application)
        }
    }

    fun onSyncClicked() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _lastSyncStatus.value = "Syncing..."

            val unsyncedCountBefore = uiState.value.unsyncedMetricsCount
            if (unsyncedCountBefore == 0) {
                _snackbarEventChannel.send("No new metrics to sync.")
                _isSyncing.value = false
                _lastSyncStatus.value = "Already up-to-date."
                return@launch
            }

            val success = networkMetricsRepository.syncMetrics()

            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            if (success) {
                _lastSyncStatus.value = "Last synced: ${sdf.format(Date())}"
                _snackbarEventChannel.send("$unsyncedCountBefore metrics synced successfully!")
            } else {
                _lastSyncStatus.value = "Sync failed. Check logs."
                _snackbarEventChannel.send("Sync failed.")
            }
            _isSyncing.value = false
        }
    }
}
