package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class BreathPhase {
    INHALE,
    EXHALE,
    COMPLETED
}

@Composable
fun BreathingPreparationScreen(
    title: String = "Prepare Your Attention",
    subtitle: String = "A brief moment to settle your mind before beginning.",
    continueButtonText: String = "Enter Application",
    skipButtonText: String = "Continue",
    onComplete: () -> Unit
) {
    // Exactly three cycles with progressive pacing
    // Cycle 1: slow (4000ms in, 4000ms out)
    // Cycle 2: slightly faster (3000ms in, 3000ms out)
    // Cycle 3: slightly faster again (2200ms in, 2200ms out)
    var currentCycle by remember { mutableIntStateOf(1) }
    var phase by remember { mutableStateOf(BreathPhase.INHALE) }

    val currentDuration = when (currentCycle) {
        1 -> 4000
        2 -> 3000
        else -> 2200
    }

    LaunchedEffect(currentCycle, phase) {
        when (phase) {
            BreathPhase.INHALE -> {
                delay(currentDuration.toLong())
                phase = BreathPhase.EXHALE
            }
            BreathPhase.EXHALE -> {
                delay(currentDuration.toLong())
                if (currentCycle < 3) {
                    currentCycle += 1
                    phase = BreathPhase.INHALE
                } else {
                    phase = BreathPhase.COMPLETED
                    delay(1200)
                    onComplete()
                }
            }
            BreathPhase.COMPLETED -> {
                // Done
            }
        }
    }

    // Smooth expanding/contracting visual scale
    val targetScale = when (phase) {
        BreathPhase.INHALE -> 1.0f
        BreathPhase.EXHALE -> 0.58f
        BreathPhase.COMPLETED -> 0.85f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = currentDuration,
            easing = FastOutSlowInEasing
        ),
        label = "breathing_scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .padding(horizontal = 28.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top guidance
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Cycle progress indicator (3 steps)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..3) {
                            val isActive = i == currentCycle && phase != BreathPhase.COMPLETED
                            val isPast = i < currentCycle || phase == BreathPhase.COMPLETED

                            Box(
                                modifier = Modifier
                                    .size(if (isActive) 16.dp else 12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isPast -> MaterialTheme.colorScheme.primary
                                            isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (phase == BreathPhase.COMPLETED)
                            "Complete"
                        else
                            "Breath $currentCycle of 3",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Central Breathing Visual Element
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .testTag("breathing_visual_element"),
                    contentAlignment = Alignment.Center
                ) {
                    // Soft outer ripple circle
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(animatedScale)
                            .clip(CircleShape)
                            .background(primaryContainer.copy(alpha = 0.5f))
                    )

                    // Inner main breathing circle
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(animatedScale)
                            .clip(CircleShape)
                            .background(primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (phase) {
                                BreathPhase.INHALE -> "Breathe in…"
                                BreathPhase.EXHALE -> "Breathe out…"
                                BreathPhase.COMPLETED -> "Ready"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                // Bottom Controls / Skip
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (phase == BreathPhase.COMPLETED) {
                        Button(
                            onClick = onComplete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .testTag("breathing_complete_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = continueButtonText,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onComplete,
                            modifier = Modifier
                                .testTag("skip_breathing_button")
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = skipButtonText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
