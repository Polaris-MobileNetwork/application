package com.iust.polaris

import android.os.Bundle
import android.Manifest
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.iust.polaris.ui.components.HandlePermissions
import com.iust.polaris.ui.screens.MetricsCollectionScreen
import com.iust.polaris.ui.theme.PolarisAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
        ).apply {
            // Add FOREGROUND_SERVICE_LOCATION for Android 14+ if service type is location
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34
                add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            }
            // Add POST_NOTIFICATIONS for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // API 33
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            // Add READ_PHONE_STATE if you need more detailed telephony info later
            // add(Manifest.permission.READ_PHONE_STATE)
        }.toList()

        setContent {
            // Apply application's theme.
            PolarisAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HandlePermissions (permissions = requiredPermissions) {
                        MetricsCollectionScreen()
                    }
                }
             }
        }
    }
}