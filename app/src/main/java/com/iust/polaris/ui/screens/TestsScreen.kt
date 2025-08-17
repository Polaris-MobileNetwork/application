package com.iust.polaris.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.iust.polaris.R
import com.iust.polaris.data.local.Test
import com.iust.polaris.data.local.TestResult
import com.iust.polaris.ui.viewmodel.TestItemUiState
import com.iust.polaris.ui.viewmodel.TestsScreenState
import com.iust.polaris.ui.viewmodel.TestsViewModel
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TestsScreen(
    modifier: Modifier = Modifier,
    viewModel: TestsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold (
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
            _ ->
        Column(modifier = modifier.fillMaxSize()) {
            // --- NEW: Header for Sync Buttons ---
            SyncHeader(
                isSyncingTests = uiState.isSyncing,
                isSyncingResults = uiState.isSyncingResults,
                onSyncTests = { viewModel.onSyncTestsClicked() },
                onSyncResults = { viewModel.onSyncTestResultsClicked() }
            )

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.manualTests.isEmpty() && uiState.periodicTests.isEmpty() && uiState.pendingTests.isEmpty() && uiState.completedTests.isEmpty()) {
                    Text(
                        text = "No tests available. Try syncing.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Manual Tests Section
                        if (uiState.manualTests.isNotEmpty()) {
                            item { SectionHeader("Manual Tests") }
                            items(uiState.manualTests, key = { "manual-${it.test.id}" }) { item ->
                                TestItemCard(
                                    testItemState = item,
                                    isRunning = uiState.runningTestId == item.test.id,
                                    onRunTest = { viewModel.runTest(item.test) },
                                    viewModel = viewModel
                                )
                            }
                        }

                        // Periodic Tests Section
                        if (uiState.periodicTests.isNotEmpty()) {
                            item { SectionHeader("Periodic Tests", Modifier.padding(top = 16.dp)) }
                            items(
                                uiState.periodicTests,
                                key = { "periodic-${it.test.id}" }) { item ->
                                TestItemCard(
                                    testItemState = item,
                                    isRunning = uiState.runningTestId == item.test.id,
                                    onRunTest = { viewModel.runTest(item.test) }, // Allow manual trigger
                                    viewModel = viewModel
                                )
                            }
                        }

                        // Pending Scheduled Tests Section
                        if (uiState.pendingTests.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    "Pending Scheduled Tests",
                                    Modifier.padding(top = 16.dp)
                                )
                            }
                            items(uiState.pendingTests, key = { "pending-${it.test.id}" }) { item ->
                                TestItemCard(
                                    testItemState = item,
                                    isRunning = uiState.runningTestId == item.test.id,
                                    onRunTest = {},
                                    showRunButton = false, // Don't show run button for pending tests
                                    viewModel = viewModel
                                )
                            }
                        }

                        // Completed Tests Section
                        if (uiState.completedTests.isNotEmpty()) {
                            item { SectionHeader("Completed Tests", Modifier.padding(top = 16.dp)) }
                            items(
                                uiState.completedTests,
                                key = { "completed-${it.test.id}" }) { item ->
                                TestItemCard(
                                    testItemState = item,
                                    isRunning = false,
                                    onRunTest = {},
                                    showRunButton = false,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SyncHeader(
    isSyncingTests: Boolean,
    isSyncingResults: Boolean,
    onSyncTests: () -> Unit,
    onSyncResults: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onSyncTests,
            enabled = !isSyncingTests && !isSyncingResults,
            modifier = Modifier.weight(1f)
        ) {
            if (isSyncingTests) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.Refresh, "Sync Tests")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sync Tests")
            }
        }
        OutlinedButton(
            onClick = onSyncResults,
            enabled = !isSyncingTests && !isSyncingResults,
            modifier = Modifier.weight(1f)
        ) {
            if (isSyncingResults) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud),
                    contentDescription = "Sync Now"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upload Results")
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun TestItemCard(
    testItemState: TestItemUiState,
    isRunning: Boolean,
    onRunTest: () -> Unit,
    showRunButton: Boolean = true,
    viewModel: TestsViewModel
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = testItemState.test.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    TestItemSummary(testItemState = testItemState)
                }

                if (showRunButton) {
                    Spacer(modifier = Modifier.width(16.dp))
                    if (isRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    } else {
                        Button(onClick = onRunTest) {
                            Icon(Icons.Default.PlayArrow, "Run Test")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Run")
                        }
                    }
                }
            }
            // Collapsible details section
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(150)),
                exit = fadeOut(animationSpec = tween(150))
            ) {
                TestDetails(
                    test = testItemState.test,
                    viewModel = viewModel,
                    isRunning = isRunning
                )
            }
        }
    }
}

@Composable
fun TestItemSummary(testItemState: TestItemUiState) {
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val detailsText: String
    val detailsColor: Color

    when {
        testItemState.latestResult != null -> {
            detailsText = "Last: ${testItemState.latestResult.resultValue} on ${sdf.format(Date(testItemState.latestResult.timestamp))}"
            detailsColor = if (testItemState.latestResult.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        }
        testItemState.test.scheduledTimestamp != null -> {
            detailsText = "Scheduled for: ${sdf.format(Date(testItemState.test.scheduledTimestamp))}"
            detailsColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
        testItemState.test.intervalSeconds != null -> {
            detailsText = "Runs periodically (every ${testItemState.test.intervalSeconds / 60} mins)"
            detailsColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
        else -> {
            detailsText = "Ready to run"
            detailsColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }
    Text(text = detailsText, style = MaterialTheme.typography.bodyMedium, color = detailsColor)
}

@Composable
fun TestDetails(
    test: Test,
    viewModel: TestsViewModel,
    isRunning: Boolean
) {
    val resultHistory by viewModel.getResultHistoryForTest(test.id).collectAsState(initial = emptyList())

    Divider(modifier = Modifier.padding(horizontal = 16.dp))
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
//        val params = JSONObject(test.parametersJson)
//        params.keys().forEach { key ->
//            Text("• $key: ${params.getString(key)}", style = MaterialTheme.typography.bodyMedium)
//        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

        if (isRunning) {
            SkeletonResultItem()
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (resultHistory.isEmpty()) {
            if (!isRunning) {
                Text("No results yet.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            resultHistory.forEach { result ->
                TextResultItem(result = result)
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun TextResultItem(result: TestResult) {
    val sdf = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }
    val color = if (result.isSuccess) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
    Text(
        text = "• ${sdf.format(Date(result.timestamp))}: ${result.resultValue}",
        style = MaterialTheme.typography.bodyMedium,
        color = color
    )
}

@Composable
fun SkeletonResultItem() {
    Text(
        text = "• Running test...",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary
    )
}
