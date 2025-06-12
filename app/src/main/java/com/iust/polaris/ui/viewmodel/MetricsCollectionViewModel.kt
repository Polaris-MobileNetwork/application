package com.iust.polaris.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iust.polaris.common.ServiceStateHolder
import com.iust.polaris.common.ServiceStatus
import com.iust.polaris.data.repository.NetworkMetricsRepository
import com.iust.polaris.service.NetworkMetricService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class MainScreenUiState(
    val serviceStatus: ServiceStatus = ServiceStatus.STOPPED,
    val collectionDuration: String = "00:00:00",
    val lastSyncStatus: String = "Never synced",
    val isSyncing: Boolean = false // To show a loading state for sync
) {
    val isCollecting: Boolean get() = serviceStatus == ServiceStatus.COLLECTING
    val isServiceInitializing: Boolean get() = serviceStatus == ServiceStatus.INITIALIZING
    val statusText: String get() = when (serviceStatus) {
        ServiceStatus.STOPPED -> "Service is stopped."
        ServiceStatus.INITIALIZING -> "Starting..."
        ServiceStatus.COLLECTING -> "Collecting data..."
    }
}

@HiltViewModel
class MetricsCollectionViewModel @Inject constructor(
    private val application: Application,
    private val serviceStateHolder: ServiceStateHolder,
    // Re-inject the repository to handle sync operations
    private val networkMetricsRepository: NetworkMetricsRepository
) : ViewModel() {

    private val timerJob: Job? = null
    private val _duration = MutableStateFlow("00:00:00")
    // Add states for sync status
    private val _lastSyncStatus = MutableStateFlow("Never synced")
    private val _isSyncing = MutableStateFlow(false)

    val uiState: StateFlow<MainScreenUiState> = combine(
        serviceStateHolder.serviceStatus,
        _duration,
        _lastSyncStatus,
        _isSyncing
    ) { status, duration, syncStatus, isSyncing ->
        // Timer logic is now handled by the service, so we don't need to manage it here.
        // This ViewModel just consumes the state.
        MainScreenUiState(
            serviceStatus = status,
            collectionDuration = duration,
            lastSyncStatus = syncStatus,
            isSyncing = isSyncing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainScreenUiState()
    )

    fun onToggleCollection() {
        val currentStatus = serviceStateHolder.serviceStatus.value
        if (currentStatus == ServiceStatus.INITIALIZING) return

        if (currentStatus == ServiceStatus.COLLECTING) {
            NetworkMetricService.stopService(application)
        } else {
            NetworkMetricService.startService(application)
        }
    }

    /**
     * Called when the sync button is clicked. It triggers the repository's
     * sync function and updates the UI accordingly.
     */
    fun onSyncClicked() {
        if (_isSyncing.value) return // Prevent multiple syncs at once

        viewModelScope.launch {
            _isSyncing.value = true
            _lastSyncStatus.value = "Syncing..."

            val success = networkMetricsRepository.syncMetrics()

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            if (success) {
                _lastSyncStatus.value = "Last synced: ${sdf.format(Date())}"
            } else {
                _lastSyncStatus.value = "Sync failed. Check logs."
            }
            _isSyncing.value = false
        }
    }

    fun clearAllCollectedMetrics() {
        viewModelScope.launch {
            networkMetricsRepository.clearAllMetrics()
        }
    }
}
