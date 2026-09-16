package com.example.ui.screens.caregiver

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.processor.CognitiveDimensionMetric
import com.example.processor.PersonalCognitiveState
import com.example.processor.SessionInspectionDetail

@Composable
fun MemoryComparisonChart(
    cognitiveState: PersonalCognitiveState,
    modifier: Modifier = Modifier
) {
    val baselineColor = Color(0xFF0D9488) // Teal
    val recentColor = Color(0xFFF59E0B)   // Amber
    val currentColor = Color(0xFF2563EB)  // Blue

    val memory = cognitiveState.memory

    val categories = listOf(
        Triple("Shape Accuracy", memory.shapeRecognition, false),
        Triple("Color Accuracy", memory.colorRecognition, false),
        Triple("Overall Memory", memory.overallMemoryAccuracy, false),
        Triple("No Recall Rate", memory.noRecallRate, true)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Memory: Baseline vs Recent vs Current",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Compares initial established baseline against rolling recent performance and the latest session.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = baselineColor, label = "Baseline")
                LegendItem(color = recentColor, label = "Recent Avg")
                LegendItem(color = currentColor, label = "Current")
            }

            // Grouped Bar Visualizer
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                categories.forEach { (label, metric, _) ->
                    GroupedBarRow(
                        label = label,
                        metric = metric,
                        baselineColor = baselineColor,
                        recentColor = recentColor,
                        currentColor = currentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun GroupedBarRow(
    label: String,
    metric: CognitiveDimensionMetric,
    baselineColor: Color,
    recentColor: Color,
    currentColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (metric.trend != CognitiveDimensionMetric.TREND_INSUFFICIENT_DATA) {
                    "Trend: ${metric.trend.replaceFirstChar { it.uppercase() }}"
                } else "Insufficient Data",
                style = MaterialTheme.typography.labelSmall,
                color = when (metric.trend) {
                    CognitiveDimensionMetric.TREND_IMPROVING -> Color(0xFF16A34A)
                    CognitiveDimensionMetric.TREND_DECLINING -> Color(0xFFDC2626)
                    CognitiveDimensionMetric.TREND_STABLE -> Color(0xFF2563EB)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Three bars
        SingleBarVisual(
            label = "Baseline",
            value = metric.baseline,
            color = baselineColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        SingleBarVisual(
            label = "Recent  ",
            value = metric.recent,
            color = recentColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        SingleBarVisual(
            label = "Current ",
            value = metric.current,
            color = currentColor
        )
    }
}

@Composable
private fun SingleBarVisual(
    label: String,
    value: Double?,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(7.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(7.dp))
        ) {
            if (value != null) {
                val clamped = value.coerceIn(0.0, 1.0).toFloat()
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clamped)
                        .height(14.dp)
                        .background(color, RoundedCornerShape(7.dp))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = if (value != null) "${(value * 100).toInt()}%" else "—",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(42.dp)
        )
    }
}

@Composable
fun LongitudinalReactionTimeChart(
    cognitiveState: PersonalCognitiveState,
    sessions: List<SessionInspectionDetail>,
    modifier: Modifier = Modifier
) {
    val baselineRt = cognitiveState.reactionTime.averageReactionTimeMs.baseline
    val recentRt = cognitiveState.reactionTime.averageReactionTimeMs.recent
    val currentRt = cognitiveState.reactionTime.averageReactionTimeMs.current
    val variabilityMs = cognitiveState.reactionTime.variabilityMs
    val trend = cognitiveState.reactionTime.trend

    // Sort chronologically ascending for the chart
    val chronologicalSessions = sessions.reversed().filter { it.overallResponseTimeMs != null }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Reaction Time Longitudinal Trajectory",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Observed response speed across completed sessions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (trend) {
                        CognitiveDimensionMetric.TREND_IMPROVING -> Color(0xFF16A34A).copy(alpha = 0.15f)
                        CognitiveDimensionMetric.TREND_DECLINING -> Color(0xFFDC2626).copy(alpha = 0.15f)
                        CognitiveDimensionMetric.TREND_STABLE -> Color(0xFF2563EB).copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    }
                ) {
                    Text(
                        text = trend.replace("_", " ").uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (trend) {
                            CognitiveDimensionMetric.TREND_IMPROVING -> Color(0xFF16A34A)
                            CognitiveDimensionMetric.TREND_DECLINING -> Color(0xFFDC2626)
                            CognitiveDimensionMetric.TREND_STABLE -> Color(0xFF2563EB)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatChip(label = "Baseline", value = if (baselineRt != null) "${baselineRt.toInt()} ms" else "—")
                StatChip(label = "Recent Avg", value = if (recentRt != null) "${recentRt.toInt()} ms" else "—")
                StatChip(label = "Current", value = if (currentRt != null) "${currentRt.toInt()} ms" else "—")
                StatChip(label = "Variability (±)", value = if (variabilityMs != null) "${variabilityMs.toInt()} ms" else "—")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (chronologicalSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No completed session data yet to graph.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Canvas Graph
                val baselineColor = Color(0xFF0D9488)
                val recentColor = Color(0xFFF59E0B)
                val lineColor = Color(0xFF2563EB)
                val dotColor = Color(0xFF1D4ED8)

                val maxRt = maxOf(
                    chronologicalSessions.maxOf { it.overallResponseTimeMs ?: 0.0 },
                    baselineRt ?: 0.0,
                    recentRt ?: 0.0,
                    3000.0
                )
                val minRt = minOf(
                    chronologicalSessions.minOf { it.overallResponseTimeMs ?: 2000.0 },
                    baselineRt ?: 1000.0,
                    500.0
                )
                val range = (maxRt - minRt).coerceAtLeast(500.0)

                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val width = size.width
                    val height = size.height

                    fun getY(ms: Double): Float {
                        val normalized = ((ms - minRt) / range).coerceIn(0.0, 1.0)
                        return (height - (normalized * height)).toFloat()
                    }

                    // 1. Baseline horizontal dashed line
                    if (baselineRt != null) {
                        val baseBaselineY = getY(baselineRt)
                        drawLine(
                            color = baselineColor,
                            start = Offset(0f, baseBaselineY),
                            end = Offset(width, baseBaselineY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                        )
                    }

                    // 2. Recent average horizontal line
                    if (recentRt != null) {
                        val recentY = getY(recentRt)
                        drawLine(
                            color = recentColor,
                            start = Offset(0f, recentY),
                            end = Offset(width, recentY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    }

                    // 3. Connect points
                    if (chronologicalSessions.size > 1) {
                        val stepX = width / (chronologicalSessions.size - 1).coerceAtLeast(1)
                        val path = Path()

                        chronologicalSessions.forEachIndexed { index, session ->
                            val ms = session.overallResponseTimeMs ?: baselineRt ?: 1500.0
                            val x = index * stepX
                            val y = getY(ms)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }

                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // Draw dots
                        chronologicalSessions.forEachIndexed { index, session ->
                            val ms = session.overallResponseTimeMs ?: baselineRt ?: 1500.0
                            val x = index * stepX
                            val y = getY(ms)
                            drawCircle(
                                color = dotColor,
                                radius = 5.dp.toPx(),
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2.5.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    } else if (chronologicalSessions.size == 1) {
                        val ms = chronologicalSessions[0].overallResponseTimeMs ?: 1500.0
                        val y = getY(ms)
                        drawCircle(
                            color = dotColor,
                            radius = 6.dp.toPx(),
                            center = Offset(width / 2f, y)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Session 1",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Session ${chronologicalSessions.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Clinical note: Fast or slow single sessions do not indicate recovery or decline. Stable trends require repeated observations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun CoordinationComparisonChart(
    cognitiveState: PersonalCognitiveState,
    modifier: Modifier = Modifier
) {
    val baselineColor = Color(0xFF0D9488) // Teal
    val recentColor = Color(0xFFF59E0B)   // Amber
    val currentColor = Color(0xFF2563EB)  // Blue

    val coordination = cognitiveState.coordination

    val categories = listOf(
        Triple("Target Hit Accuracy", coordination.hitRate, false),
        Triple("Off-Target Tap Rate", coordination.offTargetRate, true),
        Triple("Timeout Miss Rate", coordination.timeoutRate, true)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Motor Coordination & Spatial Precision",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Compares direct and boundary target touches against off-target tap attempts and stimulus timeouts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = baselineColor, label = "Baseline")
                LegendItem(color = recentColor, label = "Recent Avg")
                LegendItem(color = currentColor, label = "Current")
            }

            // Grouped Bar Visualizer
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                categories.forEach { (label, metric, _) ->
                    GroupedBarRow(
                        label = label,
                        metric = metric,
                        baselineColor = baselineColor,
                        recentColor = recentColor,
                        currentColor = currentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Off-Target Interactions Recorded:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${coordination.totalOffTargetInteractions}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (coordination.totalOffTargetInteractions > 0) Color(0xFFD97706) else Color(0xFF16A34A)
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
