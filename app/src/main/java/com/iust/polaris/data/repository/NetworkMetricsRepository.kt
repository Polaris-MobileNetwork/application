package com.iust.polaris.data.repository

import com.iust.polaris.data.local.NetworkMetric
import kotlinx.coroutines.flow.Flow

/**
 * Interface for the repository that handles operations related to network metrics.
 * This abstraction allows for different implementations (e.g., local database, remote server)
 * and facilitates testing.
 */
interface NetworkMetricsRepository {

    /**
     * Retrieves all collected network metrics as a Flow.
     * The Flow will emit new lists of metrics whenever the underlying data changes.
     */
    fun getAllMetrics(): Flow<List<NetworkMetric>>

    /**
     * Inserts a new network metric record into the data source.
     * This is a suspend function, indicating it should be called from a coroutine.
     *
     * @param metric The [NetworkMetric] object to insert.
     */
    suspend fun insertMetric(metric: NetworkMetric)

    /**
     * Retrieves all network metrics that have not yet been uploaded to the server.
     * This is a suspend function as it's a one-shot read.
     */
    suspend fun getUnsyncedMetrics(): List<NetworkMetric>

    /**
     * Marks a list of metrics (by their IDs) as uploaded.
     *
     * @param ids A list of Long values representing the IDs of the metrics to be marked.
     */
    suspend fun markMetricsAsUploaded(ids: List<Long>)


    /**
     * Deletes all network metrics from the local data source.
     */
    suspend fun clearAllMetrics()
}
