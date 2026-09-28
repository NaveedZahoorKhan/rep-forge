package com.gymtracker.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.WeightLogEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeightLogChartCard(
    weights: List<WeightLogEntity>,
    unitSystem: UnitSystem = UnitSystem.METRIC,
    onAddWeight: (Double) -> Unit,
    onDeleteWeight: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val unitLabel = if (unitSystem == UnitSystem.METRIC) "kg" else "lb"
    val multiplier = if (unitSystem == UnitSystem.METRIC) 1.0 else 2.20462

    var selectedTimeframe by remember { mutableStateOf("All Time") }
    val timeframes = listOf("7D", "30D", "90D", "All Time")

    var showAddForm by remember { mutableStateOf(false) }
    var inputWeightStr by remember { mutableStateOf("") }
    var showHistoryList by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val dayMs = 24L * 60 * 60 * 1000

    val filteredWeights = remember(weights, selectedTimeframe) {
        val sorted = weights.sortedBy { it.loggedAt }
        when (selectedTimeframe) {
            "7D" -> sorted.filter { it.loggedAt >= now - (7 * dayMs) }
            "30D" -> sorted.filter { it.loggedAt >= now - (30 * dayMs) }
            "90D" -> sorted.filter { it.loggedAt >= now - (90 * dayMs) }
            else -> sorted
        }
    }

    val latestLog = weights.maxByOrNull { it.loggedAt }
    val firstLog = weights.minByOrNull { it.loggedAt }

    val currentDisplayW = latestLog?.let { it.weightKg * multiplier } ?: 75.0
    val startDisplayW = firstLog?.let { it.weightKg * multiplier } ?: currentDisplayW
    val deltaW = if (weights.size >= 2) currentDisplayW - startDisplayW else 0.0

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Title & Quick Log button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.MonitorWeight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Weight Tracker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Daily check-ins & 7-day trend",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        if (inputWeightStr.isBlank()) {
                            inputWeightStr = String.format(Locale.getDefault(), "%.1f", currentDisplayW)
                        }
                        showAddForm = !showAddForm
                    },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log Weight", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Inline Add Form
            AnimatedVisibility(visible = showAddForm) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputWeightStr,
                            onValueChange = { inputWeightStr = it },
                            label = { Text("Weight ($unitLabel)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val entered = inputWeightStr.toDoubleOrNull()
                                if (entered != null && entered > 0.0) {
                                    val weightInKg = if (unitSystem == UnitSystem.METRIC) entered else entered / 2.20462
                                    onAddWeight(weightInKg)
                                    showAddForm = false
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            }

            // Stats summary pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            String.format(Locale.getDefault(), "%.1f %s", currentDisplayW, unitLabel),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text("7-Day Avg", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val avgVal = latestLog?.movingAverageKg?.times(multiplier) ?: currentDisplayW
                        Text(
                            String.format(Locale.getDefault(), "%.1f %s", avgVal, unitLabel),
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Text("Total Change", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (deltaW < -0.1) {
                                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            } else if (deltaW > 0.1) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            }
                            Text(
                                String.format(Locale.getDefault(), "%+.1f %s", deltaW, unitLabel),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (deltaW <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Timeframe Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                timeframes.forEach { tf ->
                    FilterChip(
                        selected = selectedTimeframe == tf,
                        onClick = { selectedTimeframe = tf },
                        label = { Text(tf, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }
            }

            // Chart Canvas
            val displayPoints = remember(filteredWeights, multiplier) {
                filteredWeights.map { it.weightKg * multiplier }
            }
            val avgPoints = remember(filteredWeights, multiplier) {
                filteredWeights.mapNotNull { it.movingAverageKg?.times(multiplier) }
            }

            if (displayPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("No weigh-ins in this timeframe", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Log your weight daily to see your progress curve", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val minW = displayPoints.minOrNull() ?: 50.0
                val maxW = displayPoints.maxOrNull() ?: 100.0
                val paddingVal = if (maxW == minW) 2.0 else (maxW - minW) * 0.15
                val chartMin = (minW - paddingVal).coerceAtLeast(0.0)
                val chartMax = maxW + paddingVal
                val chartRange = if (chartMax > chartMin) chartMax - chartMin else 1.0

                val primaryColor = MaterialTheme.colorScheme.primary
                val tertiaryColor = MaterialTheme.colorScheme.tertiary

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(top = 10.dp, bottom = 10.dp, start = 12.dp, end = 48.dp)
                    ) {
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val w = size.width
                            val h = size.height
                            val gridColor = Color.Gray.copy(alpha = 0.2f)

                            // 3 Horizontal Guide Lines
                            drawLine(gridColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                            drawLine(gridColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)
                            drawLine(gridColor, Offset(0f, h), Offset(w, h), strokeWidth = 1f)

                            val stepX = if (displayPoints.size > 1) w / (displayPoints.size - 1) else w

                            // Daily weight points & smooth curve
                            val pts = displayPoints.mapIndexed { idx, value ->
                                val x = if (displayPoints.size > 1) idx * stepX else w / 2f
                                val normY = ((value - chartMin) / chartRange).toFloat().coerceIn(0f, 1f)
                                val y = h - (normY * (h - 16f)) - 8f
                                Offset(x, y)
                            }

                            if (pts.isNotEmpty()) {
                                val strokePath = Path()
                                val fillPath = Path()

                                strokePath.moveTo(pts.first().x, pts.first().y)
                                fillPath.moveTo(pts.first().x, h)
                                fillPath.lineTo(pts.first().x, pts.first().y)

                                for (i in 1 until pts.size) {
                                    val prev = pts[i - 1]
                                    val curr = pts[i]
                                    val midX = (prev.x + curr.x) / 2f
                                    strokePath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                                    fillPath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                                }

                                fillPath.lineTo(pts.last().x, h)
                                fillPath.close()

                                // Area gradient under curve
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(primaryColor.copy(alpha = 0.35f), primaryColor.copy(alpha = 0.02f)),
                                        startY = 0f,
                                        endY = h
                                    )
                                )

                                // Solid primary line
                                drawPath(
                                    path = strokePath,
                                    color = primaryColor,
                                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                )

                                // Points
                                pts.forEach { p ->
                                    drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = p)
                                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = p)
                                }
                            }

                            // Moving average secondary trend line
                            if (avgPoints.size >= 2) {
                                val avgStepX = w / (avgPoints.size - 1)
                                val avgPts = avgPoints.mapIndexed { idx, value ->
                                    val x = idx * avgStepX
                                    val normY = ((value - chartMin) / chartRange).toFloat().coerceIn(0f, 1f)
                                    val y = h - (normY * (h - 16f)) - 8f
                                    Offset(x, y)
                                }
                                val avgPath = Path()
                                avgPath.moveTo(avgPts.first().x, avgPts.first().y)
                                for (i in 1 until avgPts.size) {
                                    val prev = avgPts[i - 1]
                                    val curr = avgPts[i]
                                    val midX = (prev.x + curr.x) / 2f
                                    avgPath.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                                }
                                drawPath(
                                    path = avgPath,
                                    color = tertiaryColor,
                                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        // Right-aligned Y-axis Labels
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .height(180.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                String.format(Locale.getDefault(), "%.1f", chartMax),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                String.format(Locale.getDefault(), "%.1f", (chartMax + chartMin) / 2.0),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                String.format(Locale.getDefault(), "%.1f", chartMin),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(primaryColor))
                        Spacer(Modifier.width(4.dp))
                        Text("Daily Log", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
                        Spacer(Modifier.width(16.dp))
                        Box(Modifier.size(10.dp).clip(CircleShape).background(tertiaryColor))
                        Spacer(Modifier.width(4.dp))
                        Text("7-Day Avg", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
                    }
                }
            }

            // History Expandable Accordion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHistoryList = !showHistoryList }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "History (${weights.size} entries)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showHistoryList = !showHistoryList }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (showHistoryList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle History"
                    )
                }
            }

            AnimatedVisibility(visible = showHistoryList) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val recentSorted = weights.sortedByDescending { it.loggedAt }
                    if (recentSorted.isEmpty()) {
                        Text("No entries recorded yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        recentSorted.take(10).forEach { item ->
                            val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(item.loggedAt))
                            val valDisplay = item.weightKg * multiplier
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            String.format(Locale.getDefault(), "%.1f %s", valDisplay, unitLabel),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (onDeleteWeight != null) {
                                        IconButton(onClick = { onDeleteWeight(item.id) }, modifier = Modifier.size(28.dp)) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete weight log",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
