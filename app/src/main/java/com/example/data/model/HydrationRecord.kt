package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a recorded hydration event or a registered missed reminder event.
 * Ensures that confirmed drinking events and missed reminder intervals remain
 * strictly distinguishable.
 */
@Entity(tableName = "hydration_records")
data class HydrationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String, // e.g. "2026-09-15"
    val type: String = TYPE_HYDRATION,
    val status: String // "completed" or "missed"
) {
    companion object {
        const val TYPE_HYDRATION = "hydration"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_MISSED = "missed"
    }

    val isCompleted: Boolean
        get() = status == STATUS_COMPLETED

    val isMissed: Boolean
        get() = status == STATUS_MISSED
}
