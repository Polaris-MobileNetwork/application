package com.iust.polaris.di

import android.content.Context
import com.iust.polaris.data.local.AppDatabase
import com.iust.polaris.data.local.NetworkMetricDao
import com.iust.polaris.data.repository.NetworkMetricsRepository
import com.iust.polaris.data.repository.OfflineFirstNetworkMetricsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module to provide application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class) // This makes the dependencies live as long as the application.
object AppModule {

    /**
     * Provides a singleton instance of the AppDatabase.
     * @param context The application context provided by Hilt.
     * @return The singleton AppDatabase instance.
     */
    @Provides
    @Singleton // @Singleton ensures that only one instance of the database is created.
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    /**
     * Provides an instance of NetworkMetricDao.
     * Hilt knows how to create AppDatabase (from the function above), so it can provide the DAO.
     * @param database The AppDatabase instance.
     * @return An instance of NetworkMetricDao.
     */
    @Provides
    fun provideNetworkMetricDao(database: AppDatabase): NetworkMetricDao {
        return database.networkMetricDao()
    }

    /**
     * Provides an instance of the NetworkMetricsRepository.
     * Hilt knows how to create NetworkMetricDao (from the function above), so it can provide the repository.
     * This function binds the NetworkMetricsRepository interface to its OfflineFirst implementation.
     * @param networkMetricDao The DAO for network metrics.
     * @return An implementation of NetworkMetricsRepository.
     */
    @Provides
    @Singleton // The repository will also be a singleton for the app's lifecycle.
    fun provideNetworkMetricsRepository(networkMetricDao: NetworkMetricDao): NetworkMetricsRepository {
        return OfflineFirstNetworkMetricsRepository(networkMetricDao)
    }
}
