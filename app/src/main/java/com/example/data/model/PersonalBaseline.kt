package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Architectural model representing a user's personal baseline.
 * NOTE: This is strictly an internal architecture construct for future cognitive comparison.
 * It is NOT exposed or rendered anywhere in the current user interface.
 */
@Entity(tableName = "personal_baseline")
data class PersonalBaseline(
    @PrimaryKey val id: Int = 1,
    val established: Boolean = false,
    val createdAt: Long? = null,
    val baselineReactionTimeMs: Long? = null,
    val baselineMemorySpan: Int? = null,
    val baselineAttentionScore: Double? = null,
    val rawMetricsJson: String = "{}"
)
