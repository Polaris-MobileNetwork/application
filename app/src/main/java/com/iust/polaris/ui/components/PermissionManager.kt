package com.iust.polaris.ui.components

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun HandlePermissions(
    permissions: List<String>,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity

    // State to track if all permissions are granted
    var allPermissionsGranted by remember {
        mutableStateOf(permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        })
    }

    // State to track if the user has been shown the rationale and denied again
    var shouldShowSettingsDialog by remember { mutableStateOf(false) }

    // Launcher for requesting permissions
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissionsMap ->
            if (permissionsMap.values.all { it }) {
                // All permissions granted
                allPermissionsGranted = true
            } else {
                // Some permissions were denied. Check if we should show rationale or guide to settings.
                val shouldShowRationale = permissions.any {
                    activity.shouldShowRequestPermissionRationale(it)
                }
                if (!shouldShowRationale) {
                    // User has selected "Don't ask again" for at least one permission
                    shouldShowSettingsDialog = true
                }
            }
        }
    )

    if (allPermissionsGranted) {
        // If all permissions are granted, display the main content of the app
        content()
    } else {
        // If permissions are not granted, show the permission request UI
        PermissionRequestUI(
            onGrantClicked = {
                launcher.launch(permissions.toTypedArray())
            },
            onGoToSettingsClicked = {
                // Intent to open the app's settings screen
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", context.packageName, null)
                intent.data = uri
                context.startActivity(intent)
            },
            showSettingsDialog = shouldShowSettingsDialog
        )
    }
}

// A nice UI to explain why permissions are needed.
@Composable
private fun PermissionRequestUI(
    onGrantClicked: () -> Unit,
    onGoToSettingsClicked: () -> Unit,
    showSettingsDialog: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = "Permissions Icon",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Permissions Required",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "For this app to collect network and location data, you need to grant the required permissions. Your data helps us analyze network performance.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (showSettingsDialog) {
            Text(
                text = "You've permanently denied some permissions. To enable them, please go to the app settings.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onGoToSettingsClicked) {
                Text("Go to Settings")
            }
        } else {
            Button(onClick = onGrantClicked) {
                Text("Grant Permissions")
            }
        }
    }
}
