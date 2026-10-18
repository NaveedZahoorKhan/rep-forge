package com.gymtracker.app.presentation.active

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.OneRepMaxFormula
import com.gymtracker.app.data.local.entity.PerformedSetEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.model.DynamicWarmUpRoutine
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.DynamicWarmUpCatalog
import com.gymtracker.app.domain.usecase.OneRepMaxCalculator
import com.gymtracker.app.domain.usecase.PlateCalculator
import com.gymtracker.app.domain.usecase.ProgressiveOverloadUseCase
import com.gymtracker.app.notification.NotificationHelper
import com.gymtracker.app.notification.WorkoutSoundPlayer
import com.gymtracker.app.presentation.navigation.ActiveWorkoutRoute
import com.gymtracker.app.presentation.warmup.DynamicWarmUpPlayerModal
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModel @Inject constructor(
    private val repository: GymRepository,
    private val notificationHelper: NotificationHelper,
    val soundPlayer: WorkoutSoundPlayer,
) : ViewModel() {
    private val sessionId = MutableStateFlow("")
    private var routeKey = ""
    private var timerJob: Job? = null

    val isSessionLoading = MutableStateFlow(true)

    val session: StateFlow<WorkoutSessionEntity?> = repository.observeActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val exercises: StateFlow<List<ExerciseEntity>> = repository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sets: StateFlow<List<PerformedSetEntity>> = sessionId
        .flatMapLatest { id -> if (id.isBlank()) flowOf(emptyList()) else repository.observeSessionSets(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var restRemaining by mutableStateOf(0)
        private set
    var restTotal by mutableStateOf(0)
        private set
    var isTimerRunning by mutableStateOf(false)
        private set
    var isTimerPaused by mutableStateOf(false)
        private set
    var isTimerFinished by mutableStateOf(false)
        private set
    var timerExercise by mutableStateOf("")
        private set

    fun enter(route: ActiveWorkoutRoute) {
        val key = "${route.sessionId}:${route.workoutId}"
        if (routeKey == key) return
        routeKey = key
        isSessionLoading.value = true
        viewModelScope.launch {
            try {
                sessionId.value = when {
                    route.sessionId.isNotBlank() -> route.sessionId
                    route.workoutId.isNotBlank() -> repository.startWorkout(route.workoutId)
                    else -> repository.observeActiveSession().first()?.id.orEmpty()
                }
            } finally {
                isSessionLoading.value = false
            }
        }
    }

    /**
     * Simple workout logging: saves weight and reps. RPE & RIR are omitted to keep logging simple.
     * Merely correcting an already completed set updates the set without restarting the rest timer.
     */
    fun complete(set: PerformedSetEntity, reps: Int, weight: Double, notes: String = "") {
        val validReps = reps.coerceAtLeast(0)
        val validWeight = weight.coerceAtLeast(0.0)
        val wasAlreadyCompleted = set.completed
        viewModelScope.launch {
            val completed = repository.completeSet(set.id, validReps, validWeight, null, null, notes)
            if (!wasAlreadyCompleted) {
                val rest = if (completed.restSeconds > 0) completed.restSeconds else 90
                startRest(rest, completed.exerciseName)
            }
        }
    }

    fun completeAllRemaining(remainingSets: List<PerformedSetEntity>) {
        viewModelScope.launch {
            remainingSets.filter { !it.completed }.forEach { set ->
                val validReps = set.reps.coerceAtLeast(0)
                val validWeight = set.weight.coerceAtLeast(0.0)
                repository.completeSet(set.id, validReps, validWeight, null, null, set.notes)
            }
        }
    }

    fun update(set: PerformedSetEntity) {
        viewModelScope.launch { repository.updatePerformedSet(set) }
    }

    fun finish(onDone: () -> Unit) {
        val id = sessionId.value
        if (id.isBlank() || session.value == null) return
        viewModelScope.launch {
            repository.finishSession(id)
            timerJob?.cancel()
            onDone()
        }
    }

    fun cancel(onDone: () -> Unit) {
        val id = sessionId.value
        viewModelScope.launch {
            if (id.isNotBlank()) repository.cancelSession(id)
            timerJob?.cancel()
            onDone()
        }
    }

    private suspend fun triggerRestExpiredAlert() {
        isTimerRunning = false
        isTimerPaused = false
        isTimerFinished = true

        val profile = repository.observeUserProfile().first()
        val soundEnabled = profile?.soundEnabled ?: true
        val vibrationEnabled = profile?.vibrationEnabled ?: true

        // Play audible gym beeps directly via speaker
        if (soundEnabled) {
            soundPlayer.playRestCompleteSound()
        }
        // Physical vibration feedback
        if (vibrationEnabled) {
            soundPlayer.vibrateRestComplete()
        }

        notificationHelper.showRestComplete(
            exerciseName = timerExercise,
            sound = soundEnabled,
            vibration = vibrationEnabled,
        )
    }

    fun startRest(seconds: Int, exerciseName: String = "Rest Interval") {
        timerJob?.cancel()
        val total = seconds.coerceAtLeast(1)
        restTotal = total
        restRemaining = total
        timerExercise = exerciseName.ifBlank { "Rest Interval" }
        isTimerRunning = true
        isTimerPaused = false
        isTimerFinished = false

        timerJob = viewModelScope.launch {
            while (isActive && restRemaining > 0) {
                delay(1_000)
                if (!isTimerPaused) {
                    restRemaining -= 1
                }
            }
            if (isActive && restRemaining <= 0) {
                triggerRestExpiredAlert()
            }
        }
    }

    fun pauseTimer() {
        if (isTimerRunning && !isTimerPaused) {
            isTimerPaused = true
        }
    }

    fun resumeTimer() {
        if (isTimerRunning && isTimerPaused) {
            isTimerPaused = false
        } else if (!isTimerRunning && restRemaining > 0) {
            isTimerRunning = true
            isTimerPaused = false
            isTimerFinished = false
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (isActive && restRemaining > 0) {
                    delay(1_000)
                    if (!isTimerPaused) {
                        restRemaining -= 1
                    }
                }
                if (isActive && restRemaining <= 0) {
                    triggerRestExpiredAlert()
                }
            }
        }
    }

    fun togglePauseTimer() {
        if (isTimerRunning) {
            if (isTimerPaused) resumeTimer() else pauseTimer()
        } else if (restRemaining > 0) {
            resumeTimer()
        } else {
            startRest(60, "Rest Interval")
        }
    }

    fun addRestTime(secondsToAdd: Int) {
        if (isTimerRunning || restRemaining > 0) {
            val newRemaining = (restRemaining + secondsToAdd).coerceAtLeast(1)
            restRemaining = newRemaining
            restTotal = maxOf(restTotal, newRemaining)
            isTimerFinished = false
            if (!isTimerRunning) {
                resumeTimer()
            }
        } else {
            startRest(secondsToAdd, timerExercise.ifBlank { "Rest Interval" })
        }
    }

    fun subtractRestTime(secondsToSubtract: Int) {
        if (restRemaining > 1) {
            restRemaining = (restRemaining - secondsToSubtract).coerceAtLeast(1)
        }
    }

    fun restartTimer() {
        val duration = if (restTotal > 0) restTotal else 60
        startRest(duration, timerExercise.ifBlank { "Rest Interval" })
    }

    fun skipTimer() {
        timerJob?.cancel()
        timerJob = null
        restRemaining = 0
        restTotal = 0
        isTimerRunning = false
        isTimerPaused = false
        isTimerFinished = false
        timerExercise = ""
    }

    fun dismissFinished() {
        isTimerFinished = false
        restRemaining = 0
        restTotal = 0
        timerExercise = ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutRouteScreen(
    route: ActiveWorkoutRoute,
    onDone: () -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel(),
) {
    LaunchedEffect(route) { viewModel.enter(route) }
    val isSessionLoading by viewModel.isSessionLoading.collectAsStateWithLifecycle()
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    val activeSession by viewModel.session.collectAsStateWithLifecycle()
    val allExercises by viewModel.exercises.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (isSessionLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text("Loading workout session...", style = MaterialTheme.typography.bodyMedium)
            }
        }
        return
    }

    if (activeSession == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Active Workout", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDone) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            "Workout Session Not Found",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "The requested workout session is missing or has ended. Please start a new workout.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onDone,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Return")
                        }
                    }
                }
            }
        }
        return
    }

    val isSessionValid = activeSession != null
    var showWarmUpModal by remember { mutableStateOf(false) }

    val warmUpRoutine = remember(activeSession, allExercises) {
        val sessionName = activeSession?.workoutName.orEmpty()
        val dummyWorkout = com.gymtracker.app.data.local.entity.WorkoutEntity(
            id = activeSession?.workoutId.orEmpty(),
            name = sessionName,
        )
        DynamicWarmUpCatalog.suggestRoutineForWorkout(dummyWorkout, allExercises)
    }

    var editingSet by remember { mutableStateOf<PerformedSetEntity?>(null) }
    var plateWeight by remember { mutableDoubleStateOf(100.0) }
    var formula by remember { mutableStateOf(OneRepMaxFormula.EPLEY) }
    var showCalculators by remember { mutableStateOf(false) }

    // State for Finish Review Summary Modal and confirmation dialogs
    var showFinishReviewSummaryModal by remember { mutableStateOf(false) }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showFinishConfirmDialog by remember { mutableStateOf(false) }
    var showCompleteAllConfirmDialog by remember { mutableStateOf(false) }
    var isSubmittingFinish by remember { mutableStateOf(false) }

    val volume = sets.filter { it.completed }.sumOf { it.weight * it.reps }
    val latestSet = sets.lastOrNull { it.completed }
    val oneRm = latestSet?.let { OneRepMaxCalculator.estimate(it.weight, it.reps, formula) } ?: 0.0
    val plates = PlateCalculator.calculate(plateWeight)
    val suggestion = ProgressiveOverloadUseCase.suggestion(sets.filter { it.completed }, targetRepsMax = 12, lowerBodyLift = false)

    val completedCount = sets.count { it.completed }
    val remainingCount = sets.size - completedCount

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Active Workout",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${volume.toInt()} kg volume • $completedCount/${sets.size} sets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showCancelConfirmDialog = true }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Discard / Cancel workout",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showWarmUpModal = true }) {
                        Icon(
                            Icons.Default.Whatshot,
                            contentDescription = "Dynamic Warm-Up",
                            tint = Color(0xFFFF6F00),
                        )
                    }
                    Button(
                        enabled = isSessionValid && !isSubmittingFinish,
                        onClick = { showFinishReviewSummaryModal = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Finish", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            PersistentRestTimerComponent(
                remainingSeconds = viewModel.restRemaining,
                totalSeconds = viewModel.restTotal,
                isRunning = viewModel.isTimerRunning,
                isPaused = viewModel.isTimerPaused,
                isFinished = viewModel.isTimerFinished,
                exerciseName = viewModel.timerExercise,
                onStart = { sec -> viewModel.startRest(sec, "Rest Interval") },
                onPause = viewModel::pauseTimer,
                onResume = viewModel::resumeTimer,
                onToggle = viewModel::togglePauseTimer,
                onAddSeconds = viewModel::addRestTime,
                onSubtractSeconds = viewModel::subtractRestTime,
                onRestart = viewModel::restartTimer,
                onSkip = viewModel::skipTimer,
                onDismissFinished = viewModel::dismissFinished,
            )
        }
    ) { paddingValues ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Suggested Dynamic Warm-Up Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFF6F00).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFFFF6F00).copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWarmUpModal = true },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFFF6F00), modifier = Modifier.size(20.dp))
                        Column {
                            Text(
                                "🔥 Suggested Warm-Up: ${warmUpRoutine.primaryMuscleGroup.name.lowercase().replaceFirstChar { it.titlecase() }}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFFF6F00),
                            )
                            Text(
                                "${warmUpRoutine.movements.size} movements • ~${warmUpRoutine.estimatedDurationMinutes}m • Prime joints & motor units",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    Text("Start >", fontWeight = FontWeight.Bold, color = Color(0xFFFF6F00), fontSize = 12.sp)
                }
            }
            // Quick remaining status banner with Complete All action
            if (sets.any { !it.completed }) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$remainingCount set(s) left",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = { showCompleteAllConfirmDialog = true }
                        ) {
                            Text("Complete All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Collapsible Plate & 1RM Calculator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCalculators = !showCalculators },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Plates & 1RM Calculator", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                            if (!showCalculators) {
                                Text("• 1RM ${oneRm.toInt()}kg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { showCalculators = !showCalculators }, modifier = Modifier.size(32.dp)) {
                            Icon(if (showCalculators) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = "Toggle Calculator")
                        }
                    }

                    if (showCalculators) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = plateWeight.toString(),
                                onValueChange = { plateWeight = it.toDoubleOrNull() ?: plateWeight },
                                label = { Text("Bar weight kg") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                            )
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OneRepMaxFormula.entries.forEach { f ->
                                    FilterChip(
                                        selected = formula == f,
                                        onClick = { formula = f },
                                        label = { Text(f.name.take(3), fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                        val plateText = if (plates.sidePlates.isEmpty()) "None (bar only)" else plates.sidePlates.joinToString { "${it}kg" }
                        Text(
                            "Plates per side: $plateText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        suggestion?.let {
                            Text(
                                "Coach: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Grouped Exercise Sets List
            val groupedSets = remember(sets) {
                sets.groupBy { set -> set.exerciseId.ifBlank { set.exerciseName } }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                groupedSets.forEach { (exerciseKey, exerciseSets) ->
                    val exerciseName = exerciseSets.firstOrNull()?.exerciseName ?: "Exercise"
                    item(key = "header_$exerciseKey") {
                        ExerciseHeaderCard(
                            exerciseName = exerciseName,
                            exerciseSets = exerciseSets,
                            onCompleteSet = { set, reps, weight ->
                                val isUpdate = set.completed
                                viewModel.complete(set, reps, weight)
                                coroutineScope.launch {
                                    val msg = if (isUpdate) "Updated $exerciseName set ${set.setNumber}" else "Logged $exerciseName set ${set.setNumber}"
                                    snackbarHostState.showSnackbar(msg)
                                }
                            },
                            onLongPressSet = { set -> editingSet = set }
                        )
                    }
                }

                // Dedicated bottom Finish & Discard Actions Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (remainingCount > 0) {
                                Text(
                                    "$remainingCount unlogged set(s) remaining",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    "All sets logged! Ready to save session.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }

                            Button(
                                enabled = isSessionValid && !isSubmittingFinish,
                                onClick = { showFinishReviewSummaryModal = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Finish Workout", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            TextButton(
                                onClick = { showCancelConfirmDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "Discard / Cancel Workout",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for editing notes or manual set details on long-press
    editingSet?.let { target ->
        SetOptionsDialog(
            set = target,
            onDismiss = { editingSet = null },
            onSave = { updated ->
                viewModel.update(updated)
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Updated set options")
                }
                editingSet = null
            },
        )
    }

    // Confirmation dialog for Complete All bulk set completion
    if (showCompleteAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteAllConfirmDialog = false },
            title = { Text("Complete All Remaining Sets?") },
            text = { Text("This will log all $remainingCount remaining set(s) using their target values. Are you sure you want to bulk complete these sets?") },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteAllConfirmDialog = false
                        viewModel.completeAllRemaining(sets)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Logged all remaining sets")
                        }
                    }
                ) {
                    Text("Confirm Complete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteAllConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog to discard / cancel workout
    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Discard Workout?") },
            text = { Text("Are you sure you want to cancel? Any sets completed in this session will not be saved.") },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirmDialog = false
                        viewModel.cancel(onDone)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Discard Workout")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Keep Working Out")
                }
            }
        )
    }

    // Finish Review Summary Modal before committing workout
    if (showFinishReviewSummaryModal) {
        val startedAt = activeSession?.startedAt ?: System.currentTimeMillis()
        val durationMillis = System.currentTimeMillis() - startedAt
        val durationMinutes = (durationMillis / 1000 / 60).coerceAtLeast(1)

        val groupedSetsForSummary = sets.groupBy { it.exerciseId.ifBlank { it.exerciseName } }

        AlertDialog(
            onDismissRequest = { showFinishReviewSummaryModal = false },
            title = {
                Column {
                    Text(
                        text = "Finish Workout Review",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = activeSession?.workoutName ?: "Workout Session",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Summary metrics banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DURATION", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${durationMinutes}m", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("VOLUME", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${volume.toInt()}kg", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SETS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("$completedCount/${sets.size}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    // Incomplete work warning banner if applicable
                    if (remainingCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                                Text(
                                    text = "$remainingCount set(s) incomplete across exercises.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Exercise Breakdown List
                    Text(
                        text = "Exercise Breakdown",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        groupedSetsForSummary.forEach { (_, exSets) ->
                            val exName = exSets.firstOrNull()?.exerciseName ?: "Exercise"
                            val exCompleted = exSets.count { it.completed }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = exName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$exCompleted/${exSets.size} sets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (exCompleted == exSets.size) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmittingFinish,
                    onClick = {
                        if (!isSubmittingFinish) {
                            isSubmittingFinish = true
                            showFinishReviewSummaryModal = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Workout completed successfully!")
                            }
                            viewModel.finish(onDone)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isSubmittingFinish) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save & Finish", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSubmittingFinish,
                    onClick = { showFinishReviewSummaryModal = false }
                ) {
                    Text("Return to Workout")
                }
            }
        )
    }

    if (showWarmUpModal) {
        DynamicWarmUpPlayerModal(
            routine = warmUpRoutine,
            soundPlayer = viewModel.soundPlayer,
            onDismiss = { showWarmUpModal = false },
            onStartWorkout = { showWarmUpModal = false },
            targetWorkoutId = activeSession?.workoutId.orEmpty(),
        )
    }
}

/**
 * Card grouping sets by Exercise with clear header columns:
 * SET | PREVIOUS | WEIGHT (kg) | REPS | DONE
 */
@Composable
private fun ExerciseHeaderCard(
    exerciseName: String,
    exerciseSets: List<PerformedSetEntity>,
    onCompleteSet: (set: PerformedSetEntity, reps: Int, weight: Double) -> Unit,
    onLongPressSet: (set: PerformedSetEntity) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Exercise Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${exerciseSets.count { it.completed }}/${exerciseSets.size} done",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Column Header Row: SET | PREVIOUS | WEIGHT | REPS | DONE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "SET",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp)
                )
                Text(
                    text = "PREVIOUS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1.1f)
                )
                Text(
                    text = "KG",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "REPS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "DONE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            // Sets List
            exerciseSets.forEach { set ->
                SetRowItem(
                    set = set,
                    onComplete = { reps, weight -> onCompleteSet(set, reps, weight) },
                    onLongPress = { onLongPressSet(set) }
                )
            }
        }
    }
}

/**
 * Individual set row with clear columns, numeric keyboards,Next focus, unit badges,
 * secondary historical performance, and 48dp done target.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SetRowItem(
    set: PerformedSetEntity,
    onComplete: (reps: Int, weight: Double) -> Unit,
    onLongPress: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    var reps by remember(set.id, set.reps) { mutableStateOf(set.reps.toString()) }
    var weight by remember(set.id, set.weight) { mutableStateOf(set.weight.toString()) }

    val repsParsed = reps.toIntOrNull()
    val weightParsed = weight.toDoubleOrNull()
    val isRepsValid = repsParsed != null && repsParsed >= 0
    val isWeightValid = weightParsed != null && weightParsed >= 0.0

    // Visual distinction: active set vs completed set
    val rowBackground = if (set.completed) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    val rowBorderColor = if (set.completed) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    // Historical / Previous performance string (secondary)
    val previousText = if (set.weight > 0 || set.reps > 0) {
        "${set.weight.toInt()}kg × ${set.reps}"
    } else {
        "—"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBackground)
            .border(1.dp, rowBorderColor, RoundedCornerShape(8.dp))
            .combinedClickable(onClick = {}, onLongClick = onLongPress)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // SET # Badge
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (set.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${set.setNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (set.completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }

            // PREVIOUS Historical Performance
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = previousText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
                if (set.isPr) {
                    Text(
                        text = "🏆 PR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            // WEIGHT input field with Next focus
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                isError = !isWeightValid,
                singleLine = true,
                suffix = { Text("kg", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(6.dp))

            // REPS input field with Done focus
            OutlinedTextField(
                value = reps,
                onValueChange = { reps = it },
                isError = !isRepsValid,
                singleLine = true,
                suffix = { Text("reps", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isRepsValid && isWeightValid) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onComplete(repsParsed ?: set.reps, weightParsed ?: set.weight)
                        }
                    }
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(6.dp))

            // DONE Toggle Button (min 48dp touch target)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (set.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable(enabled = isRepsValid && isWeightValid) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        val finalReps = repsParsed?.coerceAtLeast(0) ?: set.reps.coerceAtLeast(0)
                        val finalWeight = weightParsed?.coerceAtLeast(0.0) ?: set.weight.coerceAtLeast(0.0)
                        onComplete(finalReps, finalWeight)
                    }
                    .semantics {
                        role = Role.Button
                        contentDescription = if (set.completed) {
                            "Completed set ${set.setNumber} for ${set.exerciseName}. Tap to update."
                        } else {
                            "Mark set ${set.setNumber} completed for ${set.exerciseName}"
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = if (set.completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (!isRepsValid || !isWeightValid) {
            Text(
                text = "Enter valid non-negative weight & reps.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun SetOptionsDialog(
    set: PerformedSetEntity,
    onDismiss: () -> Unit,
    onSave: (PerformedSetEntity) -> Unit,
) {
    var notes by remember(set.id) { mutableStateOf(set.notes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set options") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${set.exerciseName} set ${set.setNumber}")
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
            }
        },
        confirmButton = { Button(onClick = { onSave(set.copy(notes = notes)) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun SmallField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true,
    )
}
