package com.iust.polaris.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val workManager = WorkManager.getInstance(context)

    fun schedulePeriodicTests() {
        // Create a periodic work request that runs approximately every 15 minutes.
        // This is the minimum interval allowed by WorkManager for periodic tasks.
        val periodicTestRequest = PeriodicWorkRequestBuilder<TestSchedulerWorker>(
            15, TimeUnit.MINUTES
        ).build()

        // Enqueue the work as a unique periodic task.
        // Using ExistingPeriodicWorkPolicy.KEEP means that if a work request with this
        // unique name is already scheduled, the existing one will be kept and the new
        // one will be ignored. This prevents duplicate workers from being scheduled
        // every time the app starts.
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicTestRequest
        )
    }

    companion object {
        // A unique name for our periodic work to prevent duplicates.
        private const val UNIQUE_WORK_NAME = "PolarisScheduledTestWorker"
    }
}
