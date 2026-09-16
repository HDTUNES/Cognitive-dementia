package com.example.ui.screens.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.processor.SessionInspectionDetail
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ParsedTrialItem(
    val trialNumber: Int,
    val stimulusShape: String,
    val stimulusColor: String,
    val shapeAnswer: String,
    val shapeCorrect: Boolean,
    val shapeResponseType: String,
    val shapeResponseTimeMs: Long,
    val colorAnswer: String,
    val colorCorrect: Boolean,
    val colorResponseType: String,
    val colorResponseTimeMs: Long
)

data class ParsedReactionTrialItem(
    val trialNumber: Int,
    val hitType: String,
    val isSuccess: Boolean,
    val reactionTimeMs: Long?,
    val offTargetTouch: Boolean,
    val offTargetTouchesCount: Int,
    val difficultyLevel: Double,
    val timeout: Boolean
)

@Composable
fun SessionInspectionDialog(
    session: SessionInspectionDetail,
    onDismiss: () -> Unit
) {
    val dateTimeFormatter = remember { SimpleDateFormat("MMMM dd, yyyy · h:mm a", Locale.getDefault()) }
    val formattedDate = remember(session.startedAt) { dateTimeFormatter.format(Date(session.startedAt)) }
    val isReaction = session.sessionType == com.example.data.model.UserSession.TYPE_REACTION_COORDINATION

    val parsedTrials = remember(session.trialsJson, isReaction) {
        val list = mutableListOf<ParsedTrialItem>()
        if (!isReaction) {
            try {
                val array = JSONArray(session.trialsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val stim = obj.optJSONObject("stimulus")
                    val sResp = obj.optJSONObject("shape_response")
                    val cResp = obj.optJSONObject("color_response")

                    list.add(
                        ParsedTrialItem(
                            trialNumber = i + 1,
                            stimulusShape = stim?.optString("shape") ?: "Unknown",
                            stimulusColor = stim?.optString("color") ?: "Unknown",
                            shapeAnswer = sResp?.optString("answer") ?: "—",
                            shapeCorrect = sResp?.optBoolean("correct") ?: false,
                            shapeResponseType = sResp?.optString("response_type") ?: "unknown",
                            shapeResponseTimeMs = sResp?.optLong("response_time_ms") ?: 0L,
                            colorAnswer = cResp?.optString("answer") ?: "—",
                            colorCorrect = cResp?.optBoolean("correct") ?: false,
                            colorResponseType = cResp?.optString("response_type") ?: "unknown",
                            colorResponseTimeMs = cResp?.optLong("response_time_ms") ?: 0L
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        list
    }

    val parsedReactionTrials = remember(session.trialsJson, isReaction) {
        val list = mutableListOf<ParsedReactionTrialItem>()
        if (isReaction) {
            try {
                val array = JSONArray(session.trialsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ParsedReactionTrialItem(
                            trialNumber = obj.optInt("trial_number", i + 1),
                            hitType = obj.optString("hit_type", "timeout"),
                            isSuccess = obj.optBoolean("is_success", false),
                            reactionTimeMs = if (obj.has("reaction_time_ms") && !obj.isNull("reaction_time_ms")) obj.optLong("reaction_time_ms") else null,
                            offTargetTouch = obj.optBoolean("off_target_touch", false),
                            offTargetTouchesCount = obj.optInt("off_target_touches_count", 0),
                            difficultyLevel = obj.optDouble("difficulty_level", 2.0),
                            timeout = obj.optBoolean("timeout", false)
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Session Deep Inspection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dismiss_inspection_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Metric Badges Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Derived Session Metrics",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (isReaction) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricCell(
                                    label = "Hit Accuracy",
                                    value = if (session.overallAccuracy != null) "${(session.overallAccuracy * 100).toInt()}%" else "—"
                                )
                                MetricCell(
                                    label = "Avg Reaction Time",
                                    value = if (session.overallResponseTimeMs != null) "${session.overallResponseTimeMs.toInt()} ms" else "—"
                                )
                                MetricCell(
                                    label = "Duration",
                                    value = "${session.durationSeconds}s"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricCell(
                                    label = "Trials Completed",
                                    value = "${session.completedTrials}/${session.totalTrials}"
                                )
                                MetricCell(
                                    label = "Off-Target Touches",
                                    value = "${session.offTargetTouchesCount} tap(s)"
                                )
                                MetricCell(
                                    label = "Adaptive Window",
                                    value = String.format(Locale.US, "%.1fs", session.adaptiveDifficultySeconds)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricCell(
                                    label = "Overall Accuracy",
                                    value = if (session.overallAccuracy != null) "${(session.overallAccuracy * 100).toInt()}%" else "—"
                                )
                                MetricCell(
                                    label = "Avg Reaction Time",
                                    value = if (session.overallResponseTimeMs != null) "${session.overallResponseTimeMs.toInt()} ms" else "—"
                                )
                                MetricCell(
                                    label = "Duration",
                                    value = "${session.durationSeconds}s"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricCell(
                                    label = "Shape Accuracy",
                                    value = if (session.shapeAccuracy != null) "${(session.shapeAccuracy * 100).toInt()}%" else "—"
                                )
                                MetricCell(
                                    label = "Color Accuracy",
                                    value = if (session.colorAccuracy != null) "${(session.colorAccuracy * 100).toInt()}%" else "—"
                                )
                                MetricCell(
                                    label = "Trials Completed",
                                    value = "${session.completedTrials}/${session.totalTrials}"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricCell(
                                    label = "Unforced 'Don't Remember'",
                                    value = "${session.noRecallCount} times"
                                )
                                MetricCell(
                                    label = "Incorrect Answers",
                                    value = "${session.incorrectCount} times"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Comparisons with Baseline and Recent
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Longitudinal Comparisons",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val baseDelta = session.changeFromBaselineAccuracy
                            val deltaBaseText = if (baseDelta != null) {
                                val pct = (baseDelta * 100).toInt()
                                if (pct >= 0) "+$pct%" else "$pct%"
                            } else "Baseline not established"
                            Text(
                                text = "vs Personal Baseline:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = deltaBaseText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val recentDelta = session.changeFromRecentAccuracy
                            val deltaRecentText = if (recentDelta != null) {
                                val pct = (recentDelta * 100).toInt()
                                if (pct >= 0) "+$pct%" else "$pct%"
                            } else "Insufficient data"
                            Text(
                                text = "vs Recent Average:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = deltaRecentText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Raw Trial Observations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Individual stimulus presentation and registered participant responses.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isReaction) {
                    if (parsedReactionTrials.isEmpty()) {
                        Text(
                            text = "No individual trial payload available for this reaction session.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            parsedReactionTrials.forEach { trial ->
                                ReactionTrialDetailCard(trial)
                            }
                        }
                    }
                } else {
                    if (parsedTrials.isEmpty()) {
                        Text(
                            text = "No individual trial payload available for this session record.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            parsedTrials.forEach { trial ->
                                TrialDetailCard(trial)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_inspection_btn")
                ) {
                    Text("Close Inspection")
                }
            }
        }
    }
}

@Composable
private fun ReactionTrialDetailCard(trial: ParsedReactionTrialItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trial #${trial.trialNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Window: ${String.format(Locale.US, "%.1fs", trial.difficultyLevel)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val statusColor = when (trial.hitType) {
                        "direct_hit" -> Color(0xFF16A34A)
                        "boundary_hit" -> Color(0xFF2563EB)
                        "off_target" -> Color(0xFFD97706)
                        else -> Color(0xFFDC2626)
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Text(
                        text = when (trial.hitType) {
                            "direct_hit" -> "Direct Target Hit"
                            "boundary_hit" -> "Boundary Hit"
                            "off_target" -> "Off-Target Touch"
                            else -> "Stimulus Timeout"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (trial.reactionTimeMs != null) {
                        Text(
                            text = "${trial.reactionTimeMs}ms",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (trial.offTargetTouchesCount > 0) {
                        Text(
                            text = "${trial.offTargetTouchesCount} extra tap(s)",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TrialDetailCard(trial: ParsedTrialItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trial #${trial.trialNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Presented: ${trial.stimulusColor.replaceFirstChar { it.uppercase() }} ${trial.stimulusShape.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)

            // Shape response row
            ResponseRow(
                dimension = "Shape",
                selectedAnswer = trial.shapeAnswer,
                correct = trial.shapeCorrect,
                responseType = trial.shapeResponseType,
                timeMs = trial.shapeResponseTimeMs
            )

            // Color response row
            ResponseRow(
                dimension = "Color",
                selectedAnswer = trial.colorAnswer,
                correct = trial.colorCorrect,
                responseType = trial.colorResponseType,
                timeMs = trial.colorResponseTimeMs
            )
        }
    }
}

@Composable
private fun ResponseRow(
    dimension: String,
    selectedAnswer: String,
    correct: Boolean,
    responseType: String,
    timeMs: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val statusColor = when (responseType) {
                "correct" -> Color(0xFF16A34A)
                "no_recall" -> Color(0xFFD97706)
                else -> Color(0xFFDC2626)
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(statusColor, CircleShape)
            )
            Text(
                text = "$dimension: $selectedAnswer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "${timeMs}ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = when (responseType) {
                    "correct" -> "Correct"
                    "no_recall" -> "No Recall"
                    else -> "Incorrect"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when (responseType) {
                    "correct" -> Color(0xFF16A34A)
                    "no_recall" -> Color(0xFFD97706)
                    else -> Color(0xFFDC2626)
                }
            )
        }
    }
}
