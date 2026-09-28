package com.gymtracker.app.presentation.progress

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.ui.graphics.Color
import com.gymtracker.app.data.local.entity.HealthSyncLogEntity
import com.gymtracker.app.presentation.components.GoogleAccountProfileCard
import com.gymtracker.app.presentation.components.GoogleLogoIcon
import com.gymtracker.app.presentation.components.GoogleSignInButton
import com.gymtracker.app.presentation.components.GoogleSignInOptionsDialog
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.gymtracker.app.presentation.components.WeightLogChartCard
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.gymtracker.app.data.local.entity.BodyMeasurementEntity
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.PersonalRecordEntity
import com.gymtracker.app.data.local.entity.ProgressPhotoEntity
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.model.ExerciseProgressPoint
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.StrengthStandardsUseCase
import com.gymtracker.app.presentation.components.EmptyState
import com.gymtracker.app.presentation.components.LineChartCard
import com.gymtracker.app.presentation.components.MultiLineChartCard
import com.gymtracker.app.presentation.components.StreakHeatmap
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
    val isSyncingHealth: Boolean = false,
    val syncStepMessage: String? = null,
    val syncProgress: Float = 0f,
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repository: GymRepository,
    private val googleAuthService: com.gymtracker.app.data.auth.GoogleAuthService,
    private val healthSyncService: com.gymtracker.app.data.health.HealthSyncService,
) : ViewModel() {
    private val _isSyncingHealth = MutableStateFlow(false)
    private val _syncStepMessage = MutableStateFlow<String?>(null)
    private val _syncProgress = MutableStateFlow(0f)

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

    val state: StateFlow<ProgressUiState> = combine(
        dataFlow,
        _isSyncingHealth,
        _syncStepMessage,
        _syncProgress,
    ) { data, isSyncing, stepMsg, progress ->
        ProgressUiState(
            profile = data.profile,
            exercises = data.exercises,
            measurements = data.measurements,
            photos = data.photos,
            records = data.records,
            weights = data.weights,
            history = data.history,
            healthLogs = data.healthLogs,
            isSyncingHealth = isSyncing,
            syncStepMessage = stepMsg,
            syncProgress = progress,
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

    fun addPhoto(uri: Uri) {
        viewModelScope.launch { repository.addProgressPhoto(ProgressPhotoEntity(uri = uri.toString())) }
    }

    fun getGoogleSignInIntent(): Intent = googleAuthService.getSignInIntent()

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            _isSyncingHealth.value = true
            _syncStepMessage.value = "Signing into Google Account..."
            val result = googleAuthService.handleSignInResult(data)
            result.fold(
                onSuccess = { user ->
                    repository.updateUserGoogleAuth(
                        googleLinked = true,
                        googleEmail = user.email,
                        googleDisplayName = user.displayName,
                        googlePhotoUrl = user.photoUrl,
                        googleId = user.id,
                    )
                    _syncStepMessage.value = "Signed in as ${user.email}. Syncing progress..."
                    syncProgressToHealth()
                },
                onFailure = { error ->
                    _isSyncingHealth.value = false
                    _syncStepMessage.value = error.message ?: "Sign-in failed"
                }
            )
        }
    }

    fun quickGoogleSignIn(email: String, displayName: String) {
        viewModelScope.launch {
            _isSyncingHealth.value = true
            _syncStepMessage.value = "Linking Google Account ($email)..."
            val result = googleAuthService.quickSignIn(email, displayName)
            result.fold(
                onSuccess = { user ->
                    repository.updateUserGoogleAuth(
                        googleLinked = true,
                        googleEmail = user.email,
                        googleDisplayName = user.displayName,
                        googlePhotoUrl = user.photoUrl,
                        googleId = user.id,
                    )
                    _syncStepMessage.value = "Connected as ${user.email}!"
                    syncProgressToHealth()
                },
                onFailure = { error ->
                    _isSyncingHealth.value = false
                    _syncStepMessage.value = error.message ?: "Quick sign-in failed"
                }
            )
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            googleAuthService.signOut()
            repository.updateUserGoogleAuth(
                googleLinked = false,
                googleEmail = null,
                googleDisplayName = null,
                googlePhotoUrl = null,
                googleId = null,
            )
            repository.updateHealthConnectStatus(linked = false)
            _syncStepMessage.value = "Google Account unlinked."
        }
    }

    fun syncProgressToHealth(
        syncWorkouts: Boolean = true,
        syncWeights: Boolean = true,
        syncHydration: Boolean = true,
    ) {
        viewModelScope.launch {
            _isSyncingHealth.value = true
            _syncProgress.value = 0.1f
            _syncStepMessage.value = "Initiating Google Health progress link..."

            val result = repository.syncProgressToGoogleHealth(
                syncWorkouts = syncWorkouts,
                syncWeights = syncWeights,
                syncHydration = syncHydration,
                onProgressUpdate = { step, progress ->
                    _syncStepMessage.value = step
                    _syncProgress.value = progress
                }
            )

            _isSyncingHealth.value = false
            _syncProgress.value = 1f
            result.fold(
                onSuccess = { summary ->
                    _syncStepMessage.value = summary.message
                },
                onFailure = { error ->
                    _syncStepMessage.value = error.message ?: "Sync encountered an issue"
                }
            )
        }
    }

    fun updateHealthPreferences(
        syncWorkouts: Boolean,
        syncWeights: Boolean,
        syncHydration: Boolean,
        syncSteps: Boolean,
    ) {
        viewModelScope.launch {
            repository.updateHealthSyncPreferences(
                linked = true,
                syncWorkouts = syncWorkouts,
                syncWeights = syncWeights,
                syncHydration = syncHydration,
                syncSteps = syncSteps,
            )
        }
    }

    fun openHealthConnectSettings(context: android.content.Context) {
        runCatching {
            val intent = healthSyncService.getHealthConnectSettingsIntent()
            context.startActivity(intent)
        }
    }
}

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Health Link", "Strength & 1RM", "Body & Weight", "Photos")

    var showWeightDialog by remember { mutableStateOf(false) }
    var showMeasurementDialog by remember { mutableStateOf(false) }
    var showGoogleDialog by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.handleGoogleSignInResult(result.data)
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::addPhoto)
    }

    // Default select first exercise if none selected
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

    val totalSessions = state.history.size
    val activeDaysLast6Weeks = remember(workoutDays) {
        val cutoff = LocalDate.now().minusDays(41)
        workoutDays.count { it >= cutoff }
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
                    text = "Track your consistency, strength gains, and body trends",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Quick Stats Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatMetricPill(
                icon = Icons.Default.FitnessCenter,
                value = "$totalSessions",
                label = "Workouts",
                modifier = Modifier.weight(1f)
            )
            StatMetricPill(
                icon = Icons.Default.TrendingUp,
                value = String.format(Locale.getDefault(), "%.1f t", totalVolumeTons),
                label = "Volume",
                modifier = Modifier.weight(1f)
            )
            StatMetricPill(
                icon = Icons.Default.CalendarMonth,
                value = "$activeDaysLast6Weeks d",
                label = "Active (6 wk)",
                modifier = Modifier.weight(1f)
            )
            StatMetricPill(
                icon = Icons.Default.WorkspacePremium,
                value = "${state.records.size}",
                label = "PRs Set",
                modifier = Modifier.weight(1f)
            )
        }

        // Tab Navigation
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
                    }
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> OverviewTab(
                    workoutDays = workoutDays,
                    activeDays = activeDaysLast6Weeks,
                    records = state.records,
                    recentWeights = state.weights.sortedByDescending { it.loggedAt }.take(3),
                    unitSystem = state.profile.unitSystem,
                    profile = state.profile,
                    workoutsCount = state.history.size,
                    weightsCount = state.weights.size,
                    isSyncingHealth = state.isSyncingHealth,
                    syncStepMessage = state.syncStepMessage,
                    onLogWeightClick = { showWeightDialog = true },
                    onSignInGoogle = { showGoogleDialog = true },
                    onQuickConnectGoogle = { showGoogleDialog = true },
                    onSyncProgressToHealth = { viewModel.syncProgressToHealth() },
                    onOpenHealthLinkTab = { selectedTab = 1 },
                )
                1 -> HealthLinkTab(
                    profile = state.profile,
                    workoutsCount = state.history.size,
                    weightsCount = state.weights.size,
                    totalVolumeTons = totalVolumeTons,
                    isSyncing = state.isSyncingHealth,
                    syncProgress = state.syncProgress,
                    syncStepMessage = state.syncStepMessage,
                    healthLogs = state.healthLogs,
                    onSignIn = { showGoogleDialog = true },
                    onQuickSignIn = { showGoogleDialog = true },
                    onSignOut = { viewModel.signOutGoogle() },
                    onSyncNow = { viewModel.syncProgressToHealth() },
                    onUpdatePreferences = { workouts, weights, hydration, steps ->
                        viewModel.updateHealthPreferences(workouts, weights, hydration, steps)
                    },
                    onOpenSystemHealthConnect = { viewModel.openHealthConnectSettings(context) },
                )
                2 -> StrengthTab(
                    exercises = state.exercises,
                    selectedExerciseId = viewModel.selectedExerciseId,
                    onSelectExercise = viewModel::selectExercise,
                    progressPoints = viewModel.progressPoints
                )
                3 -> BodyAndWeightTab(
                    weights = state.weights,
                    measurements = state.measurements,
                    unitSystem = state.profile.unitSystem,
                    onLogWeightClick = { showWeightDialog = true },
                    onLogMeasurementClick = { showMeasurementDialog = true }
                )
                4 -> PhotosTab(
                    photos = state.photos,
                    onAddPhoto = { photoPicker.launch("image/*") },
                    onSharePhoto = { uriStr ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(uriStr))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share progress photo"))
                    }
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

    // Dialog for Logging Measurements
    if (showMeasurementDialog) {
        LogMeasurementDialog(
            unitSystem = state.profile.unitSystem,
            currentWeight = state.weights.maxByOrNull { it.loggedAt }?.weightKg ?: state.profile.weightKg,
            onDismiss = { showMeasurementDialog = false },
            onSave = { measurement ->
                viewModel.addMeasurement(measurement)
                showMeasurementDialog = false
            }
        )
    }

    // Dialog for Google Sign-In options (Play Services intent or instant test account)
    if (showGoogleDialog) {
        GoogleSignInOptionsDialog(
            onDismiss = { showGoogleDialog = false },
            onLaunchPlayServices = {
                showGoogleDialog = false
                signInLauncher.launch(viewModel.getGoogleSignInIntent())
            },
            onQuickSignIn = { email, name ->
                showGoogleDialog = false
                viewModel.quickGoogleSignIn(email, name)
            }
        )
    }
}

