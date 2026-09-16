package com.example.data.repository

import com.example.data.model.HydrationRecord
import com.example.data.model.PersonalBaseline
import com.example.data.model.User
import com.example.data.model.UserPreferences
import com.example.data.model.UserProfile
import com.example.data.model.UserSession
import com.example.processor.PersonalCognitiveState
import kotlinx.coroutines.flow.Flow

interface CognitiveRepository {
    val userProfile: Flow<UserProfile?>
    val userPreferences: Flow<UserPreferences>
    val personalBaseline: Flow<PersonalBaseline?>
    val sessions: Flow<List<UserSession>>
    val personalCognitiveState: Flow<PersonalCognitiveState>

    fun getTodayHydrationRecords(): Flow<List<HydrationRecord>>
    fun getAllHydrationRecords(): Flow<List<HydrationRecord>>

    suspend fun saveUserProfile(name: String, dateOfBirth: String)
    suspend fun updatePreferences(preferences: UserPreferences)
    suspend fun setDarkMode(isDark: Boolean)
    suspend fun setReminderIntervalHours(intervalHours: Int)

    suspend fun recordHydrationEvent(timestamp: Long = System.currentTimeMillis()): Long
    suspend fun recordMissedReminderEvent(timestamp: Long): Long

    suspend fun checkAndProcessMissedReminders(currentTimestamp: Long = System.currentTimeMillis())
    suspend fun getFullUserAggregate(): User

    suspend fun recordSession(
        sessionType: String,
        startedAt: Long,
        completedAt: Long,
        cyclesCompleted: Int,
        isCompleted: Boolean,
        metadata: String = ""
    ): Long

    suspend fun recalculateCognitiveState(): PersonalCognitiveState

    suspend fun resetAllData()
}
