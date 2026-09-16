package com.example.ui.screens.experiment

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.experiment.memory.MemoryColor
import com.example.experiment.memory.MemoryExperimentMeasurements
import com.example.experiment.memory.MemoryExperimentSessionResult
import com.example.experiment.memory.MemoryRecognitionExperiment
import com.example.experiment.memory.MemoryShape
import com.example.experiment.memory.MemoryStimulus
import com.example.experiment.memory.MemoryTrialResult
import com.example.experiment.memory.SingleResponse
import kotlinx.coroutines.delay

private enum class ExperimentStep {
    TEACHING,
    STIMULUS_DISPLAY,
    RETENTION_DELAY,
    QUESTION_SHAPE,
    QUESTION_COLOR,
    TRIAL_INTERMISSION,
    RESULTS_SUMMARY
}

@Composable
fun MemoryExperimentScreen(
    onSaveSession: (MemoryExperimentSessionResult) -> Unit,
    onFinishAndReturnHome: () -> Unit
) {
    // Total 5 trials as specified
    val totalTrials = 5
    val stimuli = remember { MemoryRecognitionExperiment.createStimuliSequence(totalTrials) }

    var step by remember { mutableStateOf(ExperimentStep.TEACHING) }
    var currentTrialIndex by remember { mutableIntStateOf(0) }
    val completedTrials = remember { mutableListOf<MemoryTrialResult>() }

    // Timing tracking
    val sessionStartTime = remember { System.currentTimeMillis() }
    var questionStartTime by remember { mutableLongStateOf(0L) }
    var currentShapeResponse by remember { mutableStateOf<SingleResponse?>(null) }

    // 5-second display timer progress
    var stimulusProgress by remember { mutableFloatStateOf(1f) }

    val currentStimulus: MemoryStimulus? = if (currentTrialIndex in stimuli.indices) {
        stimuli[currentTrialIndex]
    } else null

    // Handling automated timing sequences
    LaunchedEffect(step, currentTrialIndex) {
        when (step) {
            ExperimentStep.STIMULUS_DISPLAY -> {
                stimulusProgress = 1f
                // Display clearly for approximately 5 seconds
                val displayDurationMs = 5000L
                val intervalMs = 100L
                val steps = (displayDurationMs / intervalMs).toInt()

                for (i in 1..steps) {
                    delay(intervalMs)
                    stimulusProgress = (steps - i) / steps.toFloat()
                }

                // Remove the shape
                step = ExperimentStep.RETENTION_DELAY
            }

            ExperimentStep.RETENTION_DELAY -> {
                // Short controlled delay (1.5 seconds)
                delay(1500L)
                questionStartTime = System.currentTimeMillis()
                step = ExperimentStep.QUESTION_SHAPE
            }

            ExperimentStep.TRIAL_INTERMISSION -> {
                delay(800L)
                if (currentTrialIndex < totalTrials) {
                    step = ExperimentStep.STIMULUS_DISPLAY
                } else {
                    // Experiment completed: calculate measurements and persist session
                    val measurements = MemoryRecognitionExperiment.calculateMeasurements(completedTrials)
                    val metadataJson = MemoryRecognitionExperiment.serializeSessionMetadata(measurements, completedTrials)
                    val result = MemoryExperimentSessionResult(
                        startedAt = sessionStartTime,
                        completedAt = System.currentTimeMillis(),
                        measurements = measurements,
                        trials = completedTrials.toList(),
                        metadataJson = metadataJson
                    )
                    onSaveSession(result)
                    step = ExperimentStep.RESULTS_SUMMARY
                }
            }

            else -> {
                // User-driven steps
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (step) {
            ExperimentStep.TEACHING -> {
                MemoryTeachingPhase(
                    onReadyToBegin = {
                        step = ExperimentStep.STIMULUS_DISPLAY
                    }
                )
            }

            ExperimentStep.STIMULUS_DISPLAY -> {
                currentStimulus?.let { stimulus ->
                    StimulusDisplayView(
                        trialNumber = currentTrialIndex + 1,
                        totalTrials = totalTrials,
                        stimulus = stimulus,
                        progress = stimulusProgress
                    )
                }
            }

            ExperimentStep.RETENTION_DELAY -> {
                RetentionDelayView(
                    trialNumber = currentTrialIndex + 1,
                    totalTrials = totalTrials
                )
            }

            ExperimentStep.QUESTION_SHAPE -> {
                currentStimulus?.let { stimulus ->
                    QuestionShapeView(
                        trialNumber = currentTrialIndex + 1,
                        totalTrials = totalTrials,
                        onAnswerSelected = { answer ->
                            val responseTimeMs = (System.currentTimeMillis() - questionStartTime).coerceAtLeast(10L)
                            val response = MemoryRecognitionExperiment.evaluateResponse(
                                selectedAnswer = answer,
                                correctAnswer = stimulus.shape.displayName,
                                responseTimeMs = responseTimeMs
                            )
                            currentShapeResponse = response
                            questionStartTime = System.currentTimeMillis()
                            step = ExperimentStep.QUESTION_COLOR
                        }
                    )
                }
            }

            ExperimentStep.QUESTION_COLOR -> {
                currentStimulus?.let { stimulus ->
                    QuestionColorView(
                        trialNumber = currentTrialIndex + 1,
                        totalTrials = totalTrials,
                        onAnswerSelected = { answer ->
                            val responseTimeMs = (System.currentTimeMillis() - questionStartTime).coerceAtLeast(10L)
                            val colorResponse = MemoryRecognitionExperiment.evaluateResponse(
                                selectedAnswer = answer,
                                correctAnswer = stimulus.color.displayName,
                                responseTimeMs = responseTimeMs
                            )
                            val shapeResp = currentShapeResponse ?: SingleResponse(
                                answer = "no_recall",
                                correct = false,
                                responseType = com.example.experiment.memory.ResponseType.NO_RECALL,
                                responseTimeMs = 0L
                            )

                            val trialResult = MemoryRecognitionExperiment.buildTrialResult(
                                trialNumber = currentTrialIndex + 1,
                                stimulus = stimulus,
                                shapeResponse = shapeResp,
                                colorResponse = colorResponse
                            )
                            completedTrials.add(trialResult)

                            currentTrialIndex += 1
                            step = ExperimentStep.TRIAL_INTERMISSION
                        }
                    )
                }
            }

            ExperimentStep.TRIAL_INTERMISSION -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Next exercise coming up…",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            ExperimentStep.RESULTS_SUMMARY -> {
                ExperimentResultsSummaryView(
                    totalTrials = totalTrials,
                    onReturnHome = onFinishAndReturnHome
                )
            }
        }
    }
}

@Composable
private fun StimulusDisplayView(
    trialNumber: Int,
    totalTrials: Int,
    stimulus: MemoryStimulus,
    progress: Float
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Exercise $trialNumber of $totalTrials",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Remember this shape and color",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
            }

            // Central Shape Display
            Card(
                modifier = Modifier
                    .size(280.dp)
                    .testTag("memory_stimulus_card"),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    MemoryShapeView(
                        shape = stimulus.shape,
                        color = stimulus.color,
                        size = 200.dp
                    )
                }
            }

            // Bottom Progress Indicator (5s countdown bar)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .testTag("stimulus_timer_bar"),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Study the shape carefully…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RetentionDelayView(
    trialNumber: Int,
    totalTrials: Int
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            )
            Text(
                text = "Keep it in mind…",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun QuestionShapeView(
    trialNumber: Int,
    totalTrials: Int,
    onAnswerSelected: (String) -> Unit
) {
    // Shuffle shape options every single time for each question
    val shapes = remember(trialNumber) {
        listOf(
            MemoryShape.CIRCLE,
            MemoryShape.TRIANGLE,
            MemoryShape.SQUARE,
            MemoryShape.RECTANGLE
        ).shuffled()
    }

    var selectedAnswer by remember(trialNumber) { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Exercise $trialNumber of $totalTrials",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "What shape did you see?",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Shuffled Shape options in high-contrast buttons with selection highlight
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    shapes.forEach { shape ->
                        val isSelected = selectedAnswer == shape.displayName
                        Button(
                            onClick = { selectedAnswer = shape.displayName },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .testTag("shape_answer_${shape.id}"),
                            shape = RoundedCornerShape(18.dp),
                            border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                            else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = shape.displayName,
                                    fontSize = 22.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dedicated "I don't remember" option with selection highlight
                val isNoRecallSelected = selectedAnswer == "I don't remember"
                OutlinedButton(
                    onClick = { selectedAnswer = "I don't remember" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("shape_answer_dont_remember"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isNoRecallSelected) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent
                    ),
                    border = BorderStroke(
                        if (isNoRecallSelected) 3.dp else 1.5.dp,
                        if (isNoRecallSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isNoRecallSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = "I don't remember",
                            fontSize = 19.sp,
                            color = if (isNoRecallSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isNoRecallSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Anchored Confirmation Button at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        selectedAnswer?.let { onAnswerSelected(it) }
                    },
                    enabled = selectedAnswer != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("shape_confirm_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selectedAnswer != null) 4.dp else 0.dp)
                ) {
                    Text(
                        text = if (selectedAnswer != null) "Confirm: $selectedAnswer" else "Select an Option Above",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedAnswer != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionColorView(
    trialNumber: Int,
    totalTrials: Int,
    onAnswerSelected: (String) -> Unit
) {
    // Shuffle color options every single time for each question
    val colors = remember(trialNumber) {
        listOf(
            MemoryColor.RED,
            MemoryColor.BLUE,
            MemoryColor.GREEN,
            MemoryColor.YELLOW
        ).shuffled()
    }

    var selectedAnswer by remember(trialNumber) { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Exercise $trialNumber of $totalTrials",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "What color was it?",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))

                // Shuffled Color options with selection highlight
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    colors.forEach { color ->
                        val isSelected = selectedAnswer == color.displayName
                        Button(
                            onClick = { selectedAnswer = color.displayName },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .testTag("color_answer_${color.id}"),
                            shape = RoundedCornerShape(18.dp),
                            border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                            else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = color.displayName,
                                    fontSize = 22.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dedicated "I don't remember" option with selection highlight
                val isNoRecallSelected = selectedAnswer == "I don't remember"
                OutlinedButton(
                    onClick = { selectedAnswer = "I don't remember" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("color_answer_dont_remember"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isNoRecallSelected) MaterialTheme.colorScheme.surfaceVariant else androidx.compose.ui.graphics.Color.Transparent
                    ),
                    border = BorderStroke(
                        if (isNoRecallSelected) 3.dp else 1.5.dp,
                        if (isNoRecallSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isNoRecallSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = "I don't remember",
                            fontSize = 19.sp,
                            color = if (isNoRecallSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isNoRecallSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Anchored Confirmation Button at bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        selectedAnswer?.let { onAnswerSelected(it) }
                    },
                    enabled = selectedAnswer != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("color_confirm_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selectedAnswer != null) 4.dp else 0.dp)
                ) {
                    Text(
                        text = if (selectedAnswer != null) "Confirm: $selectedAnswer" else "Select an Option Above",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedAnswer != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Calm, non-medical summary screen complying strictly with:
 * "Do not interpret these values medically.
 * For example, do NOT display: 'Your memory is 72%.'
 * Instead, display something simple such as: 'Practice complete.'
 * The measurements are primarily for the internal data layer at this stage."
 */
@Composable
private fun ExperimentResultsSummaryView(
    totalTrials: Int,
    onReturnHome: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("results_summary_card"),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Practice complete.",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Thank you for completing today's memory practice session.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Completed $totalTrials of $totalTrials exercises. Your practice record has been saved locally.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Return to Home
            Button(
                onClick = onReturnHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("results_return_home_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "Return to Home",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
