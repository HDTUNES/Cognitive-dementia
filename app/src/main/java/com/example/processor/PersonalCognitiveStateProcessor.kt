package com.example.processor

import com.example.data.model.HydrationRecord
import com.example.data.model.PersonalBaseline
import com.example.data.model.UserSession
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.pow
import kotlin.math.sqrt

object PersonalCognitiveStateProcessor {

    data class ExtractedSessionMetrics(
        val sessionId: Long,
        val sessionType: String,
        val startedAt: Long,
        val completedAt: Long,
        val durationSeconds: Long,
        val isCompleted: Boolean,
        val totalTrials: Int,
        val completedTrials: Int,
        val shapeAccuracy: Double?,
        val colorAccuracy: Double?,
        val overallAccuracy: Double?,
        val shapeNoRecallRate: Double?,
        val colorNoRecallRate: Double?,
        val overallNoRecallRate: Double?,
        val shapeResponseTimeMs: Double?,
        val colorResponseTimeMs: Double?,
        val overallResponseTimeMs: Double?,
        val noRecallCount: Int,
        val incorrectCount: Int,
        val rawTrialsJson: String
    )

    /**
     * Extracts structured metrics from a UserSession.
     */
    fun extractSessionMetrics(session: UserSession): ExtractedSessionMetrics {
        val durationSec = if (session.completedAt != null && session.completedAt > session.startedAt) {
            (session.completedAt - session.startedAt) / 1000L
        } else 0L

        var shapeAcc: Double? = null
        var colorAcc: Double? = null
        var overallAcc: Double? = null
        var shapeNoRecall: Double? = null
        var colorNoRecall: Double? = null
        var overallNoRecall: Double? = null
        var shapeRt: Double? = null
        var colorRt: Double? = null
        var overallRt: Double? = null
        var noRecallCount = 0
        var incorrectCount = 0
        var totalTrials = session.cyclesCompleted
        var completedTrials = session.cyclesCompleted
        var rawTrials = "[]"

        if (!session.metadata.isNullOrBlank()) {
            try {
                val json = JSONObject(session.metadata)
                totalTrials = json.optInt("total_trials", session.cyclesCompleted)
                completedTrials = json.optInt("completed_trials", session.cyclesCompleted)

                if (json.has("metrics")) {
                    val m = json.getJSONObject("metrics")
                    shapeAcc = when {
                        m.has("shape_accuracy") -> m.optDouble("shape_accuracy")
                        m.has("shapeAccuracy") -> m.optDouble("shapeAccuracy")
                        else -> null
                    }?.takeIf { !it.isNaN() }

                    colorAcc = when {
                        m.has("color_accuracy") -> m.optDouble("color_accuracy")
                        m.has("colorAccuracy") -> m.optDouble("colorAccuracy")
                        else -> null
                    }?.takeIf { !it.isNaN() }

                    shapeNoRecall = when {
                        m.has("shape_no_recall_rate") -> m.optDouble("shape_no_recall_rate")
                        m.has("shapeNoRecallRate") -> m.optDouble("shapeNoRecallRate")
                        else -> null
                    }?.takeIf { !it.isNaN() }

                    colorNoRecall = when {
                        m.has("color_no_recall_rate") -> m.optDouble("color_no_recall_rate")
                        m.has("colorNoRecallRate") -> m.optDouble("colorNoRecallRate")
                        else -> null
                    }?.takeIf { !it.isNaN() }

                    shapeRt = when {
                        m.has("average_shape_response_time_ms") -> m.optDouble("average_shape_response_time_ms")
                        m.has("shapeAverageReactionTimeMs") -> m.optDouble("shapeAverageReactionTimeMs")
                        m.has("average_reaction_time_ms") -> m.optDouble("average_reaction_time_ms")
                        m.has("averageReactionTimeMs") -> m.optDouble("averageReactionTimeMs")
                        else -> null
                    }?.takeIf { !it.isNaN() }

                    colorRt = when {
                        m.has("average_color_response_time_ms") -> m.optDouble("average_color_response_time_ms")
                        m.has("colorAverageReactionTimeMs") -> m.optDouble("colorAverageReactionTimeMs")
                        m.has("average_reaction_time_ms") -> m.optDouble("average_reaction_time_ms")
                        m.has("averageReactionTimeMs") -> m.optDouble("averageReactionTimeMs")
                        else -> null
                    }?.takeIf { !it.isNaN() }

                    val explicitOverallAcc = when {
                        m.has("overall_accuracy") -> m.optDouble("overall_accuracy")
                        m.has("overallAccuracy") -> m.optDouble("overallAccuracy")
                        else -> null
                    }?.takeIf { !it.isNaN() }

                    if (explicitOverallAcc != null) {
                        overallAcc = explicitOverallAcc
                    } else if (shapeAcc != null && colorAcc != null) {
                        overallAcc = (shapeAcc + colorAcc) / 2.0
                    } else {
                        overallAcc = shapeAcc ?: colorAcc
                    }

                    if (shapeNoRecall != null && colorNoRecall != null) {
                        overallNoRecall = (shapeNoRecall + colorNoRecall) / 2.0
                    } else {
                        overallNoRecall = shapeNoRecall ?: colorNoRecall
                    }

                    val explicitOverallRt = when {
                        m.has("overall_response_time_ms") -> m.optDouble("overall_response_time_ms")
                        m.has("overallResponseTimeMs") -> m.optDouble("overallResponseTimeMs")
                        m.has("average_reaction_time_ms") -> m.optDouble("average_reaction_time_ms")
                        m.has("averageReactionTimeMs") -> m.optDouble("averageReactionTimeMs")
                        else -> null
                    }?.takeIf { !it.isNaN() }

                    if (explicitOverallRt != null) {
                        overallRt = explicitOverallRt
                    } else if (shapeRt != null && colorRt != null) {
                        overallRt = (shapeRt + colorRt) / 2.0
                    } else {
                        overallRt = shapeRt ?: colorRt
                    }
                }

                if (json.has("trials")) {
                    val trialsArray = json.getJSONArray("trials")
                    rawTrials = trialsArray.toString()
                    for (i in 0 until trialsArray.length()) {
                        val t = trialsArray.getJSONObject(i)
                        val sResp = t.optJSONObject("shape_response")
                        val cResp = t.optJSONObject("color_response")

                        if (sResp?.optString("response_type") == "no_recall") noRecallCount++
                        if (cResp?.optString("response_type") == "no_recall") noRecallCount++

                        if (sResp != null && !sResp.optBoolean("correct") && sResp.optString("response_type") != "no_recall") {
                            incorrectCount++
                        }
                        if (cResp != null && !cResp.optBoolean("correct") && cResp.optString("response_type") != "no_recall") {
                            incorrectCount++
                        }
                    }
                }
            } catch (_: Exception) {
                // Keep defaults if parsing legacy or malformed metadata
            }
        }

        return ExtractedSessionMetrics(
            sessionId = session.id,
            sessionType = session.sessionType,
            startedAt = session.startedAt,
            completedAt = session.completedAt ?: session.startedAt,
            durationSeconds = durationSec,
            isCompleted = session.isCompleted,
            totalTrials = totalTrials,
            completedTrials = completedTrials,
            shapeAccuracy = shapeAcc,
            colorAccuracy = colorAcc,
            overallAccuracy = overallAcc,
            shapeNoRecallRate = shapeNoRecall,
            colorNoRecallRate = colorNoRecall,
            overallNoRecallRate = overallNoRecall,
            shapeResponseTimeMs = shapeRt,
            colorResponseTimeMs = colorRt,
            overallResponseTimeMs = overallRt,
            noRecallCount = noRecallCount,
            incorrectCount = incorrectCount,
            rawTrialsJson = rawTrials
        )
    }

