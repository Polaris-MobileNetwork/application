package com.iust.polaris.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a test configuration that can be executed by the app.
 * This would typically be synced from a server.
 *
 * @param id The local unique identifier for the test.
 * @param serverAssignedId An optional ID from the server, useful for syncing.
 * @param name A user-friendly name for the test, e.g., "Ping Google DNS".
 * @param type The type of test to run, e.g., "PING", "DOWNLOAD_SPEED".
 * @param parametersJson A JSON string containing the parameters for the test,
 * e.g., {"host": "8.8.8.8", "packet_count": 4}.
 * @param isEnabled Whether the test is currently active and should be shown to the user.
 */
@Entity(tableName = "tests")
data class Test(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serverAssignedId: String? = null,
    val name: String,
    val type: String,
    val parametersJson: String,
    val isEnabled: Boolean = true
)
