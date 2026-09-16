package com.example

import com.example.experiment.memory.MemoryColor
import com.example.experiment.memory.MemoryRecognitionExperiment
import com.example.experiment.memory.MemoryShape
import com.example.experiment.memory.MemoryStimulus
import com.example.experiment.memory.ResponseType
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MemoryRecognitionExperimentTest {

    @Test
    fun `test response evaluation distinguishes correct, incorrect, and no_recall`() {
        // Correct answer
        val correctResponse = MemoryRecognitionExperiment.evaluateResponse(
            selectedAnswer = "Circle",
            correctAnswer = "Circle",
            responseTimeMs = 1200L
        )
        assertTrue(correctResponse.correct)
        assertEquals(ResponseType.CORRECT, correctResponse.responseType)
        assertEquals("Circle", correctResponse.answer)
        assertEquals(1200L, correctResponse.responseTimeMs)

        // Incorrect answer
        val incorrectResponse = MemoryRecognitionExperiment.evaluateResponse(
            selectedAnswer = "Square",
            correctAnswer = "Circle",
            responseTimeMs = 1500L
        )
        assertFalse(incorrectResponse.correct)
        assertEquals(ResponseType.INCORRECT, incorrectResponse.responseType)
        assertEquals("Square", incorrectResponse.answer)
        assertEquals(1500L, incorrectResponse.responseTimeMs)

        // I don't remember (no_recall) - must be distinctly recorded and never forced to guess
        val noRecallResponse = MemoryRecognitionExperiment.evaluateResponse(
            selectedAnswer = "I don't remember",
            correctAnswer = "Circle",
            responseTimeMs = 800L
        )
        assertFalse(noRecallResponse.correct)
        assertEquals(ResponseType.NO_RECALL, noRecallResponse.responseType)
        assertEquals("I don't remember", noRecallResponse.answer)
        assertEquals(800L, noRecallResponse.responseTimeMs)
    }

    @Test
    fun `test stimuli sequence generation produces 5 varied trials without consecutive duplicates`() {
        val count = 5
        val sequence = MemoryRecognitionExperiment.createStimuliSequence(count)
        assertEquals(5, sequence.size)

        for (i in 0 until sequence.size - 1) {
            val current = sequence[i]
            val next = sequence[i + 1]
            // Consecutive trials should not be identical in both shape and color
            val isExactRepeat = current.shape == next.shape && current.color == next.color
            assertFalse("Consecutive trials must avoid exact identical repeats", isExactRepeat)
        }
    }

    @Test
    fun `test measurement calculations`() {
        val stimulus = MemoryStimulus(shape = MemoryShape.CIRCLE, color = MemoryColor.RED)

        val trial1 = MemoryRecognitionExperiment.buildTrialResult(
            trialNumber = 1,
            stimulus = stimulus,
            shapeResponse = MemoryRecognitionExperiment.evaluateResponse("Circle", "Circle", 1000L),
            colorResponse = MemoryRecognitionExperiment.evaluateResponse("Red", "Red", 1000L)
        )

        val trial2 = MemoryRecognitionExperiment.buildTrialResult(
            trialNumber = 2,
            stimulus = stimulus,
            shapeResponse = MemoryRecognitionExperiment.evaluateResponse("Square", "Circle", 2000L),
            colorResponse = MemoryRecognitionExperiment.evaluateResponse("I don't remember", "Red", 1500L)
        )

        val trial3 = MemoryRecognitionExperiment.buildTrialResult(
            trialNumber = 3,
            stimulus = stimulus,
            shapeResponse = MemoryRecognitionExperiment.evaluateResponse("I don't remember", "Circle", 3000L),
            colorResponse = MemoryRecognitionExperiment.evaluateResponse("Blue", "Red", 2500L)
        )

        val trials = listOf(trial1, trial2, trial3)
        val measurements = MemoryRecognitionExperiment.calculateMeasurements(trials)

        assertEquals(3, measurements.totalTrials)
        assertEquals(3, measurements.completedTrials)

        // Shape: 1 correct, 1 incorrect, 1 no_recall -> accuracy 1/3 (0.333), no_recall 1/3 (0.333)
        assertEquals(1.0 / 3.0, measurements.shapeAccuracy, 0.001)
        assertEquals(1.0 / 3.0, measurements.shapeNoRecallRate, 0.001)

        // Color: 1 correct, 1 no_recall, 1 incorrect -> accuracy 1/3, no_recall 1/3
        assertEquals(1.0 / 3.0, measurements.colorAccuracy, 0.001)
        assertEquals(1.0 / 3.0, measurements.colorNoRecallRate, 0.001)

        // Average response times:
        // Shape: (1000 + 2000 + 3000) / 3 = 2000.0
        assertEquals(2000.0, measurements.averageShapeResponseTimeMs, 0.001)
        // Color: (1000 + 1500 + 2500) / 3 = 1666.67
        assertEquals(1666.666, measurements.averageColorResponseTimeMs, 0.01)
    }

    @Test
    fun `test session metadata JSON serialization schema`() {
        val stimulus = MemoryStimulus(shape = MemoryShape.TRIANGLE, color = MemoryColor.BLUE)

        val trial = MemoryRecognitionExperiment.buildTrialResult(
            trialNumber = 1,
            stimulus = stimulus,
            shapeResponse = MemoryRecognitionExperiment.evaluateResponse("Triangle", "Triangle", 1420L),
            colorResponse = MemoryRecognitionExperiment.evaluateResponse("Blue", "Blue", 1180L)
        )

        val measurements = MemoryRecognitionExperiment.calculateMeasurements(listOf(trial))
        val jsonString = MemoryRecognitionExperiment.serializeSessionMetadata(measurements, listOf(trial))

        val json = JSONObject(jsonString)
        assertEquals("memory_recognition", json.getString("type"))
        assertEquals(1, json.getInt("total_trials"))
        assertEquals(1, json.getInt("completed_trials"))

        val metrics = json.getJSONObject("metrics")
        assertEquals(1.0, metrics.getDouble("shape_accuracy"), 0.001)
        assertEquals(1.0, metrics.getDouble("color_accuracy"), 0.001)
        assertEquals(0.0, metrics.getDouble("shape_no_recall_rate"), 0.001)
        assertEquals(0.0, metrics.getDouble("color_no_recall_rate"), 0.001)
        assertEquals(1420.0, metrics.getDouble("average_shape_response_time_ms"), 0.001)
        assertEquals(1180.0, metrics.getDouble("average_color_response_time_ms"), 0.001)

        val trialsArray = json.getJSONArray("trials")
        assertEquals(1, trialsArray.length())

        val trialObj = trialsArray.getJSONObject(0)
        assertEquals("memory_recognition", trialObj.getString("type"))

        val stimObj = trialObj.getJSONObject("stimulus")
        assertEquals("triangle", stimObj.getString("shape"))
        assertEquals("blue", stimObj.getString("color"))

        val shapeResp = trialObj.getJSONObject("shape_response")
        assertEquals("Triangle", shapeResp.getString("answer"))
        assertTrue(shapeResp.getBoolean("correct"))
        assertEquals("correct", shapeResp.getString("response_type"))
        assertEquals(1420L, shapeResp.getLong("response_time_ms"))

        val colorResp = trialObj.getJSONObject("color_response")
        assertEquals("Blue", colorResp.getString("answer"))
        assertTrue(colorResp.getBoolean("correct"))
        assertEquals("correct", colorResp.getString("response_type"))
        assertEquals(1180L, colorResp.getLong("response_time_ms"))

        assertTrue(trialObj.has("timestamp"))
    }
}