    data class ExtractedReactionMetrics(
        val sessionId: Long,
        val startedAt: Long,
        val completedAt: Long,
        val durationSeconds: Long,
        val isCompleted: Boolean,
        val totalTrials: Int,
        val completedTrials: Int,
        val meanReactionTimeMs: Double?,
        val medianReactionTimeMs: Double?,
        val reactionTimeVariabilityMs: Double?,
        val fastestReactionTimeMs: Long?,
        val slowestReactionTimeMs: Long?,
        val hitAccuracy: Double?,
        val directHitRate: Double?,
        val boundaryHitRate: Double?,
        val missTimeoutRate: Double?,
        val offTargetTouchRate: Double?,
        val totalOffTargetTouches: Int,
        val adaptiveDifficultySeconds: Double,
        val rawTrialsJson: String
    )

    fun extractReactionSessionMetrics(session: UserSession): ExtractedReactionMetrics {
        val durationSec = if (session.completedAt != null && session.completedAt > session.startedAt) {
            (session.completedAt - session.startedAt) / 1000L
        } else 0L

        var meanRt: Double? = null
        var medianRt: Double? = null
        var rtVariability: Double? = null
        var fastestRt: Long? = null
        var slowestRt: Long? = null
        var hitAcc: Double? = null
        var directRate: Double? = null
        var boundaryRate: Double? = null
        var missRate: Double? = null
        var offTargetRate: Double? = null
        var offTargetCount = 0
        var adaptiveDiff = 2.0
        var totalTrials = session.cyclesCompleted
        var completedTrials = session.cyclesCompleted
        var rawTrials = "[]"

        if (!session.metadata.isNullOrBlank()) {
            try {
                val json = JSONObject(session.metadata)
                totalTrials = json.optInt("total_trials", session.cyclesCompleted)
                completedTrials = json.optInt("completed_trials", session.cyclesCompleted)
                if (json.has("metrics")) {
                    val m = json.getJSONObject("metrics")
                    meanRt = m.optDouble("mean_reaction_time_ms").takeIf { !it.isNaN() }
                    medianRt = m.optDouble("median_reaction_time_ms").takeIf { !it.isNaN() }
                    rtVariability = m.optDouble("reaction_time_variability_ms").takeIf { !it.isNaN() }
                    if (m.has("fastest_reaction_time_ms")) fastestRt = m.optLong("fastest_reaction_time_ms")
                    if (m.has("slowest_reaction_time_ms")) slowestRt = m.optLong("slowest_reaction_time_ms")
                    hitAcc = m.optDouble("hit_accuracy").takeIf { !it.isNaN() }
                    directRate = m.optDouble("direct_hit_rate").takeIf { !it.isNaN() }
                    boundaryRate = m.optDouble("boundary_hit_rate").takeIf { !it.isNaN() }
                    missRate = m.optDouble("miss_timeout_rate").takeIf { !it.isNaN() }
                    offTargetRate = m.optDouble("off_target_touch_rate").takeIf { !it.isNaN() }
                    offTargetCount = m.optInt("total_off_target_touches", 0)
                    adaptiveDiff = m.optDouble("adaptive_difficulty_seconds", 2.0)
                }
                if (json.has("trials")) {
                    rawTrials = json.getJSONArray("trials").toString()
                }
            } catch (_: Exception) {
                // Keep default values if metadata is invalid
            }
        }

        return ExtractedReactionMetrics(
            sessionId = session.id,
            startedAt = session.startedAt,
            completedAt = session.completedAt ?: session.startedAt,
            durationSeconds = durationSec,
            isCompleted = session.isCompleted,
            totalTrials = totalTrials,
            completedTrials = completedTrials,
            meanReactionTimeMs = meanRt,
            medianReactionTimeMs = medianRt,
            reactionTimeVariabilityMs = rtVariability,
            fastestReactionTimeMs = fastestRt,
            slowestReactionTimeMs = slowestRt,
            hitAccuracy = hitAcc,
            directHitRate = directRate,
            boundaryHitRate = boundaryRate,
            missTimeoutRate = missRate,
            offTargetTouchRate = offTargetRate,
            totalOffTargetTouches = offTargetCount,
            adaptiveDifficultySeconds = adaptiveDiff,
            rawTrialsJson = rawTrials
        )
    }

