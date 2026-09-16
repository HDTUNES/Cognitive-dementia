package com.example.experiment

/**
 * Base abstraction for all cognitive experiments (e.g. MemoryRecognition, future Reaction, Attention).
 * Ensures unified pipeline:
 * Experiment -> Trial -> Measurement -> CognitiveSession
 */
interface CognitiveExperiment<TTrial : Any, TMeasurement : Any> {
    val experimentType: String
    val displayName: String
    val description: String

    fun generateTrials(count: Int = 5): List<TTrial>
    fun calculateMeasurements(trials: List<TTrial>): TMeasurement
    fun serializeSessionMetadata(measurements: TMeasurement, trials: List<TTrial>): String
}
