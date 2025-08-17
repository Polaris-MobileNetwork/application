package com.iust.polaris.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iust.polaris.data.local.Test
import com.iust.polaris.data.local.TestResult
import com.iust.polaris.data.repository.TestsRepository
import com.iust.polaris.service.TestExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TestsViewModel"

data class TestItemUiState(
    val test: Test,
    val latestResult: TestResult? = null
)

data class TestsScreenState(
    val manualTests: List<TestItemUiState> = emptyList(),
    val periodicTests: List<TestItemUiState> = emptyList(),
    val pendingTests: List<TestItemUiState> = emptyList(),
    val completedTests: List<TestItemUiState> = emptyList(),
    val runningTestId: Long? = null,
    val isSyncing: Boolean = false,
    val isSyncingResults: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class TestsViewModel @Inject constructor(
    private val testsRepository: TestsRepository,
    private val testExecutor: TestExecutor
) : ViewModel() {

    // --- REFACTORED STATE MANAGEMENT ---
    // These are the only mutable states we manage directly.
    private val _runningTestId = MutableStateFlow<Long?>(null)
    private val _isSyncingTests = MutableStateFlow(false)
    private val _isSyncingResults = MutableStateFlow(false)

    private val _snackbarEventFlow = MutableSharedFlow<String>()
    val snackbarEvents = _snackbarEventFlow.asSharedFlow()

    // The single source of truth for the UI state, combining all data sources.
    val uiState: StateFlow<TestsScreenState> = combine(
        // --- FIX: Pass the flows as a list to the combine operator ---
        listOf(
            getTestItemsWithResults(testsRepository.getManualTests()),
            getTestItemsWithResults(testsRepository.getPeriodicTestsFlow()),
            getTestItemsWithResults(testsRepository.getPendingScheduledTests()),
            getTestItemsWithResults(testsRepository.getCompletedTests()),
            _runningTestId,
            _isSyncingTests,
            _isSyncingResults
        )
    ) { values ->
        // Destructure the array of values with type casting
        val manual = values[0] as List<TestItemUiState>
        val periodic = values[1] as List<TestItemUiState>
        val pending = values[2] as List<TestItemUiState>
        val completed = values[3] as List<TestItemUiState>
        val runningId = values[4] as Long?
        val isSyncingTests = values[5] as Boolean
        val isSyncingResults = values[6] as Boolean

        // Create the UI state from the combined values
        TestsScreenState(
            manualTests = manual,
            periodicTests = periodic,
            pendingTests = pending,
            completedTests = completed,
            runningTestId = runningId,
            isSyncing = isSyncingTests,
            isSyncingResults = isSyncingResults,
            isLoading = false // Loading is done once this combine block runs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TestsScreenState() // Initial state with isLoading = true
    )

    init {
        Log.d(TAG, "ViewModel initialized.")
        viewModelScope.launch {
            Log.d(TAG, "Populating initial mock tests.")
            testsRepository.populateInitialMockTests()
        }
    }

    private fun getTestItemsWithResults(testsFlow: Flow<List<Test>>): Flow<List<TestItemUiState>> {
        return testsFlow.flatMapLatest { tests ->
            if (tests.isEmpty()) {
                MutableStateFlow(emptyList())
            } else {
                val testItemFlows = tests.map { test ->
                    testsRepository.getResultsForTest(test.id).map { results ->
                        TestItemUiState(test, results.firstOrNull())
                    }
                }
                combine(testItemFlows) { it.toList() }
            }
        }
    }

    fun runTest(test: Test) {
        if (_runningTestId.value != null) {
            Log.w(TAG, "runTest ignored: a test is already running.")
            return
        }
        viewModelScope.launch {
            _runningTestId.value = test.id
            try {
                val result = testExecutor.execute(test)
                testsRepository.insertTestResult(result)
                if (test.scheduledTimestamp != null && test.intervalSeconds == null) {
                    testsRepository.markTestAsCompleted(test.id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during test run for ${test.name}", e)
            } finally {
                _runningTestId.value = null
            }
        }
    }

    fun onSyncTestsClicked() {
        if (_isSyncingTests.value) return
        viewModelScope.launch {
            Log.d(TAG, "onSyncTestsClicked: Starting sync.")
            _isSyncingTests.value = true
            try {
                val success = testsRepository.syncTests()
                _snackbarEventFlow.emit(if (success) "Tests synced successfully." else "Test sync failed.")
                Log.i(TAG, "onSyncTestsClicked: Sync finished. Success: $success")
            } catch (e: Exception) {
                Log.e(TAG, "onSyncTestsClicked: Exception during sync.", e)
                _snackbarEventFlow.emit("Error during test sync: ${e.message}")
            } finally {
                _isSyncingTests.value = false
                Log.d(TAG, "onSyncTestsClicked: Sync state reset to false.")
            }
        }
    }

    fun onSyncTestResultsClicked() {
        if (_isSyncingResults.value) return
        viewModelScope.launch {
            Log.d(TAG, "onSyncTestResultsClicked: Starting result sync.")
            _isSyncingResults.value = true
            try {
                val success = testsRepository.syncTestResults()
                _snackbarEventFlow.emit(if (success) "Test results synced successfully." else "Test result sync failed.")
                Log.i(TAG, "onSyncTestResultsClicked: Result sync finished. Success: $success")
            } catch (e: Exception) {
                Log.e(TAG, "onSyncTestResultsClicked: Exception during result sync.", e)
                _snackbarEventFlow.emit("Error during result sync: ${e.message}")
            } finally {
                _isSyncingResults.value = false
                Log.d(TAG, "onSyncTestResultsClicked: Sync state reset to false.")
            }
        }
    }

    fun getResultHistoryForTest(testId: Long): Flow<List<TestResult>> {
        return testsRepository.getResultsForTest(testId)
    }
}
