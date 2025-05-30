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

    override fun getAllMetrics(): Flow<List<NetworkMetric>> {
        return networkMetricDao.getAllMetrics()
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
