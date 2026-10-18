package com.gymtracker.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

data class ChartDataPoint(
    val label: String,
    val value: Double,
    val timestamp: Long = 0L,
)

@Composable
fun LineChartCard(
    pointsData: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    unit: String = "kg",
    title: String? = null,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    var selectedPointIndex by remember(pointsData) { mutableStateOf<Int?>(null) }
    var showDataList by remember { mutableStateOf(false) }

    val cleanPoints = remember(pointsData) {
        pointsData.filter { !it.value.isNaN() && it.value >= 0.0 }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Optional Title & Accessible Toggle Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(Modifier.width(1.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showDataList = !showDataList },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = if (showDataList) "Hide accessible data list" else "Show accessible data list"
                        }
                ) {
                    Icon(
                        Icons.Default.ListAlt,
                        contentDescription = null,
                        tint = if (showDataList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        when {
            cleanPoints.isEmpty() -> {
                // NO DATA STATE
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "No data available for this selection",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Log your entries to start building trend analytics",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            cleanPoints.size == 1 -> {
                // SINGLE POINT STATE - explicitly avoiding trend line
                val single = cleanPoints.first()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = lineColor,
                            modifier = Modifier.size(16.dp)
                        ) {}
                        Text(
                            text = "Single entry: ${String.format(Locale.getDefault(), "%.1f %s", single.value, unit)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Logged on ${single.label}. Log at least 2 entries to display trend lines.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            else -> {
                // POPULATED SERIES CHART
                val minVal = remember(cleanPoints) { cleanPoints.minOf { it.value } }
                val maxVal = remember(cleanPoints) { cleanPoints.maxOf { it.value } }
                val valRange = if (maxVal > minVal) maxVal - minVal else 1.0

                val activeIndex = selectedPointIndex ?: (cleanPoints.size - 1)
                val activePoint = cleanPoints.getOrNull(activeIndex) ?: cleanPoints.last()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Selected-Value Overlay Details Header
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = activePoint.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "%.1f %s", activePoint.value, unit),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = lineColor
                                    )
                                }
                                Text(
                                    text = "Tap points to inspect",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Canvas Chart
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(horizontal = 8.dp, vertical = 12.dp)
                                    .pointerInput(cleanPoints) {
                                        detectTapGestures { tapOffset ->
                                            val stepX = if (cleanPoints.size > 1) size.width / (cleanPoints.size - 1) else size.width
                                            val closestIndex = (tapOffset.x / stepX).toInt().coerceIn(0, cleanPoints.size - 1)
                                            selectedPointIndex = closestIndex
                                        }
                                    }
                            ) {
                                val w = size.width
                                val h = size.height
                                val stepX = w / (cleanPoints.size - 1)

                                // Horizontal reference lines
                                val gridColor = Color.Gray.copy(alpha = 0.2f)
                                drawLine(gridColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                                drawLine(gridColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)
                                drawLine(gridColor, Offset(0f, h), Offset(w, h), strokeWidth = 1f)

                                val calculatedPoints = cleanPoints.mapIndexed { index, pt ->
                                    val x = index * stepX
                                    val normY = ((pt.value - minVal) / valRange).toFloat().coerceIn(0f, 1f)
                                    val y = h - (normY * (h - 16f)) - 8f
                                    Offset(x, y)
                                }

                                val strokePath = Path()
                                val fillPath = Path()

                                strokePath.moveTo(calculatedPoints.first().x, calculatedPoints.first().y)
                                fillPath.moveTo(calculatedPoints.first().x, h)
                                fillPath.lineTo(calculatedPoints.first().x, calculatedPoints.first().y)

                                for (i in 1 until calculatedPoints.size) {
                                    val prev = calculatedPoints[i - 1]
                                    val curr = calculatedPoints[i]
                                    val cX = (prev.x + curr.x) / 2f
                                    strokePath.cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
                                    fillPath.cubicTo(cX, prev.y, cX, curr.y, curr.x, curr.y)
                                }

                                fillPath.lineTo(calculatedPoints.last().x, h)
                                fillPath.close()

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(lineColor.copy(alpha = 0.30f), lineColor.copy(alpha = 0.02f)),
                                        startY = 0f,
                                        endY = h
                                    )
                                )

                                drawPath(
                                    path = strokePath,
                                    color = lineColor,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )

                                calculatedPoints.forEachIndexed { idx, point ->
                                    val isSelected = idx == activeIndex
                                    val radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx()
                                    drawCircle(color = lineColor, radius = radius, center = point)
                                    drawCircle(color = Color.White, radius = radius / 2f, center = point)

                                    if (isSelected) {
                                        // Vertical indicator line
                                        drawLine(
                                            color = lineColor.copy(alpha = 0.5f),
                                            start = Offset(point.x, 0f),
                                            end = Offset(point.x, h),
                                            strokeWidth = 1.5.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Accessible Data List Table
        AnimatedVisibility(visible = showDataList) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Accessible Data Table (${cleanPoints.size} entries)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    cleanPoints.forEach { pt ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(pt.label, style = MaterialTheme.typography.bodySmall)
                            Text(
                                String.format(Locale.getDefault(), "%.1f %s", pt.value, unit),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LineChartCard(
    values: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    val pointsData = remember(values) {
        values.mapIndexed { idx, valNum ->
            ChartDataPoint(label = "Point ${idx + 1}", value = valNum)
        }
    }
    LineChartCard(
        pointsData = pointsData,
        modifier = modifier,
        unit = "kg",
        lineColor = lineColor
    )
}

@Composable
fun MultiLineChartCard(
    series: List<List<Double>>,
    modifier: Modifier = Modifier,
) {
    val nonNullSeries = remember(series) { series.map { s -> s.filter { !it.isNaN() && it >= 0.0 } }.filter { it.isNotEmpty() } }
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
                text = "Log data to view comparison trends",
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
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
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
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            pathEffect = if (sIndex % 2 == 1) PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f) else null
                        ),
                    )
                    points.forEach { pt ->
                        drawCircle(color = color, radius = 3.5.dp.toPx(), center = pt)
                    }
                }
            }
        }
    }
}
