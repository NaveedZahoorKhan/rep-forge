package com.gymtracker.app.presentation.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.domain.model.ExerciseVolumePoint
import com.gymtracker.app.domain.model.ExerciseVolumeSummary
import com.gymtracker.app.domain.model.PastExerciseDraft
import com.gymtracker.app.domain.model.PastSetDraft
import com.gymtracker.app.domain.model.PastWorkoutLogDraft
import com.gymtracker.app.domain.model.WorkoutSessionSummary
import com.gymtracker.app.presentation.components.EmptyState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    onNavigateBack: (() -> Unit)? = null,
    viewModel: WorkoutHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var sessionToDelete by remember { mutableStateOf<WorkoutSessionSummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Workout History Log",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${state.totalWorkoutsCount} sessions • ${state.totalVolumeAllTimeKg.toInt()} kg total volume",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setShowAddDialog(true) },
                        modifier = Modifier.testTag("log_past_workout_button"),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Log Past Workout", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.setShowAddDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_past_workout"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Log Workout", fontWeight = FontWeight.SemiBold)
                }
            }
        },
    ) { padding ->
        WorkoutHistoryContent(
            state = state,
            onSetTimeFilter = viewModel::setTimeFilter,
            onSetViewMode = viewModel::setViewMode,
            onSetSearchQuery = viewModel::setSearchQuery,
            onSelectExercise = viewModel::selectExercise,
            onToggleSessionExpanded = viewModel::toggleSessionExpanded,
            onDeleteSessionRequest = { sessionToDelete = it },
            onOpenAddDialog = { viewModel.setShowAddDialog(true) },
            modifier = Modifier.padding(padding),
        )
    }

    // Confirmation dialog for deleting session
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Workout Session?") },
            text = {
                Text("Are you sure you want to remove '${session.session.workoutName}' completed on ${formatCompletionDate(session.completedAt)}? This will remove its recorded exercise volume from your history.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(session.session.id)
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancel") }
            },
        )
    }

    if (state.showAddPastWorkoutDialog) {
        AddPastWorkoutDialog(
            exercises = state.exercises,
            onDismiss = { viewModel.setShowAddDialog(false) },
            onConfirm = { draft -> viewModel.logPastWorkout(draft) },
        )
    }
}

