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

    @Query("SELECT * FROM test_results WHERE localTestId = :testId ORDER BY timestamp DESC")
    fun getResultsForTest(testId: Long): Flow<List<TestResult>>

    // --- NEW: Query to get all unsynced test results ---
    @Query("SELECT * FROM test_results WHERE isUploaded = 0")
    suspend fun getUnsyncedResults(): List<TestResult>

    // --- NEW: Query to mark results as uploaded ---
    @Query("UPDATE test_results SET isUploaded = 1 WHERE id IN (:ids)")
    suspend fun markResultsAsUploaded(ids: List<Long>)

    @Query("DELETE FROM test_results")
    suspend fun clearAllTestResults()
}
