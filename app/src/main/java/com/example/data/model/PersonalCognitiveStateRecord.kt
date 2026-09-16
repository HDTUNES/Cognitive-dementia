package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personal_cognitive_state")
data class PersonalCognitiveStateRecord(
    @PrimaryKey val id: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
    val totalSessionsProcessed: Int = 0,
    val baselineEstablished: Boolean = false,
    val stateJson: String = "{}"
)