@Composable
fun WorkoutHistoryContent(
    state: WorkoutHistoryUiState,
    onSetTimeFilter: (TimeRangeFilter) -> Unit,
    onSetViewMode: (HistoryViewMode) -> Unit,
    onSetSearchQuery: (String) -> Unit,
    onSelectExercise: (String) -> Unit,
    onToggleSessionExpanded: (String) -> Unit,
    onDeleteSessionRequest: (WorkoutSessionSummary) -> Unit,
    onOpenAddDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp),
    ) {
        // High-level Stats Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                HistoryMetricCard(
                    title = "Workouts",
                    value = "${state.totalWorkoutsCount}",
                    subtitle = "Logged in Room",
                    icon = Icons.Default.History,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                HistoryMetricCard(
                    title = "Total Volume",
                    value = "${state.totalVolumeAllTimeKg.toInt()} kg",
                    subtitle = "Weight x Reps",
                    icon = Icons.Default.FitnessCenter,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                HistoryMetricCard(
                    title = "Avg / Session",
                    value = "${state.averageVolumePerWorkoutKg.toInt()} kg",
                    subtitle = "Average volume",
                    icon = Icons.Default.Timeline,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // View Mode Switch Tabs (Session Logs vs Exercise Volume Over Time)
        item {
            TabRow(
                selectedTabIndex = if (state.viewMode == HistoryViewMode.SESSIONS) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.clip(RoundedCornerShape(12.dp)),
            ) {
                Tab(
                    selected = state.viewMode == HistoryViewMode.SESSIONS,
                    onClick = { onSetViewMode(HistoryViewMode.SESSIONS) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Session Logs (${state.filteredSummaries.size})", fontWeight = FontWeight.SemiBold)
                        }
                    },
                )
                Tab(
                    selected = state.viewMode == HistoryViewMode.EXERCISE_VOLUME_TREND,
                    onClick = { onSetViewMode(HistoryViewMode.EXERCISE_VOLUME_TREND) },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Default.Timeline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Volume Over Time", fontWeight = FontWeight.SemiBold)
                        }
                    },
                )
            }
        }

        // Time Range Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Filter:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TimeRangeFilter.entries) { filter ->
                        FilterChip(
                            selected = state.timeFilter == filter,
                            onClick = { onSetTimeFilter(filter) },
                            label = { Text(filter.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        )
                    }
                }
            }
        }

        // Content based on View Mode
        if (state.viewMode == HistoryViewMode.SESSIONS) {
            // Search Input
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSetSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search by workout or exercise...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSetSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                )
            }

            // Session Volume Progression Chart
            if (state.filteredSummaries.isNotEmpty()) {
                item {
                    OverallVolumeTrendCard(summaries = state.filteredSummaries)
                }
            }

            if (state.filteredSummaries.isEmpty()) {
                item {
                    EmptyState(
                        title = "No workout sessions found",
                        detail = if (state.searchQuery.isNotBlank()) "No sessions matching '${state.searchQuery}'" else "Complete a workout or tap '+ Log Workout' to save your first session to Room.",
                    )
                }
            } else {
                item {
                    Text(
                        "Past Completed Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }

                items(state.filteredSummaries, key = { it.session.id }) { summary ->
                    val isExpanded = state.expandedSessionId == summary.session.id
                    PastSessionSummaryCard(
                        summary = summary,
                        isExpanded = isExpanded,
                        onToggleExpand = { onToggleSessionExpanded(summary.session.id) },
                        onDelete = { onDeleteSessionRequest(summary) },
                    )
                }
            }
        } else {
            // EXERCISE VOLUME TREND VIEW
            item {
                Text(
                    "Exercise Volume Over Time",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Exercise Selector Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Select exercise to view volume progression across sessions:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.exercises, key = { it.id }) { exercise ->
                            FilterChip(
                                selected = exercise.id == state.selectedExerciseId,
                                onClick = { onSelectExercise(exercise.id) },
                                label = { Text(exercise.name, fontSize = 12.sp) },
                                leadingIcon = {
                                    if (exercise.id == state.selectedExerciseId) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Exercise Volume Trend Section
            val currentExercise = state.exercises.firstOrNull { it.id == state.selectedExerciseId }
            item {
                ExerciseVolumeTrendCard(
                    exercise = currentExercise,
                    points = state.exerciseVolumePoints,
                )
            }

            if (state.exerciseVolumePoints.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Text(
                                "No history recorded for ${currentExercise?.name ?: "this exercise"}",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "Perform this exercise in an active session or log it using 'Log Workout' to track volume over time.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "History Table (${state.exerciseVolumePoints.size} entries)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                items(state.exerciseVolumePoints.reversed(), key = { "${it.sessionId}:${it.dateEpochMilli}" }) { point ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(point.workoutName, fontWeight = FontWeight.SemiBold)
                                Text(
                                    formatCompletionDate(point.dateEpochMilli),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "${point.setsCount} sets • Max: ${point.maxWeightKg.toInt()} kg",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    "${point.volumeKg.toInt()} kg",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
fun PastSessionSummaryCard(
    summary: WorkoutSessionSummary,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDelete: () -> Unit,
) {
    val completionDateStr = remember(summary.completedAt) { formatCompletionDate(summary.completedAt) }
    val relativeDateStr = remember(summary.completedAt) { formatRelativeDate(summary.completedAt) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header with completion date, day badge, and delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            relativeDateStr.uppercase(Locale.getDefault()),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Column {
                        Text(
                            summary.session.workoutName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            completionDateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Session",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Key Metrics Pill Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Volume", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${summary.totalVolumeKg.toInt()} kg", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(summary.durationFormatted, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Sets / Reps", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${summary.completedSetsCount}s • ${summary.totalRepsCount}r", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Star Rating and Notes if present
            if (summary.session.rating != null || summary.session.notes.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    summary.session.rating?.let { rating ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(rating) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (summary.session.notes.isNotBlank()) {
                        Text(
                            summary.session.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Toggle Expand Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isExpanded) "Hide Exercise Volume Details" else "View ${summary.exerciseSummaries.size} Exercises & Set Details",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            // Expandable Exercise Breakdown
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        "Exercise Volume Breakdown",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    summary.exerciseSummaries.forEach { exSummary ->
                        ExerciseBreakdownRow(exSummary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseBreakdownRow(summary: ExerciseVolumeSummary) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(summary.exerciseName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "${summary.totalVolumeKg.toInt()} kg",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${summary.completedSets} completed sets • Max: ${summary.maxWeightKg.toInt()} kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${summary.totalReps} total reps",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Individual Set Badges
            if (summary.setsDetail.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    summary.setsDetail.forEach { set ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (set.isPr) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "S${set.setNumber}: ${set.weightKg.toInt()}kg × ${set.reps}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (set.isPr) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface,
                                )
                                if (set.isPr) {
                                    Text("PR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OverallVolumeTrendCard(summaries: List<WorkoutSessionSummary>) {
    val chronological = remember(summaries) { summaries.sortedBy { it.completedAt } }
    val volumes = remember(chronological) { chronological.map { it.totalVolumeKg } }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Session Volume Over Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Total volume lifted per session across completion dates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        "kg / session",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            InteractiveVolumeBarChart(
                items = chronological.map { summary ->
                    VolumeBarItem(
                        label = formatDateShort(summary.completedAt),
                        fullDate = formatCompletionDate(summary.completedAt),
                        title = summary.session.workoutName,
                        volumeKg = summary.totalVolumeKg,
                    )
                },
                modifier = Modifier.height(170.dp),
            )
        }
    }
}

@Composable
fun ExerciseVolumeTrendCard(
    exercise: ExerciseEntity?,
    points: List<ExerciseVolumePoint>,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        exercise?.name ?: "Exercise Volume Progression",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "${points.size} recorded sessions for this exercise",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                points.maxByOrNull { it.volumeKg }?.let { best ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            "Best: ${best.volumeKg.toInt()} kg",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }

            if (points.isNotEmpty()) {
                InteractiveVolumeBarChart(
                    items = points.map { pt ->
                        VolumeBarItem(
                            label = formatDateShort(pt.dateEpochMilli),
                            fullDate = formatCompletionDate(pt.dateEpochMilli),
                            title = pt.workoutName,
                            volumeKg = pt.volumeKg,
                        )
                    },
                    barColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.height(170.dp),
                )
            }
        }
    }
}

data class VolumeBarItem(
    val label: String,
    val fullDate: String,
    val title: String,
    val volumeKg: Double,
)

@Composable
fun InteractiveVolumeBarChart(
    items: List<VolumeBarItem>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (items.isEmpty()) return
    var selectedIndex by remember { mutableIntStateOf(items.lastIndex) }
    val maxVal = remember(items) { (items.maxOfOrNull { it.volumeKg } ?: 100.0).coerceAtLeast(10.0) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Selected Tooltip Header
        val selectedItem = items.getOrNull(selectedIndex) ?: items.last()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${selectedItem.fullDate} • ${selectedItem.title}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${selectedItem.volumeKg.toInt()} kg",
                fontWeight = FontWeight.Bold,
                color = barColor,
                fontSize = 12.sp,
            )
        }

        // Custom Canvas Chart with Bars and Trend Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val width = size.width
                val height = size.height
                val count = items.size
                val barWidth = (width / count * 0.55f).coerceIn(8f, 36f)
                val spacing = width / count

                // Draw background grid lines
                val gridColor = Color.Gray.copy(alpha = 0.15f)
                drawLine(gridColor, Offset(0f, 0f), Offset(width, 0f), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, height / 2f), Offset(width, height / 2f), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, height), Offset(width, height), strokeWidth = 1f)

                val points = mutableListOf<Offset>()

                items.forEachIndexed { index, item ->
                    val centerX = (index * spacing) + (spacing / 2f)
                    val normHeight = ((item.volumeKg / maxVal) * (height - 18f)).toFloat().coerceIn(4f, height - 10f)
                    val barTop = height - normHeight

                    val isSelected = index == selectedIndex
                    val currentBarColor = if (isSelected) barColor else barColor.copy(alpha = 0.55f)

                    // Draw rounded bar
                    drawRoundRect(
                        color = currentBarColor,
                        topLeft = Offset(centerX - (barWidth / 2f), barTop),
                        size = androidx.compose.ui.geometry.Size(barWidth, normHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )

                    points.add(Offset(centerX, barTop))
                }

                // Draw connecting trend line
                if (points.size > 1) {
                    val path = Path()
                    path.moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val p0 = points[i - 1]
                        val p1 = points[i]
                        val midX = (p0.x + p1.x) / 2f
                        path.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y)
                    }
                    drawPath(
                        path = path,
                        color = barColor.copy(alpha = 0.85f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    )
                }

                // Draw point dots
                points.forEachIndexed { i, pt ->
                    val isSelected = i == selectedIndex
                    drawCircle(
                        color = if (isSelected) barColor else Color.White,
                        radius = if (isSelected) 4.5.dp.toPx() else 3.dp.toPx(),
                        center = pt,
                    )
                    drawCircle(
                        color = barColor,
                        radius = if (isSelected) 2.5.dp.toPx() else 1.5.dp.toPx(),
                        center = pt,
                    )
                }
            }

            // Click overlays for each bar
            Row(modifier = Modifier.matchParentSize()) {
                items.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .clickable { selectedIndex = index }
                    )
                }
            }
        }

        // Date Labels Row below chart
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val step = if (items.size > 5) items.size / 4 else 1
            items.filterIndexed { index, _ -> index % step == 0 || index == items.lastIndex }.take(5).forEach { item ->
                Text(item.label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPastWorkoutDialog(
    exercises: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onConfirm: (PastWorkoutLogDraft) -> Unit,
) {
    var workoutName by remember { mutableStateOf("Custom Routine") }
    var daysAgo by remember { mutableIntStateOf(0) }
    var durationMinutes by remember { mutableStateOf("45") }
    var rating by remember { mutableIntStateOf(5) }
    var notes by remember { mutableStateOf("") }

    val selectedExercises = remember { mutableStateListOf<PastExerciseDraft>() }
    var exerciseDropdownExpanded by remember { mutableStateOf(false) }

    // Pre-populate with first exercise if none added
    if (selectedExercises.isEmpty() && exercises.isNotEmpty()) {
        val first = exercises.first()
        selectedExercises.add(
            PastExerciseDraft(
                exerciseId = first.id,
                exerciseName = first.name,
                sets = listOf(
                    PastSetDraft(weightKg = 60.0, reps = 10),
                    PastSetDraft(weightKg = 70.0, reps = 8),
                    PastSetDraft(weightKg = 75.0, reps = 6),
                ),
            )
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Log Past Workout to Room", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    OutlinedTextField(
                        value = workoutName,
                        onValueChange = { workoutName = it },
                        label = { Text("Workout Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                item {
                    Text("Completion Date:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf(0 to "Today", 1 to "Yesterday", 2 to "2d ago", 3 to "3d ago", 5 to "5d ago").forEach { (days, label) ->
                            FilterChip(
                                selected = daysAgo == days,
                                onClick = { daysAgo = days },
                                label = { Text(label, fontSize = 11.sp) },
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = durationMinutes,
                            onValueChange = { durationMinutes = it },
                            label = { Text("Duration (mins)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )

                        // Rating selector
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Rating", style = MaterialTheme.typography.labelSmall)
                            Row {
                                (1..5).forEach { star ->
                                    IconButton(
                                        onClick = { rating = star },
                                        modifier = Modifier.size(24.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "$star stars",
                                            tint = if (star <= rating) Color(0xFFFFB300) else Color.Gray.copy(alpha = 0.4f),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                    )
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Exercises & Sets", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        // Add exercise dropdown
                        ExposedDropdownMenuBox(
                            expanded = exerciseDropdownExpanded,
                            onExpandedChange = { exerciseDropdownExpanded = !exerciseDropdownExpanded },
                        ) {
                            OutlinedButton(
                                onClick = { exerciseDropdownExpanded = true },
                                modifier = Modifier.menuAnchor(),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Exercise", fontSize = 12.sp)
                            }
                            ExposedDropdownMenu(
                                expanded = exerciseDropdownExpanded,
                                onDismissRequest = { exerciseDropdownExpanded = false },
                            ) {
                                exercises.forEach { ex ->
                                    DropdownMenuItem(
                                        text = { Text(ex.name) },
                                        onClick = {
                                            selectedExercises.add(
                                                PastExerciseDraft(
                                                    exerciseId = ex.id,
                                                    exerciseName = ex.name,
                                                    sets = listOf(
                                                        PastSetDraft(weightKg = 50.0, reps = 10),
                                                        PastSetDraft(weightKg = 55.0, reps = 8),
                                                    ),
                                                )
                                            )
                                            exerciseDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                // List of added exercises with sets
                items(selectedExercises.size) { exIndex ->
                    val exerciseDraft = selectedExercises[exIndex]
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(exerciseDraft.exerciseName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(
                                    onClick = { selectedExercises.removeAt(exIndex) },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                }
                            }

                            exerciseDraft.sets.forEachIndexed { setIdx, set ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("Set ${setIdx + 1}", fontSize = 12.sp, modifier = Modifier.width(36.dp))
                                    OutlinedTextField(
                                        value = set.weightKg.toInt().toString(),
                                        onValueChange = { newVal ->
                                            val w = newVal.toDoubleOrNull() ?: set.weightKg
                                            val updatedSets = exerciseDraft.sets.toMutableList()
                                            updatedSets[setIdx] = set.copy(weightKg = w)
                                            selectedExercises[exIndex] = exerciseDraft.copy(sets = updatedSets)
                                        },
                                        label = { Text("Weight (kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                    )
                                    OutlinedTextField(
                                        value = set.reps.toString(),
                                        onValueChange = { newVal ->
                                            val r = newVal.toIntOrNull() ?: set.reps
                                            val updatedSets = exerciseDraft.sets.toMutableList()
                                            updatedSets[setIdx] = set.copy(reps = r)
                                            selectedExercises[exIndex] = exerciseDraft.copy(sets = updatedSets)
                                        },
                                        label = { Text("Reps") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                    )
                                }
                            }

                            TextButton(
                                onClick = {
                                    val last = exerciseDraft.sets.lastOrNull()
                                    val newSet = PastSetDraft(weightKg = last?.weightKg ?: 50.0, reps = last?.reps ?: 10)
                                    selectedExercises[exIndex] = exerciseDraft.copy(sets = exerciseDraft.sets + newSet)
                                },
                                contentPadding = PaddingValues(vertical = 2.dp),
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Set", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val oneDayMs = 24L * 60 * 60 * 1000
                    val targetTime = System.currentTimeMillis() - (daysAgo * oneDayMs)
                    val draft = PastWorkoutLogDraft(
                        workoutName = workoutName.ifBlank { "Workout Routine" },
                        dateEpochMilli = targetTime,
                        durationMinutes = durationMinutes.toLongOrNull() ?: 45L,
                        rating = rating,
                        notes = notes,
                        exercises = selectedExercises.toList(),
                    )
                    onConfirm(draft)
                },
                modifier = Modifier.testTag("dialog_confirm_log_workout"),
            ) {
                Text("Save to Room")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// Helpers for formatted dates
private fun formatCompletionDate(epochMilli: Long): String {
    val instant = Instant.ofEpochMilli(epochMilli)
    val zone = ZoneId.systemDefault()
    val localDate = instant.atZone(zone).toLocalDate()
    val now = LocalDate.now(zone)

    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    val timeStr = instant.atZone(zone).format(timeFormatter)

    return when {
        localDate.isEqual(now) -> "Today • $timeStr"
        localDate.isEqual(now.minusDays(1)) -> "Yesterday • $timeStr"
        else -> {
            val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault())
            "${localDate.format(dateFormatter)} • $timeStr"
        }
    }
}

private fun formatRelativeDate(epochMilli: Long): String {
    val instant = Instant.ofEpochMilli(epochMilli)
    val zone = ZoneId.systemDefault()
    val localDate = instant.atZone(zone).toLocalDate()
    val now = LocalDate.now(zone)

    val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(localDate, now)
    return when {
        daysDiff == 0L -> "Today"
        daysDiff == 1L -> "Yesterday"
        daysDiff < 7L -> "$daysDiff days ago"
        daysDiff < 30L -> "${daysDiff / 7}w ago"
        else -> "${daysDiff / 30}mo ago"
    }
}

private fun formatDateShort(epochMilli: Long): String {
    val instant = Instant.ofEpochMilli(epochMilli)
    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}
