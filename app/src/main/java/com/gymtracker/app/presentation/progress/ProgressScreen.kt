@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gymtracker.app.presentation.progress

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.gymtracker.app.data.local.entity.BodyMeasurementEntity
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.HealthSyncLogEntity
import com.gymtracker.app.data.local.entity.PersonalRecordEntity
import com.gymtracker.app.data.local.entity.ProgressPhotoEntity
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.model.ExerciseProgressPoint
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.StrengthStandardsUseCase
import com.gymtracker.app.presentation.components.ChartDataPoint
import com.gymtracker.app.presentation.components.EmptyState
import com.gymtracker.app.presentation.components.LineChartCard
import com.gymtracker.app.presentation.components.StreakHeatmap
import com.gymtracker.app.presentation.components.WeightLogChartCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class ProgressData(
    val profile: UserProfileEntity = UserProfileEntity(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val measurements: List<BodyMeasurementEntity> = emptyList(),
    val photos: List<ProgressPhotoEntity> = emptyList(),
    val records: List<PersonalRecordEntity> = emptyList(),
    val weights: List<WeightLogEntity> = emptyList(),
    val history: List<WorkoutSessionEntity> = emptyList(),
    val healthLogs: List<HealthSyncLogEntity> = emptyList(),
)

data class ProgressUiState(
    val profile: UserProfileEntity = UserProfileEntity(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val measurements: List<BodyMeasurementEntity> = emptyList(),
    val photos: List<ProgressPhotoEntity> = emptyList(),
    val records: List<PersonalRecordEntity> = emptyList(),
    val weights: List<WeightLogEntity> = emptyList(),
    val history: List<WorkoutSessionEntity> = emptyList(),
    val healthLogs: List<HealthSyncLogEntity> = emptyList(),
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repository: GymRepository,
) : ViewModel() {

    private val dataFlow = combine(
        repository.observeUserProfile(),
        repository.observeExercises(),
        repository.observeMeasurements(),
        repository.observeProgressPhotos(),
        repository.observePersonalRecords(),
        repository.observeWeightLogs(),
        repository.observeHistory(),
        repository.observeHealthSyncLogs(),
    ) { array: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        ProgressData(
            profile = array[0] as? UserProfileEntity ?: UserProfileEntity(),
            exercises = array[1] as? List<ExerciseEntity> ?: emptyList(),
            measurements = array[2] as? List<BodyMeasurementEntity> ?: emptyList(),
            photos = array[3] as? List<ProgressPhotoEntity> ?: emptyList(),
            records = array[4] as? List<PersonalRecordEntity> ?: emptyList(),
            weights = array[5] as? List<WeightLogEntity> ?: emptyList(),
            history = array[6] as? List<WorkoutSessionEntity> ?: emptyList(),
            healthLogs = array[7] as? List<HealthSyncLogEntity> ?: emptyList(),
        )
    }

    val state: StateFlow<ProgressUiState> = dataFlow.combine(MutableStateFlow(Unit)) { data, _ ->
        ProgressUiState(
            profile = data.profile,
            exercises = data.exercises,
            measurements = data.measurements,
            photos = data.photos,
            records = data.records,
            weights = data.weights,
            history = data.history,
            healthLogs = data.healthLogs,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    var selectedExerciseId by mutableStateOf<String?>(null)
        private set
    var progressPoints by mutableStateOf<List<ExerciseProgressPoint>>(emptyList())
        private set

    fun selectExercise(id: String) {
        selectedExerciseId = id
        viewModelScope.launch { progressPoints = repository.progressForExercise(id) }
    }

    fun addMeasurement(measurement: BodyMeasurementEntity) {
        viewModelScope.launch { repository.addMeasurement(measurement) }
    }

    fun deleteMeasurement(id: String) {
        viewModelScope.launch { repository.deleteMeasurement(id) }
    }

    fun addWeightLog(weightKg: Double) {
        viewModelScope.launch {
            repository.addWeightLog(
                WeightLogEntity(
                    weightKg = weightKg,
                    loggedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteWeightLog(id: String) {
        viewModelScope.launch { repository.deleteWeightLog(id) }
    }

    fun addPhoto(uri: Uri) {
        viewModelScope.launch { repository.addProgressPhoto(ProgressPhotoEntity(uri = uri.toString())) }
    }

    fun deletePhoto(id: String) {
        viewModelScope.launch { repository.deleteProgressPhoto(id) }
    }
}

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Strength", "Body", "Photos", "History")

    var showWeightDialog by remember { mutableStateOf(false) }
    var showMeasurementDialog by remember { mutableStateOf(false) }
    var editingMeasurement by remember { mutableStateOf<BodyMeasurementEntity?>(null) }

    var photoPermissionError by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            photoPermissionError = null
            viewModel.addPhoto(uri)
        } else {
            photoPermissionError = "Photo selection cancelled. Tap 'Add Photo' to try again."
        }
    }

    // Default select first popular exercise
    LaunchedEffect(state.exercises) {
        if (viewModel.selectedExerciseId == null && state.exercises.isNotEmpty()) {
            val popularLifts = listOf("Bench Press", "Squat", "Deadlift", "Overhead Press")
            val defaultExercise = state.exercises.firstOrNull { ex ->
                popularLifts.any { lift -> ex.name.contains(lift, ignoreCase = true) }
            } ?: state.exercises.first()
            viewModel.selectExercise(defaultExercise.id)
        }
    }

    val workoutDays = remember(state.history) {
        state.history.map {
            Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
        }.toSet()
    }

    val totalVolumeTons = remember(state.history) {
        state.history.sumOf { it.totalVolume } / 1000.0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Progress & Analytics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Track your consistency, 1RM strength gains, and body recomp trends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tab Navigation Bar
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            divider = {},
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.semantics {
                        role = Role.Tab
                        contentDescription = "Switch to $title tab"
                    }
                )
            }
        }

        // Tab Content Area
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> OverviewSection(
                    weights = state.weights,
                    history = state.history,
                    records = state.records,
                    photos = state.photos,
                    measurements = state.measurements,
                    unitSystem = state.profile.unitSystem,
                    onNavigateToTab = { tabIdx -> selectedTab = tabIdx },
                    onLogWeightClick = { showWeightDialog = true }
                )
                1 -> StrengthDrillInView(
                    exercises = state.exercises,
                    selectedExerciseId = viewModel.selectedExerciseId,
                    onSelectExercise = viewModel::selectExercise,
                    progressPoints = viewModel.progressPoints
                )
                2 -> BodyDrillInView(
                    weights = state.weights,
                    measurements = state.measurements,
                    unitSystem = state.profile.unitSystem,
                    onLogWeightClick = { showWeightDialog = true },
                    onAddMeasurementClick = { showMeasurementDialog = true },
                    onEditMeasurement = { m ->
                        editingMeasurement = m
                        showMeasurementDialog = true
                    },
                    onDeleteMeasurement = viewModel::deleteMeasurement,
                    onDeleteWeight = viewModel::deleteWeightLog
                )
                3 -> PhotosDrillInView(
                    photos = state.photos,
                    permissionError = photoPermissionError,
                    onAddPhoto = { photoPicker.launch("image/*") },
                    onDeletePhoto = viewModel::deletePhoto
                )
                4 -> HistoryDrillInView(
                    workoutDays = workoutDays,
                    history = state.history,
                    records = state.records,
                    totalVolumeTons = totalVolumeTons,
                    unitSystem = state.profile.unitSystem
                )
            }
        }
    }

    // Dialog for Logging Weight
    if (showWeightDialog) {
        LogWeightDialog(
            unitSystem = state.profile.unitSystem,
            onDismiss = { showWeightDialog = false },
            onConfirm = { weightKg ->
                viewModel.addWeightLog(weightKg)
                showWeightDialog = false
            }
        )
    }

    // Dialog for Adding / Editing Body Measurements
    if (showMeasurementDialog) {
        LogMeasurementDialog(
            unitSystem = state.profile.unitSystem,
            currentWeight = state.weights.maxByOrNull { it.loggedAt }?.weightKg ?: state.profile.weightKg,
            existingMeasurement = editingMeasurement,
            onDismiss = {
                showMeasurementDialog = false
                editingMeasurement = null
            },
            onSave = { measurement ->
                viewModel.addMeasurement(measurement)
                showMeasurementDialog = false
                editingMeasurement = null
            }
        )
    }
}

