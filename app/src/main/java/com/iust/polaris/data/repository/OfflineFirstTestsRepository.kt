package com.iust.polaris.data.repository

import com.iust.polaris.data.local.Test
import com.iust.polaris.data.local.TestDao
import com.iust.polaris.data.local.TestResult
import com.iust.polaris.data.local.TestResultDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Concrete implementation of [TestsRepository] that uses a local Room database
 * as its data source.
 */
class OfflineFirstTestsRepository(
    private val testDao: TestDao,
    private val testResultDao: TestResultDao
) : TestsRepository {

    override fun getManualTests(): Flow<List<Test>> {
        return testDao.getManualTests()
    }

    override fun getPendingScheduledTests(): Flow<List<Test>> {
        return testDao.getPendingScheduledTests(System.currentTimeMillis())
    }

    override fun getCompletedTests(): Flow<List<Test>> {
        return testDao.getCompletedTests()
    }

    // --- NEWLY IMPLEMENTED to return a Flow ---
    override fun getPeriodicTestsFlow(): Flow<List<Test>> {
        return testDao.getPeriodicTestsFlow()
    }

    // --- NEWLY IMPLEMENTED suspend fun ---
    override suspend fun getPeriodicTests(): List<Test> {
        return testDao.getPeriodicTests()
    }


    override suspend fun getDueScheduledTests(): List<Test> {
        return testDao.getDueScheduledTests(System.currentTimeMillis())
    }

    override suspend fun getTestById(localTestId: Long): Test? {
        return testDao.getTestById(localTestId)
    }

    override suspend fun markTestAsCompleted(localTestId: Long) {
        testDao.markTestAsCompleted(localTestId)
    }

    /**
     * Populates the database with initial data.
     * - Ensures a default set of manual tests exist without duplicating them.
     * - Ensures a default periodic test exists.
     * - Appends a new unique scheduled test to simulate receiving one from a server.
     */
    override suspend fun populateInitialMockTests() {
        // --- Logic for ensuring default manual tests exist ---
        val manualTestsTemplate = listOf(
            Test(
                name = "Ping Google DNS",
                type = "PING",
                parametersJson = """{"host": "8.8.8.8", "count": 4}"""
            ),
            Test(
                name = "DNS Lookup (Google)",
                type = "DNS",
                parametersJson = """{"host": "google.com"}"""
            ),
            Test(
                name = "Web Page Load",
                type = "WEB",
                parametersJson = """{"url": "https://www.google.com"}"""
            ),
            Test(
                name = "Download Speed Test (100MB)",
                type = "DOWNLOAD_SPEED",
                parametersJson = """{"url": "https://fsn1-speed.hetzner.com/100MB.bin"}"""
            ),
            Test(
                name = "Upload Speed Test (1MB)",
                type = "UPLOAD_SPEED",
                parametersJson = """{"url": "https://httpbin.org/post", "size_kb": 1024}"""
            ),
            Test(
                name = "Send Test SMS",
                type = "SMS",
                // Note: The recipient number should be valid for a real test.
                parametersJson = """{"recipient": "+989133559810", "message": "Polaris connectivity test message"}"""
            )
        )

        val existingManualTests = testDao.getManualTests().first()
        val manualTestsToInsert = manualTestsTemplate.filter { template ->
            existingManualTests.none { existing -> existing.name == template.name }
        }

        if (manualTestsToInsert.isNotEmpty()) {
            testDao.insertOrUpdateTests(manualTestsToInsert)
        }

        // --- Logic for ensuring a default periodic test exists ---
        val periodicTestTemplate = Test(
            name = "Periodic Ping (Every 15 mins)",
            type = "PING",
            parametersJson = """{"host": "1.1.1.1", "count": 2}""",
            intervalSeconds = 900 // 15 minutes
        )
        val existingPeriodicTests = testDao.getPeriodicTests()
        if (existingPeriodicTests.none { it.name == periodicTestTemplate.name }) {
            testDao.insertOrUpdateTests(listOf(periodicTestTemplate))
        }


        // --- Logic for appending a new scheduled test ---
        val now = System.currentTimeMillis()
        val scheduledTime = now + (2 * 60 * 1000) // 2 minutes from now

        val newScheduledTest = Test(
            name = "Scheduled Ping @ ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(scheduledTime))}",
            type = "PING",
            parametersJson = """{"host": "iust.ac.ir", "count": 2}""",
            scheduledTimestamp = scheduledTime
        )

        testDao.insertOrUpdateTests(listOf(newScheduledTest))
    }

    override fun getResultsForTest(localTestId: Long): Flow<List<TestResult>> {
        return testResultDao.getResultsForTest(localTestId)
    }

    override suspend fun insertTestResult(result: TestResult) {
        testResultDao.insertTestResult(result)
    }
}
