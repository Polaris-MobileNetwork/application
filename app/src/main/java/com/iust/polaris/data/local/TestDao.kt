package com.iust.polaris.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the Test entity.
 * Defines database operations for test configurations.
 */
@Dao
interface TestDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTests(tests: List<Test>)

    @Query("SELECT * FROM tests WHERE id = :testId")
    suspend fun getTestById(testId: Long): Test?

    // --- NEW: Query to get all existing server-assigned IDs ---
    /**
     * Retrieves a list of all non-null server-assigned IDs from the tests table.
     * This is used to tell the server which tests the client already has.
     */
    @Query("SELECT serverAssignedId FROM tests WHERE serverAssignedId IS NOT NULL")
    suspend fun getAllServerAssignedIds(): List<String>

    @Query("SELECT * FROM tests WHERE isEnabled = 1 AND scheduledTimestamp IS NULL AND intervalSeconds IS NULL ORDER BY id ASC")
    fun getManualTests(): Flow<List<Test>>

    @Query("SELECT * FROM tests WHERE isEnabled = 1 AND isCompleted = 0 AND (intervalSeconds IS NULL OR intervalSeconds = 0) AND scheduledTimestamp IS NOT NULL AND scheduledTimestamp > :currentTimeMillis ORDER BY scheduledTimestamp ASC")
    fun getPendingScheduledTests(currentTimeMillis: Long): Flow<List<Test>>

    @Query("SELECT * FROM tests WHERE isEnabled = 1 AND isCompleted = 0 AND (intervalSeconds IS NULL OR intervalSeconds = 0) AND scheduledTimestamp IS NOT NULL AND scheduledTimestamp <= :currentTimeMillis")
    suspend fun getDueScheduledTests(currentTimeMillis: Long): List<Test>

    @Query("SELECT * FROM tests WHERE isEnabled = 1 AND (intervalSeconds IS NOT NULL AND intervalSeconds != 0) ")
    fun getPeriodicTestsFlow(): Flow<List<Test>>

    @Query("SELECT * FROM tests WHERE isEnabled = 1 AND (intervalSeconds IS NOT NULL AND intervalSeconds != 0)")
    suspend fun getPeriodicTests(): List<Test>

    @Query("SELECT * FROM tests WHERE isCompleted = 1 AND (intervalSeconds IS NULL OR intervalSeconds = 0) ORDER BY scheduledTimestamp DESC")
    fun getCompletedTests(): Flow<List<Test>>

    @Query("UPDATE tests SET isCompleted = 1 WHERE id = :testId")
    suspend fun markTestAsCompleted(testId: Long)

    @Query("DELETE FROM tests")
    suspend fun clearAllTests()
}
