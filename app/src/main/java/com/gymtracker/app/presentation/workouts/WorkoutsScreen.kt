@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.gymtracker.app.presentation.workouts

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import com.gymtracker.app.data.local.entity.SessionStatus
import java.time.Instant
import java.time.ZoneId
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.gymtracker.app.presentation.warmup.DynamicWarmUpPlayerModal
import com.gymtracker.app.presentation.history.WorkoutHistoryScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymtracker.app.data.local.entity.Difficulty
import com.gymtracker.app.data.local.entity.Equipment
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.data.local.entity.SetType
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WeekDay
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.label
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.model.DynamicWarmUpRoutine
import com.gymtracker.app.domain.model.WorkoutDraft
import com.gymtracker.app.domain.model.WorkoutExerciseDraft
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.DynamicWarmUpCatalog
import com.gymtracker.app.notification.WorkoutSoundPlayer
import com.gymtracker.app.presentation.components.ChipGroup
import com.gymtracker.app.presentation.components.EmptyState
import com.gymtracker.app.presentation.components.SectionTitle
import com.gymtracker.app.presentation.components.TagRow
import com.gymtracker.app.presentation.history.WorkoutHistoryScreen
import com.gymtracker.app.presentation.warmup.DynamicWarmUpPlayerModal
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EditableExerciseItem(
    val instanceId: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val exerciseName: String,
    val primaryMuscle: String,
    val equipment: String,
    var setCountText: String = "3",
    var repsMinText: String = "8",
    var repsMaxText: String = "12",
    var weightText: String = "0.0",
    var restSecondsText: String = "90",
    var setType: SetType = SetType.NORMAL,
    var supersetGroup: String = "",
    var amrapLastSet: Boolean = false,
    var isAdvancedExpanded: Boolean = false,
) {
    val validationError: String?
        get() {
            val sets = setCountText.toIntOrNull()
            if (sets == null || sets <= 0) return "$exerciseName: Sets must be at least 1"
            val repsMin = repsMinText.toIntOrNull()
            if (repsMin == null || repsMin <= 0) return "$exerciseName: Min reps must be at least 1"
            val repsMax = repsMaxText.toIntOrNull()
            if (repsMax == null || repsMax <= 0) return "$exerciseName: Max reps must be at least 1"
            if (repsMax < repsMin) return "$exerciseName: Max reps cannot be less than min reps"
            val weight = weightText.toDoubleOrNull()
            if (weight == null || weight < 0.0) return "$exerciseName: Weight cannot be negative"
            val rest = restSecondsText.toIntOrNull()
            if (rest == null || rest < 0) return "$exerciseName: Rest seconds cannot be negative"
            return null
        }
}

data class WorkoutsUiState(
    val workouts: List<WorkoutEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val schedule: List<WeeklyScheduleEntity> = emptyList(),
    val historySessions: List<WorkoutSessionEntity> = emptyList(),
    val profile: UserProfileEntity = UserProfileEntity(),
)

