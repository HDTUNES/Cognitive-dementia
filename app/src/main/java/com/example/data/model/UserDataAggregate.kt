package com.example.data.model

/**
 * Conceptual extensible data aggregate representing the full state of a user.
 * Provides a unified structure ready for future cloud sync or AI analysis.
 */
data class User(
    val profile: UserProfile?,
    val baseline: PersonalBaseline?,
    val sessions: List<UserSession> = emptyList(),
    val hydration: List<HydrationRecord> = emptyList(),
    val preferences: UserPreferences = UserPreferences()
)
