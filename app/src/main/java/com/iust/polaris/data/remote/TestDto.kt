package com.iust.polaris.data.remote

import com.iust.polaris.data.local.Test
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO for the request body when syncing tests.
 */
@Serializable
data class TestSyncRequestDto(
    val excludedIds: List<String>
)

/**
 * DTO for the top-level response from the test sync API.
 */
@Serializable
data class TestSyncResponseDto(
    val success: Boolean,
    val code: Int,
    val message: String?,
    val tests: List<TestDto>
)

/**
 * DTO representing a single test object received from the server.
 */
@Serializable
data class TestDto(
    @SerialName("id") val serverId: String,
    val name: String,
    val type: String,
    val parametersJson: String,
    val isEnabled: Boolean,
    val scheduledTimestamp: Long?,
    val intervalSeconds: Int?,
    val isCompleted: Boolean
)

/**
 * An extension function to easily convert a list of TestDto objects from the server
 * into a list of local Test entities suitable for saving in the Room database.
 */
fun List<TestDto>.toEntity(): List<Test> {
    return this.map { dto ->
        Test(
            // We let Room auto-generate the local 'id'
            serverAssignedId = dto.serverId,
            name = dto.name,
            type = dto.type,
            parametersJson = dto.parametersJson,
            isEnabled = dto.isEnabled,
            scheduledTimestamp = dto.scheduledTimestamp,
            intervalSeconds = dto.intervalSeconds,
            isCompleted = dto.isCompleted
        )
    }
}

@Serializable
data class DeletedTestsResponseDto(
    val success: Boolean,
    val code: Int,
    val message: String?,
    val deletedTestIds: List<String>
)
