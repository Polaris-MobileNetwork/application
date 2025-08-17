package com.iust.polaris.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iust.polaris.data.repository.NetworkMetricsRepository
import com.iust.polaris.data.repository.TestsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "TestResultSyncWorker"

/**
 * A CoroutineWorker responsible for periodically syncing collected test results
 * with the server.
 */
@HiltWorker
class TestResultSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    // Hilt injects the repository that knows how to sync test results
    private val testsRepository: TestsRepository,
    private val networkMetricsRepository: NetworkMetricsRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker starting: Attempting to sync test results with server.")

        return try {
            val wasSuccessful = testsRepository.syncTestResults()
            val wasMetricSyncSuccessful = networkMetricsRepository.syncMetrics()
            if (wasMetricSyncSuccessful) {
                Log.i(TAG, "Network metrics sync finished successfully.")
                Result.success()
            } else {
                Log.w(TAG, "Network sync failed, will retry later.")
                Result.retry() // Tell WorkManager to retry this work later
            }
            if (wasSuccessful) {
                Log.i(TAG, "Test result sync finished successfully.")
                Result.success()
            } else {
                Log.w(TAG, "Test result sync failed, will retry later.")
                Result.retry() // Tell WorkManager to retry this work later
            }
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred during test result sync execution.", e)
            Result.failure() // A permanent failure
        }
    }
}
