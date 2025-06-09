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
     * Retrieves all enabled tests from the database, ordered by their ID.
     * Returns a Flow so the UI can observe changes to the list of available tests.
     */
    @Query("SELECT * FROM tests WHERE isEnabled = 1 ORDER BY id ASC")
    fun getEnabledTests(): Flow<List<Test>>

    /**
     * Deletes all tests from the table. Useful for a full refresh from the server.
     */
    @Query("DELETE FROM tests")
    suspend fun clearAllTests()
}
