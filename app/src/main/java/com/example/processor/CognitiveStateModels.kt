package com.example.processor

import org.json.JSONObject

/**
 * Encapsulates a measured or derived dimension across the three required tiers:
 * BASELINE -> RECENT -> CURRENT, along with changes, variance, trend and confidence.
 */
data class CognitiveDimensionMetric(
    val baseline: Double? = null,
    val recent: Double? = null,
    val current: Double? = null,
    val changeFromBaseline: Double? = null,
    val changeFromRecent: Double? = null,
    val variability: Double? = null,
    val trend: String = TREND_INSUFFICIENT_DATA,
    val confidence: Double = 0.0,
    val dataQuality: String = QUALITY_INSUFFICIENT_DATA
) {
    companion object {
        const val TREND_IMPROVING = "improving"
        const val TREND_STABLE = "stable"
        const val TREND_DECLINING = "declining"
        const val TREND_VARIABLE = "variable"
        const val TREND_INSUFFICIENT_DATA = "insufficient_data"

        const val QUALITY_HIGH = "High"
        const val QUALITY_MODERATE = "Moderate"
        const val QUALITY_LOW = "Low"
        const val QUALITY_INSUFFICIENT_DATA = "Insufficient Data"
    }

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("baseline", baseline ?: JSONObject.NULL)
            put("recent", recent ?: JSONObject.NULL)
            put("current", current ?: JSONObject.NULL)
            put("change_from_baseline", changeFromBaseline ?: JSONObject.NULL)
            put("change_from_recent", changeFromRecent ?: JSONObject.NULL)
            put("variability", variability ?: JSONObject.NULL)
            put("trend", trend)
            put("confidence", confidence)
            put("data_quality", dataQuality)
        }
    }
}

data class MemoryCognitiveState(
    val shapeRecognition: CognitiveDimensionMetric = CognitiveDimensionMetric(),
    val colorRecognition: CognitiveDimensionMetric = CognitiveDimensionMetric(),
    val overallMemoryAccuracy: CognitiveDimensionMetric = CognitiveDimensionMetric(),
    val noRecallRate: CognitiveDimensionMetric = CognitiveDimensionMetric(),
    val averageResponseTimeMs: CognitiveDimensionMetric = CognitiveDimensionMetric()
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("shape_recognition", shapeRecognition.toJsonObject())
            put("color_recognition", colorRecognition.toJsonObject())
            put("overall_accuracy", overallMemoryAccuracy.toJsonObject())
            put("no_recall_rate", noRecallRate.toJsonObject())
            put("response_time_ms", averageResponseTimeMs.toJsonObject())
        }
    }
}

data class AttentionCognitiveState(
    val taskConsistency: CognitiveDimensionMetric = CognitiveDimensionMetric(),
    val missedRate: CognitiveDimensionMetric = CognitiveDimensionMetric(),
    val falseResponseRate: CognitiveDimensionMetric = CognitiveDimensionMetric(),
    val preparationAdherence: CognitiveDimensionMetric = CognitiveDimensionMetric()
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("task_consistency", taskConsistency.toJsonObject())
            put("missed_rate", missedRate.toJsonObject())
            put("false_response_rate", falseResponseRate.toJsonObject())
            put("preparation_adherence", preparationAdherence.toJsonObject())
        }
    }
}

data class ReactionTimeCognitiveState(
    val averageReactionTimeMs: CognitiveDimensionMetric = CognitiveDimensionMetric(),
    val variabilityMs: Double? = null,
    val fastestMs: Long? = null,
    val slowestMs: Long? = null,
    val trend: String = CognitiveDimensionMetric.TREND_INSUFFICIENT_DATA,
    val confidence: Double = 0.0
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("baseline", averageReactionTimeMs.baseline ?: JSONObject.NULL)
            put("recent", averageReactionTimeMs.recent ?: JSONObject.NULL)
            put("current", averageReactionTimeMs.current ?: JSONObject.NULL)
            put("trend", trend)
            put("confidence", confidence)
            put("average_reaction_time_ms", averageReactionTimeMs.toJsonObject())
            put("variability_ms", variabilityMs ?: JSONObject.NULL)
            put("fastest_ms", fastestMs ?: JSONObject.NULL)
            put("slowest_ms", slowestMs ?: JSONObject.NULL)
        }
    }
}

data class CoordinationCognitiveState(
    val hitRate: CognitiveDimensionMetric = CognitiveDimensionMetric(),
    val offTargetRate: CognitiveDimensionMetric = CognitiveDimensionMetric(),
    val timeoutRate: CognitiveDimensionMetric = CognitiveDimensionMetric(),
    val totalOffTargetInteractions: Int = 0,
    val averageAdaptiveDifficultySeconds: Double = 2.0,
    val trend: String = CognitiveDimensionMetric.TREND_INSUFFICIENT_DATA,
    val confidence: Double = 0.0
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("baseline", hitRate.baseline ?: JSONObject.NULL)
            put("recent", hitRate.recent ?: JSONObject.NULL)
            put("current", hitRate.current ?: JSONObject.NULL)
            put("trend", trend)
            put("confidence", confidence)
            put("hit_rate", hitRate.toJsonObject())
            put("off_target_rate", offTargetRate.toJsonObject())
            put("timeout_rate", timeoutRate.toJsonObject())
            put("total_off_target_interactions", totalOffTargetInteractions)
            put("adaptive_difficulty_seconds", averageAdaptiveDifficultySeconds)
        }
    }
}

