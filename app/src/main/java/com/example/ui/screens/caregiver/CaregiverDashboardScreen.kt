package com.example.ui.screens.caregiver

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processor.CognitiveDimensionMetric
import com.example.processor.PersonalCognitiveState
import com.example.processor.SessionInspectionDetail
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CaregiverDashboardScreen(
    cognitiveState: PersonalCognitiveState,
    onRecalculateState: () -> Unit,
    onBackToHome: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var inspectingSession by remember { mutableStateOf<SessionInspectionDetail?>(null) }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var jsonCopiedNotification by remember { mutableStateOf(false) }

    val tabs = listOf("Overview", "Graphs", "Session Timeline", "AI / Data Spec")

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CaregiverTopBar(
                onBack = onBackToHome,
                onRecalculate = onRecalculateState
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
            ) {
                // Non-medical Disclaimer Banner
                NonMedicalDisclaimerBanner()

                // Navigation Tabs
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("caregiver_tab_$index")
                        )
                    }
                }

                // Tab Content
                when (selectedTabIndex) {
                    0 -> OverviewTabContent(cognitiveState = cognitiveState)
                    1 -> GraphsTabContent(cognitiveState = cognitiveState)
                    2 -> TimelineTabContent(
                        sessions = cognitiveState.inspectionSessions,
                        onInspect = { inspectingSession = it }
                    )
                    3 -> AiDataSpecTabContent(
                        cognitiveState = cognitiveState,
                        onCopyJson = {
                            clipboardManager.setText(AnnotatedString(cognitiveState.toGeminiContextJson()))
                            jsonCopiedNotification = true
                        },
                        jsonCopied = jsonCopiedNotification
                    )
                }
            }
        }
    }

    // Detail Inspection Modal
    inspectingSession?.let { session ->
        SessionInspectionDialog(
            session = session,
            onDismiss = { inspectingSession = null }
        )
    }
}

