package com.gymtracker.app.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun LineChartCard(
    values: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    val cleanValues = remember(values) { values.filter { !it.isNaN() && it > 0.0 } }
    if (cleanValues.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Log workout sessions to see trend line",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val minVal = remember(cleanValues) { cleanValues.minOrNull() ?: 0.0 }
    val maxVal = remember(cleanValues) { cleanValues.maxOrNull() ?: 100.0 }
    val range = if (maxVal > minVal) maxVal - minVal else 1.0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(8.dp),
    ) {
        Canvas(modifier = Modifier.matchParentSize().padding(horizontal = 12.dp, vertical = 12.dp)) {
            val width = size.width
            val height = size.height
            val stepX = if (cleanValues.size > 1) width / (cleanValues.size - 1) else width

            // Draw horizontal reference lines
            val gridColor = Color.Gray.copy(alpha = 0.2f)
            drawLine(gridColor, Offset(0f, 0f), Offset(width, 0f), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, height / 2f), Offset(width, height / 2f), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, height), Offset(width, height), strokeWidth = 1f)

            val strokePath = Path()
            val fillPath = Path()

            val points = cleanValues.mapIndexed { index, value ->
                val x = if (cleanValues.size > 1) index * stepX else width / 2f
                val normY = ((value - minVal) / range).toFloat().coerceIn(0f, 1f)
                val y = height - (normY * (height - 10f)) - 5f
                Offset(x, y)
            }

            if (points.isNotEmpty()) {
                strokePath.moveTo(points.first().x, points.first().y)
                fillPath.moveTo(points.first().x, height)
                fillPath.lineTo(points.first().x, points.first().y)

                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val curr = points[i]
                    val cX = (prev.x + curr.x) / 2f
                    strokePath.cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
                    fillPath.cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
                }

                fillPath.lineTo(points.last().x, height)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0.02f)),
                        startY = 0f,
                        endY = height,
                    ),
                )

                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )

                points.forEach { point ->
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = point)
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = point)
                }
            }
        }
    }
}

@Composable
fun MultiLineChartCard(
    series: List<List<Double>>,
    modifier: Modifier = Modifier,
) {
    val nonNullSeries = remember(series) { series.map { s -> s.filter { !it.isNaN() && it > 0.0 } }.filter { it.isNotEmpty() } }
    if (nonNullSeries.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Log weight data to view moving average",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val allValues = nonNullSeries.flatten()
    val minVal = allValues.minOrNull() ?: 0.0
    val maxVal = allValues.maxOrNull() ?: 100.0
    val range = if (maxVal > minVal) maxVal - minVal else 1.0

    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .padding(8.dp),
    ) {
        Canvas(modifier = Modifier.matchParentSize().padding(horizontal = 12.dp, vertical = 12.dp)) {
            val width = size.width
            val height = size.height

            val gridColor = Color.Gray.copy(alpha = 0.2f)
            drawLine(gridColor, Offset(0f, 0f), Offset(width, 0f), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, height / 2f), Offset(width, height / 2f), strokeWidth = 1f)
            drawLine(gridColor, Offset(0f, height), Offset(width, height), strokeWidth = 1f)

            nonNullSeries.forEachIndexed { sIndex, lineValues ->
                val color = colors[sIndex % colors.size]
                val stepX = if (lineValues.size > 1) width / (lineValues.size - 1) else width
                val path = Path()

                val points = lineValues.mapIndexed { index, value ->
                    val x = if (lineValues.size > 1) index * stepX else width / 2f
                    val normY = ((value - minVal) / range).toFloat().coerceIn(0f, 1f)
                    val y = height - (normY * (height - 10f)) - 5f
                    Offset(x, y)
                }

                if (points.isNotEmpty()) {
                    path.moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val cX = (prev.x + curr.x) / 2f
                        path.cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
                    }
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
                    )
                    points.forEach { pt ->
                        drawCircle(color = color, radius = 3.5.dp.toPx(), center = pt)
                    }
                }
            }
        }
    }
}
