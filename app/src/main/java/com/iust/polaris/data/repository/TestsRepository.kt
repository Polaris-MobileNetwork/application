package com.iust.polaris.data.repository

import com.iust.polaris.data.local.Test
import com.iust.polaris.data.local.TestResult
import kotlinx.coroutines.flow.Flow

/**
 * Interface for the repository that handles operations related to network tests
 * and their results.
 */
interface TestsRepository {

    // --- Test Configuration Methods ---
    fun getManualTests(): Flow<List<Test>>
    fun getPendingScheduledTests(): Flow<List<Test>>
    fun getCompletedTests(): Flow<List<Test>>
    fun getPeriodicTestsFlow(): Flow<List<Test>>
    suspend fun getPeriodicTests(): List<Test>
    suspend fun getDueScheduledTests(): List<Test>
    suspend fun getTestById(localTestId: Long): Test?
    suspend fun markTestAsCompleted(localTestId: Long)
    suspend fun populateInitialMockTests()
    suspend fun syncTests(): Boolean

    // --- Test Result Methods ---
    fun getResultsForTest(localTestId: Long): Flow<List<TestResult>>
    suspend fun insertTestResult(result: TestResult)

    /**
     * Syncs all unsynced test results with the server.
     * @return True if the sync was successful, false otherwise.
     */
    suspend fun syncTestResults(): Boolean // --- NEW ---
}