    /**
     * Processes all sessions, computes Baseline -> Recent -> Current, and updates the Personal Cognitive State.
     */
    fun processCognitiveState(
        allSessions: List<UserSession>,
        existingBaseline: PersonalBaseline?,
        hydrationRecords: List<HydrationRecord> = emptyList()
    ): Pair<PersonalCognitiveState, PersonalBaseline> {
        // Filter memory recognition sessions in chronological order
        val memorySessions = allSessions
            .filter { it.sessionType == UserSession.TYPE_MEMORY_RECOGNITION && it.isCompleted }
            .sortedBy { it.startedAt }
            .map { extractSessionMetrics(it) }

        // Filter reaction & coordination sessions in chronological order
        val reactionSessions = allSessions
            .filter { it.sessionType == UserSession.TYPE_REACTION_COORDINATION && it.isCompleted }
            .sortedBy { it.startedAt }
            .map { extractReactionSessionMetrics(it) }

        val preparationSessions = allSessions
            .filter { it.sessionType == UserSession.TYPE_PREPARATION }
            .sortedBy { it.startedAt }

        // 1. BASELINE DETERMINATION
        val baselineEstablished = existingBaseline?.established == true || memorySessions.isNotEmpty() || reactionSessions.isNotEmpty()
        val baselineRecord: PersonalBaseline

        val baselineMetrics: BaselineValues
        if (existingBaseline != null && existingBaseline.established && existingBaseline.rawMetricsJson.isNotBlank()) {
            val parsed = parseBaselineJson(existingBaseline.rawMetricsJson)
            // If existing baseline didn't have reaction metrics yet, but reaction sessions are available, supplement it
            if (parsed.reactionMeanRtMs == null && reactionSessions.isNotEmpty()) {
                val reactionCohort = reactionSessions.take(minOf(2, reactionSessions.size))
                val baseMeanRt = reactionCohort.mapNotNull { it.meanReactionTimeMs }.averageOrNull() ?: 1200.0
                val baseCoordHit = reactionCohort.mapNotNull { it.hitAccuracy }.averageOrNull() ?: 0.90
                val baseOffTarget = reactionCohort.mapNotNull { it.offTargetTouchRate }.averageOrNull() ?: 0.05
                val baseDiff = reactionCohort.map { it.adaptiveDifficultySeconds }.averageOrNull() ?: 2.0
                baselineMetrics = parsed.copy(
                    reactionMeanRtMs = baseMeanRt,
                    coordinationHitRate = baseCoordHit,
                    coordinationOffTargetRate = baseOffTarget,
                    adaptiveDifficultySeconds = baseDiff
                )
                val jsonStr = baselineMetrics.toJson().toString()
                baselineRecord = existingBaseline.copy(
                    rawMetricsJson = jsonStr,
                    baselineReactionTimeMs = existingBaseline.baselineReactionTimeMs ?: baseMeanRt.toLong()
                )
            } else {
                baselineRecord = existingBaseline
                baselineMetrics = parsed
            }
        } else if (memorySessions.isNotEmpty() || reactionSessions.isNotEmpty()) {
            // Establish baseline from the first session (or first 2 if available)
            val memoryCohort = memorySessions.take(minOf(2, memorySessions.size))
            val baseShapeAcc = memoryCohort.mapNotNull { it.shapeAccuracy }.averageOrNull() ?: 0.8
            val baseColorAcc = memoryCohort.mapNotNull { it.colorAccuracy }.averageOrNull() ?: 0.8
            val baseOverallAcc = memoryCohort.mapNotNull { it.overallAccuracy }.averageOrNull() ?: 0.8
            val baseNoRecall = memoryCohort.mapNotNull { it.overallNoRecallRate }.averageOrNull() ?: 0.0
            val baseMemoryRt = memoryCohort.mapNotNull { it.overallResponseTimeMs }.averageOrNull() ?: 1500.0

            val reactionCohort = reactionSessions.take(minOf(2, reactionSessions.size))
            val baseMeanRt = reactionCohort.mapNotNull { it.meanReactionTimeMs }.averageOrNull()
            val baseCoordHit = reactionCohort.mapNotNull { it.hitAccuracy }.averageOrNull()
            val baseOffTarget = reactionCohort.mapNotNull { it.offTargetTouchRate }.averageOrNull()
            val baseDiff = reactionCohort.map { it.adaptiveDifficultySeconds }.averageOrNull() ?: 2.0

            val effectiveRt = baseMeanRt ?: baseMemoryRt

            baselineMetrics = BaselineValues(
                established = true,
                createdAt = memorySessions.firstOrNull()?.startedAt ?: reactionSessions.first().startedAt,
                shapeAccuracy = baseShapeAcc,
                colorAccuracy = baseColorAcc,
                overallAccuracy = baseOverallAcc,
                noRecallRate = baseNoRecall,
                reactionTimeMs = baseMemoryRt,
                attentionConsistency = 0.90,
                reactionMeanRtMs = baseMeanRt,
                coordinationHitRate = baseCoordHit,
                coordinationOffTargetRate = baseOffTarget,
                adaptiveDifficultySeconds = baseDiff
            )

            val jsonStr = baselineMetrics.toJson().toString()
            baselineRecord = PersonalBaseline(
                id = 1,
                established = true,
                createdAt = baselineMetrics.createdAt,
                baselineReactionTimeMs = effectiveRt.toLong(),
                baselineMemorySpan = 4,
                baselineAttentionScore = baseOverallAcc,
                rawMetricsJson = jsonStr
            )
        } else {
            baselineRecord = existingBaseline ?: PersonalBaseline(id = 1, established = false)
            baselineMetrics = BaselineValues(established = false)
        }

        // 2. CURRENT SESSION
        val currentMemorySession = memorySessions.lastOrNull()
        val currentReactionSession = reactionSessions.lastOrNull()

        // 3. RECENT PERFORMANCE (Rolling window of up to 5 most recent sessions)
        val recentMemoryCohort = if (memorySessions.size > 1) {
            memorySessions.takeLast(minOf(5, memorySessions.size))
        } else {
            memorySessions
        }

        val recentShapeAcc = recentMemoryCohort.mapNotNull { it.shapeAccuracy }.averageOrNull()
        val recentColorAcc = recentMemoryCohort.mapNotNull { it.colorAccuracy }.averageOrNull()
        val recentOverallAcc = recentMemoryCohort.mapNotNull { it.overallAccuracy }.averageOrNull()
        val recentNoRecall = recentMemoryCohort.mapNotNull { it.overallNoRecallRate }.averageOrNull()
        val recentMemoryRt = recentMemoryCohort.mapNotNull { it.overallResponseTimeMs }.averageOrNull()

        // Recent variability (standard deviation)
        val memoryRtValues = recentMemoryCohort.mapNotNull { it.overallResponseTimeMs }
        val memoryRtVariability = calculateStandardDeviation(memoryRtValues)
        val accValues = recentMemoryCohort.mapNotNull { it.overallAccuracy }
        val accVariability = calculateStandardDeviation(accValues)

        // 4. REACTION SESSIONS RECENT PERFORMANCE
        val recentReactionCohort = if (reactionSessions.size > 1) {
            reactionSessions.takeLast(minOf(5, reactionSessions.size))
        } else {
            reactionSessions
        }

        val recentMeanReactionRt = recentReactionCohort.mapNotNull { it.meanReactionTimeMs }.averageOrNull()
        val recentHitRate = recentReactionCohort.mapNotNull { it.hitAccuracy }.averageOrNull()
        val recentOffTargetRate = recentReactionCohort.mapNotNull { it.offTargetTouchRate }.averageOrNull()
        val recentTimeoutRate = recentReactionCohort.mapNotNull { it.missTimeoutRate }.averageOrNull()
        val reactionRtValues = recentReactionCohort.mapNotNull { it.meanReactionTimeMs }
        val reactionRtVariability = calculateStandardDeviation(reactionRtValues)

        // 5. METRIC POINTS WITH BASELINE -> RECENT -> CURRENT & TRENDS
        val totalMemorySessions = memorySessions.size
        val totalReactionSessions = reactionSessions.size
        val totalCompletedCognitiveSessions = totalMemorySessions + totalReactionSessions

        val shapeMetric = buildDimensionMetric(
            baseline = if (baselineMetrics.established) baselineMetrics.shapeAccuracy else null,
            recent = recentShapeAcc,
            current = currentMemorySession?.shapeAccuracy,
            history = memorySessions.mapNotNull { it.shapeAccuracy },
            isLowerBetter = false
        )

        val colorMetric = buildDimensionMetric(
            baseline = if (baselineMetrics.established) baselineMetrics.colorAccuracy else null,
            recent = recentColorAcc,
            current = currentMemorySession?.colorAccuracy,
            history = memorySessions.mapNotNull { it.colorAccuracy },
            isLowerBetter = false
        )

        val overallMemoryAccMetric = buildDimensionMetric(
            baseline = if (baselineMetrics.established) baselineMetrics.overallAccuracy else null,
            recent = recentOverallAcc,
            current = currentMemorySession?.overallAccuracy,
            history = memorySessions.mapNotNull { it.overallAccuracy },
            isLowerBetter = false,
            variability = accVariability
        )

        val noRecallMetric = buildDimensionMetric(
            baseline = if (baselineMetrics.established) baselineMetrics.noRecallRate else null,
            recent = recentNoRecall,
            current = currentMemorySession?.overallNoRecallRate,
            history = memorySessions.mapNotNull { it.overallNoRecallRate },
            isLowerBetter = true
        )

        // Reaction Time Dimension: prioritized from dedicated reaction experiment if performed,
        // otherwise falling back to memory experiment response time.
        val reactionTimeMetric = if (reactionSessions.isNotEmpty()) {
            buildDimensionMetric(
                baseline = baselineMetrics.reactionMeanRtMs ?: recentMeanReactionRt,
                recent = recentMeanReactionRt,
                current = currentReactionSession?.meanReactionTimeMs,
                history = reactionSessions.mapNotNull { it.meanReactionTimeMs },
                isLowerBetter = true,
                variability = currentReactionSession?.reactionTimeVariabilityMs ?: reactionRtVariability
            )
        } else {
            buildDimensionMetric(
                baseline = if (baselineMetrics.established) baselineMetrics.reactionTimeMs else null,
                recent = recentMemoryRt,
                current = currentMemorySession?.overallResponseTimeMs,
                history = memorySessions.mapNotNull { it.overallResponseTimeMs },
                isLowerBetter = true,
                variability = memoryRtVariability
            )
        }

        // Coordination Dimension
        val coordinationHitRateMetric = buildDimensionMetric(
            baseline = baselineMetrics.coordinationHitRate,
            recent = recentHitRate,
            current = currentReactionSession?.hitAccuracy,
            history = reactionSessions.mapNotNull { it.hitAccuracy },
            isLowerBetter = false
        )

        val coordinationOffTargetMetric = buildDimensionMetric(
            baseline = baselineMetrics.coordinationOffTargetRate,
            recent = recentOffTargetRate,
            current = currentReactionSession?.offTargetTouchRate,
            history = reactionSessions.mapNotNull { it.offTargetTouchRate },
            isLowerBetter = true
        )

        val coordinationTimeoutMetric = buildDimensionMetric(
            baseline = if (reactionSessions.isNotEmpty()) 0.0 else null,
            recent = recentTimeoutRate,
            current = currentReactionSession?.missTimeoutRate,
            history = reactionSessions.mapNotNull { it.missTimeoutRate },
            isLowerBetter = true
        )

        val coordinationState = CoordinationCognitiveState(
            hitRate = coordinationHitRateMetric,
            offTargetRate = coordinationOffTargetMetric,
            timeoutRate = coordinationTimeoutMetric,
            totalOffTargetInteractions = reactionSessions.sumOf { it.totalOffTargetTouches },
            averageAdaptiveDifficultySeconds = currentReactionSession?.adaptiveDifficultySeconds ?: 2.0,
            trend = coordinationHitRateMetric.trend,
            confidence = coordinationHitRateMetric.confidence
        )

        val reactionTimeState = ReactionTimeCognitiveState(
            averageReactionTimeMs = reactionTimeMetric,
            variabilityMs = if (reactionSessions.isNotEmpty()) currentReactionSession?.reactionTimeVariabilityMs ?: reactionRtVariability else memoryRtVariability,
            fastestMs = currentReactionSession?.fastestReactionTimeMs,
            slowestMs = currentReactionSession?.slowestReactionTimeMs,
            trend = reactionTimeMetric.trend,
            confidence = reactionTimeMetric.confidence
        )

        // Attention metrics derived from preparation adherence and consistency
        val prepCompleted = preparationSessions.count { it.isCompleted }
        val prepTotal = preparationSessions.size.coerceAtLeast(1)
        val prepAdherenceRate = prepCompleted.toDouble() / prepTotal.toDouble()

        val attentionConsistency = buildDimensionMetric(
            baseline = if (baselineMetrics.established) 0.90 else null,
            recent = prepAdherenceRate,
            current = if (preparationSessions.isNotEmpty()) (if (preparationSessions.last().isCompleted) 1.0 else 0.0) else null,
            history = preparationSessions.map { if (it.isCompleted) 1.0 else 0.0 },
            isLowerBetter = false
        )

        val attentionMissedRate = buildDimensionMetric(
            baseline = if (baselineMetrics.established) 0.05 else null,
            recent = (prepTotal - prepCompleted).toDouble() / prepTotal.toDouble(),
            current = if (preparationSessions.isNotEmpty()) (if (preparationSessions.last().isCompleted) 0.0 else 1.0) else null,
            history = preparationSessions.map { if (it.isCompleted) 0.0 else 1.0 },
            isLowerBetter = true
        )

        val attentionState = AttentionCognitiveState(
            taskConsistency = attentionConsistency,
            missedRate = attentionMissedRate,
            falseResponseRate = CognitiveDimensionMetric(
                baseline = 0.0,
                recent = 0.0,
                current = 0.0,
                trend = CognitiveDimensionMetric.TREND_STABLE,
                confidence = if (totalCompletedCognitiveSessions >= 3) 0.8 else 0.4,
                dataQuality = if (totalCompletedCognitiveSessions >= 3) CognitiveDimensionMetric.QUALITY_HIGH else CognitiveDimensionMetric.QUALITY_LOW
            ),
            preparationAdherence = attentionConsistency
        )

        val memoryState = MemoryCognitiveState(
            shapeRecognition = shapeMetric,
            colorRecognition = colorMetric,
            overallMemoryAccuracy = overallMemoryAccMetric,
            noRecallRate = noRecallMetric,
            averageResponseTimeMs = reactionTimeMetric
        )

        // Overall State calculation combines available cognitive indicators
        val memoryScore = overallMemoryAccMetric.current
        val coordScore = coordinationHitRateMetric.current
        val combinedOverallCurrent = when {
            memoryScore != null && coordScore != null -> (memoryScore + coordScore) / 2.0
            memoryScore != null -> memoryScore
            coordScore != null -> coordScore
            else -> null
        }

        val combinedOverallBaseline = when {
            baselineMetrics.overallAccuracy != null && baselineMetrics.coordinationHitRate != null ->
                (baselineMetrics.overallAccuracy + baselineMetrics.coordinationHitRate) / 2.0
            baselineMetrics.overallAccuracy != null -> baselineMetrics.overallAccuracy
            baselineMetrics.coordinationHitRate != null -> baselineMetrics.coordinationHitRate
            else -> null
        }

        val combinedOverallRecent = when {
            recentOverallAcc != null && recentHitRate != null -> (recentOverallAcc + recentHitRate) / 2.0
            recentOverallAcc != null -> recentOverallAcc
            recentHitRate != null -> recentHitRate
            else -> null
        }

        val overallHistory = (memorySessions.mapNotNull { it.overallAccuracy } + reactionSessions.mapNotNull { it.hitAccuracy })

        val overallMetric = buildDimensionMetric(
            baseline = combinedOverallBaseline,
            recent = combinedOverallRecent,
            current = combinedOverallCurrent,
            history = overallHistory,
            isLowerBetter = false,
            variability = accVariability
        )

        val observedPerformanceLabel = when {
            totalCompletedCognitiveSessions == 0 -> "Insufficient Data"
            overallMetric.current == null -> "Insufficient Data"
            overallMetric.current >= 0.85 -> "Consistent / Robust"
            overallMetric.current >= 0.65 -> "Moderate Stability"
            else -> "Variable Stability"
        }

        val overallState = OverallCognitiveState(
            overallScore = overallMetric,
            observedPerformanceLabel = observedPerformanceLabel,
            trend = overallMetric.trend,
            confidence = overallMetric.confidence,
            hasSufficientDataForTrend = totalCompletedCognitiveSessions >= 3
        )

        // Activity monitoring summary
        val hydrationCompleted = hydrationRecords.count { it.isCompleted }
        val hydrationMissed = hydrationRecords.count { it.isMissed }
        val totalSessionsDuration = memorySessions.sumOf { it.durationSeconds } + reactionSessions.sumOf { it.durationSeconds }
        val avgDuration = if (totalCompletedCognitiveSessions > 0) totalSessionsDuration / totalCompletedCognitiveSessions else 0L

        val activitySummary = ActivityMonitoringSummary(
            sessionsCompleted = allSessions.count { it.isCompleted },
            sessionsMissed = hydrationMissed,
            recentActivityFrequencyPerWeek = (allSessions.count { it.isCompleted } * 7.0 / 30.0).coerceAtLeast(0.0),
            lastActivityTimestamp = allSessions.maxOfOrNull { it.completedAt ?: it.startedAt },
            averageSessionDurationSeconds = avgDuration,
            completedCognitiveActivitiesCount = totalCompletedCognitiveSessions,
            hydrationRemindersCompleted = hydrationCompleted,
            hydrationRemindersMissed = hydrationMissed
        )

        // Inspection sessions: both Memory and Reaction & Coordination sessions, ordered by startedAt descending
        val memoryInspectionList = memorySessions.map { session ->
            val changeBase = if (baselineMetrics.overallAccuracy != null && session.overallAccuracy != null) {
                session.overallAccuracy - baselineMetrics.overallAccuracy
            } else null
            val changeRecent = if (recentOverallAcc != null && session.overallAccuracy != null) {
                session.overallAccuracy - recentOverallAcc
            } else null

            SessionInspectionDetail(
                sessionId = session.sessionId,
                sessionType = session.sessionType,
                startedAt = session.startedAt,
                completedAt = session.completedAt,
                durationSeconds = session.durationSeconds,
                isCompleted = session.isCompleted,
                totalTrials = session.totalTrials,
                completedTrials = session.completedTrials,
                shapeAccuracy = session.shapeAccuracy,
                colorAccuracy = session.colorAccuracy,
                overallAccuracy = session.overallAccuracy,
                shapeResponseTimeMs = session.shapeResponseTimeMs,
                colorResponseTimeMs = session.colorResponseTimeMs,
                overallResponseTimeMs = session.overallResponseTimeMs,
                noRecallCount = session.noRecallCount,
                incorrectCount = session.incorrectCount,
                trialsJson = session.rawTrialsJson,
                changeFromBaselineAccuracy = changeBase,
                changeFromRecentAccuracy = changeRecent
            )
        }

        val reactionInspectionList = reactionSessions.map { session ->
            SessionInspectionDetail(
                sessionId = session.sessionId,
                sessionType = UserSession.TYPE_REACTION_COORDINATION,
                startedAt = session.startedAt,
                completedAt = session.completedAt,
                durationSeconds = session.durationSeconds,
                isCompleted = session.isCompleted,
                totalTrials = session.totalTrials,
                completedTrials = session.completedTrials,
                overallAccuracy = session.hitAccuracy,
                overallResponseTimeMs = session.meanReactionTimeMs,
                reactionMeanMs = session.meanReactionTimeMs,
                reactionVariabilityMs = session.reactionTimeVariabilityMs,
                reactionFastestMs = session.fastestReactionTimeMs,
                reactionSlowestMs = session.slowestReactionTimeMs,
                hitAccuracy = session.hitAccuracy,
                offTargetTouchesCount = session.totalOffTargetTouches,
                adaptiveDifficultySeconds = session.adaptiveDifficultySeconds,
                trialsJson = session.rawTrialsJson
            )
        }

        val combinedInspectionList = (memoryInspectionList + reactionInspectionList)
            .sortedByDescending { it.startedAt }

        val personalCognitiveState = PersonalCognitiveState(
            updatedAt = System.currentTimeMillis(),
            totalCompletedSessions = totalCompletedCognitiveSessions,
            baselineEstablished = baselineRecord.established,
            lastCompletedSessionAt = listOfNotNull(currentMemorySession?.completedAt, currentReactionSession?.completedAt).maxOrNull(),
            memory = memoryState,
            attention = attentionState,
            reactionTime = reactionTimeState,
            coordination = coordinationState,
            overall = overallState,
            activitySummary = activitySummary,
            inspectionSessions = combinedInspectionList
        )

        return Pair(personalCognitiveState, baselineRecord)
    }

