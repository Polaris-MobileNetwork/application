package com.iust.polaris.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iust.polaris.ui.components.NetworkMetricItem // Reuse our existing component
import com.iust.polaris.ui.viewmodel.MetricsDisplayViewModel

@Composable
fun MetricsDisplayScreen(
    modifier: Modifier = Modifier,
    viewModel: MetricsDisplayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // This derived state is true if the last visible item is nearing the end of the list
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (layoutInfo.totalItemsCount == 0) {
                false
            } else {
                val lastVisibleItem = visibleItemsInfo.lastOrNull()
                // Load more when the user is 5 items away from the end
                lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 5
            }
        }
    }

    // Trigger loading more items when shouldLoadMore becomes true
    LaunchedEffect (shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadNextItems()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isLoading) {
            // Show a full-screen loader for the initial load
            CircularProgressIndicator()
        } else if (uiState.items.isEmpty()) {
            // Show a message if no metrics have been collected yet.
            Text(
                text = "No metrics have been collected yet.\nGo to the 'Home' tab to start.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(all = 16.dp)
            ) {
                items(
                    items = uiState.items,
                    key = { metric -> metric.id }
                ) { metric ->
                    NetworkMetricItem(metric = metric)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Show a small loading indicator at the bottom while paginating
                item {
                    if (uiState.isPaginating) {
                        Row (
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}
