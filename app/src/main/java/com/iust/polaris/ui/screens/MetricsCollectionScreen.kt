package com.iust.polaris.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iust.polaris.data.local.NetworkMetric
import com.iust.polaris.ui.viewmodel.MetricsCollectionViewModel
import com.iust.polaris.ui.viewmodel.MetricsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsCollectionScreen(
    viewModel: MetricsCollectionViewModel = viewModel() // Hilt injects the ViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Polaris Network Monitor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = { viewModel.clearAllCollectedMetrics() }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Clear All Metrics",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
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
        Text(
            text = uiState.collectionStatusText,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = onToggleCollection,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Icon(
                imageVector = if (uiState.isCollecting) Icons.Filled.Delete else Icons.Filled.PlayArrow,
                contentDescription = if (uiState.isCollecting) "Stop Collection" else "Start Collection"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (uiState.isCollecting) "Stop Collection" else "Start Collection")
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isLoading && uiState.collectedMetrics.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else if (uiState.collectedMetrics.isEmpty()) {
            Text(
                "No metrics collected yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(uiState.collectedMetrics, key = { metric -> metric.id }) { metric ->
                    NetworkMetricItem(metric = metric)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun NetworkMetricItem(metric: NetworkMetric) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: ${metric.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = sdf.format(Date(metric.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${metric.networkType} | Signal: ${metric.signalStrength} dBm",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (metric.latitude != null && metric.longitude != null) {
                Text(
                    text = "Loc: ${String.format("%.4f", metric.latitude)}, ${String.format("%.4f", metric.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (metric.rsrp != null) {
                Text("RSRP: ${metric.rsrp} dBm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (metric.rsrq != null) {
                Text("RSRQ: ${metric.rsrq} dB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // You can add more Text composables here to display other fields from NetworkMetric
            // like plmnId, lac, tac, arfcn, etc.
        }
    }
}
