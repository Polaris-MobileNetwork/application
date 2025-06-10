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

    /**
     * Retrieves all enabled periodic tests as an observable Flow for the UI.
     */
    fun getPeriodicTestsFlow(): Flow<List<Test>>

    /**
     * Retrieves all enabled periodic tests as a simple list for the background worker.
     */
    suspend fun getPeriodicTests(): List<Test>

    /**
     * Retrieves all tests that are scheduled and due to be run.
     */
    suspend fun getDueScheduledTests(): List<Test>

    suspend fun getTestById(localTestId: Long): Test?

    suspend fun markTestAsCompleted(localTestId: Long)

    suspend fun populateInitialMockTests()

    // --- Test Result Methods ---

    fun getResultsForTest(localTestId: Long): Flow<List<TestResult>>

    suspend fun insertTestResult(result: TestResult)
}
