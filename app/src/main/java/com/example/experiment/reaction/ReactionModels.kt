package com.example.experiment.reaction

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Individual trial measurement for the Reaction & Coordination experiment.
 * Distinguishes direct touches, surrounding boundary touches, off-target interactions,
 * and records both target appearance and vibration timestamps separately.
 */
data class ReactionTrialMeasurement(
    val trialId: String,
    val trialNumber: Int,
    val timestamp: String,
    val targetAppearanceTimestamp: Long,
    val targetLocationX: Float, // Normalized (0f..1f)
    val targetLocationY: Float, // Normalized (0f..1f)
    val targetRadiusDp: Float = 38f,
    val boundaryRadiusDp: Float = 65f,
    val visibilityDurationMs: Long,
    val difficultyLevel: Double, // Visibility duration in seconds (e.g., 2.0, 1.8)
    val vibrationTimestamp: Long,
    val firstTouchTimestamp: Long? = null,
    val reactionTimeMs: Long? = null, // Calculated from target appearance to first valid touch
    val hitType: String, // "direct_hit", "boundary_hit", "off_target", "timeout"
    val offTargetTouch: Boolean = false,
    val offTargetTouchesCount: Int = 0,
    val touchOffsetX: Float? = null,
    val touchOffsetY: Float? = null,
    val touchDistance: Float? = null,
    val timeout: Boolean = false
) {
    val isSuccess: Boolean
        get() = hitType == HIT_DIRECT || hitType == HIT_BOUNDARY

    companion object {
        const val HIT_DIRECT = "direct_hit"
        const val HIT_BOUNDARY = "boundary_hit"
        const val HIT_OFF_TARGET = "off_target"
        const val HIT_TIMEOUT = "timeout"
    }

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("trial_id", trialId)
            put("trial_number", trialNumber)
            put("timestamp", timestamp)
            put("target_appearance_timestamp", targetAppearanceTimestamp)
            put("target_location", JSONObject().apply {
                put("x", targetLocationX)
                put("y", targetLocationY)
                put("target_radius_dp", targetRadiusDp)
                put("boundary_radius_dp", boundaryRadiusDp)
            })
            put("visibility_duration_ms", visibilityDurationMs)
            put("difficulty_level", difficultyLevel)
            put("vibration_timestamp", vibrationTimestamp)
            put("first_touch_timestamp", firstTouchTimestamp ?: JSONObject.NULL)
            put("reaction_time_ms", reactionTimeMs ?: JSONObject.NULL)
            put("hit_type", hitType)
            put("is_success", isSuccess)
            put("off_target_touch", offTargetTouch)
            put("off_target_touches_count", offTargetTouchesCount)
            put("touch_offset_x", touchOffsetX ?: JSONObject.NULL)
            put("touch_offset_y", touchOffsetY ?: JSONObject.NULL)
            put("touch_distance", touchDistance ?: JSONObject.NULL)
            put("timeout", timeout)
        }
    }
}

/**
 * Summary metrics computed after every completed Reaction & Coordination session.
 */
data class ReactionSessionMeasurements(
    val totalTrials: Int,
    val completedTrials: Int,
    val meanReactionTimeMs: Double?,
    val medianReactionTimeMs: Double?,
    val reactionTimeVariabilityMs: Double?,
    val fastestReactionTimeMs: Long?,
    val slowestReactionTimeMs: Long?,
    val hitAccuracy: Double, // (direct + boundary hits) / completed
    val directHitRate: Double,
    val boundaryHitRate: Double,
    val missTimeoutRate: Double,
    val offTargetTouchRate: Double,
    val totalOffTargetTouches: Int,
    val adaptiveDifficultySeconds: Double
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("total_trials", totalTrials)
            put("completed_trials", completedTrials)
            put("mean_reaction_time_ms", meanReactionTimeMs ?: JSONObject.NULL)
            put("median_reaction_time_ms", medianReactionTimeMs ?: JSONObject.NULL)
            put("reaction_time_variability_ms", reactionTimeVariabilityMs ?: JSONObject.NULL)
            put("fastest_reaction_time_ms", fastestReactionTimeMs ?: JSONObject.NULL)
            put("slowest_reaction_time_ms", slowestReactionTimeMs ?: JSONObject.NULL)
            put("hit_accuracy", hitAccuracy)
            put("direct_hit_rate", directHitRate)
            put("boundary_hit_rate", boundaryHitRate)
            put("miss_timeout_rate", missTimeoutRate)
            put("off_target_touch_rate", offTargetTouchRate)
            put("total_off_target_touches", totalOffTargetTouches)
            put("adaptive_difficulty_seconds", adaptiveDifficultySeconds)
        }
    }
}

/**
 * Encapsulates the entire session result ready for persistence in UserSession.
 */
data class ReactionExperimentSessionResult(
    val startedAt: Long,
    val completedAt: Long,
    val measurements: ReactionSessionMeasurements,
    val trials: List<ReactionTrialMeasurement>,
    val metadataJson: String
)