@HiltViewModel
class WorkoutsViewModel @Inject constructor(
    private val repository: GymRepository,
    val soundPlayer: WorkoutSoundPlayer,
) : ViewModel() {
    val state: StateFlow<WorkoutsUiState> = combine(
        repository.observeWorkouts(),
        repository.observeExercises(),
        repository.observeWeeklySchedule(),
        repository.observeHistory(),
        repository.observeUserProfile(),
    ) { workouts, exercises, schedule, history, profile ->
        WorkoutsUiState(
            workouts = workouts,
            exercises = exercises,
            schedule = schedule,
            historySessions = history,
            profile = profile ?: UserProfileEntity(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutsUiState())

    fun createWorkout(draft: WorkoutDraft, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            onCreated(repository.createCustomWorkout(draft))
        }
    }

    fun applySplit(splitName: String) {
        viewModelScope.launch {
            repository.applyRoutineSplitSchedule(splitName)
            val currentProfile = repository.observeUserProfile().first() ?: UserProfileEntity()
            repository.createOrUpdateProfile(currentProfile.copy(preferredSplit = splitName))
        }
    }

    fun assignWorkoutToDay(weekDay: WeekDay, workoutId: String, workoutName: String) {
        viewModelScope.launch {
            repository.deleteScheduleForDay(weekDay)
            val entity = WeeklyScheduleEntity(
                id = java.util.UUID.randomUUID().toString(),
                weekDay = weekDay,
                workoutId = workoutId,
                workoutName = workoutName,
            )
            repository.upsertSchedule(entity)
        }
    }

    fun removeSchedule(scheduleId: String) {
        viewModelScope.launch {
            repository.deleteSchedule(scheduleId)
        }
    }

    fun rescheduleWorkout(scheduleId: String, currentWeekDay: WeekDay, targetWeekDay: WeekDay, workoutId: String, workoutName: String) {
        viewModelScope.launch {
            repository.deleteSchedule(scheduleId)
            repository.deleteScheduleForDay(targetWeekDay)
            val entity = WeeklyScheduleEntity(
                id = java.util.UUID.randomUUID().toString(),
                weekDay = targetWeekDay,
                workoutId = workoutId,
                workoutName = workoutName,
            )
            repository.upsertSchedule(entity)
        }
    }
}

@Composable
fun WorkoutsScreen(
    onStartWorkout: (String) -> Unit,
    onOpenGemini: (() -> Unit)? = null,
    viewModel: WorkoutsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var editingWorkout by remember { mutableStateOf<WorkoutEntity?>(null) }
    var activeWarmUpRoutine by remember { mutableStateOf<DynamicWarmUpRoutine?>(null) }
    val tabs = listOf("Workouts", "History", if (editingWorkout != null) "Edit Routine" else "Create", "Library", "Planner")

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Train", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = {
                        if (index != 2 && tab == 2) {
                            editingWorkout = null
                        }
                        tab = index
                    },
                    text = { Text(title) }
                )
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> WorkoutList(
                    workouts = state.workouts,
                    onStartWorkout = onStartWorkout,
                    onEditWorkout = { workout ->
                        editingWorkout = workout
                        tab = 2
                    },
                    onOpenGemini = onOpenGemini,
                )
                1 -> WorkoutHistoryScreen()
                2 -> CreateWorkoutPanel(
                    editingWorkout = editingWorkout,
                    exercises = state.exercises,
                    viewModel = viewModel,
                    onStartWorkout = onStartWorkout,
                    onOpenGemini = onOpenGemini,
                    onSaved = {
                        editingWorkout = null
                        tab = 0
                    },
                    onDiscard = {
                        editingWorkout = null
                        tab = 0
                    }
                )
                3 -> ExerciseLibrary(state.exercises)
                4 -> PlannerPanel(
                    schedule = state.schedule,
                    workouts = state.workouts,
                    exercises = state.exercises,
                    historySessions = state.historySessions,
                    currentSplit = state.profile.preferredSplit,
                    onStartWorkout = onStartWorkout,
                    onApplySplit = { viewModel.applySplit(it) },
                    onStartWarmUp = { activeWarmUpRoutine = it },
                    onAssignWorkout = { weekDay, wId, wName -> viewModel.assignWorkoutToDay(weekDay, wId, wName) },
                    onRemoveSchedule = { sId -> viewModel.removeSchedule(sId) },
                    onRescheduleWorkout = { sId, curDay, targetDay, wId, wName -> viewModel.rescheduleWorkout(sId, curDay, targetDay, wId, wName) },
                )
            }
        }
    }

    activeWarmUpRoutine?.let { routine ->
        DynamicWarmUpPlayerModal(
            routine = routine,
            soundPlayer = viewModel.soundPlayer,
            onDismiss = { activeWarmUpRoutine = null },
            onStartWorkout = { wId ->
                activeWarmUpRoutine = null
                onStartWorkout(wId)
            },
            targetWorkoutId = state.workouts.firstOrNull { it.name.equals(routine.targetWorkoutName, ignoreCase = true) }?.id.orEmpty(),
        )
    }
}

