package com.iust.polaris.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a test configuration that can be executed by the app.
 *
 * @param id The local unique identifier for the test.
 * @param serverAssignedId An optional ID from the server, useful for syncing.
 * @param name A user-friendly name for the test, e.g., "Ping Google DNS".
 * @param type The type of test to run, e.g., "PING", "DOWNLOAD_SPEED".
 * @param parametersJson A JSON string containing the parameters for the test.
 * @param isEnabled Whether the test is currently active.
 * @param scheduledTimestamp The specific time (as Unix milliseconds) when this test should be executed. Null means it's a manual or periodic test.
 * @param intervalSeconds The interval in seconds at which this test should be repeated. Null means it's a manual or one-time scheduled test.
 * @param isCompleted A flag to mark if a one-time scheduled test has been run. (Not used for periodic tests).
 */
@Entity(tableName = "tests")
data class Test(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverAssignedId: String? = null,
    val name: String,
    val type: String,
    val parametersJson: String,
    val isEnabled: Boolean = true,
    val scheduledTimestamp: Long? = null,
    val intervalSeconds: Int? = null,
    val isCompleted: Boolean = false
)
