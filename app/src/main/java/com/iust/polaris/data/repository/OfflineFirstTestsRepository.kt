package com.iust.polaris.data.repository

import android.util.Log
import com.iust.polaris.data.local.Test
import com.iust.polaris.data.local.TestDao
import com.iust.polaris.data.local.TestResult
import com.iust.polaris.data.local.TestResultDao
import com.iust.polaris.data.remote.ApiService
import com.iust.polaris.data.remote.TestResultDto
import com.iust.polaris.data.remote.TestResultSyncRequestDto
import com.iust.polaris.data.remote.TestSyncRequestDto
import com.iust.polaris.data.remote.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

private const val TAG = "TestsRepository"

/**
 * Concrete implementation of [TestsRepository] that uses a local Room database
 * and a remote Retrofit service.
 */
class OfflineFirstTestsRepository(
    private val testDao: TestDao,
    private val testResultDao: TestResultDao,
    private val apiService: ApiService
) : TestsRepository {

    override fun getManualTests(): Flow<List<Test>> = testDao.getManualTests()
    override fun getPendingScheduledTests(): Flow<List<Test>> = testDao.getPendingScheduledTests(System.currentTimeMillis())
    override fun getCompletedTests(): Flow<List<Test>> = testDao.getCompletedTests()
    override fun getPeriodicTestsFlow(): Flow<List<Test>> = testDao.getPeriodicTestsFlow()
    override suspend fun getPeriodicTests(): List<Test> = testDao.getPeriodicTests()
    override suspend fun getDueScheduledTests(): List<Test> = testDao.getDueScheduledTests(System.currentTimeMillis())
    override suspend fun getTestById(localTestId: Long): Test? = testDao.getTestById(localTestId)
    override suspend fun markTestAsCompleted(localTestId: Long) = testDao.markTestAsCompleted(localTestId)
    override fun getResultsForTest(localTestId: Long): Flow<List<TestResult>> = testResultDao.getResultsForTest(localTestId)
    override suspend fun insertTestResult(result: TestResult) = testResultDao.insertTestResult(result)

    /**
     * Syncs the local test configurations with the server.
     */
    override suspend fun syncTests(): Boolean {
        Log.d(TAG, "Starting test sync process...")
        return try {
            val existingIds = testDao.getAllServerAssignedIds()
            Log.d(TAG, "Sending ${existingIds.size} existing IDs to server.")
            val response = apiService.getTests(TestSyncRequestDto(excludedIds = existingIds))

            if (response.isSuccessful && response.body()?.success == true) {
                val newTestsDto = response.body()?.tests
                if (newTestsDto.isNullOrEmpty()) {
                    Log.i(TAG, "Test sync successful, no new tests received.")
                } else {
                    Log.i(TAG, "Test sync successful, received ${newTestsDto.size} new/updated tests.")
                    testDao.insertOrUpdateTests(newTestsDto.toEntity())
                }
                true
            } else {
                Log.e(TAG, "Test sync API call failed with code: ${response.code()} - ${response.message()}")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Test sync failed due to an exception.", e)
            false
        }
    }

    override suspend fun syncTestResults(): Boolean {
        Log.d(TAG, "Starting test result sync process...")
        val unsyncedResults = testResultDao.getUnsyncedResults()

        if (unsyncedResults.isEmpty()) {
            Log.d(TAG, "No test results to sync.")
            return true // Nothing to do, so sync is "successful"
        }

        Log.d(TAG, "Found ${unsyncedResults.size} test results to sync.")

        // Convert local entities to DTOs for the API request
        val resultsDto = unsyncedResults.map { result ->
            TestResultDto(
                timestamp = result.timestamp,
                testType = result.testType,
                targetHost = result.targetHost,
                resultValue = result.resultValue,
                isSuccess = result.isSuccess,
                details = result.details,
                serverTestId = result.serverTestId
            )
        }

        val syncRequest = TestResultSyncRequestDto(testResults = resultsDto)

        return try {
            val response = apiService.syncTestResults(syncRequest)
            if (response.isSuccessful) {
                Log.i(TAG, "Test result sync API call successful. Marking results as uploaded.")
                val idsToUpdate = unsyncedResults.map { it.id }
                testResultDao.markResultsAsUploaded(idsToUpdate)
                true
            } else {
                Log.e(TAG, "Test result sync API call failed with code: ${response.code()} - ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Test result sync failed due to an exception.", e)
            false
        }
    }

    /**
     * Populates the database with initial data.
     * This now only ensures default manual tests exist and then triggers a sync.
     */
    override suspend fun populateInitialMockTests() {
        // Ensure default manual tests exist without duplicating them.
        val manualTestsTemplate = listOf(
            Test(
                name = "Ping Google DNS",
                type = "PING",
                parametersJson = """{"host": "8.8.8.8", "count": 4}"""
            ),
            Test(
                name = "DNS Lookup (Google)",
                type = "DNS",
                parametersJson = """{"host": "google.com"}"""
            ),
            Test(
                name = "Web Page Load",
                type = "WEB",
                parametersJson = """{"url": "https://www.google.com"}"""
            )
        )

        val existingManualTests = testDao.getManualTests().first()
        val testsToInsert = manualTestsTemplate.filter { template ->
            existingManualTests.none { existing -> existing.name == template.name }
        }

        if (testsToInsert.isNotEmpty()) {
            testDao.insertOrUpdateTests(testsToInsert)
        }

        // Trigger a sync to get any scheduled or periodic tests from the server.
        syncTests()
    }
}