data class OverallCognitiveState(
    val overallScore: CognitiveDimensionMetric = CognitiveDimensionMetric(),
    val observedPerformanceLabel: String = "Insufficient Data",
    val trend: String = CognitiveDimensionMetric.TREND_INSUFFICIENT_DATA,
    val confidence: Double = 0.0,
    val hasSufficientDataForTrend: Boolean = false
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("overall_score", overallScore.toJsonObject())
            put("observed_performance_label", observedPerformanceLabel)
            put("trend", trend)
            put("confidence", confidence)
            put("has_sufficient_data_for_trend", hasSufficientDataForTrend)
        }
    }
}

data class ActivityMonitoringSummary(
    val sessionsCompleted: Int = 0,
    val sessionsMissed: Int = 0,
    val recentActivityFrequencyPerWeek: Double = 0.0,
    val lastActivityTimestamp: Long? = null,
    val averageSessionDurationSeconds: Long = 0L,
    val completedCognitiveActivitiesCount: Int = 0,
    val hydrationRemindersCompleted: Int = 0,
    val hydrationRemindersMissed: Int = 0
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("sessions_completed", sessionsCompleted)
            put("sessions_missed", sessionsMissed)
            put("recent_activity_frequency_per_week", recentActivityFrequencyPerWeek)
            put("last_activity_timestamp", lastActivityTimestamp ?: JSONObject.NULL)
            put("average_session_duration_seconds", averageSessionDurationSeconds)
            put("completed_cognitive_activities_count", completedCognitiveActivitiesCount)
            put("hydration_reminders_completed", hydrationRemindersCompleted)
            put("hydration_reminders_missed", hydrationRemindersMissed)
        }
    }
}

/**
 * Structured inspection item for a completed session in the caregiver timeline.
 */
data class SessionInspectionDetail(
    val sessionId: Long,
    val sessionType: String,
    val startedAt: Long,
    val completedAt: Long,
    val durationSeconds: Long,
    val isCompleted: Boolean,
    val totalTrials: Int,
    val completedTrials: Int,
    val shapeAccuracy: Double? = null,
    val colorAccuracy: Double? = null,
    val overallAccuracy: Double? = null,
    val shapeResponseTimeMs: Double? = null,
    val colorResponseTimeMs: Double? = null,
    val overallResponseTimeMs: Double? = null,
    val noRecallCount: Int = 0,
    val incorrectCount: Int = 0,
    val trialsJson: String = "[]",
    val changeFromBaselineAccuracy: Double? = null,
    val changeFromRecentAccuracy: Double? = null,
    val reactionMeanMs: Double? = null,
    val reactionVariabilityMs: Double? = null,
    val reactionFastestMs: Long? = null,
    val reactionSlowestMs: Long? = null,
    val hitAccuracy: Double? = null,
    val offTargetTouchesCount: Int = 0,
    val adaptiveDifficultySeconds: Double? = null
)

/**
 * Complete Personal Cognitive State representing the user's observed behavioral performance.
 *
 * NOTE: This is NOT a medical diagnosis and does NOT claim to represent exact neural activity.
 */
data class PersonalCognitiveState(
    val updatedAt: Long = System.currentTimeMillis(),
    val totalCompletedSessions: Int = 0,
    val baselineEstablished: Boolean = false,
    val lastCompletedSessionAt: Long? = null,
    val memory: MemoryCognitiveState = MemoryCognitiveState(),
    val attention: AttentionCognitiveState = AttentionCognitiveState(),
    val reactionTime: ReactionTimeCognitiveState = ReactionTimeCognitiveState(),
    val coordination: CoordinationCognitiveState = CoordinationCognitiveState(),
    val overall: OverallCognitiveState = OverallCognitiveState(),
    val activitySummary: ActivityMonitoringSummary = ActivityMonitoringSummary(),
    val inspectionSessions: List<SessionInspectionDetail> = emptyList(),
    val rawJsonExport: String = "{}"
) {
    /**
     * Prepares structured JSON payload for future Gemini backend reasoning.
     */
    fun toGeminiContextJson(): String {
        return JSONObject().apply {
            put("version", "1.0")
            put("source", "behavioral_cognitive_state_processor")
            put("disclaimer", "Observed behavioral performance over time. Not a medical diagnosis.")
            put("cognitive_state", JSONObject().apply {
                put("baseline_established", baselineEstablished)
                put("total_completed_sessions", totalCompletedSessions)
                put("updated_at", updatedAt)
                put("last_completed_session_at", lastCompletedSessionAt ?: JSONObject.NULL)
                put("memory", memory.toJsonObject())
                put("attention", attention.toJsonObject())
                put("reaction_time", reactionTime.toJsonObject())
                put("coordination", coordination.toJsonObject())
                put("overall", overall.toJsonObject())
                put("activity_summary", activitySummary.toJsonObject())
            })
        }.toString(2)
    }
}
