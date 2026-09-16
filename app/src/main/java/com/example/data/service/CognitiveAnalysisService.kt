package com.example.data.service

import com.example.data.model.HydrationRecord
import com.example.data.model.UserSession

/**
 * Extensible interface where future server-side or local Gemini AI analysis can be plugged in.
 * Designed to analyze routine consistency, hydration habits, and session engagement
 * without modifying UI or persistence logic.
 */
interface CognitiveAnalysisService {
    /**
     * Future hook to analyze routine adherence and suggest gentle adaptive reminders.
     */
    suspend fun analyzeRoutinePatterns(
        hydrationRecords: List<HydrationRecord>,
        sessions: List<UserSession>
    ): RoutineInsights?
}

data class RoutineInsights(
    val adherenceRating: String,
    val suggestedIntervalHours: Int,
    val encouragingNote: String
)

/**
 * Clean local placeholder for future AI service integration.
 */
class LocalPlaceholderCognitiveAnalysisService : CognitiveAnalysisService {
    override suspend fun analyzeRoutinePatterns(
        hydrationRecords: List<HydrationRecord>,
        sessions: List<UserSession>
    ): RoutineInsights? {
        // AI analysis integration is reserved for future versions
        return null
    }
}