@Composable
private fun WorkoutList(
    workouts: List<WorkoutEntity>,
    onStartWorkout: (String) -> Unit,
    onEditWorkout: (WorkoutEntity) -> Unit,
    onOpenGemini: (() -> Unit)? = null,
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Push", "Pull", "Legs", "Bro Split", "Upper", "Lower")

    val filteredWorkouts = remember(workouts, selectedFilter) {
        if (selectedFilter == "All") {
            workouts
        } else {
            workouts.filter {
                it.splitType.contains(selectedFilter, ignoreCase = true) ||
                it.name.contains(selectedFilter, ignoreCase = true)
            }
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        onOpenGemini?.let { openGemini ->
            item {
                Card(
                    onClick = openGemini,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Gemini AI Workout Builder", fontWeight = FontWeight.Bold)
                            Text(
                                "Generate a routine tailored to your registered gym machines & personal notes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filters) { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp) }
                    )
                }
            }
        }

        if (filteredWorkouts.isEmpty()) {
            item {
                EmptyState(
                    title = "No workouts found",
                    detail = if (selectedFilter != "All") "No workouts matching '$selectedFilter'" else "Create a custom workout or build with Gemini to get started",
                )
            }
        } else {
            items(filteredWorkouts, key = { it.id }) { workout ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(workout.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    workout.splitType,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (workout.description.isNotBlank()) {
                            Text(workout.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                onClick = { onEditWorkout(workout) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Routine", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit", fontSize = 13.sp)
                            }

                            Button(
                                onClick = { onStartWorkout(workout.id) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Text("⚡ Start Workout", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateWorkoutPanel(
    editingWorkout: WorkoutEntity? = null,
    exercises: List<ExerciseEntity>,
    viewModel: WorkoutsViewModel,
    onStartWorkout: (String) -> Unit,
    onOpenGemini: (() -> Unit)? = null,
    onSaved: () -> Unit,
    onDiscard: () -> Unit,
) {
    var name by remember(editingWorkout) { mutableStateOf(editingWorkout?.name ?: "Custom Strength Day") }
    var split by remember(editingWorkout) { mutableStateOf(editingWorkout?.splitType ?: "Custom") }
    var description by remember(editingWorkout) { mutableStateOf(editingWorkout?.description ?: "Custom workout") }

    val selectedExercises = remember(editingWorkout) { mutableStateListOf<EditableExerciseItem>() }

    // Pre-populate default exercises when creating new if list is empty
    LaunchedEffect(editingWorkout, exercises) {
        if (selectedExercises.isEmpty() && exercises.isNotEmpty()) {
            if (editingWorkout == null) {
                val defaultList = exercises.take(3).map { ex ->
                    EditableExerciseItem(
                        exerciseId = ex.id,
                        exerciseName = ex.name,
                        primaryMuscle = ex.primaryMuscle.label(),
                        equipment = ex.equipment.label(),
                    )
                }
                selectedExercises.addAll(defaultList)
            } else {
                // If editing existing workout, add default exercises for demonstration/editing
                val defaultList = exercises.take(2).map { ex ->
                    EditableExerciseItem(
                        exerciseId = ex.id,
                        exerciseName = ex.name,
                        primaryMuscle = ex.primaryMuscle.label(),
                        equipment = ex.equipment.label(),
                    )
                }
                selectedExercises.addAll(defaultList)
            }
        }
    }

    var showExercisePicker by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Undo state for exercise removal
    var recentlyRemoved by remember { mutableStateOf<Pair<Int, EditableExerciseItem>?>(null) }

    val isDirty = name.isNotBlank() || selectedExercises.isNotEmpty()
    BackHandler(enabled = isDirty) {
        showDiscardDialog = true
    }

    val exerciseErrors = selectedExercises.mapNotNull { it.validationError }
    val validationError: String? = when {
        name.isBlank() -> "Workout name cannot be empty"
        selectedExercises.isEmpty() -> "Select at least 1 exercise"
        exerciseErrors.isNotEmpty() -> exerciseErrors.first()
        else -> null
    }
    val isValid = validationError == null

    Box(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                onOpenGemini?.let { openGemini ->
                    item {
                        Card(
                            onClick = openGemini,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f)) {
                                    Text("Want AI to build this for you?", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Snap gym machines & set health goals for instant custom routines",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        if (editingWorkout != null) "Edit Routine" else "Create Routine",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Routine name") },
                        isError = name.isBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = split,
                            onValueChange = { split = it },
                            label = { Text("Split") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionTitle("Exercises (${selectedExercises.size})")
                        Button(
                            onClick = { showExercisePicker = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Exercise")
                        }
                    }
                }

                // Recently removed Undo banner
                recentlyRemoved?.let { (index, item) ->
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Removed '${item.exerciseName}'",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                TextButton(
                                    onClick = {
                                        val targetIndex = index.coerceIn(0, selectedExercises.size)
                                        selectedExercises.add(targetIndex, item)
                                        recentlyRemoved = null
                                    },
                                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo removal", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Undo", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (selectedExercises.isEmpty()) {
                    item {
                        EmptyState(
                            title = "No exercises added",
                            detail = "Click 'Add Exercise' to choose exercises for this routine.",
                        )
                    }
                } else {
                    itemsIndexed(selectedExercises, key = { _, item -> item.instanceId }) { index, item ->
                        ExerciseRowCard(
                            item = item,
                            index = index,
                            totalCount = selectedExercises.size,
                            onMoveUp = {
                                if (index > 0) {
                                    val element = selectedExercises.removeAt(index)
                                    selectedExercises.add(index - 1, element)
                                }
                            },
                            onMoveDown = {
                                if (index < selectedExercises.lastIndex) {
                                    val element = selectedExercises.removeAt(index)
                                    selectedExercises.add(index + 1, element)
                                }
                            },
                            onRemove = {
                                recentlyRemoved = Pair(index, item)
                                selectedExercises.removeAt(index)
                            },
                            onUpdate = { updated ->
                                selectedExercises[index] = updated
                            }
                        )
                    }
                }
            }

            // Persistent Sticky Save Bar
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                tonalElevation = 6.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (validationError != null) {
                        Text(
                            validationError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f).sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                            enabled = isValid,
                            onClick = {
                                val draft = WorkoutDraft(
                                    name = name.trim(),
                                    description = description.trim(),
                                    splitType = split.trim().ifBlank { "Custom" },
                                    exercises = selectedExercises.map { ex ->
                                        WorkoutExerciseDraft(
                                            exerciseId = ex.exerciseId,
                                            restSeconds = ex.restSecondsText.toIntOrNull() ?: 90,
                                            setCount = ex.setCountText.toIntOrNull() ?: 3,
                                            repsMin = ex.repsMinText.toIntOrNull() ?: 8,
                                            repsMax = ex.repsMaxText.toIntOrNull() ?: 12,
                                            weight = ex.weightText.toDoubleOrNull() ?: 0.0,
                                            setType = ex.setType,
                                            supersetGroup = ex.supersetGroup.takeIf { it.isNotBlank() },
                                            amrapLastSet = ex.amrapLastSet,
                                        )
                                    },
                                )
                                viewModel.createWorkout(draft) {
                                    onSaved()
                                }
                            }
                        ) {
                            Text("Save Routine")
                        }

                        Button(
                            modifier = Modifier.weight(1.2f).sizeIn(minWidth = 48.dp, minHeight = 48.dp),
                            enabled = isValid,
                            onClick = {
                                val draft = WorkoutDraft(
                                    name = name.trim(),
                                    description = description.trim(),
                                    splitType = split.trim().ifBlank { "Custom" },
                                    exercises = selectedExercises.map { ex ->
                                        WorkoutExerciseDraft(
                                            exerciseId = ex.exerciseId,
                                            restSeconds = ex.restSecondsText.toIntOrNull() ?: 90,
                                            setCount = ex.setCountText.toIntOrNull() ?: 3,
                                            repsMin = ex.repsMinText.toIntOrNull() ?: 8,
                                            repsMax = ex.repsMaxText.toIntOrNull() ?: 12,
                                            weight = ex.weightText.toDoubleOrNull() ?: 0.0,
                                            setType = ex.setType,
                                            supersetGroup = ex.supersetGroup.takeIf { it.isNotBlank() },
                                            amrapLastSet = ex.amrapLastSet,
                                        )
                                    },
                                )
                                viewModel.createWorkout(draft) { id ->
                                    onSaved()
                                    onStartWorkout(id)
                                }
                            }
                        ) {
                            Text("Save & Start")
                        }
                    }
                }
            }
        }
    }

    if (showExercisePicker) {
        ExercisePickerModal(
            allExercises = exercises,
            existingSelectedIds = selectedExercises.map { it.exerciseId }.toSet(),
            onDismiss = { showExercisePicker = false },
            onConfirmSelection = { newlySelectedIds ->
                val existingIds = selectedExercises.map { it.exerciseId }.toSet()
                newlySelectedIds.filterNot { it in existingIds }.forEach { id ->
                    val ex = exercises.firstOrNull { it.id == id }
                    if (ex != null) {
                        selectedExercises.add(
                            EditableExerciseItem(
                                exerciseId = ex.id,
                                exerciseName = ex.name,
                                primaryMuscle = ex.primaryMuscle.label(),
                                equipment = ex.equipment.label(),
                            )
                        )
                    }
                }
                showExercisePicker = false
            }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard unsaved changes?") },
            text = { Text("You have unsaved changes in this routine. Are you sure you want to discard them?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onDiscard()
                    },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardDialog = false },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text("Keep editing")
                }
            }
        )
    }
}

@Composable
private fun ExerciseRowCard(
    item: EditableExerciseItem,
    index: Int,
    totalCount: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onUpdate: (EditableExerciseItem) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Row 1: Header + Reorder & Remove controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            "${index + 1}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Column {
                        Text(item.exerciseName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("${item.primaryMuscle} • ${item.equipment}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = index > 0,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "Move ${item.exerciseName} up",
                            tint = if (index > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    IconButton(
                        onClick = onMoveDown,
                        enabled = index < totalCount - 1,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = "Move ${item.exerciseName} down",
                            tint = if (index < totalCount - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove ${item.exerciseName}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Row 2: Target Inputs (Sets, Reps, Weight, Rest) with explicit units
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                SmallNumberField(
                    label = "Sets",
                    value = item.setCountText,
                    onValueChange = { onUpdate(item.copy(setCountText = it)) },
                    modifier = Modifier.weight(1f)
                )
                SmallNumberField(
                    label = "Min reps",
                    value = item.repsMinText,
                    onValueChange = { onUpdate(item.copy(repsMinText = it)) },
                    modifier = Modifier.weight(1f)
                )
                SmallNumberField(
                    label = "Max reps",
                    value = item.repsMaxText,
                    onValueChange = { onUpdate(item.copy(repsMaxText = it)) },
                    modifier = Modifier.weight(1f)
                )
                SmallNumberField(
                    label = "Weight (kg)",
                    value = item.weightText,
                    onValueChange = { onUpdate(item.copy(weightText = it)) },
                    modifier = Modifier.weight(1.1f)
                )
                SmallNumberField(
                    label = "Rest (s)",
                    value = item.restSecondsText,
                    onValueChange = { onUpdate(item.copy(restSecondsText = it)) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Inline validation error if row is invalid
            item.validationError?.let { err ->
                Text(
                    err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            // Row 3: Advanced Set Options toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { onUpdate(item.copy(isAdvancedExpanded = !item.isAdvancedExpanded)) },
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text(
                        if (item.isAdvancedExpanded) "Hide Advanced Options" else "Advanced Options",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        if (item.isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Expandable Advanced Options section
            AnimatedVisibility(visible = item.isAdvancedExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    SetTypeDropdown(setType = item.setType, onChange = { onUpdate(item.copy(setType = it)) })

                    OutlinedTextField(
                        value = item.supersetGroup,
                        onValueChange = { onUpdate(item.copy(supersetGroup = it)) },
                        label = { Text("Superset Group Tag") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("AMRAP Last Set", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = item.amrapLastSet,
                            onCheckedChange = { onUpdate(item.copy(amrapLastSet = it)) },
                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerModal(
    allExercises: List<ExerciseEntity>,
    existingSelectedIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirmSelection: (Set<String>) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscleFilter by remember { mutableStateOf<String?>(null) }

    // Picker retains selections continuously across search/filtering
    val pickerSelectedIds = remember { mutableStateListOf<String>().apply { addAll(existingSelectedIds) } }

    val filteredExercises = remember(allExercises, searchQuery, selectedMuscleFilter) {
        allExercises.filter { exercise ->
            (searchQuery.isBlank() || exercise.name.contains(searchQuery, ignoreCase = true)) &&
            (selectedMuscleFilter == null || exercise.primaryMuscle.label().equals(selectedMuscleFilter, ignoreCase = true))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Exercises", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close picker")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search exercises...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                // Muscle Filter Chips
                val muscles = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedMuscleFilter == null,
                            onClick = { selectedMuscleFilter = null },
                            label = { Text("All", fontSize = 11.sp) },
                            modifier = Modifier.sizeIn(minHeight = 48.dp)
                        )
                    }
                    items(muscles) { muscle ->
                        FilterChip(
                            selected = selectedMuscleFilter == muscle,
                            onClick = { selectedMuscleFilter = if (selectedMuscleFilter == muscle) null else muscle },
                            label = { Text(muscle, fontSize = 11.sp) },
                            modifier = Modifier.sizeIn(minHeight = 48.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Exercise List
                Box(Modifier.weight(1f)) {
                    if (filteredExercises.isEmpty()) {
                        EmptyState(
                            title = "No exercises found",
                            detail = "Try adjusting your search or muscle filter.",
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredExercises, key = { it.id }) { exercise ->
                                val isChecked = pickerSelectedIds.contains(exercise.id)
                                val isAlreadyInRoutine = existingSelectedIds.contains(exercise.id)

                                Card(
                                    onClick = {
                                        if (isChecked) pickerSelectedIds.remove(exercise.id) else pickerSelectedIds.add(exercise.id)
                                    },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked) pickerSelectedIds.add(exercise.id) else pickerSelectedIds.remove(exercise.id)
                                            },
                                            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                        )
                                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(exercise.name, fontWeight = FontWeight.SemiBold)
                                                if (isAlreadyInRoutine) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.secondaryContainer
                                                    ) {
                                                        Text("Added", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                                    }
                                                }
                                            }
                                            TagRow(listOf(exercise.primaryMuscle.label(), exercise.equipment.label(), exercise.difficulty.label()))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Sticky Selection Footer
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 4.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${pickerSelectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = { onConfirmSelection(pickerSelectedIds.toSet()) },
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Text("Confirm (${pickerSelectedIds.size})")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseLibrary(exercises: List<ExerciseEntity>) {
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<String?>(null) }
    var equipment by remember { mutableStateOf<String?>(null) }
    var difficulty by remember { mutableStateOf<String?>(null) }
    val filtered by remember(query, muscle, equipment, difficulty, exercises) {
        derivedStateOf {
            exercises.filter {
                (query.isBlank() || it.name.contains(query, ignoreCase = true)) &&
                    (muscle == null || it.primaryMuscle.label() == muscle) &&
                    (equipment == null || it.equipment.label() == equipment) &&
                    (difficulty == null || it.difficulty.label() == difficulty)
            }
        }
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search exercises") }, modifier = Modifier.fillMaxWidth())
        }
        item { ChipGroup(items = MuscleGroup.entries.map { it.label() }, selected = muscle, onSelected = { muscle = it }) }
        item { ChipGroup(items = Equipment.entries.map { it.label() }, selected = equipment, onSelected = { equipment = it }) }
        item { ChipGroup(items = Difficulty.entries.map { it.label() }, selected = difficulty, onSelected = { difficulty = it }) }
        items(filtered, key = { it.id }) { exercise ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(exercise.name, fontWeight = FontWeight.SemiBold)
                    TagRow(listOf(exercise.primaryMuscle.label(), exercise.equipment.label(), exercise.difficulty.label()))
                    Text(exercise.instructions, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun PlannerPanel(
    schedule: List<WeeklyScheduleEntity>,
    workouts: List<WorkoutEntity>,
    exercises: List<ExerciseEntity>,
    historySessions: List<WorkoutSessionEntity>,
    currentSplit: String,
    onStartWorkout: (String) -> Unit,
    onApplySplit: (String) -> Unit,
    onStartWarmUp: (DynamicWarmUpRoutine) -> Unit,
    onAssignWorkout: (WeekDay, String, String) -> Unit,
    onRemoveSchedule: (String) -> Unit,
    onRescheduleWorkout: (scheduleId: String, currentWeekDay: WeekDay, targetWeekDay: WeekDay, workoutId: String, workoutName: String) -> Unit,
) {
    val currentSplitName = currentSplit.ifBlank { "Push Pull Legs (PPL)" }
    val splits = listOf(
        "Push Pull Legs (PPL)",
        "PPLUL (Push Pull Legs Upper Lower)",
        "Upper / Lower Split",
        "Bro Split (Body Part Split)",
        "Full Body Circuit",
    )

    var weekOffset by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Dialog state for assigning / rescheduling / removing
    var assignWorkoutForDay by remember { mutableStateOf<WeekDay?>(null) }
    var rescheduleScheduleItem by remember { mutableStateOf<WeeklyScheduleEntity?>(null) }
    var removeConfirmScheduleItem by remember { mutableStateOf<WeeklyScheduleEntity?>(null) }

    val todayDate = remember { LocalDate.now() }
    val currentMonday = remember(todayDate, weekOffset) {
        val mon = todayDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        mon.plusWeeks(weekOffset.toLong())
    }

    val weekDays = remember(currentMonday) {
        (0..6).map { currentMonday.plusDays(it.toLong()) }
    }

    val selectedDayOfWeek = when (selectedDate.dayOfWeek) {
        DayOfWeek.MONDAY -> WeekDay.MONDAY
        DayOfWeek.TUESDAY -> WeekDay.TUESDAY
        DayOfWeek.WEDNESDAY -> WeekDay.WEDNESDAY
        DayOfWeek.THURSDAY -> WeekDay.THURSDAY
        DayOfWeek.FRIDAY -> WeekDay.FRIDAY
        DayOfWeek.SATURDAY -> WeekDay.SATURDAY
        DayOfWeek.SUNDAY -> WeekDay.SUNDAY
    }

    val selectedSchedule = schedule.firstOrNull { it.weekDay == selectedDayOfWeek }
    val selectedScheduledWorkout = selectedSchedule?.let { s ->
        workouts.firstOrNull { it.id == s.workoutId } ?: workouts.firstOrNull { it.name.equals(s.workoutName, ignoreCase = true) }
    }

    // Find completed sessions on selectedDate
    val selectedDayCompletedSessions = remember(historySessions, selectedDate) {
        val startOfDay = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = selectedDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        historySessions.filter { session ->
            val timestamp = session.endedAt ?: session.startedAt
            timestamp in startOfDay until endOfDay && session.status == SessionStatus.COMPLETED
        }
    }

    val dateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("EEE, MMM d") }
    val headerDateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy") }
    val monthYearFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Week Strip Header & Navigation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Month / Year Title + Prev, Today, Next buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentMonday.format(monthYearFormatter),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    weekOffset -= 1
                                    selectedDate = selectedDate.minusWeeks(1)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous week")
                            }

                            if (weekOffset != 0) {
                                OutlinedButton(
                                    onClick = {
                                        weekOffset = 0
                                        selectedDate = LocalDate.now()
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Today", fontSize = 11.sp)
                                }
                            }

                            IconButton(
                                onClick = {
                                    weekOffset += 1
                                    selectedDate = selectedDate.plusWeeks(1)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next week")
                            }
                        }
                    }

                    // Week Strip Days (7 columns)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        weekDays.forEach { date ->
                            val isSelected = date == selectedDate
                            val isToday = date == todayDate
                            val dayEnum = when (date.dayOfWeek) {
                                DayOfWeek.MONDAY -> WeekDay.MONDAY
                                DayOfWeek.TUESDAY -> WeekDay.TUESDAY
                                DayOfWeek.WEDNESDAY -> WeekDay.WEDNESDAY
                                DayOfWeek.THURSDAY -> WeekDay.THURSDAY
                                DayOfWeek.FRIDAY -> WeekDay.FRIDAY
                                DayOfWeek.SATURDAY -> WeekDay.SATURDAY
                                DayOfWeek.SUNDAY -> WeekDay.SUNDAY
                            }
                            val daySchedule = schedule.firstOrNull { it.weekDay == dayEnum }
                            val dayCompleted = historySessions.any { session ->
                                val timestamp = session.endedAt ?: session.startedAt
                                val sessionDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
                                sessionDate == date && session.status == SessionStatus.COMPLETED
                            }

                            Surface(
                                onClick = { selectedDate = date },
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                },
                                border = BorderStroke(
                                    width = if (isSelected || isToday) 1.5.dp else 0.5.dp,
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .size(width = 44.dp, height = 64.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = date.dayOfWeek.name.take(3),
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )

                                    // Indicator dots for status
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        if (dayCompleted) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(if (isSelected) Color.White else MaterialTheme.colorScheme.tertiary, shape = RoundedCornerShape(50))
                                            )
                                        } else if (daySchedule != null) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50))
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

        // Selected Day Vertical Agenda Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedDate.format(headerDateFormatter),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (selectedDate == todayDate) {
                        Text(
                            text = "Today's Agenda",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (selectedSchedule == null) {
                    Button(
                        onClick = { assignWorkoutForDay = selectedDayOfWeek },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Schedule Workout", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 1. Completed Workouts Section (if any on this day)
        if (selectedDayCompletedSessions.isNotEmpty()) {
            item {
                SectionTitle("Completed Sessions")
            }
            items(selectedDayCompletedSessions, key = { it.id }) { session ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Text(session.workoutName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            }
                            Text(
                                text = "Completed • Duration: ${session.durationSeconds / 60}m • Volume: ${session.totalVolume.toInt()} kg",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text("HISTORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }

        // 2. Scheduled Routine Entry for Selected Day
        item {
            if (selectedSchedule != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedDate == todayDate) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (selectedDate == todayDate) 1.5.dp else 1.dp,
                        color = if (selectedDate == todayDate) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Scheduled Routine", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selectedSchedule.workoutName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { rescheduleScheduleItem = selectedSchedule }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = "Reschedule workout")
                                }
                                IconButton(onClick = { removeConfirmScheduleItem = selectedSchedule }, modifier = Modifier.size(48.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove schedule", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Text(
                            text = "${selectedSchedule.periodizationType.name} periodization • Deload every ${selectedSchedule.deloadEveryWeeks} wks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            selectedScheduledWorkout?.let { w ->
                                val routine = remember(w, exercises) {
                                    DynamicWarmUpCatalog.suggestRoutineForWorkout(w, exercises)
                                }
                                OutlinedButton(
                                    onClick = { onStartWarmUp(routine) },
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFF6F00)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                ) {
                                    Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFFF6F00), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Warm-Up (${routine.primaryMuscleGroup.name.lowercase().replaceFirstChar { it.titlecase() }})", color = Color(0xFFFF6F00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onStartWorkout(w.id) },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                ) {
                                    Text(if (selectedDate == todayDate) "⚡ Start Today's Workout" else "⚡ Start Workout", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else if (selectedDayCompletedSessions.isEmpty()) {
                // Empty Rest Day View (Visually quiet)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text("REST DAY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                                Text("No workout scheduled", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Text(
                            text = "Rest days are crucial for muscle recovery, adaptation, and preventing overtraining.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val restMobility = DynamicWarmUpCatalog.getWarmUpRoutine(MuscleGroup.FULL_BODY, workoutName = "Rest Day Mobility")
                                    onStartWarmUp(restMobility)
                                },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, Color(0xFFFF6F00).copy(alpha = 0.6f)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
                                Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFFF6F00), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Mobility Flow", color = Color(0xFFFF6F00), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            TextButton(
                                onClick = { assignWorkoutForDay = selectedDayOfWeek },
                            ) {
                                Text("Schedule Workout", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Split Configuration Collapsible Section
        item {
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
                        Text("Active Routine Split Preset", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text("Preset", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text("Selecting a split preset will overwrite your current weekly schedule:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    splits.forEach { splitItem ->
                        val isSelected = currentSplitName.trim().equals(splitItem.trim(), ignoreCase = true)
                        Surface(
                            onClick = { onApplySplit(splitItem) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onApplySplit(splitItem) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text(splitItem, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Assign Workout Dialog
    assignWorkoutForDay?.let { targetDay ->
        var selectedWorkoutToAssign by remember { mutableStateOf(workouts.firstOrNull()?.id.orEmpty()) }
        val targetDayName = targetDay.name.lowercase().replaceFirstChar { it.titlecase() }

        AlertDialog(
            onDismissRequest = { assignWorkoutForDay = null },
            title = { Text("Schedule Workout for $targetDayName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Selected Date: ${selectedDate.format(headerDateFormatter)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("Select a workout routine to assign:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (workouts.isEmpty()) {
                        Text("No custom or template workouts available. Please create a workout first.", color = MaterialTheme.colorScheme.error)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().size(width = 300.dp, height = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(workouts, key = { it.id }) { w ->
                                val isSelected = w.id == selectedWorkoutToAssign
                                Surface(
                                    onClick = { selectedWorkoutToAssign = w.id },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 0.5.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RadioButton(selected = isSelected, onClick = { selectedWorkoutToAssign = w.id })
                                        Column {
                                            Text(w.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(w.splitType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = selectedWorkoutToAssign.isNotBlank(),
                    onClick = {
                        val chosenWorkout = workouts.firstOrNull { it.id == selectedWorkoutToAssign }
                        if (chosenWorkout != null) {
                            onAssignWorkout(targetDay, chosenWorkout.id, chosenWorkout.name)
                        }
                        assignWorkoutForDay = null
                    }
                ) {
                    Text("Confirm Schedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { assignWorkoutForDay = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reschedule Dialog
    rescheduleScheduleItem?.let { currentSchedule ->
        var selectedTargetDay by remember { mutableStateOf(currentSchedule.weekDay) }
        val weekDaysList = WeekDay.entries

        AlertDialog(
            onDismissRequest = { rescheduleScheduleItem = null },
            title = { Text("Reschedule '${currentSchedule.workoutName}'") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Move from ${currentSchedule.weekDay.name.lowercase().replaceFirstChar { it.titlecase() }} to another day:", style = MaterialTheme.typography.bodySmall)

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        weekDaysList.forEach { day ->
                            val dayName = day.name.lowercase().replaceFirstChar { it.titlecase() }
                            val isSelected = day == selectedTargetDay
                            Surface(
                                onClick = { selectedTargetDay = day },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    if (isSelected) 1.5.dp else 0.5.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(selected = isSelected, onClick = { selectedTargetDay = day })
                                    Text(dayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRescheduleWorkout(
                            currentSchedule.id,
                            currentSchedule.weekDay,
                            selectedTargetDay,
                            currentSchedule.workoutId,
                            currentSchedule.workoutName
                        )
                        rescheduleScheduleItem = null
                    }
                ) {
                    Text("Confirm Reschedule")
                }
            },
            dismissButton = {
                TextButton(onClick = { rescheduleScheduleItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remove Confirmation Dialog
    removeConfirmScheduleItem?.let { currentSchedule ->
        AlertDialog(
            onDismissRequest = { removeConfirmScheduleItem = null },
            title = { Text("Remove Schedule") },
            text = {
                Text("Are you sure you want to remove '${currentSchedule.workoutName}' from ${currentSchedule.weekDay.name.lowercase().replaceFirstChar { it.titlecase() }}? The day will revert to a Rest Day.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        onRemoveSchedule(currentSchedule.id)
                        removeConfirmScheduleItem = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { removeConfirmScheduleItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SmallNumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 10.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier,
    )
}

@Composable
private fun SetTypeDropdown(setType: SetType, onChange: (SetType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = setType.name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() },
            onValueChange = {},
            readOnly = true,
            label = { Text("Set type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SetType.entries.forEach {
                DropdownMenuItem(text = { Text(it.name.lowercase().replace('_', ' ').replaceFirstChar { c -> c.titlecase() }) }, onClick = {
                    onChange(it)
                    expanded = false
                })
            }
        }
    }
}
