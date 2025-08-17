package com.iust.polaris.data.repository

import android.util.Log
import com.iust.polaris.data.local.NetworkMetric
import com.iust.polaris.data.local.NetworkMetricDao
import com.iust.polaris.data.remote.ApiService
import com.iust.polaris.data.remote.SyncRequestDto
import com.iust.polaris.data.remote.toDto
import kotlinx.coroutines.flow.Flow

private const val TAG = "NetworkMetricsRepo"

/**
 * Concrete implementation of [NetworkMetricsRepository] that uses a local Room database
 * (via [NetworkMetricDao]) as its primary data source.
 *
 * @param networkMetricDao The Data Access Object for network metrics. This will be injected.
 */
class OfflineFirstNetworkMetricsRepository(
    private val networkMetricDao: NetworkMetricDao,
    private val apiService: ApiService
) : NetworkMetricsRepository {

    override fun getAllMetricsFlow(): Flow<List<NetworkMetric>> {
        return networkMetricDao.getAllMetricsFlow()
    }

    override suspend fun getMetricsPaged(page: Int, pageSize: Int): List<NetworkMetric> {
        // Calculate the offset for the database query based on the page number and size
        val offset = page * pageSize
        return networkMetricDao.getMetricsPaged(limit = pageSize, offset = offset)
    }

    override suspend fun insertMetric(metric: NetworkMetric) {
        networkMetricDao.insertMetric(metric)
    }

    override suspend fun getUnsyncedMetrics(): List<NetworkMetric> {
        return networkMetricDao.getUnsyncedMetrics()
    }

    override suspend fun markMetricsAsUploaded(ids: List<Long>) {
        networkMetricDao.markMetricsAsUploaded(ids)
    }

    override suspend fun clearAllMetrics() {
        networkMetricDao.clearAllMetrics()
    }

    override suspend fun syncMetrics(): Boolean {
        Log.d(TAG, "Starting sync process...")
        val unsyncedMetrics = getUnsyncedMetrics()

        if (unsyncedMetrics.isEmpty()) {
            Log.d(TAG, "No metrics to sync.")
            return true // Nothing to do, so sync is "successful"
        }

        Log.d(TAG, "Found ${unsyncedMetrics.size} metrics to sync.")
        val syncRequest = SyncRequestDto(measurements = unsyncedMetrics.toDto())

        return try {
            val response = apiService.syncMeasurements(syncRequest)
            if (response.isSuccessful) {
                Log.i(TAG, "Sync API call successful. Marking metrics as uploaded.")
                val idsToUpdate = unsyncedMetrics.map { it.id }
                markMetricsAsUploaded(idsToUpdate)
                true
            } else {
                Log.e(TAG, "Sync API call failed with code: ${response.code()} - ${response.message()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed due to an exception.", e)
            false
        }
    }

    override fun getUnsyncedMetricsCount(): Flow<Int> {
        return networkMetricDao.getUnsyncedMetricsCount()
    }
}