    /**
     * Builds a single CognitiveDimensionMetric comparing Baseline -> Recent -> Current
     * with rigorous trend logic requiring repeated evidence and confidence estimation.
     */
    private fun buildDimensionMetric(
        baseline: Double?,
        recent: Double?,
        current: Double?,
        history: List<Double>,
        isLowerBetter: Boolean,
        variability: Double? = null
    ): CognitiveDimensionMetric {
        val changeFromBaseline = if (baseline != null && current != null) {
            current - baseline
        } else null

        val changeFromRecent = if (recent != null && current != null) {
            current - recent
        } else null

        val sessionCount = history.size

        // Trend logic: Do NOT declare improvement or decline from one session.
        // Require repeated evidence (minimum 3 sessions).
        val trend: String
        val confidence: Double
        val dataQuality: String

        if (sessionCount < 3) {
            trend = CognitiveDimensionMetric.TREND_INSUFFICIENT_DATA
            confidence = (sessionCount * 0.25).coerceIn(0.0, 0.5)
            dataQuality = CognitiveDimensionMetric.QUALITY_INSUFFICIENT_DATA
        } else {
            val calcVariability = variability ?: calculateStandardDeviation(history.takeLast(5))
            val mean = recent ?: history.takeLast(5).average()
            val relativeVariability = if (mean != 0.0) calcVariability / mean else 0.0

            if (relativeVariability > 0.35) {
                trend = CognitiveDimensionMetric.TREND_VARIABLE
            } else if (baseline != null && recent != null) {
                val delta = recent - baseline
                val threshold = if (isLowerBetter) -200.0 else 0.08 // For ms vs percentage

                if (isLowerBetter) {
                    when {
                        delta < -250.0 -> trend = CognitiveDimensionMetric.TREND_IMPROVING
                        delta > 250.0 -> trend = CognitiveDimensionMetric.TREND_DECLINING
                        else -> trend = CognitiveDimensionMetric.TREND_STABLE
                    }
                } else {
                    when {
                        delta >= 0.08 -> trend = CognitiveDimensionMetric.TREND_IMPROVING
                        delta <= -0.08 -> trend = CognitiveDimensionMetric.TREND_DECLINING
                        else -> trend = CognitiveDimensionMetric.TREND_STABLE
                    }
                }
            } else {
                trend = CognitiveDimensionMetric.TREND_STABLE
            }

            // Confidence calculation
            val countFactor = (sessionCount.toDouble() / 6.0).coerceAtMost(1.0)
            val consistencyFactor = (1.0 - relativeVariability.coerceIn(0.0, 0.5) * 2).coerceIn(0.5, 1.0)
            confidence = (0.5 + (countFactor * 0.3) + (consistencyFactor * 0.2)).coerceIn(0.0, 0.95)

            dataQuality = when {
                confidence >= 0.80 -> CognitiveDimensionMetric.QUALITY_HIGH
                confidence >= 0.55 -> CognitiveDimensionMetric.QUALITY_MODERATE
                else -> CognitiveDimensionMetric.QUALITY_LOW
            }
        }

        return CognitiveDimensionMetric(
            baseline = baseline,
            recent = recent,
            current = current,
            changeFromBaseline = changeFromBaseline,
            changeFromRecent = changeFromRecent,
            variability = variability,
            trend = trend,
            confidence = confidence,
            dataQuality = dataQuality
        )
    }

