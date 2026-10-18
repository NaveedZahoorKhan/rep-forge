@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gymtracker.app.presentation.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.domain.model.DynamicWarmUpRoutine
import com.gymtracker.app.domain.usecase.DynamicWarmUpCatalog
import com.gymtracker.app.notification.WorkoutSoundPlayer
import com.gymtracker.app.presentation.warmup.DynamicWarmUpPlayerModal
import com.gymtracker.app.presentation.history.WorkoutHistoryScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymtracker.app.data.local.entity.Difficulty
import com.gymtracker.app.data.local.entity.Equipment
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.SetType
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WeekDay
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.domain.model.WorkoutDraft
import com.gymtracker.app.domain.model.WorkoutExerciseDraft
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.presentation.components.ChipGroup
import com.gymtracker.app.presentation.components.EmptyState
import com.gymtracker.app.presentation.components.SectionTitle
import com.gymtracker.app.presentation.components.TagRow
import com.gymtracker.app.data.local.entity.label
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutsUiState(
    val workouts: List<WorkoutEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val schedule: List<WeeklyScheduleEntity> = emptyList(),
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
        repository.observeUserProfile(),
    ) { workouts, exercises, schedule, profile ->
        WorkoutsUiState(
            workouts = workouts,
            exercises = exercises,
            schedule = schedule,
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
}

@Composable
fun WorkoutsScreen(
    onStartWorkout: (String) -> Unit,
    onOpenGemini: (() -> Unit)? = null,
    viewModel: WorkoutsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var activeWarmUpRoutine by remember { mutableStateOf<DynamicWarmUpRoutine?>(null) }
    val tabs = listOf("Workouts", "History", "Create", "Library", "Planner")

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Workouts", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> WorkoutList(
                    workouts = state.workouts,
                    onStartWorkout = onStartWorkout,
                    onOpenGemini = onOpenGemini,
                )
                1 -> WorkoutHistoryScreen()
                2 -> CreateWorkoutPanel(
                    exercises = state.exercises,
                    viewModel = viewModel,
                    onStartWorkout = onStartWorkout,
                    onOpenGemini = onOpenGemini,
                    onSaved = { tab = 0 }
                )
                3 -> ExerciseLibrary(state.exercises)
                4 -> PlannerPanel(
                    schedule = state.schedule,
                    workouts = state.workouts,
                    exercises = state.exercises,
                    currentSplit = state.profile.preferredSplit,
                    onStartWorkout = onStartWorkout,
                    onApplySplit = { viewModel.applySplit(it) },
                    onStartWarmUp = { activeWarmUpRoutine = it },
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(workout.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
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
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { onStartWorkout(workout.id) },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateWorkoutPanel(
    exercises: List<ExerciseEntity>,
    viewModel: WorkoutsViewModel,
    onStartWorkout: (String) -> Unit,
    onOpenGemini: (() -> Unit)? = null,
    onSaved: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf("Custom Strength Day") }
    var split by remember { mutableStateOf("Custom") }
    var sets by remember { mutableIntStateOf(3) }
    var repsMin by remember { mutableIntStateOf(8) }
    var repsMax by remember { mutableIntStateOf(12) }
    var weight by remember { mutableDoubleStateOf(0.0) }
    var rest by remember { mutableIntStateOf(90) }
    var setType by remember { mutableStateOf(SetType.NORMAL) }
    var superset by remember { mutableStateOf("") }
    var amrap by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() }

    var exerciseSearchQuery by remember { mutableStateOf("") }
    var selectedMuscleFilter by remember { mutableStateOf<String?>(null) }

    val filteredExercises = remember(exercises, exerciseSearchQuery, selectedMuscleFilter) {
        exercises.filter { exercise ->
            (exerciseSearchQuery.isBlank() || exercise.name.contains(exerciseSearchQuery, ignoreCase = true)) &&
            (selectedMuscleFilter == null || exercise.primaryMuscle.label().equals(selectedMuscleFilter, ignoreCase = true))
        }
    }

    val validationError: String? = when {
        name.isBlank() -> "Workout name cannot be empty"
        sets <= 0 -> "Sets must be at least 1"
        repsMin <= 0 -> "Min reps must be at least 1"
        repsMax < repsMin -> "Max reps ($repsMax) cannot be less than min reps ($repsMin)"
        weight < 0.0 -> "Starting weight cannot be negative"
        selected.isEmpty() -> "Select at least 1 exercise"
        else -> null
    }
    val isValid = validationError == null

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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Workout name") },
                    isError = name.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = split,
                    onValueChange = { split = it },
                    label = { Text("Split") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                SectionTitle("Set defaults")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SmallNumberField(
                        label = "Sets",
                        value = sets.toString(),
                        onValueChange = { sets = it.toIntOrNull() ?: 0 },
                        modifier = Modifier.weight(1f)
                    )
                    SmallNumberField(
                        label = "Min reps",
                        value = repsMin.toString(),
                        onValueChange = { repsMin = it.toIntOrNull() ?: 0 },
                        modifier = Modifier.weight(1f)
                    )
                    SmallNumberField(
                        label = "Max reps",
                        value = repsMax.toString(),
                        onValueChange = { repsMax = it.toIntOrNull() ?: 0 },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (repsMax < repsMin) {
                    Text(
                        "Max reps cannot be less than min reps",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            item {
                SmallNumberField(
                    label = "Starting weight kg",
                    value = weight.toString(),
                    onValueChange = { weight = it.toDoubleOrNull() ?: 0.0 },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Rest ${rest}s", fontWeight = FontWeight.SemiBold)
                Slider(value = rest.toFloat(), onValueChange = { rest = it.toInt() }, valueRange = 30f..180f, steps = 4)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30, 60, 90, 120, 180).forEach {
                        FilterChip(selected = rest == it, onClick = { rest = it }, label = { Text("${it}s") })
                    }
                }
            }

            item {
                SetTypeDropdown(setType = setType, onChange = { setType = it })
            }

            item {
                OutlinedTextField(
                    value = superset,
                    onValueChange = { superset = it },
                    label = { Text("Superset group") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = amrap, onCheckedChange = { amrap = it })
                    Spacer(Modifier.width(8.dp))
                    Text("AMRAP last set")
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Select Exercises (${selected.size} selected)")
                    if (selected.isNotEmpty()) {
                        TextButton(onClick = { selected.clear() }) {
                            Text("Clear selection")
                        }
                    }
                }
                OutlinedTextField(
                    value = exerciseSearchQuery,
                    onValueChange = { exerciseSearchQuery = it },
                    placeholder = { Text("Search exercises...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (exerciseSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { exerciseSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            item {
                val muscles = listOf("Chest", "Back", "Legs", "Shoulders", "Arms", "Core")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedMuscleFilter == null,
                            onClick = { selectedMuscleFilter = null },
                            label = { Text("All Muscles", fontSize = 11.sp) }
                        )
                    }
                    items(muscles) { muscle ->
                        FilterChip(
                            selected = selectedMuscleFilter == muscle,
                            onClick = { selectedMuscleFilter = if (selectedMuscleFilter == muscle) null else muscle },
                            label = { Text(muscle, fontSize = 11.sp) }
                        )
                    }
                }
            }

            if (filteredExercises.isEmpty()) {
                item {
                    EmptyState(
                        title = "No exercises found",
                        detail = "Try adjusting your exercise search or muscle filter",
                    )
                }
            } else {
                items(filteredExercises, key = { it.id }) { exercise ->
                    val checked = selected.contains(exercise.id)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (checked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    if (checked) selected.remove(exercise.id) else selected.add(exercise.id)
                                },
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(exercise.name, fontWeight = FontWeight.SemiBold)
                                TagRow(listOf(exercise.primaryMuscle.label(), exercise.equipment.label(), exercise.difficulty.label()))
                            }
                        }
                    }
                }
            }
        }

        // Persistent / Sticky Save Action Bar
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
                        modifier = Modifier.weight(1f),
                        enabled = isValid,
                        onClick = {
                            val draft = WorkoutDraft(
                                name = name.trim(),
                                description = "Custom workout",
                                splitType = split.trim().ifBlank { "Custom" },
                                exercises = selected.map {
                                    WorkoutExerciseDraft(
                                        exerciseId = it,
                                        restSeconds = rest,
                                        setCount = sets,
                                        repsMin = repsMin,
                                        repsMax = repsMax,
                                        weight = weight,
                                        setType = setType,
                                        supersetGroup = superset.takeIf { value -> value.isNotBlank() },
                                        amrapLastSet = amrap,
                                    )
                                },
                            )
                            viewModel.createWorkout(draft) {
                                onSaved?.invoke()
                            }
                        }
                    ) {
                        Text("Save Workout")
                    }

                    Button(
                        modifier = Modifier.weight(1.2f),
                        enabled = isValid,
                        onClick = {
                            val draft = WorkoutDraft(
                                name = name.trim(),
                                description = "Custom workout",
                                splitType = split.trim().ifBlank { "Custom" },
                                exercises = selected.map {
                                    WorkoutExerciseDraft(
                                        exerciseId = it,
                                        restSeconds = rest,
                                        setCount = sets,
                                        repsMin = repsMin,
                                        repsMax = repsMax,
                                        weight = weight,
                                        setType = setType,
                                        supersetGroup = superset.takeIf { value -> value.isNotBlank() },
                                        amrapLastSet = amrap,
                                    )
                                },
                            )
                            viewModel.createWorkout(draft, onStartWorkout)
                        }
                    ) {
                        Text("Save & Start")
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
    currentSplit: String,
    onStartWorkout: (String) -> Unit,
    onApplySplit: (String) -> Unit,
    onStartWarmUp: (DynamicWarmUpRoutine) -> Unit,
) {
    val currentSplitName = currentSplit.ifBlank { "Push Pull Legs (PPL)" }
    val splits = listOf(
        "Push Pull Legs (PPL)",
        "PPLUL (Push Pull Legs Upper Lower)",
        "Upper / Lower Split",
        "Bro Split (Body Part Split)",
        "Full Body Circuit",
    )

    val todayWeekDay = when (LocalDate.now().dayOfWeek) {
        DayOfWeek.MONDAY -> WeekDay.MONDAY
        DayOfWeek.TUESDAY -> WeekDay.TUESDAY
        DayOfWeek.WEDNESDAY -> WeekDay.WEDNESDAY
        DayOfWeek.THURSDAY -> WeekDay.THURSDAY
        DayOfWeek.FRIDAY -> WeekDay.FRIDAY
        DayOfWeek.SATURDAY -> WeekDay.SATURDAY
        DayOfWeek.SUNDAY -> WeekDay.SUNDAY
    }

    val weekDaysOrdered = listOf(
        WeekDay.MONDAY,
        WeekDay.TUESDAY,
        WeekDay.WEDNESDAY,
        WeekDay.THURSDAY,
        WeekDay.FRIDAY,
        WeekDay.SATURDAY,
        WeekDay.SUNDAY,
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active Routine Split", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text("Single select", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Text("Select a split to automatically populate your weekly workout schedule:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    splits.forEach { splitItem ->
                        val isSelected = currentSplitName.trim().equals(splitItem.trim(), ignoreCase = true)
                        Surface(
                            onClick = { onApplySplit(splitItem) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(
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
                                androidx.compose.material3.RadioButton(
                                    selected = isSelected,
                                    onClick = { onApplySplit(splitItem) },
                                    colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text(splitItem, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        item { SectionTitle("Weekly Schedule by Day") }

        items(weekDaysOrdered, key = { it.name }) { day ->
            val isToday = day == todayWeekDay
            val scheduled = schedule.firstOrNull { it.weekDay == day }
            val workout = scheduled?.let { s ->
                workouts.firstOrNull { it.id == s.workoutId } ?: workouts.firstOrNull { it.name.equals(s.workoutName, ignoreCase = true) }
            }
            val dayName = day.name.lowercase().replaceFirstChar { it.titlecase() }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isToday) 2.dp else 1.dp,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(dayName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                            if (isToday) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text("TODAY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }

                        if (scheduled != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(scheduled.workoutName, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text("Rest Day", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }

                    if (scheduled != null) {
                        Text(
                            "${scheduled.periodizationType.name} periodization • Deload every ${scheduled.deloadEveryWeeks} wks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        workout?.let { w ->
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                val routine = remember(w, exercises) {
                                    DynamicWarmUpCatalog.suggestRoutineForWorkout(w, exercises)
                                }
                                OutlinedButton(
                                    onClick = { onStartWarmUp(routine) },
                                    shape = RoundedCornerShape(6.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFF6F00)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                ) {
                                    Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFFF6F00), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Warm-Up (${routine.primaryMuscleGroup.name.lowercase().replaceFirstChar { it.titlecase() }})", color = Color(0xFFFF6F00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onStartWorkout(w.id) },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                ) {
                                    Text(if (isToday) "Start Today's Workout" else "Start Workout", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("Active recovery, mobility, and hydration.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(
                                onClick = {
                                    val restMobility = DynamicWarmUpCatalog.getWarmUpRoutine(MuscleGroup.FULL_BODY, workoutName = "Rest Day Mobility")
                                    onStartWarmUp(restMobility)
                                },
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(0.8.dp, Color(0xFFFF6F00).copy(alpha = 0.6f)),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFFF6F00), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Mobility Flow", color = Color(0xFFFF6F00), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallNumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