// =================================================================
// TAB 0: OVERVIEW (Streak, PRs, Summary)
// =================================================================
@Composable
private fun OverviewTab(
    workoutDays: Set<LocalDate>,
    activeDays: Int,
    records: List<PersonalRecordEntity>,
    recentWeights: List<WeightLogEntity>,
    unitSystem: UnitSystem,
    profile: UserProfileEntity,
    workoutsCount: Int,
    weightsCount: Int,
    isSyncingHealth: Boolean,
    syncStepMessage: String?,
    onLogWeightClick: () -> Unit,
    onSignInGoogle: () -> Unit,
    onQuickConnectGoogle: () -> Unit,
    onSyncProgressToHealth: () -> Unit,
    onOpenHealthLinkTab: () -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Google Health Progress Link Showcase Card
        item {
            GoogleHealthProgressCard(
                profile = profile,
                workoutsCount = workoutsCount,
                weightsCount = weightsCount,
                isSyncing = isSyncingHealth,
                syncStepMessage = syncStepMessage,
                onSignInGoogle = onSignInGoogle,
                onQuickConnectGoogle = onQuickConnectGoogle,
                onSyncProgressToHealth = onSyncProgressToHealth,
                onOpenHealthLinkTab = onOpenHealthLinkTab,
            )
        }

        // Consistency Heatmap Card
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
                                text = "$activeDays days active",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    StreakHeatmap(workoutDays = workoutDays)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Each dot represents a training day",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                            Text("Rest", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                            Spacer(Modifier.width(4.dp))
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
                            Text("Trained", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Personal Records Showcase
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Personal Records (PRs)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${records.size} records",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (records.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EmptyState(
                        title = "No PRs logged yet",
                        detail = "Keep training! As you hit new heavy weights and reps during workouts, your personal records will shine here automatically."
                    )
                }
            }
        } else {
            items(records.take(6), key = { it.id }) { pr ->
                PersonalRecordCard(record = pr, unitSystem = unitSystem)
            }
        }

        // Weight Progression Chart
        item {
            WeightLogChartCard(
                weights = recentWeights,
                unitSystem = unitSystem,
                onAddWeight = { onLogWeightClick() },
                onDeleteWeight = null,
            )
        }
    }
}

