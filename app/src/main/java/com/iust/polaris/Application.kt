package com.iust.polaris

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Custom Application class required for Hilt setup.
 * The @HiltAndroidApp annotation triggers Hilt's code generation, including a
 * base class for your application that serves as the application-level dependency container.
 */
@HiltAndroidApp
class Application : Application() {

    // You can add onCreate logic here for app-wide initializations if needed later.
}