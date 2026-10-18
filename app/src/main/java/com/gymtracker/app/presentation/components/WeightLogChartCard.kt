package com.gymtracker.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
    val timeframes = listOf("7D", "30D", "90D", "1Y", "All Time")

    var showAddForm by remember { mutableStateOf(false) }
    var inputWeightStr by remember { mutableStateOf("") }
    var showHistoryList by remember { mutableStateOf(false) }
    var showDataList by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<WeightLogEntity?>(null) }
    var selectedIndex by remember(weights, selectedTimeframe) { mutableStateOf<Int?>(null) }

    val now = System.currentTimeMillis()
    val dayMs = 24L * 60 * 60 * 1000

    val filteredWeights = remember(weights, selectedTimeframe) {
        val sorted = weights.sortedBy { it.loggedAt }
        when (selectedTimeframe) {
            "7D" -> sorted.filter { it.loggedAt >= now - (7 * dayMs) }
            "30D" -> sorted.filter { it.loggedAt >= now - (30 * dayMs) }
            "90D" -> sorted.filter { it.loggedAt >= now - (90 * dayMs) }
            "1Y" -> sorted.filter { it.loggedAt >= now - (365 * dayMs) }
            else -> sorted
        }
    }

    val latestLog = filteredWeights.maxByOrNull { it.loggedAt } ?: weights.maxByOrNull { it.loggedAt }
    val firstLog = filteredWeights.minByOrNull { it.loggedAt }

    val currentDisplayW = latestLog?.let { it.weightKg * multiplier } ?: 75.0
    val startDisplayW = firstLog?.let { it.weightKg * multiplier } ?: currentDisplayW
    val deltaW = if (filteredWeights.size >= 2) currentDisplayW - startDisplayW else 0.0

    val dateRangeText = remember(filteredWeights) {
        if (filteredWeights.isEmpty()) {
            "No data in selected timeframe"
        } else {
            val startStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(filteredWeights.first().loggedAt))
            val endStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(filteredWeights.last().loggedAt))
            if (filteredWeights.size == 1) startStr else "$startStr - $endStr"
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Title & Quick Actions
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
                            text = "Body Weight Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateRangeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showDataList = !showDataList },
                        modifier = Modifier
                            .size(36.dp)
                            .semantics {
                                role = Role.Button
                                contentDescription = if (showDataList) "Hide weight data list" else "Show weight data list"
                            }
                    ) {
                        Icon(
                            Icons.Default.ListAlt,
                            contentDescription = null,
                            tint = if (showDataList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

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
                        Text("Latest", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Period Change", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (deltaW < -0.1) {
                                Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            } else if (deltaW > 0.1) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
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
                        label = { Text(tf, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.semantics {
                            role = Role.Button
                            contentDescription = "Select timeframe $tf"
                        }
                    )
                }
            }

            // Chart Canvas & States
            val displayPoints = remember(filteredWeights, multiplier) {
                filteredWeights.map { it.weightKg * multiplier }
            }
            val avgPoints = remember(filteredWeights, multiplier) {
                filteredWeights.mapNotNull { it.movingAverageKg?.times(multiplier) }
            }

            when {
                filteredWeights.isEmpty() -> {
                    // NO DATA STATE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                            Text("No weight logs in this period ($selectedTimeframe)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Tap 'Log Weight' to add a weigh-in", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                filteredWeights.size == 1 -> {
                    // SINGLE POINT STATE - No trend line implied
                    val single = filteredWeights.first()
                    val valDisplay = single.weightKg * multiplier
                    val dateStr = SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(single.loggedAt))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) {}
                            Text(
                                String.format(Locale.getDefault(), "1 entry recorded: %.1f %s", valDisplay, unitLabel),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Logged on $dateStr. Add at least two weigh-ins over time to render a trend curve.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    // POPULATED TREND CHART
                    val minW = displayPoints.minOrNull() ?: 50.0
                    val maxW = displayPoints.maxOrNull() ?: 100.0
                    val paddingVal = if (maxW == minW) 2.0 else (maxW - minW) * 0.15
                    val chartMin = (minW - paddingVal).coerceAtLeast(0.0)
                    val chartMax = maxW + paddingVal
                    val chartRange = if (chartMax > chartMin) chartMax - chartMin else 1.0

                    val primaryColor = MaterialTheme.colorScheme.primary
                    val tertiaryColor = MaterialTheme.colorScheme.tertiary

                    val activeIdx = selectedIndex ?: (filteredWeights.size - 1)
                    val activeWeightLog = filteredWeights.getOrNull(activeIdx) ?: filteredWeights.last()
                    val activeVal = activeWeightLog.weightKg * multiplier
                    val activeDateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(activeWeightLog.loggedAt))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Selected-Value Overlay Banner
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                                        text = activeDateStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.getDefault(), "Selected: %.1f %s", activeVal, unitLabel),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryColor
                                    )
                                }
                                Text(
                                    text = "Tap point to detail",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Chart Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .padding(top = 10.dp, bottom = 10.dp, start = 12.dp, end = 48.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .matchParentSize()
                                    .pointerInput(filteredWeights) {
                                        detectTapGestures { tapOffset ->
                                            val stepX = if (displayPoints.size > 1) size.width / (displayPoints.size - 1) else size.width
                                            val closestIndex = (tapOffset.x / stepX).toInt().coerceIn(0, filteredWeights.size - 1)
                                            selectedIndex = closestIndex
                                        }
                                    }
                            ) {
                                val w = size.width
                                val h = size.height
                                val gridColor = Color.Gray.copy(alpha = 0.2f)

                                // Horizontal Guide Lines
                                drawLine(gridColor, Offset(0f, 0f), Offset(w, 0f), strokeWidth = 1f)
                                drawLine(gridColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)
                                drawLine(gridColor, Offset(0f, h), Offset(w, h), strokeWidth = 1f)

                                val stepX = if (displayPoints.size > 1) w / (displayPoints.size - 1) else w

                                // Daily weight points
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

                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(primaryColor.copy(alpha = 0.35f), primaryColor.copy(alpha = 0.02f)),
                                            startY = 0f,
                                            endY = h
                                        )
                                    )

                                    drawPath(
                                        path = strokePath,
                                        color = primaryColor,
                                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    pts.forEachIndexed { idx, p ->
                                        val isSel = idx == activeIdx
                                        val radius = if (isSel) 6.dp.toPx() else 4.dp.toPx()
                                        drawCircle(color = primaryColor, radius = radius, center = p)
                                        drawCircle(color = Color.White, radius = radius / 2f, center = p)

                                        if (isSel) {
                                            drawLine(
                                                color = primaryColor.copy(alpha = 0.5f),
                                                start = Offset(p.x, 0f),
                                                end = Offset(p.x, h),
                                                strokeWidth = 1.5.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                            )
                                        }
                                    }
                                }

                                // Moving average dashed trend line
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
                                        style = Stroke(
                                            width = 2.dp.toPx(),
                                            cap = StrokeCap.Round,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                                        )
                                    )
                                }
                            }

                            // Y-axis Labels
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .height(170.dp),
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

                        // Legend with color & line style
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(primaryColor))
                            Spacer(Modifier.width(4.dp))
                            Text("Daily Weigh-in (Solid)", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
                            Spacer(Modifier.width(16.dp))
                            Box(Modifier.size(10.dp).clip(CircleShape).background(tertiaryColor))
                            Spacer(Modifier.width(4.dp))
                            Text("7-Day Moving Avg (Dashed)", style = MaterialTheme.typography.labelSmall, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Accessible Data List
            AnimatedVisibility(visible = showDataList) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Accessible Weight Entries (${filteredWeights.size} items)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        filteredWeights.sortedByDescending { it.loggedAt }.forEach { log ->
                            val dStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(log.loggedAt))
                            val valDisp = log.weightKg * multiplier
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(dStr, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    String.format(Locale.getDefault(), "%.1f %s", valDisp, unitLabel),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // History Accordion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHistoryList = !showHistoryList }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Log History (${weights.size} total entries)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { showHistoryList = !showHistoryList },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        if (showHistoryList) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle weigh-in history list"
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
                                        IconButton(
                                            onClick = { pendingDelete = item },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete weight log entry",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
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

    // Confirmation Dialog
    val itemToDelete = pendingDelete
    if (itemToDelete != null && onDeleteWeight != null) {
        val weight = String.format(Locale.getDefault(), "%.1f %s", itemToDelete.weightKg * multiplier, unitLabel)
        val date = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(itemToDelete.loggedAt))
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete weight entry?") },
            text = { Text("Delete the $weight entry from $date? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteWeight(itemToDelete.id)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete Entry")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}
