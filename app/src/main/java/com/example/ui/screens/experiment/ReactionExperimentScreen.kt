package com.example.ui.screens.experiment

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.experiment.reaction.ReactionCoordinationExperiment
import com.example.experiment.reaction.ReactionExperimentSessionResult
import com.example.experiment.reaction.ReactionTrialMeasurement
import com.example.util.VibrationHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

private enum class ReactionExperimentStep {
    INSTRUCTIONS,
    WAITING_FOR_TARGET,
    TARGET_VISIBLE,
    TRIAL_FEEDBACK,
    RESULTS_SUMMARY
}

@Composable
fun ReactionExperimentScreen(
    startingDifficultySeconds: Double = 2.0,
    totalTrials: Int = 6,
    onSaveSession: (ReactionExperimentSessionResult) -> Unit,
    onFinishAndReturnHome: () -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(ReactionExperimentStep.INSTRUCTIONS) }
    var currentTrialIndex by remember { mutableIntStateOf(0) }
    var sessionStartTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Adaptive difficulty for this session (seconds visible)
    var currentDifficultySeconds by remember { mutableFloatStateOf(startingDifficultySeconds.toFloat()) }

    // Trial state
    var targetAppearanceTime by remember { mutableLongStateOf(0L) }
    var vibrationTime by remember { mutableLongStateOf(0L) }
    var targetNormalizedX by remember { mutableFloatStateOf(0.5f) }
    var targetNormalizedY by remember { mutableFloatStateOf(0.5f) }
    var offTargetTouchesInTrial by remember { mutableIntStateOf(0) }
    var lastTrialHitType by remember { mutableStateOf("") }
    var lastTrialReactionTimeMs by remember { mutableStateOf<Long?>(null) }
    var lastTrialOffTargetDetected by remember { mutableStateOf(false) }

    val completedTrials = remember { mutableStateListOf<ReactionTrialMeasurement>() }

    val targetRadiusDp = 38f
    val boundaryRadiusDp = 68f

    // Automated timing for trial transitions
    LaunchedEffect(step, currentTrialIndex) {
        when (step) {
            ReactionExperimentStep.WAITING_FOR_TARGET -> {
                // Randomized anticipation interval (1.2s to 2.8s) so timing is unpredictable
                val randomDelayMs = Random.nextLong(1200L, 2800L)
                delay(randomDelayMs)

                // Pick safe normalized coordinates (0.2f to 0.8f)
                targetNormalizedX = Random.nextFloat() * 0.6f + 0.2f
                targetNormalizedY = Random.nextFloat() * 0.5f + 0.25f

                // Record target appearance timestamp first
                targetAppearanceTime = System.currentTimeMillis()

                // Trigger brief sensory cue vibration and record vibration timestamp separately
                vibrationTime = System.currentTimeMillis()
                VibrationHelper.triggerCueVibration(context, 50L)

                offTargetTouchesInTrial = 0
                step = ReactionExperimentStep.TARGET_VISIBLE
            }

            ReactionExperimentStep.TARGET_VISIBLE -> {
                // Controlled visibility duration countdown
                val durationMs = (currentDifficultySeconds * 1000L).toLong()
                delay(durationMs)

                // If still visible, trial timed out (no response before timeout)
                if (step == ReactionExperimentStep.TARGET_VISIBLE) {
                    val now = System.currentTimeMillis()
                    val trialMeasurement = ReactionTrialMeasurement(
                        trialId = "trial_${currentTrialIndex + 1}_$now",
                        trialNumber = currentTrialIndex + 1,
                        timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date(now)),
                        targetAppearanceTimestamp = targetAppearanceTime,
                        targetLocationX = targetNormalizedX,
                        targetLocationY = targetNormalizedY,
                        targetRadiusDp = targetRadiusDp,
                        boundaryRadiusDp = boundaryRadiusDp,
                        visibilityDurationMs = durationMs,
                        difficultyLevel = currentDifficultySeconds.toDouble(),
                        vibrationTimestamp = vibrationTime,
                        firstTouchTimestamp = null,
                        reactionTimeMs = null,
                        hitType = ReactionTrialMeasurement.HIT_TIMEOUT,
                        offTargetTouch = offTargetTouchesInTrial > 0,
                        offTargetTouchesCount = offTargetTouchesInTrial,
                        timeout = true
                    )
                    completedTrials.add(trialMeasurement)
                    lastTrialHitType = ReactionTrialMeasurement.HIT_TIMEOUT
                    lastTrialReactionTimeMs = null
                    lastTrialOffTargetDetected = offTargetTouchesInTrial > 0
                    step = ReactionExperimentStep.TRIAL_FEEDBACK
                }
            }

            ReactionExperimentStep.TRIAL_FEEDBACK -> {
                delay(900L)
                if (currentTrialIndex + 1 < totalTrials) {
                    currentTrialIndex += 1
                    step = ReactionExperimentStep.WAITING_FOR_TARGET
                } else {
                    // Experiment completed: calculate measurements and persist session
                    val measurements = ReactionCoordinationExperiment.calculateMeasurements(
                        trials = completedTrials.toList(),
                        totalTrials = totalTrials,
                        adaptiveDifficultySeconds = currentDifficultySeconds.toDouble()
                    )
                    val metadataJson = ReactionCoordinationExperiment.serializeSessionMetadata(
                        measurements = measurements,
                        trials = completedTrials.toList()
                    )
                    val result = ReactionExperimentSessionResult(
                        startedAt = sessionStartTime,
                        completedAt = System.currentTimeMillis(),
                        measurements = measurements,
                        trials = completedTrials.toList(),
                        metadataJson = metadataJson
                    )
                    onSaveSession(result)
                    step = ReactionExperimentStep.RESULTS_SUMMARY
                }
            }

            else -> { /* User-driven steps */ }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (step) {
            ReactionExperimentStep.INSTRUCTIONS -> {
                ReactionInstructionsView(
                    difficultySeconds = currentDifficultySeconds.toDouble(),
                    totalTrials = totalTrials,
                    onStartExperiment = {
                        sessionStartTime = System.currentTimeMillis()
                        step = ReactionExperimentStep.WAITING_FOR_TARGET
                    },
                    onBack = onFinishAndReturnHome
                )
            }

            ReactionExperimentStep.WAITING_FOR_TARGET -> {
                WaitingForTargetView(
                    trialNumber = currentTrialIndex + 1,
                    totalTrials = totalTrials
                )
            }

            ReactionExperimentStep.TARGET_VISIBLE -> {
                ActiveTargetView(
                    trialNumber = currentTrialIndex + 1,
                    totalTrials = totalTrials,
                    targetNormalizedX = targetNormalizedX,
                    targetNormalizedY = targetNormalizedY,
                    targetRadiusDp = targetRadiusDp,
                    boundaryRadiusDp = boundaryRadiusDp,
                    visibilityDurationMs = (currentDifficultySeconds * 1000f).toLong(),
                    onDirectHit = { touchOffset, distance ->
                        val touchTime = System.currentTimeMillis()
                        val rt = (touchTime - targetAppearanceTime).coerceAtLeast(10L)
                        val trialMeasurement = ReactionTrialMeasurement(
                            trialId = "trial_${currentTrialIndex + 1}_$touchTime",
                            trialNumber = currentTrialIndex + 1,
                            timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date(touchTime)),
                            targetAppearanceTimestamp = targetAppearanceTime,
                            targetLocationX = targetNormalizedX,
                            targetLocationY = targetNormalizedY,
                            targetRadiusDp = targetRadiusDp,
                            boundaryRadiusDp = boundaryRadiusDp,
                            visibilityDurationMs = (currentDifficultySeconds * 1000f).toLong(),
                            difficultyLevel = currentDifficultySeconds.toDouble(),
                            vibrationTimestamp = vibrationTime,
                            firstTouchTimestamp = touchTime,
                            reactionTimeMs = rt,
                            hitType = ReactionTrialMeasurement.HIT_DIRECT,
                            offTargetTouch = offTargetTouchesInTrial > 0,
                            offTargetTouchesCount = offTargetTouchesInTrial,
                            touchOffsetX = touchOffset.x,
                            touchOffsetY = touchOffset.y,
                            touchDistance = distance,
                            timeout = false
                        )
                        completedTrials.add(trialMeasurement)
                        lastTrialHitType = ReactionTrialMeasurement.HIT_DIRECT
                        lastTrialReactionTimeMs = rt
                        lastTrialOffTargetDetected = offTargetTouchesInTrial > 0
                        step = ReactionExperimentStep.TRIAL_FEEDBACK
                    },
                    onBoundaryHit = { touchOffset, distance ->
                        val touchTime = System.currentTimeMillis()
                        val rt = (touchTime - targetAppearanceTime).coerceAtLeast(10L)
                        val trialMeasurement = ReactionTrialMeasurement(
                            trialId = "trial_${currentTrialIndex + 1}_$touchTime",
                            trialNumber = currentTrialIndex + 1,
                            timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date(touchTime)),
                            targetAppearanceTimestamp = targetAppearanceTime,
                            targetLocationX = targetNormalizedX,
                            targetLocationY = targetNormalizedY,
                            targetRadiusDp = targetRadiusDp,
                            boundaryRadiusDp = boundaryRadiusDp,
                            visibilityDurationMs = (currentDifficultySeconds * 1000f).toLong(),
                            difficultyLevel = currentDifficultySeconds.toDouble(),
                            vibrationTimestamp = vibrationTime,
                            firstTouchTimestamp = touchTime,
                            reactionTimeMs = rt,
                            hitType = ReactionTrialMeasurement.HIT_BOUNDARY,
                            offTargetTouch = offTargetTouchesInTrial > 0,
                            offTargetTouchesCount = offTargetTouchesInTrial,
                            touchOffsetX = touchOffset.x,
                            touchOffsetY = touchOffset.y,
                            touchDistance = distance,
                            timeout = false
                        )
                        completedTrials.add(trialMeasurement)
                        lastTrialHitType = ReactionTrialMeasurement.HIT_BOUNDARY
                        lastTrialReactionTimeMs = rt
                        lastTrialOffTargetDetected = offTargetTouchesInTrial > 0
                        step = ReactionExperimentStep.TRIAL_FEEDBACK
                    },
                    onOffTargetTouch = {
                        offTargetTouchesInTrial++
                    }
                )
            }

            ReactionExperimentStep.TRIAL_FEEDBACK -> {
                TrialFeedbackView(
                    trialNumber = currentTrialIndex + 1,
                    totalTrials = totalTrials,
                    hitType = lastTrialHitType,
                    reactionTimeMs = lastTrialReactionTimeMs,
                    offTargetDetected = lastTrialOffTargetDetected
                )
            }

            ReactionExperimentStep.RESULTS_SUMMARY -> {
                ReactionResultsSummaryView(
                    totalTrials = totalTrials,
                    completedTrials = completedTrials.size,
                    validHits = completedTrials.count { it.isSuccess },
                    averageRtMs = completedTrials.mapNotNull { it.reactionTimeMs }.average().takeIf { !it.isNaN() }?.roundToInt(),
                    adaptiveDifficulty = currentDifficultySeconds.toDouble(),
                    onReturnHome = onFinishAndReturnHome
                )
            }
        }
    }
}

