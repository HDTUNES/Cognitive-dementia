package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists user visual and reminder preferences.
 */
@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 1,
    val isDarkMode: Boolean = false,
    val reminderIntervalHours: Int = 2,
    val reminderStartHour: Int = 8,  // 8:00 AM
    val reminderEndHour: Int = 20    // 8:00 PM
)
