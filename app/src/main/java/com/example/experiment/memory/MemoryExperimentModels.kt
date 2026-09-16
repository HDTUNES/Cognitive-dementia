package com.example.experiment.memory

import androidx.compose.ui.graphics.Color

enum class MemoryShape(val displayName: String, val id: String) {
    CIRCLE("Circle", "circle"),
    TRIANGLE("Triangle", "triangle"),
    SQUARE("Square", "square"),
    RECTANGLE("Rectangle", "rectangle");

    companion object {
        fun fromId(id: String): MemoryShape? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}

enum class MemoryColor(
    val displayName: String,
    val id: String,
    val lightComposeColor: Color,
    val darkComposeColor: Color
) {
    RED(
        displayName = "Red",
        id = "red",
        lightComposeColor = Color(0xFFD32F2F),
        darkComposeColor = Color(0xFFEF5350)
    ),
    BLUE(
        displayName = "Blue",
        id = "blue",
        lightComposeColor = Color(0xFF1976D2),
        darkComposeColor = Color(0xFF42A5F5)
    ),
    GREEN(
        displayName = "Green",
        id = "green",
        lightComposeColor = Color(0xFF2E7D32),
        darkComposeColor = Color(0xFF66BB6A)
    ),
    YELLOW(
        displayName = "Yellow",
        id = "yellow",
        lightComposeColor = Color(0xFFF57F17),
        darkComposeColor = Color(0xFFFFCA28)
    );

    fun getColor(isDark: Boolean): Color = if (isDark) darkComposeColor else lightComposeColor

    companion object {
        fun fromId(id: String): MemoryColor? = entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }
}

enum class ResponseType(val value: String) {
    CORRECT("correct"),
    INCORRECT("incorrect"),
    NO_RECALL("no_recall");

    companion object {
        fun fromValue(value: String): ResponseType = entries.firstOrNull { it.value == value } ?: INCORRECT
    }
}

data class MemoryStimulus(
    val shape: MemoryShape,
    val color: MemoryColor
)

data class SingleResponse(
    val answer: String,
    val correct: Boolean,
    val responseType: ResponseType,
    val responseTimeMs: Long
)

data class MemoryTrial(
    val trialNumber: Int,
    val stimulus: MemoryStimulus
)

data class MemoryTrialResult(
    val trialId: String,
    val trialNumber: Int,
    val type: String = "memory_recognition",
    val stimulus: MemoryStimulus,
    val shapeResponse: SingleResponse,
    val colorResponse: SingleResponse,
    val timestamp: String
)

data class MemoryExperimentMeasurements(
    val totalTrials: Int,
    val completedTrials: Int,
    val shapeAccuracy: Double,
    val colorAccuracy: Double,
    val shapeNoRecallRate: Double,
    val colorNoRecallRate: Double,
    val averageShapeResponseTimeMs: Double,
    val averageColorResponseTimeMs: Double
)

data class MemoryExperimentSessionResult(
    val sessionType: String = "memory_recognition",
    val startedAt: Long,
    val completedAt: Long,
    val measurements: MemoryExperimentMeasurements,
    val trials: List<MemoryTrialResult>,
    val metadataJson: String
)
