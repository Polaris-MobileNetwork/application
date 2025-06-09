package com.iust.polaris.data.repository

import com.iust.polaris.data.local.NetworkMetric
import kotlinx.coroutines.flow.Flow

/**
 * Interface for the repository that handles operations related to network metrics.
 */
interface NetworkMetricsRepository {

    /**
     * Retrieves all collected network metrics as a Flow.
     * This is suitable for observing the entire dataset if needed.
     */
    fun getAllMetricsFlow(): Flow<List<NetworkMetric>>

    /**
     * Fetches a single "page" of network metrics from the data source.
     *
     * @param page The page number to retrieve (e.g., page 0 is the first page).
     * @param pageSize The number of items to retrieve per page.
     * @return A list of [NetworkMetric] objects for the requested page.
     */
    suspend fun getMetricsPaged(page: Int, pageSize: Int): List<NetworkMetric>

    /**
     * Inserts a new network metric record into the data source.
     */
    suspend fun insertMetric(metric: NetworkMetric)

    /**
     * Retrieves all network metrics that have not yet been uploaded to the server.
     */
    suspend fun getUnsyncedMetrics(): List<NetworkMetric>

    /**
     * Marks a list of metrics (by their IDs) as uploaded.
     */
    suspend fun markMetricsAsUploaded(ids: List<Long>)

    /**
     * Deletes all network metrics from the local data source.
     */
    suspend fun clearAllMetrics()
}