/**
 * Engine and calculations for the Reaction & Coordination experiment.
 */
object ReactionCoordinationExperiment {
    const val EXPERIMENT_TYPE = "reaction_coordination"
    const val DEFAULT_DIFFICULTY_SECONDS = 2.0
    const val MIN_DIFFICULTY_SECONDS = 1.0
    const val MAX_DIFFICULTY_SECONDS = 2.6
    const val DIFFICULTY_STEP = 0.2

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    /**
     * Determines the starting adaptive difficulty for a new session based on previous performance.
     * Starts at 2.0 seconds. Adapts gradually without knee-jerk reactions from single trials.
     */
    fun computeAdaptiveDifficulty(previousReactionSessions: List<ReactionSessionMeasurements>): Double {
        if (previousReactionSessions.isEmpty()) {
            return DEFAULT_DIFFICULTY_SECONDS
        }

        val lastSession = previousReactionSessions.last()
        val currentDiff = lastSession.adaptiveDifficultySeconds

        // Need stable high accuracy (>= 80%) with low off-target rate to increase difficulty
        return when {
            lastSession.hitAccuracy >= 0.80 && lastSession.missTimeoutRate <= 0.15 -> {
                (currentDiff - DIFFICULTY_STEP).coerceAtLeast(MIN_DIFFICULTY_SECONDS)
            }
            lastSession.hitAccuracy < 0.60 || lastSession.missTimeoutRate >= 0.35 -> {
                (currentDiff + DIFFICULTY_STEP).coerceAtMost(MAX_DIFFICULTY_SECONDS)
            }
            else -> currentDiff
        }
    }

    /**
     * Calculates summary metrics from a completed set of trials.
     */
    fun calculateMeasurements(
        trials: List<ReactionTrialMeasurement>,
        totalTrials: Int,
        adaptiveDifficultySeconds: Double
    ): ReactionSessionMeasurements {
        val total = totalTrials.coerceAtLeast(1)
        val completed = trials.size

        val validHits = trials.filter { it.isSuccess }
        val reactionTimes = validHits.mapNotNull { it.reactionTimeMs }.sorted()

        val meanRt = if (reactionTimes.isNotEmpty()) reactionTimes.average() else null

        val medianRt = if (reactionTimes.isNotEmpty()) {
            val mid = reactionTimes.size / 2
            if (reactionTimes.size % 2 == 0) {
                (reactionTimes[mid - 1] + reactionTimes[mid]) / 2.0
            } else {
                reactionTimes[mid].toDouble()
            }
        } else null

        val rtVariability = if (reactionTimes.size >= 2 && meanRt != null) {
            val variance = reactionTimes.map { (it - meanRt).pow(2) }.average()
            sqrt(variance)
        } else null

        val fastestRt = reactionTimes.minOrNull()
        val slowestRt = reactionTimes.maxOrNull()

        val directHits = trials.count { it.hitType == ReactionTrialMeasurement.HIT_DIRECT }
        val boundaryHits = trials.count { it.hitType == ReactionTrialMeasurement.HIT_BOUNDARY }
        val timeouts = trials.count { it.timeout || it.hitType == ReactionTrialMeasurement.HIT_TIMEOUT }
        val offTargetTrials = trials.count { it.offTargetTouch }
        val totalOffTargetCount = trials.sumOf { it.offTargetTouchesCount }

        return ReactionSessionMeasurements(
            totalTrials = total,
            completedTrials = completed,
            meanReactionTimeMs = meanRt,
            medianReactionTimeMs = medianRt,
            reactionTimeVariabilityMs = rtVariability,
            fastestReactionTimeMs = fastestRt,
            slowestReactionTimeMs = slowestRt,
            hitAccuracy = validHits.size.toDouble() / total,
            directHitRate = directHits.toDouble() / total,
            boundaryHitRate = boundaryHits.toDouble() / total,
            missTimeoutRate = timeouts.toDouble() / total,
            offTargetTouchRate = offTargetTrials.toDouble() / total,
            totalOffTargetTouches = totalOffTargetCount,
            adaptiveDifficultySeconds = adaptiveDifficultySeconds
        )
    }

    /**
     * Serializes session measurements and raw trial data into structured JSON.
     */
    fun serializeSessionMetadata(
        measurements: ReactionSessionMeasurements,
        trials: List<ReactionTrialMeasurement>
    ): String {
        val root = JSONObject()
        root.put("type", EXPERIMENT_TYPE)
        root.put("total_trials", measurements.totalTrials)
        root.put("completed_trials", measurements.completedTrials)
        root.put("metrics", measurements.toJsonObject())

        val trialsArray = JSONArray()
        trials.forEach { trial ->
            trialsArray.put(trial.toJsonObject())
        }
        root.put("trials", trialsArray)

        return root.toString()
    }
}
