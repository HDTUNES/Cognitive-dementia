package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks engagement sessions such as breathing preparation routines or future cognitive activities.
 */
@Entity(tableName = "user_sessions")
data class UserSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionType: String = TYPE_PREPARATION,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val isCompleted: Boolean = false,
    val cyclesCompleted: Int = 0,
    val metadata: String = ""
) {
    companion object {
        const val TYPE_PREPARATION = "breathing_preparation"
        const val TYPE_COGNITIVE_ACTIVITY = "cognitive_activity"
        const val TYPE_MEMORY_RECOGNITION = "memory_recognition"
        const val TYPE_REACTION_COORDINATION = "reaction_coordination"
    }
}