/**
 * Top interactive card in Overview showing Google Account & Health Connect status.
 */
@Composable
private fun GoogleHealthProgressCard(
    profile: UserProfileEntity,
    workoutsCount: Int,
    weightsCount: Int,
    isSyncing: Boolean,
    syncStepMessage: String?,
    onSignInGoogle: () -> Unit,
    onQuickConnectGoogle: () -> Unit,
    onSyncProgressToHealth: () -> Unit,
    onOpenHealthLinkTab: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (profile.googleLinked) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            }
        ),
        border = BorderStroke(
            1.dp,
            if (profile.googleLinked) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoogleLogoIcon(Modifier.size(22.dp))
                    Text(
                        text = "Google Health Progress Link",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (profile.googleLinked && profile.healthConnectLinked) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (profile.googleLinked && profile.healthConnectLinked) Color(0xFF2E7D32) else Color(0xFF757575))
                        )
                        Text(
                            text = if (profile.googleLinked && profile.healthConnectLinked) "Linked & Synced" else if (profile.googleLinked) "Account Linked" else "Not Linked",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (profile.googleLinked && profile.healthConnectLinked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (profile.googleLinked && !profile.googleEmail.isNullOrBlank()) {
                Text(
                    text = "Connected as ${profile.googleEmail}. Your gym workouts, weight records, and hydration are linked with Google Health Connect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text("Workouts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$workoutsCount synced", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text("Weight Logs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$weightsCount synced", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text("Hydration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Target linked", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                if (isSyncing) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(
                            text = syncStepMessage ?: "Syncing progress to Health Connect...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else if (!syncStepMessage.isNullOrBlank()) {
                    Text(
                        text = syncStepMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onSyncProgressToHealth,
                        enabled = !isSyncing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (isSyncing) "Syncing..." else "Sync Now")
                    }
                    OutlinedButton(
                        onClick = onOpenHealthLinkTab,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Health Details")
                    }
                }
            } else {
                Text(
                    text = "Sign in with your Google Account to automatically export workouts, body weight logs, and active calories to Google Health Connect.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoogleSignInButton(
                        onClick = onSignInGoogle,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedButton(
                        onClick = onQuickConnectGoogle
                    ) {
                        Text("Quick Connect")
                    }
                }
            }
        }
    }
}

