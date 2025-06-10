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

    /**
     * Inserts one or more test configurations. If a test with the same ID already exists,
     * it will be replaced. This is useful when syncing from a server.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTests(tests: List<Test>)

    /**
     * Retrieves a single test by its local ID.
     */
    @Query("SELECT * FROM tests WHERE id = :testId")
    suspend fun getTestById(testId: Long): Test?

    /**
     * Retrieves all enabled tests that are meant for manual execution (no scheduled time or interval).
     */
    @Query("SELECT * FROM tests WHERE isEnabled = 1 AND scheduledTimestamp IS NULL AND intervalSeconds IS NULL ORDER BY id ASC")
    fun getManualTests(): Flow<List<Test>>

    /**
     * Retrieves all "pending" tests that are scheduled for the future and not yet completed.
     * This query now excludes periodic tests.
     * @param currentTimeMillis The current time to compare against the scheduled time.
     */
    @Query("SELECT * FROM tests WHERE isEnabled = 1 AND isCompleted = 0 AND intervalSeconds IS NULL AND scheduledTimestamp IS NOT NULL AND scheduledTimestamp > :currentTimeMillis ORDER BY scheduledTimestamp ASC")
    fun getPendingScheduledTests(currentTimeMillis: Long): Flow<List<Test>>

    /**
     * Retrieves all "due" one-time scheduled tests that should be run now.
     * This query now excludes periodic tests.
     */
    @Query("SELECT * FROM tests WHERE isEnabled = 1 AND isCompleted = 0 AND intervalSeconds IS NULL AND scheduledTimestamp IS NOT NULL AND scheduledTimestamp <= :currentTimeMillis")
    suspend fun getDueScheduledTests(currentTimeMillis: Long): List<Test>

    /**
     * Retrieves all enabled periodic tests as an observable Flow for the UI.
     */
    @Query("SELECT * FROM tests WHERE isEnabled = 1 AND intervalSeconds IS NOT NULL")
    fun getPeriodicTestsFlow(): Flow<List<Test>>

    /**
     * Retrieves all enabled periodic tests as a simple list for the background worker.
     */
    @Query("SELECT * FROM tests WHERE isEnabled = 1 AND intervalSeconds IS NOT NULL")
    suspend fun getPeriodicTests(): List<Test>

    /**
     * Retrieves all "done" (completed) tests, ordered by most recent first.
     */
    @Query("SELECT * FROM tests WHERE isCompleted = 1 ORDER BY scheduledTimestamp DESC")
    fun getCompletedTests(): Flow<List<Test>>

    /**
     * Marks a specific test as completed.
     * @param testId The local ID of the test to update.
     */
    @Query("UPDATE tests SET isCompleted = 1 WHERE id = :testId")
    suspend fun markTestAsCompleted(testId: Long)

    /**
     * Deletes all tests from the table. Useful for a full refresh from the server.
     */
    @Query("DELETE FROM tests")
    suspend fun clearAllTests()
}
