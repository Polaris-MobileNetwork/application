package com.iust.polaris.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the request body when syncing test results.
 * This object contains a list of individual result DTOs.
 */
@Serializable
data class TestResultSyncRequestDto(
    val testResults: List<TestResultDto>
)

/**
 * DTO representing a single test result object sent to the server.
 * The field names match the API specification.
 */
@Serializable
data class TestResultDto(
    val timestamp: Long,
    val testType: String,
    val targetHost: String?,
    val resultValue: String,
    val isSuccess: Boolean,
    val details: String?,
    @SerialName("testId") val serverTestId: String? // Maps to the server-assigned ID of the parent test
)