    private fun calculateStandardDeviation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        val sumSquaredDiffs = values.sumOf { (it - mean).pow(2) }
        return sqrt(sumSquaredDiffs / (values.size - 1))
    }

    private fun List<Double>.averageOrNull(): Double? {
        return if (isNotEmpty()) average() else null
    }

    private data class BaselineValues(
        val established: Boolean,
        val createdAt: Long = System.currentTimeMillis(),
        val shapeAccuracy: Double? = null,
        val colorAccuracy: Double? = null,
        val overallAccuracy: Double? = null,
        val noRecallRate: Double? = null,
        val reactionTimeMs: Double? = null,
        val attentionConsistency: Double? = null,
        val reactionMeanRtMs: Double? = null,
        val coordinationHitRate: Double? = null,
        val coordinationOffTargetRate: Double? = null,
        val adaptiveDifficultySeconds: Double? = null
    ) {
        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("established", established)
                put("created_at", createdAt)
                put("shape_accuracy", shapeAccuracy ?: JSONObject.NULL)
                put("color_accuracy", colorAccuracy ?: JSONObject.NULL)
                put("overall_accuracy", overallAccuracy ?: JSONObject.NULL)
                put("no_recall_rate", noRecallRate ?: JSONObject.NULL)
                put("reaction_time_ms", reactionTimeMs ?: JSONObject.NULL)
                put("attention_consistency", attentionConsistency ?: JSONObject.NULL)
                put("reaction_mean_rt_ms", reactionMeanRtMs ?: JSONObject.NULL)
                put("coordination_hit_rate", coordinationHitRate ?: JSONObject.NULL)
                put("coordination_off_target_rate", coordinationOffTargetRate ?: JSONObject.NULL)
                put("adaptive_difficulty_seconds", adaptiveDifficultySeconds ?: JSONObject.NULL)
            }
        }
    }

    private fun parseBaselineJson(jsonStr: String): BaselineValues {
        return try {
            val json = JSONObject(jsonStr)
            BaselineValues(
                established = json.optBoolean("established", false),
                createdAt = json.optLong("created_at", System.currentTimeMillis()),
                shapeAccuracy = json.optDouble("shape_accuracy").takeIf { !it.isNaN() },
                colorAccuracy = json.optDouble("color_accuracy").takeIf { !it.isNaN() },
                overallAccuracy = json.optDouble("overall_accuracy").takeIf { !it.isNaN() },
                noRecallRate = json.optDouble("no_recall_rate").takeIf { !it.isNaN() },
                reactionTimeMs = json.optDouble("reaction_time_ms").takeIf { !it.isNaN() },
                attentionConsistency = json.optDouble("attention_consistency").takeIf { !it.isNaN() },
                reactionMeanRtMs = json.optDouble("reaction_mean_rt_ms").takeIf { !it.isNaN() },
                coordinationHitRate = json.optDouble("coordination_hit_rate").takeIf { !it.isNaN() },
                coordinationOffTargetRate = json.optDouble("coordination_off_target_rate").takeIf { !it.isNaN() },
                adaptiveDifficultySeconds = json.optDouble("adaptive_difficulty_seconds").takeIf { !it.isNaN() }
            )
        } catch (_: Exception) {
            BaselineValues(established = false)
        }
    }
}
