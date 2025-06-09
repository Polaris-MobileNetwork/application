package com.iust.polaris.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Define keys for your settings
object SettingsKeys {
    val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
    val COLLECTION_INTERVAL_SECONDS = intPreferencesKey("collection_interval_seconds")
    // --- New Keys Added ---
    val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
    val SYNC_INTERVAL_MINUTES = intPreferencesKey("sync_interval_minutes")
}

// Create a DataStore instance via delegation
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsManager @Inject constructor(@ApplicationContext private val context: Context) {

    // --- Theme Preference ---
    val themePreferenceFlow = context.dataStore.data.map { preferences ->
        preferences[SettingsKeys.THEME_PREFERENCE] ?: "System"
    }
    suspend fun setThemePreference(theme: String) {
        context.dataStore.edit { settings ->
            settings[SettingsKeys.THEME_PREFERENCE] = theme
        }
    }

    // --- Collection Interval ---
    val collectionIntervalFlow = context.dataStore.data.map { preferences ->
        preferences[SettingsKeys.COLLECTION_INTERVAL_SECONDS] ?: 15 // Default 15 seconds
    }
    suspend fun setCollectionInterval(intervalInSeconds: Int) {
        context.dataStore.edit { settings ->
            settings[SettingsKeys.COLLECTION_INTERVAL_SECONDS] = intervalInSeconds
        }
    }

    // --- Auto-sync Preference (New) ---
    val autoSyncEnabledFlow = context.dataStore.data.map { preferences ->
        preferences[SettingsKeys.AUTO_SYNC_ENABLED] ?: true // Default to enabled
    }
    suspend fun setAutoSyncEnabled(isEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[SettingsKeys.AUTO_SYNC_ENABLED] = isEnabled
        }
    }

    // --- Sync Interval Preference (New) ---
    val syncIntervalFlow = context.dataStore.data.map { preferences ->
        preferences[SettingsKeys.SYNC_INTERVAL_MINUTES] ?: 60 // Default 60 minutes
    }
    suspend fun setSyncInterval(intervalInMinutes: Int) {
        context.dataStore.edit { settings ->
            settings[SettingsKeys.SYNC_INTERVAL_MINUTES] = intervalInMinutes
        }
    }
}
