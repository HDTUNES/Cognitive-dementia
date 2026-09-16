package com.example.ui.screens.experiment

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.experiment.memory.MemoryColor
import com.example.experiment.memory.MemoryShape

@Composable
fun MemoryShapeView(
    shape: MemoryShape,
    color: MemoryColor,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
    showOutline: Boolean = true
) {
    val isDark = isSystemInDarkTheme()
    val fillColor = color.getColor(isDark)
    val outlineColor = if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.25f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasWidth = this.size.width
            val canvasHeight = this.size.height
            val strokeWidth = 4.dp.toPx()

            when (shape) {
                MemoryShape.CIRCLE -> {
                    val radius = (canvasWidth.coerceAtMost(canvasHeight) / 2f) - (strokeWidth / 2f)
                    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)

                    drawCircle(
                        color = fillColor,
                        radius = radius,
                        center = center,
                        style = Fill
                    )
                    if (showOutline) {
                        drawCircle(
                            color = outlineColor,
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }

                MemoryShape.TRIANGLE -> {
                    val path = Path().apply {
                        val top = strokeWidth
                        val bottom = canvasHeight - strokeWidth
                        val left = strokeWidth
                        val right = canvasWidth - strokeWidth

                        moveTo(canvasWidth / 2f, top)
                        lineTo(right, bottom)
                        lineTo(left, bottom)
                        close()
                    }

                    drawPath(path = path, color = fillColor, style = Fill)
                    if (showOutline) {
                        drawPath(path = path, color = outlineColor, style = Stroke(width = strokeWidth))
                    }
                }

                MemoryShape.SQUARE -> {
                    val edge = (canvasWidth.coerceAtMost(canvasHeight)) - (strokeWidth * 2f)
                    val topLeft = Offset(
                        (canvasWidth - edge) / 2f,
                        (canvasHeight - edge) / 2f
                    )

                    drawRect(
                        color = fillColor,
                        topLeft = topLeft,
                        size = Size(edge, edge),
                        style = Fill
                    )
                    if (showOutline) {
                        drawRect(
                            color = outlineColor,
                            topLeft = topLeft,
                            size = Size(edge, edge),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }

                MemoryShape.RECTANGLE -> {
                    // Distinct 1.8:1 aspect ratio rectangle
                    val rectWidth = canvasWidth - (strokeWidth * 2f)
                    val rectHeight = (rectWidth * 0.55f).coerceAtMost(canvasHeight - (strokeWidth * 2f))
                    val topLeft = Offset(
                        (canvasWidth - rectWidth) / 2f,
                        (canvasHeight - rectHeight) / 2f
                    )

                    drawRect(
                        color = fillColor,
                        topLeft = topLeft,
                        size = Size(rectWidth, rectHeight),
                        style = Fill
                    )
                    if (showOutline) {
                        drawRect(
                            color = outlineColor,
                            topLeft = topLeft,
                            size = Size(rectWidth, rectHeight),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
            }
        }
    }
}
