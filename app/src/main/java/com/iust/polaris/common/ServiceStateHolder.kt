package com.iust.polaris.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Defines the possible states of the collection service.
 */
enum class ServiceStatus {
    STOPPED,
    INITIALIZING,
    COLLECTING
}

/**
 * A Singleton class provided by Hilt to hold and broadcast the real-time status and
 * data of the NetworkMetricService.
 */
@Singleton
class ServiceStateHolder @Inject constructor() {
    private val _serviceStatus = MutableStateFlow(ServiceStatus.STOPPED)
    val serviceStatus = _serviceStatus.asStateFlow()

    // Add a StateFlow for the formatted duration string
    private val _durationFormatted = MutableStateFlow("00:00:00")
    val durationFormatted = _durationFormatted.asStateFlow()

    fun updateStatus(status: ServiceStatus) {
        _serviceStatus.value = status
    }

    fun updateDuration(duration: String) {
        _durationFormatted.value = duration
    }
}
