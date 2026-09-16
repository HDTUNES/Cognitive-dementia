package com.example

import com.example.experiment.reaction.AdaptiveDifficultyEngine
import com.example.experiment.reaction.HitType
import com.example.experiment.reaction.ReactionCoordinationExperiment
import com.example.experiment.reaction.ReactionTrialResult
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
class ReactionCoordinationExperimentTest {

    @Test
    fun `test adaptive difficulty decreases window on success and increases on failure`() {
        val initial = 2.2
        val onDirectHit = AdaptiveDifficultyEngine.calculateNextWindowSeconds(
            currentWindowSeconds = initial,
            isSuccess = true,
            reactionTimeMs = 400L
        )
        // Successful fast response decreases window (makes next stimulus slightly more challenging)
        assertTrue(onDirectHit < initial)

        val onTimeout = AdaptiveDifficultyEngine.calculateNextWindowSeconds(
            currentWindowSeconds = initial,
            isSuccess = false,
            reactionTimeMs = null
        )
        // Failure/timeout increases window (makes next stimulus easier and longer to tap)
        assertTrue(onTimeout > initial)

        // Clamping bounds
        val clampedMin = AdaptiveDifficultyEngine.calculateNextWindowSeconds(1.0, true, 200L)
        assertEquals(AdaptiveDifficultyEngine.MIN_WINDOW_SECONDS, clampedMin, 0.001)

        val clampedMax = AdaptiveDifficultyEngine.calculateNextWindowSeconds(3.5, false, null)
        assertEquals(AdaptiveDifficultyEngine.MAX_WINDOW_SECONDS, clampedMax, 0.001)
    }

    @Test
    fun `test stimulus delay is non-predictable between 1000ms and 2500ms`() {
        for (i in 0 until 20) {
            val delay = ReactionCoordinationExperiment.generateRandomDelayMs()
            assertTrue(delay in 1000L..2500L)
        }
    }

    @Test
    fun `test reaction metrics computation accurately tracks hits, off-target, and timeouts`() {
        val trials = listOf(
            ReactionTrialResult(
                trialNumber = 1,
                hitType = HitType.DIRECT_HIT,
                isSuccess = true,
                reactionTimeMs = 450L,
                stimulusDisplayDurationMs = 2000L,
                offTargetTouch = false,
                offTargetTouchesCount = 0,
                difficultyLevel = 2.0
            ),
            ReactionTrialResult(
                trialNumber = 2,
                hitType = HitType.BOUNDARY_HIT,
                isSuccess = true,
                reactionTimeMs = 550L,
                stimulusDisplayDurationMs = 2000L,
                offTargetTouch = true,
                offTargetTouchesCount = 1,
                difficultyLevel = 1.9
            ),
            ReactionTrialResult(
                trialNumber = 3,
                hitType = HitType.TIMEOUT,
                isSuccess = false,
                reactionTimeMs = null,
                stimulusDisplayDurationMs = 2000L,
                offTargetTouch = false,
                offTargetTouchesCount = 0,
                difficultyLevel = 1.8
            )
        )

        val measurements = ReactionCoordinationExperiment.calculateMeasurements(trials)

        assertEquals(3, measurements.totalTrials)
        assertEquals(2, measurements.successfulHits)
        assertEquals(1, measurements.timeouts)
        assertEquals(1, measurements.offTargetInteractions)
        // Hit rate = 2/3 = 66.67%
        assertEquals(2.0 / 3.0, measurements.hitAccuracyRate, 0.01)
        // Off target rate = 1/3 = 33.33%
        assertEquals(1.0 / 3.0, measurements.offTargetRate, 0.01)
        // Timeout rate = 1/3 = 33.33%
        assertEquals(1.0 / 3.0, measurements.timeoutRate, 0.01)
        // Mean RT of hits (450 + 550) / 2 = 500ms
        assertEquals(500.0, measurements.meanReactionTimeMs ?: 0.0, 0.01)
        assertEquals(450L, measurements.fastestReactionTimeMs)
        assertEquals(550L, measurements.slowestReactionTimeMs)
    }

    @Test
    fun `test serialization and deserialization of reaction session metadata`() {
        val trials = listOf(
            ReactionTrialResult(
                trialNumber = 1,
                hitType = HitType.DIRECT_HIT,
                isSuccess = true,
                reactionTimeMs = 400L,
                stimulusDisplayDurationMs = 2000L,
                offTargetTouch = false,
                offTargetTouchesCount = 0,
                difficultyLevel = 2.0
            )
        )
        val measurements = ReactionCoordinationExperiment.calculateMeasurements(trials)
        val json = ReactionCoordinationExperiment.serializeSessionMetadata(measurements, trials)

        assertNotNull(json)
        assertTrue(json.contains("reaction_coordination"))
        assertTrue(json.contains("direct_hit"))
    }
}
