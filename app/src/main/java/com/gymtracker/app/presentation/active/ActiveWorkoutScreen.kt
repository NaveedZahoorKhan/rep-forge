package com.gymtracker.app.presentation.active

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ActiveWorkoutViewModel @Inject constructor(
    private val repository: GymRepository,
    private val notificationHelper: NotificationHelper,
) : ViewModel() {
    private val sessionId = MutableStateFlow("")
    private var routeKey = ""
    private var timerJob: Job? = null

    val sets: StateFlow<List<PerformedSetEntity>> = sessionId
        .flatMapLatest { id -> if (id.isBlank()) flowOf(emptyList()) else repository.observeSessionSets(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var restRemaining by mutableStateOf(0)
        private set
    var restTotal by mutableStateOf(0)
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

    fun complete(set: PerformedSetEntity, reps: Int, weight: Double, rpe: Double?, rir: Int?, notes: String = "") {
        viewModelScope.launch {
            val completed = repository.completeSet(set.id, reps, weight, rpe, rir, notes)
            startRest(completed.restSeconds, completed.exerciseName)
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

    fun skipTimer() {
        timerJob?.cancel()
        restRemaining = 0
        restTotal = 0
        timerExercise = ""
    }

    private fun startRest(seconds: Int, exerciseName: String) {
        timerJob?.cancel()
        restTotal = seconds
        restRemaining = seconds
        timerExercise = exerciseName
        timerJob = viewModelScope.launch {
            while (restRemaining > 0) {
                delay(1_000)
                restRemaining -= 1
            }
            val profile = repository.observeUserProfile().first()
            notificationHelper.showRestComplete(
                exerciseName = exerciseName,
                sound = profile?.soundEnabled ?: true,
                vibration = profile?.vibrationEnabled ?: true,
            )
            timerExercise = ""
            restTotal = 0
        }
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
    var editingSet by remember { mutableStateOf<PerformedSetEntity?>(null) }
    var plateWeight by remember { mutableDoubleStateOf(100.0) }
    var formula by remember { mutableStateOf(OneRepMaxFormula.EPLEY) }
    val volume = sets.filter { it.completed }.sumOf { it.weight * it.reps }
    val latestSet = sets.lastOrNull { it.completed }
    val oneRm = latestSet?.let { OneRepMaxCalculator.estimate(it.weight, it.reps, formula) } ?: 0.0
    val plates = PlateCalculator.calculate(plateWeight)
    val suggestion = ProgressiveOverloadUseCase.suggestion(sets.filter { it.completed }, targetRepsMax = 12, lowerBodyLift = false)

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Active workout", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${volume.toInt()} kg volume", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { viewModel.cancel(onDone) }) { Text("Cancel") }
                Button(onClick = { viewModel.finish(onDone) }) { Text("Finish") }
            }
        }
        if (viewModel.restRemaining > 0) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rest ${viewModel.restRemaining}s - ${viewModel.timerExercise}", fontWeight = FontWeight.SemiBold)
                    LinearProgressIndicator(
                        progress = { 1f - (viewModel.restRemaining.toFloat() / viewModel.restTotal.coerceAtLeast(1)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = viewModel::skipTimer) { Text("Skip timer") }
                }
            }
        }
        Card {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Calculators", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = plateWeight.toString(),
                        onValueChange = { plateWeight = it.toDoubleOrNull() ?: plateWeight },
                        label = { Text("Bar weight kg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    Column(Modifier.weight(1f)) {
                        Text("Per side: ${plates.sidePlates.joinToString(" + ").ifBlank { "none" }}")
                        Text("1RM ${oneRm.toInt()} kg")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OneRepMaxFormula.entries.forEach {
                        FilterChip(selected = formula == it, onClick = { formula = it }, label = { Text(it.name.lowercase().replaceFirstChar { c -> c.titlecase() }) })
                    }
                }
                Text(suggestion, style = MaterialTheme.typography.bodySmall)
            }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(sets, key = { it.id }) { set ->
                SwipeSetRow(
                    set = set,
                    onComplete = { reps, weight, rpe, rir ->
                        viewModel.complete(set, reps, weight, rpe, rir)
                    },
                    onLongPress = { editingSet = set },
                )
            }
        }
    }

    editingSet?.let { set ->
        SetOptionsDialog(
            set = set,
            onDismiss = { editingSet = null },
            onSave = {
                viewModel.update(it)
                editingSet = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SwipeSetRow(
    set: PerformedSetEntity,
    onComplete: (Int, Double, Double?, Int?) -> Unit,
    onLongPress: () -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    var reps by remember(set.id, set.reps) { mutableStateOf(set.reps.toString()) }
    var weight by remember(set.id, set.weight) { mutableStateOf(set.weight.toString()) }
    var rpe by remember(set.id, set.rpe) { mutableStateOf(set.rpe?.toString().orEmpty()) }
    var rir by remember(set.id, set.rir) { mutableStateOf(set.rir?.toString().orEmpty()) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it != SwipeToDismissBoxValue.Settled && !set.completed) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onComplete(reps.toIntOrNull() ?: set.reps, weight.toDoubleOrNull() ?: set.weight, rpe.toDoubleOrNull(), rir.toIntOrNull())
            }
            false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Text("Complete", color = MaterialTheme.colorScheme.onPrimaryContainer) }
        },
    ) {
        val color by animateColorAsState(
            targetValue = if (set.completed) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            label = "setColor",
        )
        Card(
            modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onLongPress),
            colors = CardDefaults.cardColors(containerColor = color),
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${set.exerciseName} - Set ${set.setNumber}", fontWeight = FontWeight.SemiBold)
                    Text(if (set.isPr) "PR" else set.setType.name.replace('_', ' '))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallField("Reps", reps, { reps = it }, Modifier.weight(1f))
                    SmallField("Weight", weight, { weight = it }, Modifier.weight(1f))
                    SmallField("RPE", rpe, { rpe = it }, Modifier.weight(1f))
                    SmallField("RIR", rir, { rir = it }, Modifier.weight(1f))
                }
                Button(
                    enabled = !set.completed,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onComplete(reps.toIntOrNull() ?: set.reps, weight.toDoubleOrNull() ?: set.weight, rpe.toDoubleOrNull(), rir.toIntOrNull())
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (set.completed) "Completed" else "Complete set")
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
