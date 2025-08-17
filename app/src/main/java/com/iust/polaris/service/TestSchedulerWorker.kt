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
        Log.d(TAG, "Worker starting: Checking for ONE-TIME due scheduled tests.")
        return try {
            // --- UPDATED: Only fetch and run one-time scheduled tests ---
            val dueTests = testsRepository.getDueScheduledTests()

            if (dueTests.isEmpty()) {
                Log.d(TAG, "No due one-time tests found.")
                return Result.success()
            }

            Log.i(TAG, "Found ${dueTests.size} due one-time test(s). Executing now...")

            for (test in dueTests) {
                Log.d(TAG, "Executing test: ${test.name} (ID: ${test.id})")
                val result = testExecutor.execute(test)
                testsRepository.insertTestResult(result)
                // Mark the test as completed so it doesn't run again
                testsRepository.markTestAsCompleted(test.id)
                Log.i(TAG, "Finished test: ${test.name}. Result: ${result.resultValue}")
            }

            Log.i(TAG, "All due one-time tests executed successfully.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred during scheduled test execution.", e)
            Result.failure()
        }
    }
}
