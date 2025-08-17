package com.iust.polaris.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iust.polaris.R
import com.iust.polaris.common.ServiceStatus
import com.iust.polaris.ui.theme.status_success_light
import com.iust.polaris.ui.viewmodel.MainScreenUiState
import com.iust.polaris.ui.viewmodel.MainUiState
import com.iust.polaris.ui.viewmodel.MetricsCollectionViewModel

@Composable
fun MetricsCollectionScreen(
    uiState: MainUiState,
    onToggleCollection: () -> Unit,
    modifier: Modifier = Modifier,
) {

    // A simple Scaffold without an AppBar to host the main content.
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Main Start/Stop/Loading button
            ServiceControlButton(
                isCollecting = uiState.serviceStatus == ServiceStatus.COLLECTING,
                isInitializing = uiState.isServiceInitializing,
                onClick = onToggleCollection
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Status Text below the button
            Text(
                text = uiState.serviceStatusText,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Timer display, only visible when collecting
            if (uiState.serviceStatus == com.iust.polaris.common.ServiceStatus.COLLECTING) {
                Text(
                    text = uiState.collectionDuration,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }

        // --- NEW: Status card at the bottom ---
        SyncStatusCard(
            lastSyncStatus = uiState.lastSyncStatus,
            unsyncedCount = uiState.unsyncedMetricsCount,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
fun MainScreenContent(
    modifier: Modifier = Modifier,
    uiState: MainScreenUiState,
    onToggleCollection: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Start/Stop/Loading button
        ServiceControlButton(
            isCollecting = uiState.isCollecting,
            isInitializing = uiState.isServiceInitializing,
            onClick = onToggleCollection
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Status Text below the button
        Text(
            text = uiState.statusText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))


        if (uiState.isCollecting && !uiState.isServiceInitializing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_timer),
                    contentDescription = "Collection active",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.padding(start = 4.dp))
                Text(
                    text = uiState.collectionDuration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // A spacer to maintain layout consistency when timer is not shown
            Spacer(modifier = Modifier.height(18.dp)) // Matches approximate height of timer row
        }
    }
}

@Composable
fun ServiceControlButton(
    isCollecting: Boolean,
    isInitializing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonSize = 180.dp
    val connectedColor = status_success_light

    // Animate elevation for a smooth transition
    val animatedElevation by animateDpAsState(
        targetValue = if (isCollecting) 16.dp else 4.dp,
        animationSpec = tween(500), label = "ButtonElevationAnimation"
    )

    // Animate icon color for a smooth transition
    val animatedIconColor by animateColorAsState(
        targetValue = if (isCollecting) connectedColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        animationSpec = tween(500), label = "IconColorAnimation"
    )

    Card(
        modifier = modifier
            .size(buttonSize)
            .shadow(
                elevation = animatedElevation,
                shape = CircleShape,
                spotColor = if (isCollecting) connectedColor else Color.Black
            ),
        shape = CircleShape,
        // The button's container color is now static
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // Shadow is handled by the modifier
        onClick = { if (!isInitializing) onClick() } // Disable clicks while initializing
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isInitializing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(80.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                }
                else -> { // The icon is always visible, but its color changes
                    Icon(
                        painter = painterResource(R.drawable.ic_signal),
                        contentDescription = if (isCollecting) "Collecting" else "Stopped",
                        modifier = Modifier.size(100.dp),
                        tint = animatedIconColor // Use the animated color for the icon's tint
                    )
                }
            }
        }
    }
}

@Composable
fun SyncStatusCard(
    lastSyncStatus: String,
    unsyncedCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Sync Status", style = MaterialTheme.typography.labelMedium)
                Text(lastSyncStatus, style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Unsynced", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = unsyncedCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
