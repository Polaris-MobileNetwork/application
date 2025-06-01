package com.iust.polaris.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iust.polaris.data.local.NetworkMetric

@Composable
fun MetricsDataList(
    isLoading: Boolean,
    collectedMetrics: List<NetworkMetric>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (isLoading && collectedMetrics.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally))
        } else if (collectedMetrics.isEmpty()) {
            Text(
                "No metrics collected yet. Press 'Start Collection'.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp).align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(collectedMetrics, key = { metric -> metric.id }) { metric ->
                    NetworkMetricItem(metric = metric) // This will call the composable from NetworkMetricItem.kt
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