// =================================================================
// TAB 1: HEALTH LINK (Dedicated Google Health Connect Integration)
// =================================================================
@Composable
private fun HealthLinkTab(
    profile: UserProfileEntity,
    workoutsCount: Int,
    weightsCount: Int,
    totalVolumeTons: Double,
    isSyncing: Boolean,
    syncProgress: Float,
    syncStepMessage: String?,
    healthLogs: List<HealthSyncLogEntity>,
    onSignIn: () -> Unit,
    onQuickSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncNow: () -> Unit,
    onUpdatePreferences: (syncWorkouts: Boolean, syncWeights: Boolean, syncHydration: Boolean, syncSteps: Boolean) -> Unit,
    onOpenSystemHealthConnect: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Google Account Section
        item {
            if (profile.googleLinked && !profile.googleEmail.isNullOrBlank()) {
                GoogleAccountProfileCard(
                    email = profile.googleEmail,
                    displayName = profile.googleDisplayName ?: "Athlete",
                    photoUrl = profile.googlePhotoUrl,
                    healthLinked = profile.healthConnectLinked,
                    onSignOut = onSignOut
                )
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoogleLogoIcon(Modifier.size(24.dp))
                            Text("Sign in with Google", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Connect your Google Account to synchronize workout routines, personal records, and weight check-ins directly with Google Health Connect.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoogleSignInButton(onClick = onSignIn, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = onQuickSignIn) { Text("Quick Connect") }
                        }
                    }
                }
            }
        }

        // Live Health Connect Synchronization Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Google Health Connect Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val lastSyncedText = if (profile.healthLastSyncedAt != null && profile.healthLastSyncedAt > 0) {
                                SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(profile.healthLastSyncedAt))
                            } else {
                                "Never synced"
                            }
                            Text(
                                "Last synchronized: $lastSyncedText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (profile.healthConnectLinked) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = if (profile.healthConnectLinked) "Active Link" else "Link Pending",
                                color = if (profile.healthConnectLinked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (isSyncing) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(progress = { syncProgress }, modifier = Modifier.fillMaxWidth())
                            Text(
                                text = syncStepMessage ?: "Syncing data to Google Health...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (!syncStepMessage.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34A853), modifier = Modifier.size(16.dp))
                            Text(syncStepMessage, style = MaterialTheme.typography.bodySmall, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onSyncNow,
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isSyncing) "Syncing..." else "Sync Now")
                        }
                        OutlinedButton(onClick = onOpenSystemHealthConnect) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Health Settings")
                        }
                    }
                }
            }
        }

        // Linked Progress Categories Breakdown
        item {
            Text("Linked Health Data Streams", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            HealthStreamCard(
                icon = Icons.Default.FitnessCenter,
                title = "Gym Workouts & Session Volume",
                detail = "$workoutsCount workouts • ${String.format(Locale.getDefault(), "%.1f", totalVolumeTons)} tons tonnage • Est. ${(totalVolumeTons * 25 + workoutsCount * 180).toInt()} kcal",
                checked = profile.healthSyncWorkouts,
                onCheckedChange = { checked ->
                    onUpdatePreferences(checked, profile.healthSyncWeights, profile.healthSyncHydration, profile.healthSyncSteps)
                }
            )
        }

        item {
            HealthStreamCard(
                icon = Icons.Default.MonitorWeight,
                title = "Body Weight & Moving Averages",
                detail = "$weightsCount weigh-ins synced • Current: ${profile.weightKg} kg",
                checked = profile.healthSyncWeights,
                onCheckedChange = { checked ->
                    onUpdatePreferences(profile.healthSyncWorkouts, checked, profile.healthSyncHydration, profile.healthSyncSteps)
                }
            )
        }

        item {
            HealthStreamCard(
                icon = Icons.Default.WaterDrop,
                title = "Daily Hydration Logs",
                detail = "Daily water logs synchronized against your ${profile.waterGoalMl} ml target",
                checked = profile.healthSyncHydration,
                onCheckedChange = { checked ->
                    onUpdatePreferences(profile.healthSyncWorkouts, profile.healthSyncWeights, checked, profile.healthSyncSteps)
                }
            )
        }

        item {
            HealthStreamCard(
                icon = Icons.Default.LocalFireDepartment,
                title = "Daily Movement & Steps",
                detail = "Syncs active energy burned and step count with Google Health",
                checked = profile.healthSyncSteps,
                onCheckedChange = { checked ->
                    onUpdatePreferences(profile.healthSyncWorkouts, profile.healthSyncWeights, profile.healthSyncHydration, checked)
                }
            )
        }

        // Recent Activity Sync Log
        item {
            Text("Recent Health Sync Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (healthLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EmptyState(
                        title = "No sync events yet",
                        detail = "Tap 'Sync Now' above to synchronize your workout volume, weight check-ins, and hydration with Google Health Connect."
                    )
                }
            }
        } else {
            items(healthLogs.take(8), key = { it.id }) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = when (log.category) {
                                "WORKOUT" -> MaterialTheme.colorScheme.primaryContainer
                                "WEIGHT" -> MaterialTheme.colorScheme.secondaryContainer
                                "HYDRATION" -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (log.category) {
                                        "WORKOUT" -> Icons.Default.FitnessCenter
                                        "WEIGHT" -> Icons.Default.MonitorWeight
                                        "HYDRATION" -> Icons.Default.WaterDrop
                                        else -> Icons.Default.Sync
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.summary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(log.syncedAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = Color(0xFF34A853),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthStreamCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
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
                    Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

// =================================================================
// TAB 1: STRENGTH & 1RM
// =================================================================
@Composable
private fun StrengthTab(
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

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Exercise Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Exercise", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // Popular Lifts Quick Filter Chips
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

        // Strength & 1RM Progression Card
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
                                text = "Estimated 1RM: ${maxOneRm.toInt()} kg",
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

                    // Key Stats summary for this exercise
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStat(label = "Peak Weight", value = "${maxWeight.toInt()} kg", modifier = Modifier.weight(1f))
                        MiniStat(label = "Peak 1RM", value = "${maxOneRm.toInt()} kg", modifier = Modifier.weight(1f))
                        MiniStat(label = "Peak Volume", value = "${maxVolume.toInt()} kg", modifier = Modifier.weight(1f))
                    }

                    Text("Weight Lifted Progression (kg)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LineChartCard(
                        values = progressPoints.map { it.weight },
                        lineColor = MaterialTheme.colorScheme.primary
                    )

                    Spacer(Modifier.height(4.dp))
                    Text("Session Volume Trend (kg)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LineChartCard(
                        values = progressPoints.map { it.volume },
                        lineColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

// =================================================================
// TAB 2: BODY & WEIGHT TRACKER
// =================================================================
@Composable
private fun BodyAndWeightTab(
    weights: List<WeightLogEntity>,
    measurements: List<BodyMeasurementEntity>,
    unitSystem: UnitSystem,
    onLogWeightClick: () -> Unit,
    onLogMeasurementClick: () -> Unit,
) {
    val chronologicalWeights = remember(weights) { weights.sortedBy { it.loggedAt } }
    val unitLabel = if (unitSystem == UnitSystem.METRIC) "kg" else "lb"
    val multiplier = if (unitSystem == UnitSystem.METRIC) 1.0 else 2.20462

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Weight Progression Chart Card
        item {
            WeightLogChartCard(
                weights = weights,
                unitSystem = unitSystem,
                onAddWeight = { onLogWeightClick() },
                onDeleteWeight = null,
            )
        }

        // Body Measurements Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Body Measurements", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = onLogMeasurementClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Straighten, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Measurements", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                        detail = "Tap 'Add Measurements' to record waist, chest, arms, and body fat percentages over time."
                    )
                }
            }
        } else {
            items(measurements.sortedByDescending { it.loggedAt }, key = { it.id }) { m ->
                val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(m.loggedAt))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dateStr, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            m.bodyFatPercent?.let { bf ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        "$bf% Body Fat",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            m.weightKg?.let { w ->
                                Text("Weight: ${String.format(Locale.getDefault(), "%.1f", if (unitSystem == UnitSystem.METRIC) w else w * 2.20462)} $unitLabel", style = MaterialTheme.typography.bodySmall)
                            }
                            m.waistCm?.let { waist ->
                                Text("• Waist: ${waist.toInt()} cm", style = MaterialTheme.typography.bodySmall)
                            }
                            m.chestCm?.let { chest ->
                                Text("• Chest: ${chest.toInt()} cm", style = MaterialTheme.typography.bodySmall)
                            }
                            m.armsCm?.let { arms ->
                                Text("• Arms: ${arms.toInt()} cm", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =================================================================
// TAB 3: PHOTOS & TRANSFORMATION
// =================================================================
@Composable
private fun PhotosTab(
    photos: List<ProgressPhotoEntity>,
    onAddPhoto: () -> Unit,
    onSharePhoto: (String) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Transformation Gallery", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${photos.size} photos saved", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

        if (photos.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EmptyState(
                        title = "No progress photos yet",
                        detail = "Take regular physique check-in photos to visualize your muscle growth and body recomp transformation!"
                    )
                }
            }
        } else {
            items(photos.sortedByDescending { it.takenAt }, key = { it.id }) { photo ->
                val dateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(photo.takenAt))
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
                            IconButton(onClick = { onSharePhoto(photo.uri) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                            }
                        }

                        AsyncImage(
                            model = photo.uri,
                            contentDescription = "Physique progress photo taken on $dateStr",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }
        }
    }
}

// =================================================================
// HELPER COMPONENTS & DIALOGS
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
private fun PersonalRecordCard(record: PersonalRecordEntity, unitSystem: UnitSystem) {
    val unitLabel = if (unitSystem == UnitSystem.METRIC) "kg" else "lb"
    val weightDisplay = if (unitSystem == UnitSystem.METRIC) record.value else record.value * 2.20462
    val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(record.achievedAt))

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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🏆", fontSize = 16.sp)
                    }
                }
                Column {
                    Text(record.exerciseName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Text(dateStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${weightDisplay.toInt()} $unitLabel",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${record.reps} rep(s) • ${record.type}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
        title = { Text("Log Bodyweight") },
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
    onDismiss: () -> Unit,
    onSave: (BodyMeasurementEntity) -> Unit,
) {
    val unitLabel = if (unitSystem == UnitSystem.METRIC) "kg" else "lb"
    val displayW = if (unitSystem == UnitSystem.METRIC) currentWeight else currentWeight * 2.20462

    var weightStr by remember { mutableStateOf(String.format(Locale.getDefault(), "%.1f", displayW)) }
    var bodyFatStr by remember { mutableStateOf("15.0") }
    var waistStr by remember { mutableStateOf("82.0") }
    var chestStr by remember { mutableStateOf("100.0") }
    var armsStr by remember { mutableStateOf("36.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Body Measurements") },
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
                    onSave(
                        BodyMeasurementEntity(
                            weightKg = wKg,
                            bodyFatPercent = bodyFatStr.toDoubleOrNull(),
                            waistCm = waistStr.toDoubleOrNull(),
                            chestCm = chestStr.toDoubleOrNull(),
                            armsCm = armsStr.toDoubleOrNull(),
                            loggedAt = System.currentTimeMillis()
                        )
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
