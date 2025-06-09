package com.iust.polaris.data.repository

import com.iust.polaris.data.local.NetworkMetric
import com.iust.polaris.data.local.NetworkMetricDao
import kotlinx.coroutines.flow.Flow

/**
 * Concrete implementation of [NetworkMetricsRepository] that uses a local Room database
 * (via [NetworkMetricDao]) as its primary data source.
 *
 * @param networkMetricDao The Data Access Object for network metrics. This will be injected.
 */
class OfflineFirstNetworkMetricsRepository(
    private val networkMetricDao: NetworkMetricDao
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
}
