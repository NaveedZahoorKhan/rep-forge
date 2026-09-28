package com.gymtracker.app.presentation.active

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.model.DynamicWarmUpRoutine
import com.gymtracker.app.domain.usecase.DynamicWarmUpCatalog
import com.gymtracker.app.presentation.warmup.DynamicWarmUpPlayerModal
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymtracker.app.data.local.entity.OneRepMaxFormula
import com.gymtracker.app.data.local.entity.PerformedSetEntity
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.OneRepMaxCalculator
import com.gymtracker.app.domain.usecase.PlateCalculator
import com.gymtracker.app.domain.usecase.ProgressiveOverloadUseCase
import com.gymtracker.app.notification.NotificationHelper
import com.gymtracker.app.notification.WorkoutSoundPlayer
import com.gymtracker.app.presentation.navigation.ActiveWorkoutRoute
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
        viewModelScope.launch {
            sessionId.value = when {
                route.sessionId.isNotBlank() -> route.sessionId
                route.workoutId.isNotBlank() -> repository.startWorkout(route.workoutId)
                else -> repository.observeActiveSession().first()?.id.orEmpty()
            }
        }
    }

    /**
     * Simple workout logging: saves weight and reps. RPE & RIR are omitted to keep logging simple.
     */
    fun complete(set: PerformedSetEntity, reps: Int, weight: Double, notes: String = "") {
        viewModelScope.launch {
            val completed = repository.completeSet(set.id, reps, weight, null, null, notes)
            val rest = if (completed.restSeconds > 0) completed.restSeconds else 90
            startRest(rest, completed.exerciseName)
        }
    }

    fun update(set: PerformedSetEntity) {
        viewModelScope.launch { repository.updatePerformedSet(set) }
    }

    fun finish(onDone: () -> Unit) {
        val id = sessionId.value
        viewModelScope.launch {
            if (id.isNotBlank()) repository.finishSession(id)
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
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    val activeSession by viewModel.session.collectAsStateWithLifecycle()
    val allExercises by viewModel.exercises.collectAsStateWithLifecycle()
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

    // Dialog state for clean, safe Cancel and Finish actions
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showFinishConfirmDialog by remember { mutableStateOf(false) }

    val volume = sets.filter { it.completed }.sumOf { it.weight * it.reps }
    val latestSet = sets.lastOrNull { it.completed }
    val oneRm = latestSet?.let { OneRepMaxCalculator.estimate(it.weight, it.reps, formula) } ?: 0.0
    val plates = PlateCalculator.calculate(plateWeight)
    val suggestion = ProgressiveOverloadUseCase.suggestion(sets.filter { it.completed }, targetRepsMax = 12, lowerBodyLift = false)

    val completedCount = sets.count { it.completed }
    val remainingCount = sets.size - completedCount

    Scaffold(
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
                        onClick = {
                            if (remainingCount > 0) {
                                showFinishConfirmDialog = true
                            } else {
                                viewModel.finish(onDone)
                            }
                        },
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
                            onClick = {
                                sets.filter { !it.completed }.forEach { set ->
                                    viewModel.complete(set, set.reps, set.weight)
                                }
                            }
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

            // Sets List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sets, key = { it.id }) { set ->
                    SwipeSetRow(
                        set = set,
                        onComplete = { reps, weight ->
                            viewModel.complete(set, reps, weight)
                        },
                        onLongPress = { editingSet = set },
                    )
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
                                onClick = {
                                    if (remainingCount > 0) {
                                        showFinishConfirmDialog = true
                                    } else {
                                        viewModel.finish(onDone)
                                    }
                                },
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
                editingSet = null
            },
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

    // Confirmation dialog when finishing with uncompleted sets
    if (showFinishConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmDialog = false },
            title = { Text("Finish Workout?") },
            text = { Text("You still have $remainingCount uncompleted set(s). Do you want to finish and save your progress now?") },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishConfirmDialog = false
                        viewModel.finish(onDone)
                    },
                ) {
                    Text("Finish & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmDialog = false }) {
                    Text("Back to Workout")
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
 * Simple, streamlined set logging row.
 * Displays Weight (kg) and Reps with quick stepper chips.
 * RPE and RIR have been removed to keep logging simple and fast.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SwipeSetRow(
    set: PerformedSetEntity,
    onComplete: (reps: Int, weight: Double) -> Unit,
    onLongPress: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var reps by remember(set.id, set.reps) { mutableStateOf(set.reps.toString()) }
    var weight by remember(set.id, set.weight) { mutableStateOf(set.weight.toString()) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it != SwipeToDismissBoxValue.Settled && !set.completed) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onComplete(reps.toIntOrNull() ?: set.reps, weight.toDoubleOrNull() ?: set.weight)
            }
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Text("Complete ✓", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold) }
        },
    ) {
        val color by animateColorAsState(
            targetValue = if (set.completed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
            label = "setColor",
        )
        Card(
            modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress),
            colors = CardDefaults.cardColors(containerColor = color),
            border = BorderStroke(
                width = if (set.completed) 1.5.dp else 1.dp,
                color = if (set.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            ),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Header: Exercise Name, Set #, PR badge
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${set.exerciseName} • Set ${set.setNumber}", fontWeight = FontWeight.Bold)
                        if (set.notes.isNotBlank()) {
                            Text(set.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (set.isPr) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        ) {
                            Text("🏆 PR", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp)
                        }
                    }
                }

                // Quick Adjust Steppers for Weight & Reps
                if (!set.completed) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quick adjust:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilterChip(
                            selected = false,
                            onClick = {
                                val currentW = weight.toDoubleOrNull() ?: set.weight
                                weight = (currentW - 2.5).coerceAtLeast(0.0).toString()
                            },
                            label = { Text("-2.5kg", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                val currentW = weight.toDoubleOrNull() ?: set.weight
                                weight = (currentW + 2.5).toString()
                            },
                            label = { Text("+2.5kg", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                val currentR = reps.toIntOrNull() ?: set.reps
                                reps = (currentR - 1).coerceAtLeast(0).toString()
                            },
                            label = { Text("-1 rep", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                val currentR = reps.toIntOrNull() ?: set.reps
                                reps = (currentR + 1).toString()
                            },
                            label = { Text("+1 rep", fontSize = 11.sp) }
                        )
                    }
                }

                // Streamlined Inputs: Only Weight (kg) and Reps
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    SmallField("Weight (kg)", weight, { weight = it }, Modifier.weight(1f))
                    SmallField("Reps", reps, { reps = it }, Modifier.weight(1f))
                }

                // Log / Completed Button
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onComplete(reps.toIntOrNull() ?: set.reps, weight.toDoubleOrNull() ?: set.weight)
                    },
                    colors = if (set.completed) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    if (set.completed) {
                        Text("Completed ✓ (Tap to update)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    } else {
                        Text("Log Set ${set.setNumber} (${weight}kg × ${reps} reps)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
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
private fun SmallField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true,
    )
}
