package com.iust.polaris.service

import android.Manifest.permission.SEND_SMS
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.iust.polaris.data.local.Test
import com.iust.polaris.data.local.TestResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@Singleton
class TestExecutor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Executes a given test and returns its result.
     * This is a suspend function as it performs a potentially long-running operation.
     */
    suspend fun execute(test: Test): TestResult = withContext(Dispatchers.IO) {
        when (test.type.uppercase()) {
            "PING" -> executePingTest(test)
            "DNS" -> executeDnsTest(test)
            "WEB" -> executeWebTest(test)
            "DOWNLOAD_SPEED" -> executeDownloadSpeedTest(test)
            "UPLOAD_SPEED" -> executeUploadSpeedTest(test)
            "SMS" -> executeSmsTest(test)
            else -> executeNotImplemented(test, "Unknown test type: ${test.type}")
        }
    }

    private fun executeSmsTest(test: Test): TestResult {
        if (ContextCompat.checkSelfPermission(context, SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return createErrorResult(test, "SEND_SMS permission not granted.")
        }

        val params = try {
            JSONObject(test.parametersJson)
        } catch (e: Exception) {
            return createErrorResult(test, "Invalid JSON parameters: ${e.message}")
        }

        val recipient = params.optString("recipient", null)
        val message = params.optString("message", null)

        if (recipient.isNullOrBlank() || message.isNullOrBlank()) {
            return createErrorResult(test, "Recipient or message is missing from SMS parameters.")
        }

        return try {
            val smsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(recipient, null, message, null, null)

            TestResult(
                localTestId = test.id,
                serverTestId = test.serverAssignedId,
                timestamp = System.currentTimeMillis(),
                testType = test.type,
                targetHost = recipient,
                resultValue = "Sent Successfully",
                isSuccess = true,
                details = "SMS sent to $recipient."
            )
        } catch (e: Exception) {
            createErrorResult(test, "SMS sending failed: ${e.message}")
        }
    }

    /**
     * Executes an Upload Speed test by sending data to a server endpoint.
     */
    private fun executeUploadSpeedTest(test: Test): TestResult {
        val params = try {
            JSONObject(test.parametersJson)
        } catch (e: Exception) {
            return createErrorResult(test, "Invalid JSON parameters: ${e.message}")
        }

        val urlString = params.optString("url", null)
        val sizeKb = params.optInt("size_kb", 1024)
        val sizeBytes = sizeKb * 1024

        if (urlString.isNullOrBlank()) {
            return createErrorResult(test, "URL is missing from UPLOAD_SPEED parameters.")
        }

        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            val randomData = Random.nextBytes(sizeBytes)
            var responseCode: Int

            val duration = measureTimeMillis {
                connection = url.openConnection() as HttpURLConnection
                connection?.apply {
                    doOutput = true
                    requestMethod = "POST"
                    connectTimeout = 5000
                    readTimeout = 15000
                    setRequestProperty("Content-Type", "application/octet-stream")
                    setFixedLengthStreamingMode(randomData.size)
                }

                connection?.outputStream?.use { it.write(randomData) }

                responseCode = connection?.responseCode ?: -1
                if (responseCode !in 200..299) {
                    throw Exception("Server returned non-OK status: $responseCode")
                }
            }

            val durationInSeconds = duration / 1000.0
            if (durationInSeconds == 0.0) {
                return createErrorResult(test, "Upload was too fast to measure.")
            }
            val bitsSent = sizeBytes * 8
            val speedBps = bitsSent / durationInSeconds
            val speedMbps = speedBps / 1_000_000

            TestResult(
                localTestId = test.id,
                serverTestId = test.serverAssignedId,
                timestamp = System.currentTimeMillis(),
                testType = test.type,
                targetHost = urlString,
                resultValue = "%.2f Mbps".format(speedMbps),
                isSuccess = true,
                details = "Successfully uploaded %.2f MB in %.2f seconds.".format(sizeBytes / 1_000_000.0, durationInSeconds)
            )
        } catch (e: Exception) {
            createErrorResult(test, "Upload test failed for '$urlString': ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }


    /**
     * Executes a Download Speed test by downloading content from a URL.
     */
    private fun executeDownloadSpeedTest(test: Test): TestResult {
        val params = try {
            JSONObject(test.parametersJson)
        } catch (e: Exception) {
            return createErrorResult(test, "Invalid JSON parameters: ${e.message}")
        }

        val urlString = params.optString("url", null)
        if (urlString.isNullOrBlank()) {
            return createErrorResult(test, "URL is missing from DOWNLOAD_SPEED parameters.")
        }

        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            var bytesRead = 0L
            var inputStream: InputStream? = null

            val duration = measureTimeMillis {
                connection = url.openConnection() as HttpURLConnection
                connection?.requestMethod = "GET"
                connection?.connectTimeout = 5000
                connection?.readTimeout = 15000

                val responseCode = connection?.responseCode ?: -1
                if (responseCode !in 200..299) {
                    throw Exception("Server returned non-OK status: $responseCode")
                }

                inputStream = connection?.inputStream
                val buffer = ByteArray(4096)
                var bytes: Int
                while (inputStream?.read(buffer).also { bytes = it ?: -1 } != -1) {
                    bytesRead += bytes
                }
            }

            val durationInSeconds = duration / 1000.0
            if (durationInSeconds == 0.0) {
                return createErrorResult(test, "Download was too fast to measure.")
            }
            val bitsRead = bytesRead * 8
            val speedBps = bitsRead / durationInSeconds
            val speedMbps = speedBps / 1_000_000

            TestResult(
                localTestId = test.id,
                serverTestId = test.serverAssignedId,
                timestamp = System.currentTimeMillis(),
                testType = test.type,
                targetHost = urlString,
                resultValue = "%.2f Mbps".format(speedMbps),
                isSuccess = true,
                details = "Successfully downloaded %.2f MB in %.2f seconds.".format(bytesRead / 1_000_000.0, durationInSeconds)
            )
        } catch (e: Exception) {
            createErrorResult(test, "Download test failed for '$urlString': ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Executes a Web test to measure the time to get a response from a URL.
     */
    private fun executeWebTest(test: Test): TestResult {
        val params = try {
            JSONObject(test.parametersJson)
        } catch (e: Exception) {
            return createErrorResult(test, "Invalid JSON parameters: ${e.message}")
        }

        val urlString = params.optString("url", null)
        if (urlString.isNullOrBlank()) {
            return createErrorResult(test, "URL is missing from WEB parameters.")
        }

        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            var responseCode: Int
            val duration = measureTimeMillis {
                connection = url.openConnection() as HttpURLConnection
                connection?.requestMethod = "GET"
                connection?.connectTimeout = 5000
                connection?.readTimeout = 5000
                responseCode = connection?.responseCode ?: -1
            }

            if (responseCode in 200..399) {
                TestResult(
                    localTestId = test.id,
                    serverTestId = test.serverAssignedId,
                    timestamp = System.currentTimeMillis(),
                    testType = test.type,
                    targetHost = urlString,
                    resultValue = "$duration ms",
                    isSuccess = true,
                    details = "Successfully connected. HTTP Status: $responseCode"
                )
            } else {
                createErrorResult(test, "Connection failed. HTTP Status: $responseCode")
            }
        } catch (e: Exception) {
            createErrorResult(test, "Web test failed for '$urlString': ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Executes a DNS lookup test to measure resolution time.
     */
    private fun executeDnsTest(test: Test): TestResult {
        val params = try {
            JSONObject(test.parametersJson)
        } catch (e: Exception) {
            return createErrorResult(test, "Invalid JSON parameters: ${e.message}")
        }

        val host = params.optString("host", null)
        if (host.isNullOrBlank()) {
            return createErrorResult(test, "Host is missing from DNS parameters.")
        }

        return try {
            var resolvedIp = ""
            val duration = measureTimeMillis {
                val addresses = InetAddress.getAllByName(host)
                resolvedIp = addresses.joinToString { it.hostAddress }
            }

            TestResult(
                localTestId = test.id,
                serverTestId = test.serverAssignedId,
                timestamp = System.currentTimeMillis(),
                testType = test.type,
                targetHost = host,
                resultValue = "$duration ms",
                isSuccess = true,
                details = "Resolved '$host' to: $resolvedIp"
            )
        } catch (e: Exception) {
            createErrorResult(test, "DNS lookup failed for '$host': ${e.message}")
        }
    }

    /**
     * Executes a PING test using the system's ping command.
     */
    private fun executePingTest(test: Test): TestResult {
        val params = try {
            JSONObject(test.parametersJson)
        } catch (e: Exception) {
            return createErrorResult(test, "Invalid JSON parameters: ${e.message}")
        }

        val host = params.optString("host", null)
        val count = params.optInt("count", 4)

        if (host.isNullOrBlank()) {
            return createErrorResult(test, "Host is missing from PING parameters.")
        }

        val pingCommand = "ping -c $count -W 3 $host"

        return try {
            val process = Runtime.getRuntime().exec(pingCommand)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = reader.readText()
            val errorOutput = errorReader.readText()

            val exitCode = process.waitFor()

            if (exitCode == 0 && output.contains("avg")) {
                val avgLatency = output.lines().lastOrNull { it.startsWith("rtt min/avg/max/mdev") || it.startsWith("round-trip min/avg/max/stddev") }
                    ?.split('/')?.getOrNull(4)
                    ?.let { "%.2f ms".format(it.toDouble()) }
                    ?: "Success"

                TestResult(
                    localTestId = test.id,
                    serverTestId = test.serverAssignedId,
                    timestamp = System.currentTimeMillis(),
                    testType = test.type,
                    targetHost = host,
                    resultValue = avgLatency,
                    isSuccess = true,
                    details = output.trim()
                )
            } else {
                val failureDetails = "Ping failed with exit code: $exitCode.\nOutput:\n${output.trim()}\nError:\n${errorOutput.trim()}"
                createErrorResult(test, failureDetails)
            }
        } catch (e: Exception) {
            createErrorResult(test, "Ping execution failed: ${e.message}")
        }
    }

    /**
     * Helper to create a TestResult for a test that is not implemented yet.
     */
    private fun executeNotImplemented(test: Test, reason: String): TestResult {
        return createErrorResult(test, reason)
    }

    /**
     * Helper to create a generic failed TestResult.
     */
    private fun createErrorResult(test: Test, details: String): TestResult {
        val host = try {
            val params = JSONObject(test.parametersJson)
            params.optString("host", null)
                ?: params.optString("url", null)
                ?: params.optString("recipient", "Unknown")
        } catch (e: Exception) { "Invalid JSON" }

        return TestResult(
            localTestId = test.id,
            serverTestId = test.serverAssignedId,
            timestamp = System.currentTimeMillis(),
            testType = test.type,
            targetHost = host,
            resultValue = "Failed",
            isSuccess = false,
            details = details
        )
    }
}
