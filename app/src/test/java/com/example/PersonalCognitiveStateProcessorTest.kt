package com.example

import com.example.data.model.HydrationRecord
import com.example.data.model.PersonalBaseline
import com.example.data.model.UserSession
import com.example.processor.CognitiveDimensionMetric
import com.example.processor.PersonalCognitiveStateProcessor
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PersonalCognitiveStateProcessorTest {

    private fun createSampleMetadata(
        shapeAcc: Double = 0.8,
        colorAcc: Double = 0.7,
        overallAcc: Double = 0.75,
        avgRt: Double = 1400.0,
        noRecallCount: Int = 1,
        incorrectCount: Int = 1
    ): String {
        return JSONObject().apply {
            put("metrics", JSONObject().apply {
                put("completedTrials", 5)
                put("totalTrials", 5)
                put("shapeAccuracy", shapeAcc)
                put("colorAccuracy", colorAcc)
                put("overallAccuracy", overallAcc)
                put("averageReactionTimeMs", avgRt)
                put("shapeAverageReactionTimeMs", avgRt)
                put("colorAverageReactionTimeMs", avgRt)
                put("noRecallResponsesCount", noRecallCount)
                put("incorrectResponsesCount", incorrectCount)
            })
            put("trials", org.json.JSONArray())
        }.toString()
    }

    @Test
    fun `test initial state with no sessions establishes no baseline and marks insufficient data`() {
        val (state, baseline) = PersonalCognitiveStateProcessor.processCognitiveState(
            allSessions = emptyList(),
            existingBaseline = null,
            hydrationRecords = emptyList()
        )

        assertFalse(state.baselineEstablished)
        assertFalse(baseline.established)
        assertEquals(0, state.totalCompletedSessions)
        assertEquals(CognitiveDimensionMetric.TREND_INSUFFICIENT_DATA, state.overall.trend)
        assertEquals(CognitiveDimensionMetric.TREND_INSUFFICIENT_DATA, state.memory.overallMemoryAccuracy.trend)
        assertEquals(CognitiveDimensionMetric.QUALITY_INSUFFICIENT_DATA, state.overall.overallScore.dataQuality)
    }

    @Test
    fun `test baseline established on first complete session and remains stable`() {
        val session1 = UserSession(
            id = 1,
            sessionType = UserSession.TYPE_MEMORY_RECOGNITION,
            startedAt = 1700000000000L,
            completedAt = 1700000060000L,
            cyclesCompleted = 5,
            isCompleted = true,
            metadata = createSampleMetadata(overallAcc = 0.8, avgRt = 1500.0)
        )

        val (state1, baseline1) = PersonalCognitiveStateProcessor.processCognitiveState(
            allSessions = listOf(session1),
            existingBaseline = null,
            hydrationRecords = emptyList()
        )

        assertTrue(state1.baselineEstablished)
        assertTrue(baseline1.established)
        assertEquals(0.8, state1.memory.overallMemoryAccuracy.baseline ?: 0.0, 0.001)
        assertEquals(1500L, baseline1.baselineReactionTimeMs)

        // Session 2 comes with different performance
        val session2 = UserSession(
            id = 2,
            sessionType = UserSession.TYPE_MEMORY_RECOGNITION,
            startedAt = 1700000100000L,
            completedAt = 1700000160000L,
            cyclesCompleted = 5,
            isCompleted = true,
            metadata = createSampleMetadata(overallAcc = 0.6, avgRt = 1800.0)
        )

        val (state2, baseline2) = PersonalCognitiveStateProcessor.processCognitiveState(
            allSessions = listOf(session2, session1),
            existingBaseline = baseline1,
            hydrationRecords = emptyList()
        )

        // Baseline must NOT be overwritten! It remains 0.8 and 1500
        assertEquals(0.8, state2.memory.overallMemoryAccuracy.baseline ?: 0.0, 0.001)
        assertEquals(1500L, baseline2.baselineReactionTimeMs)

        // Current reflects session 2
        assertEquals(0.6, state2.memory.overallMemoryAccuracy.current ?: 0.0, 0.001)
        assertEquals(1800.0, state2.reactionTime.averageReactionTimeMs.current ?: 0.0, 0.001)
    }

    @Test
    fun `test trend requires at least 3 completed sessions`() {
        val s1 = UserSession(
            id = 1,
            sessionType = UserSession.TYPE_MEMORY_RECOGNITION,
            startedAt = 1700000000000L,
            completedAt = 1700000060000L,
            cyclesCompleted = 5,
            isCompleted = true,
            metadata = createSampleMetadata(overallAcc = 0.8)
        )
        val s2 = UserSession(
            id = 2,
            sessionType = UserSession.TYPE_MEMORY_RECOGNITION,
            startedAt = 1700000100000L,
            completedAt = 1700000160000L,
            cyclesCompleted = 5,
            isCompleted = true,
            metadata = createSampleMetadata(overallAcc = 0.9)
        )

        val (stateTwoSessions, _) = PersonalCognitiveStateProcessor.processCognitiveState(
            allSessions = listOf(s2, s1),
            existingBaseline = null,
            hydrationRecords = emptyList()
        )

        // With only 2 sessions, trend must be insufficient_data
        assertEquals(CognitiveDimensionMetric.TREND_INSUFFICIENT_DATA, stateTwoSessions.overall.trend)
        assertFalse(stateTwoSessions.overall.hasSufficientDataForTrend)

        // Add 3rd session
        val s3 = UserSession(
            id = 3,
            sessionType = UserSession.TYPE_MEMORY_RECOGNITION,
            startedAt = 1700000200000L,
            completedAt = 1700000260000L,
            cyclesCompleted = 5,
            isCompleted = true,
            metadata = createSampleMetadata(overallAcc = 0.85)
        )

        val (stateThreeSessions, _) = PersonalCognitiveStateProcessor.processCognitiveState(
            allSessions = listOf(s3, s2, s1),
            existingBaseline = null,
            hydrationRecords = emptyList()
        )

        assertTrue(stateThreeSessions.overall.hasSufficientDataForTrend)
        assertFalse(stateThreeSessions.overall.trend == CognitiveDimensionMetric.TREND_INSUFFICIENT_DATA)
    }

    @Test
    fun `test gemini context json contains non-medical disclaimer and valid structured payload`() {
        val s1 = UserSession(
            id = 1,
            sessionType = UserSession.TYPE_MEMORY_RECOGNITION,
            startedAt = 1700000000000L,
            completedAt = 1700000060000L,
            cyclesCompleted = 5,
            isCompleted = true,
            metadata = createSampleMetadata(overallAcc = 0.8, avgRt = 1350.0)
        )

        val (state, _) = PersonalCognitiveStateProcessor.processCognitiveState(
            allSessions = listOf(s1),
            existingBaseline = null,
            hydrationRecords = listOf(
                HydrationRecord(timestamp = 1700000000000L, dateString = "2026-09-15", status = HydrationRecord.STATUS_COMPLETED)
            )
        )

        val jsonString = state.toGeminiContextJson()
        assertNotNull(jsonString)

        val json = JSONObject(jsonString)
        val disclaimer = json.getString("disclaimer")
        assertTrue(disclaimer.contains("medical diagnosis", ignoreCase = true))
        assertTrue(json.has("cognitive_state"))
        val cogState = json.getJSONObject("cognitive_state")
        assertTrue(cogState.has("memory"))
        assertTrue(cogState.has("attention"))
        assertTrue(cogState.has("reaction_time"))
        assertTrue(cogState.has("overall"))
        assertTrue(cogState.has("activity_summary"))
    }
}
