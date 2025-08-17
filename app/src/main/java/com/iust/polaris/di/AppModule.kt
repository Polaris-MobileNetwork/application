package com.iust.polaris.di

import android.content.Context
import com.iust.polaris.data.local.AppDatabase
import com.iust.polaris.data.local.NetworkMetricDao
import com.iust.polaris.data.local.TestDao
import com.iust.polaris.data.local.TestResultDao
import com.iust.polaris.data.remote.ApiService
import com.iust.polaris.data.repository.NetworkMetricsRepository
import com.iust.polaris.data.repository.OfflineFirstNetworkMetricsRepository
import com.iust.polaris.data.repository.OfflineFirstTestsRepository
import com.iust.polaris.data.repository.TestsRepository
import com.iust.polaris.service.TestExecutor
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
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
    fun provideNetworkMetricsRepository(networkMetricDao: NetworkMetricDao, apiService: ApiService): NetworkMetricsRepository {
        return OfflineFirstNetworkMetricsRepository(networkMetricDao, apiService)
    }

    @Provides
    @Singleton
    fun provideTestsRepository(testDao: TestDao, apiService: ApiService, testResultDao: TestResultDao): TestsRepository {
        return OfflineFirstTestsRepository(testDao, testResultDao, apiService)
    }

    @Provides
    @Singleton
    fun provideTestExecutor(@ApplicationContext context: Context): TestExecutor {
        return TestExecutor(context)
    }

    private const val BASE_URL = "https://api.lebasee.ir/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY) // Log request and response bodies
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        val json = Json { ignoreUnknownKeys = true } // Configure JSON parser

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }
}
