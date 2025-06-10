package com.iust.polaris.service

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iust.polaris.data.repository.TestsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "TestSchedulerWorker"

@HiltWorker
class TestSchedulerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val testsRepository: TestsRepository,
    private val testExecutor: TestExecutor
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Worker starting: Checking for all due and periodic tests.")
        return try {
            // Fetch both one-time due tests and all periodic tests
            val dueScheduledTests = testsRepository.getDueScheduledTests()
            val periodicTests = testsRepository.getPeriodicTests()
            val allTestsToRun = dueScheduledTests + periodicTests

            if (allTestsToRun.isEmpty()) {
                Log.d(TAG, "No due or periodic tests to run at this time.")
                return Result.success()
            }

            Log.i(TAG, "Found ${allTestsToRun.size} test(s) to run: ${dueScheduledTests.size} scheduled, ${periodicTests.size} periodic.")

            // Execute each test
            for (test in allTestsToRun) {
                Log.d(TAG, "Executing test: ${test.name} (ID: ${test.id})")
                val result = testExecutor.execute(test)
                testsRepository.insertTestResult(result)

                // IMPORTANT: Only mark one-time scheduled tests as completed.
                // Periodic tests should remain incomplete to run again.
                if (test.scheduledTimestamp != null) {
                    testsRepository.markTestAsCompleted(test.id)
                    Log.d(TAG, "Marked one-time scheduled test as completed: ${test.name}")
                }
            }

            Log.i(TAG, "All due and periodic tests executed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred during scheduled test execution.", e)
            Result.failure()
        }
    }
}
