package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores basic user profile information gathered on first launch.
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val dateOfBirth: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
