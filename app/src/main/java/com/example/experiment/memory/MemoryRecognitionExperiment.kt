package com.example.experiment.memory

import com.example.experiment.CognitiveExperiment
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.random.Random

object MemoryRecognitionExperiment : CognitiveExperiment<MemoryTrialResult, MemoryExperimentMeasurements> {

    override val experimentType: String = "memory_recognition"
    override val displayName: String = "Memory Recognition"
    override val description: String = "Tests recognition of geometric shapes and colors after a brief retention interval."

    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Generates a sequence of non-predictable, varied trials where no consecutive trial
     * shares both the same shape and color, ensuring balanced variety across available options.
     */
    override fun generateTrials(count: Int): List<MemoryTrialResult> {
        // This is the trial generator placeholder for the interface;
        // See createStimuliSequence below for generating stimuli.
        return emptyList()
    }

    fun createStimuliSequence(count: Int = 5): List<MemoryStimulus> {
        val shapes = MemoryShape.entries.toMutableList()
        val colors = MemoryColor.entries.toMutableList()
        val stimuli = mutableListOf<MemoryStimulus>()

        var lastShape: MemoryShape? = null
        var lastColor: MemoryColor? = null

        for (i in 0 until count) {
            val availableShapes = shapes.filter { it != lastShape }.ifEmpty { shapes }
            val chosenShape = availableShapes.random()

            val availableColors = colors.filter { it != lastColor }.ifEmpty { colors }
            val chosenColor = availableColors.random()

            stimuli.add(MemoryStimulus(shape = chosenShape, color = chosenColor))
            lastShape = chosenShape
            lastColor = chosenColor
        }
        return stimuli
    }

    fun evaluateResponse(
        selectedAnswer: String,
        correctAnswer: String,
        responseTimeMs: Long
    ): SingleResponse {
        val isNoRecall = selectedAnswer.equals("I don't remember", ignoreCase = true) ||
                         selectedAnswer.equals("no_recall", ignoreCase = true)

        return when {
            isNoRecall -> SingleResponse(
                answer = "I don't remember",
                correct = false,
                responseType = ResponseType.NO_RECALL,
                responseTimeMs = responseTimeMs
            )
            selectedAnswer.equals(correctAnswer, ignoreCase = true) -> SingleResponse(
                answer = selectedAnswer,
                correct = true,
                responseType = ResponseType.CORRECT,
                responseTimeMs = responseTimeMs
            )
            else -> SingleResponse(
                answer = selectedAnswer,
                correct = false,
                responseType = ResponseType.INCORRECT,
                responseTimeMs = responseTimeMs
            )
        }
    }

    fun buildTrialResult(
        trialNumber: Int,
        stimulus: MemoryStimulus,
        shapeResponse: SingleResponse,
        colorResponse: SingleResponse,
        timestampMs: Long = System.currentTimeMillis()
    ): MemoryTrialResult {
        val trialId = "trial_${trialNumber}_${timestampMs}"
        val isoTimestamp = isoDateFormat.format(Date(timestampMs))
        return MemoryTrialResult(
            trialId = trialId,
            trialNumber = trialNumber,
            type = experimentType,
            stimulus = stimulus,
            shapeResponse = shapeResponse,
            colorResponse = colorResponse,
            timestamp = isoTimestamp
        )
    }

    override fun calculateMeasurements(trials: List<MemoryTrialResult>): MemoryExperimentMeasurements {
        val total = trials.size
        if (total == 0) {
            return MemoryExperimentMeasurements(
                totalTrials = 0,
                completedTrials = 0,
                shapeAccuracy = 0.0,
                colorAccuracy = 0.0,
                shapeNoRecallRate = 0.0,
                colorNoRecallRate = 0.0,
                averageShapeResponseTimeMs = 0.0,
                averageColorResponseTimeMs = 0.0
            )
        }

        val shapeCorrectCount = trials.count { it.shapeResponse.responseType == ResponseType.CORRECT }
        val colorCorrectCount = trials.count { it.colorResponse.responseType == ResponseType.CORRECT }

        val shapeNoRecallCount = trials.count { it.shapeResponse.responseType == ResponseType.NO_RECALL }
        val colorNoRecallCount = trials.count { it.colorResponse.responseType == ResponseType.NO_RECALL }

        val avgShapeResponseTime = trials.map { it.shapeResponse.responseTimeMs }.average()
        val avgColorResponseTime = trials.map { it.colorResponse.responseTimeMs }.average()

        return MemoryExperimentMeasurements(
            totalTrials = total,
            completedTrials = total,
            shapeAccuracy = shapeCorrectCount.toDouble() / total,
            colorAccuracy = colorCorrectCount.toDouble() / total,
            shapeNoRecallRate = shapeNoRecallCount.toDouble() / total,
            colorNoRecallRate = colorNoRecallCount.toDouble() / total,
            averageShapeResponseTimeMs = avgShapeResponseTime,
            averageColorResponseTimeMs = avgColorResponseTime
        )
    }

    override fun serializeSessionMetadata(
        measurements: MemoryExperimentMeasurements,
        trials: List<MemoryTrialResult>
    ): String {
        val root = JSONObject()
        root.put("type", experimentType)
        root.put("total_trials", measurements.totalTrials)
        root.put("completed_trials", measurements.completedTrials)

        val metricsJson = JSONObject()
        metricsJson.put("shape_accuracy", measurements.shapeAccuracy)
        metricsJson.put("color_accuracy", measurements.colorAccuracy)
        metricsJson.put("shape_no_recall_rate", measurements.shapeNoRecallRate)
        metricsJson.put("color_no_recall_rate", measurements.colorNoRecallRate)
        metricsJson.put("average_shape_response_time_ms", measurements.averageShapeResponseTimeMs)
        metricsJson.put("average_color_response_time_ms", measurements.averageColorResponseTimeMs)
        root.put("metrics", metricsJson)

        val trialsArray = JSONArray()
        trials.forEach { trial ->
            val trialObj = JSONObject()
            trialObj.put("trial_id", trial.trialId)
            trialObj.put("type", trial.type)

            val stimulusObj = JSONObject()
            stimulusObj.put("shape", trial.stimulus.shape.id)
            stimulusObj.put("color", trial.stimulus.color.id)
            trialObj.put("stimulus", stimulusObj)

            val shapeRespObj = JSONObject()
            shapeRespObj.put("answer", trial.shapeResponse.answer)
            shapeRespObj.put("correct", trial.shapeResponse.correct)
            shapeRespObj.put("response_type", trial.shapeResponse.responseType.value)
            shapeRespObj.put("response_time_ms", trial.shapeResponse.responseTimeMs)
            trialObj.put("shape_response", shapeRespObj)

            val colorRespObj = JSONObject()
            colorRespObj.put("answer", trial.colorResponse.answer)
            colorRespObj.put("correct", trial.colorResponse.correct)
            colorRespObj.put("response_type", trial.colorResponse.responseType.value)
            colorRespObj.put("response_time_ms", trial.colorResponse.responseTimeMs)
            trialObj.put("color_response", colorRespObj)

            trialObj.put("timestamp", trial.timestamp)
            trialsArray.put(trialObj)
        }
        root.put("trials", trialsArray)

        return root.toString()
    }
}
