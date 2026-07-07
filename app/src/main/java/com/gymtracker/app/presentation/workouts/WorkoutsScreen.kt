package com.gymtracker.app.presentation.workouts

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymtracker.app.data.local.entity.Difficulty
import com.gymtracker.app.data.local.entity.Equipment
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.data.local.entity.SetType
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.domain.model.WorkoutDraft
import com.gymtracker.app.domain.model.WorkoutExerciseDraft
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.presentation.components.ChipGroup
import com.gymtracker.app.presentation.components.SectionTitle
import com.gymtracker.app.presentation.components.TagRow
import com.gymtracker.app.data.local.entity.label
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkoutsUiState(
    val workouts: List<WorkoutEntity> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val schedule: List<WeeklyScheduleEntity> = emptyList(),
)

@HiltViewModel
class WorkoutsViewModel @Inject constructor(
    private val repository: GymRepository,
) : ViewModel() {
    val state: StateFlow<WorkoutsUiState> = combine(
        repository.observeWorkouts(),
        repository.observeExercises(),
        repository.observeWeeklySchedule(),
    ) { workouts, exercises, schedule ->
        WorkoutsUiState(workouts = workouts, exercises = exercises, schedule = schedule)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutsUiState())

    fun createWorkout(draft: WorkoutDraft, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            onCreated(repository.createCustomWorkout(draft))
        }
    }
}

@Composable
fun WorkoutsScreen(
    onStartWorkout: (String) -> Unit,
    viewModel: WorkoutsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Workouts", "Create", "Library", "Planner")

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Workouts", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> WorkoutList(state.workouts, onStartWorkout)
                1 -> CreateWorkoutPanel(state.exercises, viewModel, onStartWorkout)
                2 -> ExerciseLibrary(state.exercises)
                3 -> PlannerPanel(state.schedule)
            }
        }
    }
}

@Composable
private fun WorkoutList(workouts: List<WorkoutEntity>, onStartWorkout: (String) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(workouts, key = { it.id }) { workout ->
            Card(onClick = { onStartWorkout(workout.id) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(workout.name, fontWeight = FontWeight.SemiBold)
                        Text(if (workout.isTemplate) "Template" else "Custom", color = MaterialTheme.colorScheme.primary)
                    }
                    Text(workout.splitType, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(workout.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CreateWorkoutPanel(
    exercises: List<ExerciseEntity>,
    viewModel: WorkoutsViewModel,
    onStartWorkout: (String) -> Unit,
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

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Workout name") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = split, onValueChange = { split = it }, label = { Text("Split") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            SectionTitle("Set defaults")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                SmallNumberField("Sets", sets.toString(), { sets = it.toIntOrNull() ?: sets }, Modifier.weight(1f))
                SmallNumberField("Min reps", repsMin.toString(), { repsMin = it.toIntOrNull() ?: repsMin }, Modifier.weight(1f))
                SmallNumberField("Max reps", repsMax.toString(), { repsMax = it.toIntOrNull() ?: repsMax }, Modifier.weight(1f))
            }
        }
        item {
            SmallNumberField("Starting weight kg", weight.toString(), { weight = it.toDoubleOrNull() ?: weight }, Modifier.fillMaxWidth())
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
            OutlinedTextField(value = superset, onValueChange = { superset = it }, label = { Text("Superset group") }, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = amrap, onCheckedChange = { amrap = it })
                Text("AMRAP last set")
            }
        }
        item { SectionTitle("Exercises") }
        items(exercises, key = { it.id }) { exercise ->
            val checked = selected.contains(exercise.id)
            Card {
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
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = selected.isNotEmpty() && name.isNotBlank(),
                onClick = {
                    val draft = WorkoutDraft(
                        name = name,
                        description = "Custom workout",
                        splitType = split,
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
                },
            ) {
                Text("Save and start")
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
        item { ChipGroup(MuscleGroup.entries.map { it.label() }, muscle) { muscle = it } }
        item { ChipGroup(Equipment.entries.map { it.label() }, equipment) { equipment = it } }
        item { ChipGroup(Difficulty.entries.map { it.label() }, difficulty) { difficulty = it } }
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
private fun PlannerPanel(schedule: List<WeeklyScheduleEntity>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { SectionTitle("Weekly schedule") }
        items(schedule, key = { it.id }) { item ->
            Card {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.weekDay.name.lowercase().replaceFirstChar { it.titlecase() }, fontWeight = FontWeight.SemiBold)
                    Text(item.workoutName)
                    Text("${item.periodizationType.name} - deload every ${item.deloadEveryWeeks} weeks - reset every ${item.resetEveryWeeks} weeks")
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
