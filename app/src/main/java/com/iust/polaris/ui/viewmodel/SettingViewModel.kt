package com.iust.polaris.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iust.polaris.data.local.SettingsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager
) : ViewModel() {

    // Expose the theme preference flow from the SettingsManager
    val themePreference: StateFlow<String> = settingsManager.themePreferenceFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "System"
        )

    // Expose the collection interval flow
    val collectionInterval: StateFlow<Int> = settingsManager.collectionIntervalFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 15
        )

    // --- New StateFlows for Sync Settings ---
    val autoSyncEnabled: StateFlow<Boolean> = settingsManager.autoSyncEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true // Match the default in SettingsManager
        )

    val syncInterval: StateFlow<Int> = settingsManager.syncIntervalFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 60 // Match the default in SettingsManager
        )


    // --- Functions to Update Settings ---

    fun updateTheme(theme: String) {
        viewModelScope.launch {
            settingsManager.setThemePreference(theme)
        }
    }

    fun updateCollectionInterval(intervalInSeconds: Int) {
        viewModelScope.launch {
            settingsManager.setCollectionInterval(intervalInSeconds)
        }
    }

    // New function to update auto-sync
    fun updateAutoSync(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setAutoSyncEnabled(isEnabled)
        }
    }

    // New function to update sync interval
    fun updateSyncInterval(intervalInMinutes: Int) {
        viewModelScope.launch {
            settingsManager.setSyncInterval(intervalInMinutes)
        }
    }
}
