package com.iust.polaris.di

import android.content.Context
import com.iust.polaris.data.local.AppDatabase
import com.iust.polaris.data.local.NetworkMetricDao
import com.iust.polaris.data.local.TestDao
import com.iust.polaris.data.local.TestResultDao
import com.iust.polaris.data.repository.NetworkMetricsRepository
import com.iust.polaris.data.repository.OfflineFirstNetworkMetricsRepository
import com.iust.polaris.data.repository.OfflineFirstTestsRepository
import com.iust.polaris.data.repository.TestsRepository
import com.iust.polaris.service.TestExecutor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideNetworkMetricDao(database: AppDatabase): NetworkMetricDao {
        return database.networkMetricDao()
    }

    @Provides
    fun provideTestDao(database: AppDatabase): TestDao {
        return database.testDao()
    }

    @Provides
    fun provideTestResultDao(database: AppDatabase): TestResultDao {
        return database.testResultDao()
    }

    @Provides
    @Singleton
    fun provideNetworkMetricsRepository(networkMetricDao: NetworkMetricDao): NetworkMetricsRepository {
        return OfflineFirstNetworkMetricsRepository(networkMetricDao)
    }

    @Provides
    @Singleton
    fun provideTestsRepository(testDao: TestDao, testResultDao: TestResultDao): TestsRepository {
        return OfflineFirstTestsRepository(testDao, testResultDao)
    }

    @Provides
    @Singleton
    fun provideTestExecutor(@ApplicationContext context: Context): TestExecutor {
        return TestExecutor(context)
    }
}
