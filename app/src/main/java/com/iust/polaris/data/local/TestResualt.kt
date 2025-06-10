package com.iust.polaris.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents the result of a single network test (e.g., Ping, Speed Test).
 * This entity has a foreign key relationship with the 'tests' table using the local test ID.
 */
@Entity(
    tableName = "test_results",
    foreignKeys = [
        ForeignKey(
            entity = Test::class,
            parentColumns = ["id"],
            childColumns = ["localTestId"], // Foreign key links to the local ID
            onDelete = ForeignKey.CASCADE // If a Test is deleted, its results are also deleted.
        )
    ],
    indices = [Index(value = ["localTestId"])] // Add an index for faster queries on the foreign key.
)
data class TestResult(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // --- Foreign Keys ---
    val localTestId: Long, // Foreign key to the local Test's primary key
    val serverTestId: String?, // The server-assigned ID of the test, stored for reference

    // --- Test Result Data ---
    val timestamp: Long,
    val testType: String, // e.g., "Ping", "Download Speed", "Upload Speed"
    val targetHost: String?, // For Ping/Speed Test
    val resultValue: String, // e.g., "25 ms", "50.5 Mbps"
    val isSuccess: Boolean, // Did the test complete successfully?
    val details: String?, // Any additional details or error messages

    // --- Sync Status ---
    val isUploaded: Boolean = false
)