// =================================================================
// OVERVIEW TAB: Primary Trend + Range Controls + Drill-in Rows
// =================================================================
@Composable
private fun OverviewSection(
    weights: List<WeightLogEntity>,
    history: List<WorkoutSessionEntity>,
    records: List<PersonalRecordEntity>,
    photos: List<ProgressPhotoEntity>,
    measurements: List<BodyMeasurementEntity>,
    unitSystem: UnitSystem,
    onNavigateToTab: (Int) -> Unit,
    onLogWeightClick: () -> Unit,
) {
    var primaryMetric by remember { mutableStateOf("Body Weight") }
    var selectedTimeframe by remember { mutableStateOf("30D") }

    val unitLabel = if (unitSystem == UnitSystem.METRIC) "kg" else "lb"
    val multiplier = if (unitSystem == UnitSystem.METRIC) 1.0 else 2.20462

    val now = System.currentTimeMillis()
    val dayMs = 24L * 60 * 60 * 1000

    val timeFilteredWeights = remember(weights, selectedTimeframe) {
        val sorted = weights.sortedBy { it.loggedAt }
        when (selectedTimeframe) {
            "7D" -> sorted.filter { it.loggedAt >= now - (7 * dayMs) }
            "30D" -> sorted.filter { it.loggedAt >= now - (30 * dayMs) }
            "90D" -> sorted.filter { it.loggedAt >= now - (90 * dayMs) }
            "1Y" -> sorted.filter { it.loggedAt >= now - (365 * dayMs) }
            else -> sorted
        }
    }

    val timeFilteredHistory = remember(history, selectedTimeframe) {
        val sorted = history.sortedBy { it.startedAt }
        when (selectedTimeframe) {
            "7D" -> sorted.filter { it.startedAt >= now - (7 * dayMs) }
            "30D" -> sorted.filter { it.startedAt >= now - (30 * dayMs) }
            "90D" -> sorted.filter { it.startedAt >= now - (90 * dayMs) }
            "1Y" -> sorted.filter { it.startedAt >= now - (365 * dayMs) }
            else -> sorted
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Primary Metric Trend Showcase Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header & Switcher Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (primaryMetric == "Body Weight") Icons.Default.MonitorWeight else Icons.Default.ShowChart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Primary Trend: $primaryMetric",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Timeframe: $selectedTimeframe",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Switch metric toggle button
                        OutlinedButton(
                            onClick = {
                                primaryMetric = if (primaryMetric == "Body Weight") "Session Volume" else "Body Weight"
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                if (primaryMetric == "Body Weight") "View Volume" else "View Weight",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Period Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("7D", "30D", "90D", "1Y", "All Time").forEach { tf ->
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

                    // Render Selected Metric Chart
                    if (primaryMetric == "Body Weight") {
                        WeightLogChartCard(
                            weights = timeFilteredWeights,
                            unitSystem = unitSystem,
                            onAddWeight = { onLogWeightClick() },
                            onDeleteWeight = null
                        )
                    } else {
                        // Session Volume Chart
                        val volumePoints = remember(timeFilteredHistory) {
                            timeFilteredHistory.map { session ->
                                val dateStr = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(session.startedAt))
                                val volDisplay = session.totalVolume * (if (unitSystem == UnitSystem.METRIC) 1.0 else 2.20462)
                                ChartDataPoint(label = dateStr, value = volDisplay, timestamp = session.startedAt)
                            }
                        }

                        val totalVolTons = timeFilteredHistory.sumOf { it.totalVolume } / 1000.0

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total Volume ($selectedTimeframe)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        String.format(Locale.getDefault(), "%.1f tons", totalVolTons * (if (unitSystem == UnitSystem.METRIC) 1.0 else 1.10231)),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Completed Sessions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${timeFilteredHistory.size} sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        LineChartCard(
                            pointsData = volumePoints,
                            unit = if (unitSystem == UnitSystem.METRIC) "kg" else "lb",
                            title = "Session Tonnage Progression",
                            lineColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Drill-In Section Header
        item {
            Text(
                text = "Analytics Drill-Ins",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Strength Drill-In Row
        item {
            DrillInRowCard(
                icon = Icons.Default.FitnessCenter,
                title = "Strength & 1RM Gains",
                subtitle = "${records.size} PRs achieved • Epley 1RM formula analytics",
                badgeText = "${records.size} PRs",
                onClick = { onNavigateToTab(1) }
            )
        }

        // Body Drill-In Row
        item {
            val latestW = weights.maxByOrNull { it.loggedAt }?.let { it.weightKg * multiplier }
            val latestWText = if (latestW != null) String.format(Locale.getDefault(), "%.1f %s", latestW, unitLabel) else "No entries"
            DrillInRowCard(
                icon = Icons.Default.Straighten,
                title = "Body & Measurements",
                subtitle = "Latest Weight: $latestWText • ${measurements.size} body logs",
                badgeText = "${measurements.size} logs",
                onClick = { onNavigateToTab(2) }
            )
        }

        // Photos Drill-In Row
        item {
            DrillInRowCard(
                icon = Icons.Default.PhotoLibrary,
                title = "Transformation Gallery & Comparison",
                subtitle = "${photos.size} physique photos • Side-by-side photo comparison",
                badgeText = "${photos.size} photos",
                onClick = { onNavigateToTab(3) }
            )
        }

        // History Drill-In Row
        item {
            DrillInRowCard(
                icon = Icons.Default.History,
                title = "Workout History & Heatmap",
                subtitle = "${history.size} total sessions completed • 6-week consistency heatmap",
                badgeText = "${history.size} sessions",
                onClick = { onNavigateToTab(4) }
            )
        }
    }
}

@Composable
private fun DrillInRowCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .semantics {
                role = Role.Button
                contentDescription = "Open $title section"
            }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Column {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// =================================================================
// DRILL-IN 1: STRENGTH & 1RM
// =================================================================
@Composable
private fun StrengthDrillInView(
    exercises: List<ExerciseEntity>,
    selectedExerciseId: String?,
    onSelectExercise: (String) -> Unit,
    progressPoints: List<ExerciseProgressPoint>,
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredExercises = remember(searchQuery, exercises) {
        if (searchQuery.isBlank()) exercises else exercises.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val selectedExercise = exercises.firstOrNull { it.id == selectedExerciseId }
    val standards = selectedExercise?.let { StrengthStandardsUseCase.standardFor(it.name) }
    val maxOneRm = progressPoints.maxOfOrNull { it.estimatedOneRepMax } ?: 0.0
    val maxWeight = progressPoints.maxOfOrNull { it.weight } ?: 0.0
    val maxVolume = progressPoints.maxOfOrNull { it.volume } ?: 0.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        // Exercise Selector & Search Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select Exercise for 1RM Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    val popularLifts = listOf("Bench Press", "Squat", "Deadlift", "Overhead Press", "Barbell Row")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(popularLifts) { liftName ->
                            val match = exercises.firstOrNull { it.name.contains(liftName, ignoreCase = true) }
                            if (match != null) {
                                FilterChip(
                                    selected = match.id == selectedExerciseId,
                                    onClick = { onSelectExercise(match.id) },
                                    label = { Text(liftName, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search all exercises...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (searchQuery.isNotBlank() && filteredExercises.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(filteredExercises.take(10)) { ex ->
                                FilterChip(
                                    selected = ex.id == selectedExerciseId,
                                    onClick = {
                                        onSelectExercise(ex.id)
                                        searchQuery = ""
                                    },
                                    label = { Text(ex.name, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1RM Formula & Methodology Explanation Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Estimated 1RM Formula & Source",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Text(
                        text = "1RM = Weight × (1 + Reps / 30)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Calculated using the Epley formula derived from your completed sets for ${selectedExercise?.name ?: "the selected exercise"}. Standards are calibrated against bodyweight ratios.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Strength Stats Summary & Progression Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedExercise?.name ?: "Select an exercise",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Peak Estimated 1RM: ${maxOneRm.toInt()} kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        standards?.let { std ->
                            val level = std.levelFor(maxOneRm)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = level,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStat(label = "Peak Weight", value = "${maxWeight.toInt()} kg", modifier = Modifier.weight(1f))
                        MiniStat(label = "Peak 1RM", value = "${maxOneRm.toInt()} kg", modifier = Modifier.weight(1f))
                        MiniStat(label = "Peak Volume", value = "${maxVolume.toInt()} kg", modifier = Modifier.weight(1f))
                    }

                    val oneRmChartPoints = remember(progressPoints) {
                        progressPoints.mapIndexed { idx, pt ->
                            val dateStr = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(pt.timestamp))
                            ChartDataPoint(label = dateStr, value = pt.estimatedOneRepMax, timestamp = pt.timestamp)
                        }
                    }

                    LineChartCard(
                        pointsData = oneRmChartPoints,
                        unit = "kg",
                        title = "Estimated 1RM Trend (kg)",
                        lineColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// =================================================================
// DRILL-IN 2: BODY & MEASUREMENTS
// =================================================================
@Composable
private fun BodyDrillInView(
    weights: List<WeightLogEntity>,
    measurements: List<BodyMeasurementEntity>,
    unitSystem: UnitSystem,
    onLogWeightClick: () -> Unit,
    onAddMeasurementClick: () -> Unit,
    onEditMeasurement: (BodyMeasurementEntity) -> Unit,
    onDeleteMeasurement: (String) -> Unit,
    onDeleteWeight: (String) -> Unit,
) {
    val unitLabel = if (unitSystem == UnitSystem.METRIC) "kg" else "lb"
    val multiplier = if (unitSystem == UnitSystem.METRIC) 1.0 else 2.20462

    var pendingDeleteMeasurement by remember { mutableStateOf<BodyMeasurementEntity?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        // Weight Progression Chart
        item {
            WeightLogChartCard(
                weights = weights,
                unitSystem = unitSystem,
                onAddWeight = { onLogWeightClick() },
                onDeleteWeight = onDeleteWeight
            )
        }

        // Body Measurements Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Body Circumference & Fat Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Button(
                    onClick = onAddMeasurementClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Measurement", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (measurements.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EmptyState(
                        title = "No body measurements yet",
                        detail = "Tap 'Add Measurement' to record waist, chest, arms, and body fat percentages over time."
                    )
                }
            }
        } else {
            items(measurements.sortedByDescending { it.loggedAt }, key = { it.id }) { m ->
                val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(m.loggedAt))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(dateStr, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                m.bodyFatPercent?.let { bf ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            "$bf% Fat",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onEditMeasurement(m) }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit measurement", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { pendingDeleteMeasurement = m }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete measurement", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            m.weightKg?.let { w ->
                                Text("Weight: ${String.format(Locale.getDefault(), "%.1f", w * multiplier)} $unitLabel", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                            m.waistCm?.let { waist ->
                                Text("Waist: ${waist.toInt()} cm", style = MaterialTheme.typography.bodySmall)
                            }
                            m.chestCm?.let { chest ->
                                Text("Chest: ${chest.toInt()} cm", style = MaterialTheme.typography.bodySmall)
                            }
                            m.armsCm?.let { arms ->
                                Text("Arms: ${arms.toInt()} cm", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

    // Measurement Deletion Confirmation Dialog
    val toDelete = pendingDeleteMeasurement
    if (toDelete != null) {
        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(toDelete.loggedAt))
        AlertDialog(
            onDismissRequest = { pendingDeleteMeasurement = null },
            title = { Text("Delete Measurement Entry?") },
            text = { Text("Delete the body measurement log from $dateStr? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMeasurement(toDelete.id)
                        pendingDeleteMeasurement = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteMeasurement = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =================================================================
// DRILL-IN 3: PHOTOS & SIDE-BY-SIDE COMPARISON
// =================================================================
@Composable
private fun PhotosDrillInView(
    photos: List<ProgressPhotoEntity>,
    permissionError: String?,
    onAddPhoto: () -> Unit,
    onDeletePhoto: (String) -> Unit,
) {
    var selectedBeforePhotoId by remember(photos) { mutableStateOf<String?>(photos.lastOrNull()?.id) }
    var selectedAfterPhotoId by remember(photos) { mutableStateOf<String?>(photos.firstOrNull()?.id) }
    var pendingDeletePhoto by remember { mutableStateOf<ProgressPhotoEntity?>(null) }

    val beforePhoto = photos.firstOrNull { it.id == selectedBeforePhotoId } ?: photos.lastOrNull()
    val afterPhoto = photos.firstOrNull { it.id == selectedAfterPhotoId } ?: photos.firstOrNull()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        // Gallery Header & Add Photo Action
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Transformation Gallery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${photos.size} photos saved locally", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onAddPhoto,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Permission / Cancelled Feedback Banner
        if (permissionError != null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                        Text(permissionError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Side-by-Side Photo Comparison Section
        if (photos.size >= 2) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Side-by-Side Comparison", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Before Photo Frame
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "BEFORE: ${beforePhoto?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it.takenAt)) } ?: "Select"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (beforePhoto != null) {
                                    AsyncImage(
                                        model = beforePhoto.uri,
                                        contentDescription = "Before physique photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }

                            // After Photo Frame
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        "AFTER: ${afterPhoto?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it.takenAt)) } ?: "Select"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                if (afterPhoto != null) {
                                    AsyncImage(
                                        model = afterPhoto.uri,
                                        contentDescription = "After physique photo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(180.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Photo Grid List
        if (photos.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EmptyState(
                        title = "No progress photos saved",
                        detail = "Tap 'Add Photo' above to upload physique check-ins and visualize your muscle growth."
                    )
                }
            }
        } else {
            items(photos.sortedByDescending { it.takenAt }, key = { it.id }) { photo ->
                val dateStr = SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(photo.takenAt))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dateStr, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { pendingDeletePhoto = photo }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete photo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        // Photo Display with Recoverable Placeholder Error State
                        SubcomposeAsyncImage(
                            model = photo.uri,
                            contentDescription = "Physique progress photo taken on $dateStr",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.BrokenImage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Photo unavailable or deleted from storage", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Photo Deletion Confirmation Dialog
    val photoToDelete = pendingDeletePhoto
    if (photoToDelete != null) {
        val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(photoToDelete.takenAt))
        AlertDialog(
            onDismissRequest = { pendingDeletePhoto = null },
            title = { Text("Delete Progress Photo?") },
            text = { Text("Delete the progress photo taken on $dateStr? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePhoto(photoToDelete.id)
                        pendingDeletePhoto = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePhoto = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// =================================================================
// DRILL-IN 4: HISTORY & STREAK HEATMAP
// =================================================================
@Composable
private fun HistoryDrillInView(
    workoutDays: Set<LocalDate>,
    history: List<WorkoutSessionEntity>,
    records: List<PersonalRecordEntity>,
    totalVolumeTons: Double,
    unitSystem: UnitSystem,
) {
    val activeDays6Wks = remember(workoutDays) {
        val cutoff = LocalDate.now().minusDays(41)
        workoutDays.count { it >= cutoff }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        // Quick Stats Pill Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatMetricPill(
                    icon = Icons.Default.FitnessCenter,
                    value = "${history.size}",
                    label = "Workouts",
                    modifier = Modifier.weight(1f)
                )
                StatMetricPill(
                    icon = Icons.Default.ShowChart,
                    value = String.format(Locale.getDefault(), "%.1f t", totalVolumeTons),
                    label = "Volume",
                    modifier = Modifier.weight(1f)
                )
                StatMetricPill(
                    icon = Icons.Default.CalendarMonth,
                    value = "$activeDays6Wks d",
                    label = "Active (6 wk)",
                    modifier = Modifier.weight(1f)
                )
                StatMetricPill(
                    icon = Icons.Default.WorkspacePremium,
                    value = "${records.size}",
                    label = "PRs Set",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Streak Heatmap Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Workout Consistency (Last 6 Weeks)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$activeDays6Wks days active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    StreakHeatmap(workoutDays = workoutDays)
                }
            }
        }

        // Session History Cards
        item {
            Text("Completed Session History (${history.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (history.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EmptyState(
                        title = "No workout sessions completed yet",
                        detail = "Start a workout session from the Workouts tab to build your training history!"
                    )
                }
            }
        } else {
            items(history.sortedByDescending { it.startedAt }, key = { it.id }) { session ->
                val dateStr = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(Date(session.startedAt))
                val volDisplay = session.totalVolume * (if (unitSystem == UnitSystem.METRIC) 1.0 else 2.20462)
                val unitLabel = if (unitSystem == UnitSystem.METRIC) "kg" else "lb"
                val durationMins = (session.durationSeconds / 60).coerceAtLeast(1)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(session.workoutName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                String.format(Locale.getDefault(), "%.0f %s", volDisplay, unitLabel),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "$durationMins min",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// =================================================================
// HELPER DIALOGS & PILLS
// =================================================================
@Composable
private fun StatMetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LogWeightDialog(
    unitSystem: UnitSystem,
    onDismiss: () -> Unit,
    onConfirm: (weightKg: Double) -> Unit,
) {
    var weightInput by remember { mutableStateOf("75.0") }
    val unitLabel = if (unitSystem == UnitSystem.METRIC) "kg" else "lb"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Body Weight") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight ($unitLabel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val raw = weightInput.toDoubleOrNull() ?: 75.0
                    val weightKg = if (unitSystem == UnitSystem.METRIC) raw else raw / 2.20462
                    onConfirm(weightKg)
                }
            ) {
                Text("Save Weigh-in")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LogMeasurementDialog(
    unitSystem: UnitSystem,
    currentWeight: Double,
    existingMeasurement: BodyMeasurementEntity? = null,
    onDismiss: () -> Unit,
    onSave: (BodyMeasurementEntity) -> Unit,
) {
    val unitLabel = if (unitSystem == UnitSystem.METRIC) "kg" else "lb"
    val displayW = if (unitSystem == UnitSystem.METRIC) {
        existingMeasurement?.weightKg ?: currentWeight
    } else {
        (existingMeasurement?.weightKg ?: currentWeight) * 2.20462
    }

    var weightStr by remember { mutableStateOf(String.format(Locale.getDefault(), "%.1f", displayW)) }
    var bodyFatStr by remember { mutableStateOf(existingMeasurement?.bodyFatPercent?.toString() ?: "15.0") }
    var waistStr by remember { mutableStateOf(existingMeasurement?.waistCm?.toString() ?: "82.0") }
    var chestStr by remember { mutableStateOf(existingMeasurement?.chestCm?.toString() ?: "100.0") }
    var armsStr by remember { mutableStateOf(existingMeasurement?.armsCm?.toString() ?: "36.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingMeasurement != null) "Edit Body Measurement" else "Add Body Measurements") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = weightStr,
                        onValueChange = { weightStr = it },
                        label = { Text("Weight ($unitLabel)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = bodyFatStr,
                        onValueChange = { bodyFatStr = it },
                        label = { Text("Body Fat %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = waistStr,
                        onValueChange = { waistStr = it },
                        label = { Text("Waist (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = chestStr,
                        onValueChange = { chestStr = it },
                        label = { Text("Chest (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = armsStr,
                        onValueChange = { armsStr = it },
                        label = { Text("Arms (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawW = weightStr.toDoubleOrNull() ?: currentWeight
                    val wKg = if (unitSystem == UnitSystem.METRIC) rawW else rawW / 2.20462
                    val entityToSave = existingMeasurement?.copy(
                        weightKg = wKg,
                        bodyFatPercent = bodyFatStr.toDoubleOrNull(),
                        waistCm = waistStr.toDoubleOrNull(),
                        chestCm = chestStr.toDoubleOrNull(),
                        armsCm = armsStr.toDoubleOrNull(),
                    ) ?: BodyMeasurementEntity(
                        weightKg = wKg,
                        bodyFatPercent = bodyFatStr.toDoubleOrNull(),
                        waistCm = waistStr.toDoubleOrNull(),
                        chestCm = chestStr.toDoubleOrNull(),
                        armsCm = armsStr.toDoubleOrNull(),
                        loggedAt = System.currentTimeMillis()
                    )
                    onSave(entityToSave)
                }
            ) {
                Text(if (existingMeasurement != null) "Update" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
