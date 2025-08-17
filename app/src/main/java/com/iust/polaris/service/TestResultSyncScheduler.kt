package com.iust.polaris.service

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.iust.polaris.data.local.SettingsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TestResultSyncScheduler"

@Singleton
class TestResultSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager
) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Reads the user's settings and either schedules a periodic sync worker for test results
     * or cancels any existing one.
     */
    suspend fun scheduleOrCancelSync() {
        val isAutoSyncEnabled = settingsManager.autoSyncEnabledFlow.first()
        val syncIntervalMinutes = settingsManager.syncIntervalFlow.first()

        if (isAutoSyncEnabled) {
            Log.i(TAG, "Auto-sync for test results is ENABLED. Scheduling worker for every $syncIntervalMinutes minutes.")
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicSyncRequest = PeriodicWorkRequestBuilder<TestResultSyncWorker>(
                repeatInterval = syncIntervalMinutes.toLong(),
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                periodicSyncRequest
            )
        } else {
            Log.i(TAG, "Auto-sync for test results is DISABLED. Cancelling any existing sync worker.")
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "PolarisTestResultSyncWorker"
    }
}
