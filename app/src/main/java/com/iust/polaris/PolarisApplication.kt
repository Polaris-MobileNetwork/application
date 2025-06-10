package com.iust.polaris

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.iust.polaris.service.TestScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Custom Application class for Hilt setup.
 * 1. Annotated with @HiltAndroidApp to trigger Hilt's code generation.
 * 2. Implements Configuration.Provider to supply Hilt's WorkerFactory to WorkManager.
 * 3. Injects and calls the TestScheduler to start the periodic background work.
 */
@HiltAndroidApp
class PolarisApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var testScheduler: TestScheduler

    override fun onCreate() {
        super.onCreate()
        // Schedule the periodic worker when the application starts.
        testScheduler.schedulePeriodicTests()
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
