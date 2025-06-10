package com.iust.polaris.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the TestResult entity.
 */
@Dao
interface TestResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestResult(result: TestResult)

    @Query("SELECT * FROM test_results ORDER BY timestamp DESC")
    fun getAllTestResults(): Flow<List<TestResult>>

    /**
     * Retrieves all test results for a specific test, identified by its local ID.
     * @param testId The local ID of the parent test.
     * @return A Flow emitting the list of results for the given test.
     */
    @Query("SELECT * FROM test_results WHERE localTestId = :testId ORDER BY timestamp DESC")
    fun getResultsForTest(testId: Long): Flow<List<TestResult>>

    @Query("DELETE FROM test_results")
    suspend fun clearAllTestResults()
}
