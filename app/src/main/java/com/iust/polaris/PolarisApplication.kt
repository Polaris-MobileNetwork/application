package com.iust.polaris

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.iust.polaris.service.TestResultSyncScheduler
import com.iust.polaris.service.TestScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Custom Application class for Hilt setup.
 * 1. Annotated with @HiltAndroidApp to trigger Hilt's code generation.
 * 2. Implements Configuration.Provider to supply Hilt's WorkerFactory to WorkManager.
 * 3. Injects and calls the TestScheduler to start the periodic background work.
 */
@HiltAndroidApp
class PolarisApplication : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var testScheduler: TestScheduler

    @Inject
    lateinit var  testResultSyncScheduler: TestResultSyncScheduler

    override fun onCreate() {
        super.onCreate()
        // Schedule the periodic worker when the application starts.
        testScheduler.schedulePeriodicTests()
        applicationScope.launch {
            testResultSyncScheduler.scheduleOrCancelSync()
        }
    }

    /**
     * This override provides the custom HiltWorkerFactory to WorkManager.
     * It is CRUCIAL for dependency injection in your Workers.
     * The `get()` ensures it's implemented as a property override.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