/**
 * Clean, elderly-friendly instructions view for Reaction & Coordination.
 */
@Composable
private fun ReactionInstructionsView(
    difficultySeconds: Double,
    totalTrials: Int,
    onStartExperiment: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar with Back Navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .testTag("back_from_reaction_instructions")
                            .size(52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reaction & Coordination",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Feature Overview Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reaction_instruction_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Target Response Exercise",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "A simple circle will appear at a random place on your screen. You may also feel a gentle vibration.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TouchApp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Tap the circle as quickly as you can.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Take your time and stay calm. There is no penalty.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "$totalTrials trials · Target visible for ${String.format(Locale.US, "%.1f", difficultySeconds)}s",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Button(
                    onClick = onStartExperiment,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .testTag("start_reaction_experiment_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Begin Exercise",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("return_from_reaction_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = "Return to Practice List",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Calming anticipation delay view to prevent premature guessing.
 */
@Composable
private fun WaitingForTargetView(
    trialNumber: Int,
    totalTrials: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Trial $trialNumber of $totalTrials",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary)
                )
            }

            Text(
                text = "Watch the screen…",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Stay relaxed. The circle will appear soon.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Interactive canvas where the circle appears and user touches are captured.
 * Evaluates touches:
 * - Direct hit on target (inner radius)
 * - Boundary hit (extended surrounding boundary)
 * - Off-target touch (outside boundary): records "Off-target interaction detected"
 */
@Composable
private fun ActiveTargetView(
    trialNumber: Int,
    totalTrials: Int,
    targetNormalizedX: Float,
    targetNormalizedY: Float,
    targetRadiusDp: Float,
    boundaryRadiusDp: Float,
    visibilityDurationMs: Long,
    onDirectHit: (Offset, Float) -> Unit,
    onBoundaryHit: (Offset, Float) -> Unit,
    onOffTargetTouch: () -> Unit
) {
    val density = LocalDensity.current
    var showOffTargetFeedback by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { tapOffset ->
                    val canvasWidth = size.width.toFloat()
                    val canvasHeight = size.height.toFloat()

                    val targetCenterX = targetNormalizedX * canvasWidth
                    val targetCenterY = targetNormalizedY * canvasHeight

                    val targetRadiusPx = with(density) { targetRadiusDp.dp.toPx() }
                    val boundaryRadiusPx = with(density) { boundaryRadiusDp.dp.toPx() }

                    val dx = tapOffset.x - targetCenterX
                    val dy = tapOffset.y - targetCenterY
                    val distance = sqrt(dx * dx + dy * dy)
                    val offsetRelativeToTarget = Offset(dx, dy)

                    when {
                        distance <= targetRadiusPx -> {
                            onDirectHit(offsetRelativeToTarget, distance)
                        }
                        distance <= boundaryRadiusPx -> {
                            onBoundaryHit(offsetRelativeToTarget, distance)
                        }
                        else -> {
                            // Touch was outside target & surrounding boundary
                            showOffTargetFeedback = true
                            onOffTargetTouch()
                        }
                    }
                }
            }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        val targetCenterX = targetNormalizedX * widthPx
        val targetCenterY = targetNormalizedY * heightPx

        // Trial header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 24.dp, end = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Trial $trialNumber of $totalTrials",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            AnimatedVisibility(visible = showOffTargetFeedback) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "Off-target interaction detected",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Render target and its subtle extended boundary on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(targetCenterX, targetCenterY)
            val targetRadius = targetRadiusDp.dp.toPx()
            val boundaryRadius = boundaryRadiusDp.dp.toPx()

            // Outer interaction boundary (subtle, non-distracting)
            drawCircle(
                color = Color(0xFF00897B).copy(alpha = 0.12f),
                radius = boundaryRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFF00897B).copy(alpha = 0.25f),
                radius = boundaryRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Primary high-contrast target circle
            drawCircle(
                color = Color(0xFF00796B), // Rich emerald teal
                radius = targetRadius,
                center = center
            )

            // Inner center dot for precise focal alignment
            drawCircle(
                color = Color.White,
                radius = targetRadius * 0.35f,
                center = center
            )
        }
    }
}

/**
 * Brief observational feedback shown between trials.
 */
@Composable
private fun TrialFeedbackView(
    trialNumber: Int,
    totalTrials: Int,
    hitType: String,
    reactionTimeMs: Long?,
    offTargetDetected: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "Trial $trialNumber of $totalTrials",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            when (hitType) {
                ReactionTrialMeasurement.HIT_DIRECT -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Target Touched",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (reactionTimeMs != null) {
                        Text(
                            text = "Response time: ${reactionTimeMs} ms",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                ReactionTrialMeasurement.HIT_BOUNDARY -> {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Boundary Touched",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (reactionTimeMs != null) {
                        Text(
                            text = "Response time: ${reactionTimeMs} ms",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                else -> {
                    Text(
                        text = "Time Expired",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Target disappeared before touch",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (offTargetDetected) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Off-target interaction recorded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * Factual, non-medical summary screen complying with guidelines.
 */
@Composable
private fun ReactionResultsSummaryView(
    totalTrials: Int,
    completedTrials: Int,
    validHits: Int,
    averageRtMs: Int?,
    adaptiveDifficulty: Double,
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
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

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
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Exercise Complete",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Your response times and target coordination measurements have been recorded securely.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Factual performance cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Target Hits",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$validHits / $totalTrials",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Avg Reaction",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (averageRtMs != null) "${averageRtMs} ms" else "—",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Observational non-diagnostic note
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Current difficulty setting: ${String.format(Locale.US, "%.1f", adaptiveDifficulty)} seconds visible per target. This updates adaptively over time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onReturnHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("reaction_finish_button"),
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
