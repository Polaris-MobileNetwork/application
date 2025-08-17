package com.iust.polaris.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NetworkMetricDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMetric(metric: NetworkMetric)

    // This flow is great for real-time updates but loads everything.
    // We'll keep it, but create a new paginated query for the list screen.
    @Query("SELECT * FROM network_metrics ORDER BY timestamp DESC")
    fun getAllMetricsFlow(): Flow<List<NetworkMetric>>

    // --- NEW: Query to get a "page" of metrics ---
    @Query("SELECT * FROM network_metrics ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getMetricsPaged(limit: Int, offset: Int): List<NetworkMetric>


    @Query("SELECT * FROM network_metrics WHERE isUploaded = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedMetrics(): List<NetworkMetric>


    @Query("DELETE FROM network_metrics")
    suspend fun clearAllMetrics()


    @Query("UPDATE network_metrics SET isUploaded = 1 WHERE id IN (:ids)")
    suspend fun markMetricsAsUploaded(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM network_metrics WHERE isUploaded = 0")
    fun getUnsyncedMetricsCount(): Flow<Int>
}
