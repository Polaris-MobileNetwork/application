package com.iust.polaris.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iust.polaris.data.local.Test
import com.iust.polaris.data.local.TestResult
import com.iust.polaris.data.repository.TestsRepository
import com.iust.polaris.service.TestExecutor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Represents the state of a single test item in the UI, combining the
 * test configuration with its latest result.
 */
data class TestItemUiState(
    val test: Test,
    val latestResult: TestResult? = null
)

/**
 * Represents the overall state of the Tests screen, organized by test status.
 */
data class TestsScreenState(
    val manualTests: List<TestItemUiState> = emptyList(),
    val periodicTests: List<TestItemUiState> = emptyList(),
    val pendingTests: List<TestItemUiState> = emptyList(),
    val completedTests: List<TestItemUiState> = emptyList(),
    val runningTestId: Long? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class TestsViewModel @Inject constructor(
    private val testsRepository: TestsRepository,
    private val testExecutor: TestExecutor
) : ViewModel() {

    private val _runningTestId = MutableStateFlow<Long?>(null)

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
                combine(testItemFlows) { testItems ->
                    testItems.toList()
                }
            }
        }
    }

    val uiState: StateFlow<TestsScreenState> = combine(
        getTestItemsWithResults(testsRepository.getManualTests()),
        getTestItemsWithResults(testsRepository.getPeriodicTestsFlow()),
        getTestItemsWithResults(testsRepository.getPendingScheduledTests()),
        getTestItemsWithResults(testsRepository.getCompletedTests()),
        _runningTestId
    ) { manual, periodic, pending, completed, runningId ->
        TestsScreenState(
            manualTests = manual,
            periodicTests = periodic,
            pendingTests = pending,
            completedTests = completed,
            runningTestId = runningId,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TestsScreenState()
    )

    init {
        viewModelScope.launch {
            testsRepository.populateInitialMockTests()
        }
    }

    /**
     * Executes a given test using the TestExecutor.
     */
    fun runTest(test: Test) {
        if (_runningTestId.value != null) return

        viewModelScope.launch {
            _runningTestId.value = test.id
            val result = testExecutor.execute(test)
            testsRepository.insertTestResult(result)
            if (test.scheduledTimestamp != null && test.intervalSeconds == null) {
                testsRepository.markTestAsCompleted(test.id)
            }
            _runningTestId.value = null
        }
    }

    /**
     * Retrieves the full result history for a specific test.
     * This is used by the UI to populate the expanded details card.
     */
    fun getResultHistoryForTest(testId: Long): Flow<List<TestResult>> {
        return testsRepository.getResultsForTest(testId)
    }
}