@Composable
private fun CaregiverTopBar(
    onBack: () -> Unit,
    onRecalculate: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("caregiver_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Return to Home",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Clinician / Caregiver Portal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Authorized Behavioral Performance Monitoring",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onRecalculate,
                modifier = Modifier.testTag("caregiver_recalculate_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Recalculate Personal Cognitive State",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun NonMedicalDisclaimerBanner() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Observed Behavioral Performance: Represents longitudinal task speed, consistency, and accuracy. This is NOT a medical diagnosis and does not reflect direct neural activity.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun OverviewTabContent(cognitiveState: PersonalCognitiveState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Overall Behavioral Performance Card
        item {
            OverallSummaryCard(cognitiveState = cognitiveState)
        }

        // 2. Personal Baseline Reference Card
        item {
            BaselineStatusCard(cognitiveState = cognitiveState)
        }

        // 3. Cognitive Dimension Cards (Memory, Attention, Reaction Time)
        item {
            Text(
                text = "Observed Cognitive Dimensions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        item {
            DimensionSummaryCard(
                title = "Memory Recognition",
                icon = Icons.Default.Psychology,
                metric = cognitiveState.memory.overallMemoryAccuracy,
                unit = "%",
                description = "Accuracy in recalling geometric shape and color pairs after a retention delay.",
                subMetrics = listOf(
                    "Shape Accuracy" to cognitiveState.memory.shapeRecognition,
                    "Color Accuracy" to cognitiveState.memory.colorRecognition,
                    "Unforced Non-Recall" to cognitiveState.memory.noRecallRate
                )
            )
        }

        item {
            DimensionSummaryCard(
                title = "Attention & Concentration",
                icon = Icons.Default.Timer,
                metric = cognitiveState.attention.taskConsistency,
                unit = "%",
                description = "Task engagement, adherence to preparation routines, and response consistency.",
                subMetrics = listOf(
                    "Preparation Adherence" to cognitiveState.attention.preparationAdherence,
                    "Missed Task Rate" to cognitiveState.attention.missedRate
                )
            )
        }

        item {
            val reactionSubMetrics = mutableListOf<Pair<String, CognitiveDimensionMetric>>()
            reactionSubMetrics.add(
                "Variability (±)" to CognitiveDimensionMetric(
                    current = cognitiveState.reactionTime.variabilityMs,
                    trend = cognitiveState.reactionTime.trend,
                    confidence = cognitiveState.reactionTime.confidence,
                    dataQuality = cognitiveState.reactionTime.averageReactionTimeMs.dataQuality
                )
            )
            cognitiveState.reactionTime.fastestMs?.let { fastest ->
                reactionSubMetrics.add(
                    "Fastest Response" to CognitiveDimensionMetric(
                        current = fastest.toDouble(),
                        trend = cognitiveState.reactionTime.trend,
                        confidence = cognitiveState.reactionTime.confidence,
                        dataQuality = cognitiveState.reactionTime.averageReactionTimeMs.dataQuality
                    )
                )
            }
            cognitiveState.reactionTime.slowestMs?.let { slowest ->
                reactionSubMetrics.add(
                    "Slowest Response" to CognitiveDimensionMetric(
                        current = slowest.toDouble(),
                        trend = cognitiveState.reactionTime.trend,
                        confidence = cognitiveState.reactionTime.confidence,
                        dataQuality = cognitiveState.reactionTime.averageReactionTimeMs.dataQuality
                    )
                )
            }

            DimensionSummaryCard(
                title = "Reaction Time",
                icon = Icons.Default.Speed,
                metric = cognitiveState.reactionTime.averageReactionTimeMs,
                unit = "ms",
                isLowerBetter = true,
                description = "Time elapsed between stimulus display and response registration.",
                subMetrics = reactionSubMetrics
            )
        }

        item {
            DimensionSummaryCard(
                title = "Motor Coordination",
                icon = Icons.Default.TouchApp,
                metric = cognitiveState.coordination.hitRate,
                unit = "%",
                description = "Target acquisition accuracy, spatial tap precision, and off-target avoidance.",
                subMetrics = listOf(
                    "Off-Target Interaction Rate" to cognitiveState.coordination.offTargetRate,
                    "Stimulus Timeout Rate" to cognitiveState.coordination.timeoutRate,
                    "Total Off-Target Taps" to CognitiveDimensionMetric(
                        current = cognitiveState.coordination.totalOffTargetInteractions.toDouble(),
                        trend = cognitiveState.coordination.trend,
                        confidence = cognitiveState.coordination.confidence,
                        dataQuality = cognitiveState.coordination.hitRate.dataQuality
                    ),
                    "Adaptive Difficulty Window" to CognitiveDimensionMetric(
                        current = cognitiveState.coordination.averageAdaptiveDifficultySeconds,
                        trend = cognitiveState.coordination.trend,
                        confidence = cognitiveState.coordination.confidence,
                        dataQuality = cognitiveState.coordination.hitRate.dataQuality
                    )
                )
            )
        }

        // 4. Activity Monitoring Summary
        item {
            ActivityMonitoringCard(summary = cognitiveState.activitySummary)
        }
    }
}

@Composable
private fun OverallSummaryCard(cognitiveState: PersonalCognitiveState) {
    val overall = cognitiveState.overall
    val totalSessions = cognitiveState.totalCompletedSessions
    val trend = overall.trend
    val confidencePct = (overall.confidence * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OVERALL OBSERVED PERFORMANCE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = overall.observedPerformanceLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TrendBadge(trend = trend)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStat(
                    label = "Completed Sessions",
                    value = "$totalSessions"
                )
                SummaryStat(
                    label = "Data Confidence",
                    value = "$confidencePct% (${overall.overallScore.dataQuality})"
                )
                SummaryStat(
                    label = "Trend Status",
                    value = if (overall.hasSufficientDataForTrend) "Identifiable" else "Insufficient Data"
                )
            }

            if (!overall.hasSufficientDataForTrend) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Trend analysis requires repeated evidence (at least 3 completed sessions).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BaselineStatusCard(cognitiveState: PersonalCognitiveState) {
    val isEstablished = cognitiveState.baselineEstablished
    val baselineColor = if (isEstablished) Color(0xFF0D9488) else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(baselineColor, CircleShape)
                    )
                    Text(
                        text = "Personal Baseline Reference Point",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isEstablished) Color(0xFF0D9488).copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant
                ) {
                    Text(
                        text = if (isEstablished) "ESTABLISHED" else "INSUFFICIENT DATA",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isEstablished) Color(0xFF0D9488) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isEstablished) {
                val memBase = cognitiveState.memory.overallMemoryAccuracy.baseline
                val rtBase = cognitiveState.reactionTime.averageReactionTimeMs.baseline
                Text(
                    text = "Established from initial complete dataset. Remains stable as the long-term benchmark and is NOT overwritten after each session.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        text = "Baseline Memory: ${if (memBase != null) "${(memBase * 100).toInt()}%" else "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Baseline Reaction Time: ${if (rtBase != null) "${rtBase.toInt()} ms" else "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Text(
                    text = "Baseline not established / insufficient data. Initial baseline will be formed once the user completes the first full practice sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DimensionSummaryCard(
    title: String,
    icon: ImageVector,
    metric: CognitiveDimensionMetric,
    unit: String,
    description: String,
    isLowerBetter: Boolean = false,
    subMetrics: List<Pair<String, CognitiveDimensionMetric>> = emptyList()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                TrendBadge(trend = metric.trend)
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Comparison: Baseline -> Recent -> Current
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ComparisonColumn(
                    title = "Baseline",
                    value = formatMetricValue(metric.baseline, unit),
                    subtitle = "Reference"
                )
                ComparisonColumn(
                    title = "Recent Avg",
                    value = formatMetricValue(metric.recent, unit),
                    subtitle = formatDelta(metric.changeFromBaseline, unit, isLowerBetter, "vs Base")
                )
                ComparisonColumn(
                    title = "Current",
                    value = formatMetricValue(metric.current, unit),
                    subtitle = formatDelta(metric.changeFromRecent, unit, isLowerBetter, "vs Recent")
                )
                ComparisonColumn(
                    title = "Confidence",
                    value = "${(metric.confidence * 100).toInt()}%",
                    subtitle = metric.dataQuality
                )
            }

            if (subMetrics.isNotEmpty()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    subMetrics.forEach { (subLabel, subMetric) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = subLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val subVal = if (unit == "%") {
                                if (subMetric.current != null) "${(subMetric.current * 100).toInt()}%" else "—"
                            } else {
                                if (subMetric.current != null) "${subMetric.current.toInt()} $unit" else "—"
                            }
                            Text(
                                text = subVal,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonColumn(title: String, value: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActivityMonitoringCard(summary: com.example.processor.ActivityMonitoringSummary) {
    val dateFormatter = remember { SimpleDateFormat("MMM dd · h:mm a", Locale.getDefault()) }
    val lastActivityStr = if (summary.lastActivityTimestamp != null) {
        dateFormatter.format(Date(summary.lastActivityTimestamp))
    } else "None recorded"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Activity & Compliance Monitoring",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStat("Sessions Completed", "${summary.sessionsCompleted}")
                SummaryStat("Missed Reminders", "${summary.sessionsMissed}")
                SummaryStat("Avg Duration", "${summary.averageSessionDurationSeconds}s")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryStat("Hydration Completed", "${summary.hydrationRemindersCompleted}")
                SummaryStat("Hydration Missed", "${summary.hydrationRemindersMissed}")
                SummaryStat("Last Active", lastActivityStr)
            }
        }
    }
}

@Composable
private fun GraphsTabContent(cognitiveState: PersonalCognitiveState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            MemoryComparisonChart(cognitiveState = cognitiveState)
        }

        item {
            LongitudinalReactionTimeChart(
                cognitiveState = cognitiveState,
                sessions = cognitiveState.inspectionSessions
            )
        }

        item {
            CoordinationComparisonChart(cognitiveState = cognitiveState)
        }
    }
}

@Composable
private fun TimelineTabContent(
    sessions: List<SessionInspectionDetail>,
    onInspect: (SessionInspectionDetail) -> Unit
) {
    if (sessions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No completed sessions in the longitudinal timeline yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Longitudinal Session Records (${sessions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap any session to inspect raw stimuli presentation, response times, and unforced non-recall choices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(sessions) { session ->
                SessionTimelineItemCard(session = session, onInspect = { onInspect(session) })
            }
        }
    }
}

@Composable
private fun SessionTimelineItemCard(
    session: SessionInspectionDetail,
    onInspect: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.getDefault()) }
    val dateStr = remember(session.startedAt) { formatter.format(Date(session.startedAt)) }
    val isReaction = session.sessionType == com.example.data.model.UserSession.TYPE_REACTION_COORDINATION

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isReaction) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = if (isReaction) "Reaction & Coordination" else "Memory",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isReaction) MaterialTheme.colorScheme.onTertiaryContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isReaction) "Hit Rate: ${if (session.overallAccuracy != null) "${(session.overallAccuracy * 100).toInt()}%" else "—"}"
                        else "Accuracy: ${if (session.overallAccuracy != null) "${(session.overallAccuracy * 100).toInt()}%" else "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "RT: ${if (session.overallResponseTimeMs != null) "${session.overallResponseTimeMs.toInt()}ms" else "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isReaction && session.noRecallCount > 0) {
                        Text(
                            text = "No Recall: ${session.noRecallCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD97706)
                        )
                    }
                    if (isReaction && session.offTargetTouchesCount > 0) {
                        Text(
                            text = "Off-Target: ${session.offTargetTouchesCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD97706)
                        )
                    }
                }
            }

            Button(
                onClick = onInspect,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.testTag("inspect_session_${session.sessionId}")
            ) {
                Text(
                    text = "Inspect",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AiDataSpecTabContent(
    cognitiveState: PersonalCognitiveState,
    onCopyJson: () -> Unit,
    jsonCopied: Boolean
) {
    val jsonPayload = remember(cognitiveState) { cognitiveState.toGeminiContextJson() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Data Hierarchy Architecture",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Strict 3-tier structure to prevent inferred metrics from being treated as direct observations:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    DataTierRow(
                        tier = "Tier 1: Observed Measurements",
                        desc = "Raw trial reactions, stimulus shapes, colors, and millisecond timers."
                    )
                    DataTierRow(
                        tier = "Tier 2: Derived Metrics",
                        desc = "Accuracies, variability (standard deviation), rolling recent averages."
                    )
                    DataTierRow(
                        tier = "Tier 3: Trend Interpretation",
                        desc = "Multi-session directional trajectory requiring repeated evidence (improving, stable, declining, variable, insufficient_data)."
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Structured Observation Payload",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ready for future Gemini backend integration without connecting AI yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedButton(
                    onClick = onCopyJson,
                    modifier = Modifier.testTag("copy_json_spec_btn")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (jsonCopied) "Copied!" else "Copy JSON")
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = jsonPayload,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(14.dp),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun DataTierRow(tier: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = tier,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TrendBadge(trend: String) {
    val (bgColor, textColor, label) = when (trend) {
        CognitiveDimensionMetric.TREND_IMPROVING -> Triple(Color(0xFF16A34A).copy(alpha = 0.15f), Color(0xFF16A34A), "IMPROVING")
        CognitiveDimensionMetric.TREND_DECLINING -> Triple(Color(0xFFDC2626).copy(alpha = 0.15f), Color(0xFFDC2626), "DECLINING")
        CognitiveDimensionMetric.TREND_STABLE -> Triple(Color(0xFF2563EB).copy(alpha = 0.15f), Color(0xFF2563EB), "STABLE")
        CognitiveDimensionMetric.TREND_VARIABLE -> Triple(Color(0xFFD97706).copy(alpha = 0.15f), Color(0xFFD97706), "VARIABLE")
        else -> Triple(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.onSurfaceVariant, "INSUFFICIENT DATA")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatMetricValue(value: Double?, unit: String): String {
    if (value == null) return "—"
    return if (unit == "%") {
        "${(value * 100).toInt()}%"
    } else {
        "${value.toInt()} $unit"
    }
}

private fun formatDelta(delta: Double?, unit: String, isLowerBetter: Boolean, prefix: String): String {
    if (delta == null) return "—"
    return if (unit == "%") {
        val pct = (delta * 100).toInt()
        val sign = if (pct >= 0) "+$pct%" else "$pct%"
        "$sign $prefix"
    } else {
        val sign = if (delta >= 0) "+${delta.toInt()}" else "${delta.toInt()}"
        "$sign $unit $prefix"
    }
}
