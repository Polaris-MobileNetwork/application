package com.iust.polaris.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the NetworkMetric entity.
 * This interface defines the database operations that can be performed on the network_metrics table.
 */
@Dao
interface NetworkMetricDao {

    /**
     * Inserts a new network metric into the database. If there's a conflict, it will be ignored.
     * The `suspend` keyword means this function must be called from a coroutine or another suspend function,
     * ensuring it doesn't block the main thread.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMetric(metric: NetworkMetric)

    /**
     * Retrieves all network metrics from the database, ordered by timestamp in descending order.
     * This function returns a Flow, which is an asynchronous stream of data.
     * Room will automatically update this Flow whenever the data in the table changes,
     * making it perfect for observing data changes in your UI.
     */
    @Query("SELECT * FROM network_metrics ORDER BY timestamp DESC")
    fun getAllMetrics(): Flow<List<NetworkMetric>>

    /**
     * Retrieves all network metrics that have not yet been uploaded to the server.
     */
    @Query("SELECT * FROM network_metrics WHERE isUploaded = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedMetrics(): List<NetworkMetric>

    /**
     * Deletes all metrics from the table.
     */
    @Query("DELETE FROM network_metrics")
    suspend fun clearAllMetrics()

    // We can add more specific queries later, e.g., to mark items as uploaded.
    @Query("UPDATE network_metrics SET isUploaded = 1 WHERE id IN (:ids)")
    suspend fun markMetricsAsUploaded(ids: List<Long>)
}
