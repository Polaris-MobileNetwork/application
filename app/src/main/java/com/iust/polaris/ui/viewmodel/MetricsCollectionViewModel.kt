package com.iust.polaris.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iust.polaris.common.ServiceStateHolder
import com.iust.polaris.common.ServiceStatus
import com.iust.polaris.service.NetworkMetricService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// UI State is now derived from the service's state
data class MainScreenUiState(
    val serviceStatus: ServiceStatus = ServiceStatus.STOPPED,
    val collectionDuration: String = "00:00:00"
) {
    // Helper properties for the UI to easily react to the service status
    val isCollecting: Boolean get() = serviceStatus == ServiceStatus.COLLECTING
    val isServiceInitializing: Boolean get() = serviceStatus == ServiceStatus.INITIALIZING
    val statusText: String get() = when (serviceStatus) {
        ServiceStatus.STOPPED -> "Service is stopped"
        ServiceStatus.INITIALIZING -> "Starting"
        ServiceStatus.COLLECTING -> "Collecting data"
    }
}

@HiltViewModel
class MetricsCollectionViewModel @Inject constructor(
    private val application: Application,
    private val serviceStateHolder: ServiceStateHolder // Inject the state holder
) : ViewModel() {

    // The main UI state is now a direct combination of the flows from the ServiceStateHolder
    val uiState: StateFlow<MainScreenUiState> = combine(
        serviceStateHolder.serviceStatus,
        serviceStateHolder.durationFormatted
    ) { status, duration ->
        MainScreenUiState(
            serviceStatus = status,
            collectionDuration = duration
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainScreenUiState() // Initial state
    )

    fun onToggleCollection() {
        val currentStatus = serviceStateHolder.serviceStatus.value
        // Ignore clicks while the service is already in the process of starting
        if (currentStatus == ServiceStatus.INITIALIZING) return

        if (currentStatus == ServiceStatus.COLLECTING) {
            NetworkMetricService.stopService(application)
        } else {
            // Permissions are assumed to be granted by the UI layer (HandlePermissions)
            NetworkMetricService.startService(application)
        }
    }
}
