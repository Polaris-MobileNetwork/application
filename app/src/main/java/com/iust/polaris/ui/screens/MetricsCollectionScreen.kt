package com.iust.polaris.ui.screens

import com.iust.polaris.ui.components.MetricsControls
import com.iust.polaris.ui.components.MetricsDataList
import com.iust.polaris.ui.components.AppBar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iust.polaris.ui.viewmodel.MetricsCollectionViewModel
import com.iust.polaris.ui.viewmodel.MetricsUiState

@OptIn(ExperimentalMaterial3Api::class) // Still needed for Scaffold
@Composable
fun MetricsCollectionScreen(
    viewModel: MetricsCollectionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            // Use the new PolarisAppBar component
            AppBar(
                title = "Polaris Cellular Monitor",
                actions = {
                    IconButton(onClick = { viewModel.clearAllCollectedMetrics() }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Clear All Metrics"
                            // Tint will be handled by PolarisAppBar's actionIconContentColor
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        MetricsCollectionContent(
            uiState = uiState,
            onToggleCollection = { viewModel.onToggleCollection() },
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun MetricsCollectionContent(
    uiState: MetricsUiState,
    onToggleCollection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MetricsControls(
            statusText = uiState.collectionStatusText,
            isCollecting = uiState.isCollecting,
            onToggleCollection = onToggleCollection
        )
        Spacer(modifier = Modifier.height(24.dp))
        MetricsDataList(
            isLoading = uiState.isLoading,
            collectedMetrics = uiState.collectedMetrics
        )
    }
}

