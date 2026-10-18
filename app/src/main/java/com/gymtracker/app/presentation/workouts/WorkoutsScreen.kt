@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gymtracker.app.presentation.workouts

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
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
import com.gymtracker.app.data.local.entity.Difficulty
import com.gymtracker.app.data.local.entity.Equipment
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.data.local.entity.SetTemplateEntity
import com.gymtracker.app.data.local.entity.SetType
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WeekDay
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import com.gymtracker.app.data.local.entity.label
import com.gymtracker.app.domain.model.DynamicWarmUpRoutine
import com.gymtracker.app.domain.model.WorkoutDraft
import com.gymtracker.app.domain.model.WorkoutExerciseDraft
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.DynamicWarmUpCatalog
import com.gymtracker.app.notification.WorkoutSoundPlayer
import com.gymtracker.app.presentation.components.EmptyState
import com.gymtracker.app.presentation.components.SectionTitle
import com.gymtracker.app.presentation.components.TagRow
import com.gymtracker.app.presentation.history.WorkoutHistoryScreen
import com.gymtracker.app.presentation.warmup.DynamicWarmUpPlayerModal
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
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
    val workoutExercises: List<WorkoutExerciseEntity> = emptyList(),
    val setTemplates: List<SetTemplateEntity> = emptyList(),
    val routineSearchQuery: String = "",
    val routineSplitFilter: String = "All",
    val routineEquipmentFilter: String = "All",
    val routineSubTab: Int = 0, // 0 = My Routines, 1 = Explore
    val exerciseSearchQuery: String = "",
    val exerciseMuscleFilter: String = "All",
    val exerciseEquipmentFilter: String = "All",
    val exerciseDifficultyFilter: String = "All",
    val selectedRoutineDetail: WorkoutEntity? = null,
    val feedbackMessage: String? = null,
)

private data class BaseData(
    val workouts: List<WorkoutEntity>,
    val exercises: List<ExerciseEntity>,
    val schedule: List<WeeklyScheduleEntity>,
    val profile: UserProfileEntity,
    val workoutExercises: List<WorkoutExerciseEntity>,
)

private data class RoutineFilterTuple(
    val query: String,
    val split: String,
    val equipment: String,
    val subTab: Int,
)

private data class ExerciseFilterTuple(
    val query: String,
    val muscle: String,
    val equipment: String,
    val difficulty: String,
)

@HiltViewModel
class WorkoutsViewModel @Inject constructor(
    private val repository: GymRepository,
    val soundPlayer: WorkoutSoundPlayer,
) : ViewModel() {

    private val _routineSearchQuery = MutableStateFlow("")
    private val _routineSplitFilter = MutableStateFlow("All")
    private val _routineEquipmentFilter = MutableStateFlow("All")
    private val _routineSubTab = MutableStateFlow(0)

    private val _exerciseSearchQuery = MutableStateFlow("")
    private val _exerciseMuscleFilter = MutableStateFlow("All")
    private val _exerciseEquipmentFilter = MutableStateFlow("All")
    private val _exerciseDifficultyFilter = MutableStateFlow("All")

    private val _selectedRoutineDetail = MutableStateFlow<WorkoutEntity?>(null)
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    private val baseDataFlow = combine(
        repository.observeWorkouts(),
        repository.observeExercises(),
        repository.observeWeeklySchedule(),
        repository.observeUserProfile(),
        repository.observeAllWorkoutExercises(),
    ) { workouts, exercises, schedule, profile, wExercises ->
        BaseData(workouts, exercises, schedule, profile ?: UserProfileEntity(), wExercises)
    }

    private val routineFiltersFlow = combine(
        _routineSearchQuery,
        _routineSplitFilter,
        _routineEquipmentFilter,
        _routineSubTab,
    ) { q, s, eq, sub -> RoutineFilterTuple(q, s, eq, sub) }

    private val exerciseFiltersFlow = combine(
        _exerciseSearchQuery,
        _exerciseMuscleFilter,
        _exerciseEquipmentFilter,
        _exerciseDifficultyFilter,
    ) { q, m, eq, diff -> ExerciseFilterTuple(q, m, eq, diff) }

    private val uiStateExtraFlow = combine(
        _selectedRoutineDetail,
        _feedbackMessage,
    ) { sel, feedback -> sel to feedback }

    val state: StateFlow<WorkoutsUiState> = combine(
        baseDataFlow,
        repository.observeAllSetTemplates(),
        routineFiltersFlow,
        exerciseFiltersFlow,
        uiStateExtraFlow,
    ) { baseData, sTemplates, rFilter, eFilter, (selDetail, feedback) ->
        WorkoutsUiState(
            workouts = baseData.workouts,
            exercises = baseData.exercises,
            schedule = baseData.schedule,
            profile = baseData.profile,
            workoutExercises = baseData.workoutExercises,
            setTemplates = sTemplates,
            routineSearchQuery = rFilter.query,
            routineSplitFilter = rFilter.split,
            routineEquipmentFilter = rFilter.equipment,
            routineSubTab = rFilter.subTab,
            exerciseSearchQuery = eFilter.query,
            exerciseMuscleFilter = eFilter.muscle,
            exerciseEquipmentFilter = eFilter.equipment,
            exerciseDifficultyFilter = eFilter.difficulty,
            selectedRoutineDetail = selDetail,
            feedbackMessage = feedback,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutsUiState())

    fun setRoutineSearchQuery(query: String) { _routineSearchQuery.value = query }
    fun setRoutineSplitFilter(split: String) { _routineSplitFilter.value = split }
    fun setRoutineEquipmentFilter(equipment: String) { _routineEquipmentFilter.value = equipment }
    fun clearRoutineFilters() {
        _routineSearchQuery.value = ""
        _routineSplitFilter.value = "All"
        _routineEquipmentFilter.value = "All"
    }
    fun setRoutineSubTab(subTab: Int) { _routineSubTab.value = subTab }

    fun setExerciseSearchQuery(query: String) { _exerciseSearchQuery.value = query }
    fun setExerciseMuscleFilter(muscle: String) { _exerciseMuscleFilter.value = muscle }
    fun setExerciseEquipmentFilter(equipment: String) { _exerciseEquipmentFilter.value = equipment }
    fun setExerciseDifficultyFilter(difficulty: String) { _exerciseDifficultyFilter.value = difficulty }
    fun clearExerciseFilters() {
        _exerciseSearchQuery.value = ""
        _exerciseMuscleFilter.value = "All"
        _exerciseEquipmentFilter.value = "All"
        _exerciseDifficultyFilter.value = "All"
    }

    fun openRoutineDetail(workout: WorkoutEntity) { _selectedRoutineDetail.value = workout }
    fun closeRoutineDetail() { _selectedRoutineDetail.value = null }

    fun clearFeedbackMessage() { _feedbackMessage.value = null }

    fun saveTemplateAsCustomWorkout(workout: WorkoutEntity, onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            val wExercises = repository.getAllWorkoutExercises().filter { it.workoutId == workout.id }
            val allSetTemplates = repository.observeAllSetTemplates().first().groupBy { it.workoutExerciseId }

            val draftItems = wExercises.map { we ->
                val templates = allSetTemplates[we.id].orEmpty()
                val firstT = templates.firstOrNull()
                WorkoutExerciseDraft(
                    exerciseId = we.exerciseId,
                    notes = we.notes,
                    restSeconds = we.restSeconds,
                    setCount = templates.size.coerceAtLeast(1),
                    repsMin = firstT?.targetRepsMin ?: 8,
                    repsMax = firstT?.targetRepsMax ?: 12,
                    weight = firstT?.targetWeight ?: 0.0,
                    setType = firstT?.setType ?: SetType.NORMAL,
                    supersetGroup = we.supersetGroup,
                    amrapLastSet = we.amrapLastSet,
                )
            }

            val draft = WorkoutDraft(
                name = workout.name,
                description = workout.description,
                splitType = workout.splitType,
                exercises = draftItems,
            )

            repository.createCustomWorkout(draft)
            _feedbackMessage.value = "Routine '${workout.name}' saved to My Routines"
            onSaved?.invoke()
        }
    }

    fun createWorkout(draft: WorkoutDraft, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            onCreated(repository.createCustomWorkout(draft))
            _feedbackMessage.value = "Routine '${draft.name}' saved"
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

private data class RoutineExerciseDetail(
    val orderIndex: Int,
    val exerciseName: String,
    val primaryMuscle: String,
    val equipment: String,
    val setCount: Int,
    val targetRepsRange: String,
    val targetWeightKg: Double,
    val restSeconds: Int,
)

private data class RoutineMetadata(
    val exerciseCount: Int,
    val equipmentSummary: String,
    val exerciseDetails: List<RoutineExerciseDetail>,
)

private fun calculateRoutineMetadata(
    workoutId: String,
    workoutExercises: List<WorkoutExerciseEntity>,
    exercises: List<ExerciseEntity>,
    setTemplates: List<SetTemplateEntity>,
): RoutineMetadata {
    val exerciseMap = exercises.associateBy { it.id }
    val wExercises = workoutExercises.filter { it.workoutId == workoutId }.sortedBy { it.orderIndex }
    val templatesByWE = setTemplates.groupBy { it.workoutExerciseId }

    val details = wExercises.mapIndexed { index, we ->
        val exercise = exerciseMap[we.exerciseId]
        val templates = templatesByWE[we.id].orEmpty()
        val firstT = templates.firstOrNull()

        val repsText = when {
            firstT == null -> "3 sets × 8–12 reps"
            firstT.targetRepsMin == firstT.targetRepsMax -> "${templates.size.coerceAtLeast(1)} sets × ${firstT.targetRepsMin} reps"
            else -> "${templates.size.coerceAtLeast(1)} sets × ${firstT.targetRepsMin}–${firstT.targetRepsMax} reps"
        }

        RoutineExerciseDetail(
            orderIndex = index + 1,
            exerciseName = exercise?.name ?: "Exercise ${index + 1}",
            primaryMuscle = exercise?.primaryMuscle?.label().orEmpty(),
            equipment = exercise?.equipment?.label().orEmpty(),
            setCount = templates.size.coerceAtLeast(1),
            targetRepsRange = repsText,
            targetWeightKg = firstT?.targetWeight ?: 0.0,
            restSeconds = we.restSeconds,
        )
    }

    val equipmentList = wExercises
        .mapNotNull { exerciseMap[it.exerciseId]?.equipment?.label() }
        .distinct()

    val equipmentSummary = if (equipmentList.isEmpty()) "Full Gym" else equipmentList.joinToString(", ")

    return RoutineMetadata(
        exerciseCount = wExercises.size,
        equipmentSummary = equipmentSummary,
        exerciseDetails = details,
    )
}

@Composable
fun WorkoutsScreen(
    onStartWorkout: (String) -> Unit,
    onOpenGemini: (() -> Unit)? = null,
    viewModel: WorkoutsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTopTab by remember { mutableIntStateOf(0) }
    var activeWarmUpRoutine by remember { mutableStateOf<DynamicWarmUpRoutine?>(null) }
    var showCreatePanel by remember { mutableStateOf(false) }

    val tabs = listOf("Routines", "Exercises", "Planner", "History")

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Routines & Exercises",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        TabRow(selectedTabIndex = selectedTopTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTopTab == index,
                    onClick = {
                        selectedTopTab = index
                        showCreatePanel = false
                    },
                    text = { Text(title, fontWeight = FontWeight.Medium) }
                )
            }
        }

        // Feedback Snackbar if present
        state.feedbackMessage?.let { msg ->
            Snackbar(
                action = {
                    TextButton(onClick = { viewModel.clearFeedbackMessage() }) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.inversePrimary)
                    }
                },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Text(msg)
            }
        }

        Box(Modifier.weight(1f)) {
            if (showCreatePanel) {
                CreateWorkoutPanel(
                    exercises = state.exercises,
                    viewModel = viewModel,
                    onStartWorkout = { wId ->
                        showCreatePanel = false
                        onStartWorkout(wId)
                    },
                    onOpenGemini = onOpenGemini,
                    onSaved = {
                        showCreatePanel = false
                        selectedTopTab = 0
                    }
                )
            } else {
                when (selectedTopTab) {
                    0 -> RoutinesView(
                        state = state,
                        viewModel = viewModel,
                        onStartWorkout = onStartWorkout,
                        onOpenGemini = onOpenGemini,
                        onCreateRoutine = { showCreatePanel = true }
                    )
                    1 -> ExercisesCatalogView(
                        state = state,
                        viewModel = viewModel,
                    )
                    2 -> PlannerPanel(
                        schedule = state.schedule,
                        workouts = state.workouts,
                        exercises = state.exercises,
                        currentSplit = state.profile.preferredSplit,
                        onStartWorkout = onStartWorkout,
                        onApplySplit = { viewModel.applySplit(it) },
                        onStartWarmUp = { activeWarmUpRoutine = it },
                    )
                    3 -> WorkoutHistoryScreen()
                }
            }
        }
    }

    // Routine Details Modal
    state.selectedRoutineDetail?.let { workout ->
        RoutineDetailModal(
            workout = workout,
            workoutExercises = state.workoutExercises,
            exercises = state.exercises,
            setTemplates = state.setTemplates,
            onDismiss = { viewModel.closeRoutineDetail() },
            onStartWorkout = {
                viewModel.closeRoutineDetail()
                onStartWorkout(workout.id)
            },
            onSaveRoutine = {
                viewModel.saveTemplateAsCustomWorkout(workout)
            }
        )
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
private fun RoutinesView(
    state: WorkoutsUiState,
    viewModel: WorkoutsViewModel,
    onStartWorkout: (String) -> Unit,
    onOpenGemini: (() -> Unit)?,
    onCreateRoutine: () -> Unit,
) {
    val splits = listOf("All", "Push", "Pull", "Legs", "Upper", "Lower", "Bro Split", "Full Body", "Custom")
    val equipments = listOf("All", "Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight")

    // Saved routines vs Explore templates filtering
    val myRoutines = remember(state.workouts) { state.workouts.filter { !it.isTemplate } }
    val exploreRoutines = remember(state.workouts) { state.workouts.filter { it.isTemplate } }

    val baseList = if (state.routineSubTab == 0) myRoutines else exploreRoutines

    // Compute metadata & search/filter results
    val filteredRoutines = remember(
        baseList,
        state.routineSearchQuery,
        state.routineSplitFilter,
        state.routineEquipmentFilter,
        state.workoutExercises,
        state.exercises,
        state.setTemplates
    ) {
        baseList.filter { workout ->
            val meta = calculateRoutineMetadata(workout.id, state.workoutExercises, state.exercises, state.setTemplates)

            val matchesQuery = state.routineSearchQuery.isBlank() ||
                    workout.name.contains(state.routineSearchQuery, ignoreCase = true) ||
                    workout.splitType.contains(state.routineSearchQuery, ignoreCase = true) ||
                    meta.equipmentSummary.contains(state.routineSearchQuery, ignoreCase = true) ||
                    meta.exerciseDetails.any { it.exerciseName.contains(state.routineSearchQuery, ignoreCase = true) }

            val matchesSplit = state.routineSplitFilter == "All" ||
                    workout.splitType.contains(state.routineSplitFilter, ignoreCase = true) ||
                    workout.name.contains(state.routineSplitFilter, ignoreCase = true)

            val matchesEquipment = state.routineEquipmentFilter == "All" ||
                    meta.equipmentSummary.contains(state.routineEquipmentFilter, ignoreCase = true)

            matchesQuery && matchesSplit && matchesEquipment
        }
    }

    val isFilterActive = state.routineSearchQuery.isNotBlank() ||
            state.routineSplitFilter != "All" ||
            state.routineEquipmentFilter != "All"

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Prominent Create Routine Header Action Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Workout Routines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Build custom workout programs or discover structured templates",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = onCreateRoutine,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create Routine Icon")
                            Spacer(Modifier.width(6.dp))
                            Text("Create Routine", fontWeight = FontWeight.Bold)
                        }
                    }

                    onOpenGemini?.let { openGemini ->
                        Surface(
                            onClick = openGemini,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Gemini AI", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text("Generate Routine with Gemini AI Coach", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Sub-Tabs: My Routines vs Explore
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.routineSubTab == 0,
                    onClick = { viewModel.setRoutineSubTab(0) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Saved Routines (${myRoutines.size})", fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    selected = state.routineSubTab == 1,
                    onClick = { viewModel.setRoutineSubTab(1) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.CompassCalibration, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Explore (${exploreRoutines.size})", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Search bar (persisted in ViewModel)
        item {
            OutlinedTextField(
                value = state.routineSearchQuery,
                onValueChange = { viewModel.setRoutineSearchQuery(it) },
                placeholder = { Text("Search routines by name, split, equipment...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (state.routineSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setRoutineSearchQuery("") },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear routine search query")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // Filter chips (Split)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Split:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(splits) { split ->
                        FilterChip(
                            selected = state.routineSplitFilter == split,
                            onClick = { viewModel.setRoutineSplitFilter(split) },
                            label = { Text(split, fontSize = 12.sp) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
            }
        }

        // Filter chips (Equipment)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Equipment:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(equipments) { equip ->
                        FilterChip(
                            selected = state.routineEquipmentFilter == equip,
                            onClick = { viewModel.setRoutineEquipmentFilter(equip) },
                            label = { Text(equip, fontSize = 12.sp) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
            }
        }

        // Result count & Clear All action bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Showing ${filteredRoutines.size} routine${if (filteredRoutines.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isFilterActive) {
                    TextButton(
                        onClick = { viewModel.clearRoutineFilters() },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Text("Clear all", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Routines list / No results state / No routines state
        if (filteredRoutines.isEmpty()) {
            item {
                if (isFilterActive) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "No routines matching '${state.routineSearchQuery.ifBlank { state.routineSplitFilter }}'",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Try adjusting your search query or filter chips.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { viewModel.clearRoutineFilters() },
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                            ) {
                                Text("Reset filters")
                            }
                        }
                    }
                } else if (state.routineSubTab == 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "No saved routines yet",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Create a custom routine from scratch or explore pre-built workout programs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onCreateRoutine,
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text("Create routine")
                                }
                                OutlinedButton(
                                    onClick = { viewModel.setRoutineSubTab(1) },
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Text("Explore catalog")
                                }
                            }
                        }
                    }
                } else {
                    EmptyState(
                        title = "No explore routines available",
                        detail = "Try clearing filters to view pre-built routine templates."
                    )
                }
            }
        } else {
            items(filteredRoutines, key = { it.id }) { workout ->
                val meta = calculateRoutineMetadata(
                    workout.id,
                    state.workoutExercises,
                    state.exercises,
                    state.setTemplates
                )
                val isSavedInMyRoutines = remember(myRoutines, workout) {
                    myRoutines.any { it.name.equals(workout.name, ignoreCase = true) }
                }

                RoutineCard(
                    workout = workout,
                    exerciseCount = meta.exerciseCount,
                    equipmentSummary = meta.equipmentSummary,
                    isSaved = isSavedInMyRoutines,
                    onRowClick = { viewModel.openRoutineDetail(workout) },
                    onStartWorkout = { onStartWorkout(workout.id) },
                    onSaveRoutine = {
                        viewModel.saveTemplateAsCustomWorkout(workout)
                    }
                )
            }
        }
    }
}

@Composable
private fun RoutineCard(
    workout: WorkoutEntity,
    exerciseCount: Int,
    equipmentSummary: String,
    isSaved: Boolean,
    onRowClick: () -> Unit,
    onStartWorkout: () -> Unit,
    onSaveRoutine: () -> Unit,
) {
    Card(
        onClick = onRowClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button; contentDescription = "View routine details for ${workout.name}" }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    workout.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
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
                Text(
                    workout.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        "$exerciseCount exercise${if (exerciseCount != 1) "s" else ""}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    "Equipment: $equipmentSummary",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (workout.isTemplate) {
                    OutlinedButton(
                        onClick = onSaveRoutine,
                        enabled = !isSaved,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .padding(end = 8.dp)
                    ) {
                        if (isSaved) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Saved", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save Routine", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = onStartWorkout,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("⚡ Start", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun RoutineDetailModal(
    workout: WorkoutEntity,
    workoutExercises: List<WorkoutExerciseEntity>,
    exercises: List<ExerciseEntity>,
    setTemplates: List<SetTemplateEntity>,
    onDismiss: () -> Unit,
    onStartWorkout: () -> Unit,
    onSaveRoutine: () -> Unit,
) {
    val meta = calculateRoutineMetadata(workout.id, workoutExercises, exercises, setTemplates)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(workout.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            workout.splitType,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                if (workout.description.isNotBlank()) {
                    Text(
                        workout.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${meta.exerciseCount} Exercises", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text("Equipment: ${meta.equipmentSummary}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Text("Ordered Exercises & Target Sets:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                LazyColumn(
                    modifier = Modifier.defaultMinSize(minHeight = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(meta.exerciseDetails, key = { "${it.orderIndex}_${it.exerciseName}" }) { detail ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${detail.orderIndex}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }

                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(detail.exerciseName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    TagRow(listOfNotNull(
                                        detail.primaryMuscle.ifBlank { null },
                                        detail.equipment.ifBlank { null }
                                    ))
                                    Text(
                                        "${detail.targetRepsRange} • Rest: ${detail.restSeconds}s",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onStartWorkout,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text("⚡ Start Workout", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (workout.isTemplate) {
                    OutlinedButton(
                        onClick = onSaveRoutine,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text("Save Routine")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                ) {
                    Text("Close")
                }
            }
        }
    )
}

@Composable
private fun ExercisesCatalogView(
    state: WorkoutsUiState,
    viewModel: WorkoutsViewModel,
) {
    val muscles = listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core", "Full Body")
    val equipments = listOf("All", "Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight", "EZ Bar", "Kettlebell")
    val difficulties = listOf("All", "Beginner", "Intermediate", "Advanced")

    val filtered = remember(
        state.exercises,
        state.exerciseSearchQuery,
        state.exerciseMuscleFilter,
        state.exerciseEquipmentFilter,
        state.exerciseDifficultyFilter
    ) {
        state.exercises.filter { ex ->
            val matchesQuery = state.exerciseSearchQuery.isBlank() ||
                    ex.name.contains(state.exerciseSearchQuery, ignoreCase = true) ||
                    ex.instructions.contains(state.exerciseSearchQuery, ignoreCase = true)

            val matchesMuscle = state.exerciseMuscleFilter == "All" ||
                    ex.primaryMuscle.label().equals(state.exerciseMuscleFilter, ignoreCase = true)

            val matchesEquipment = state.exerciseEquipmentFilter == "All" ||
                    ex.equipment.label().equals(state.exerciseEquipmentFilter, ignoreCase = true)

            val matchesDifficulty = state.exerciseDifficultyFilter == "All" ||
                    ex.difficulty.label().equals(state.exerciseDifficultyFilter, ignoreCase = true)

            matchesQuery && matchesMuscle && matchesEquipment && matchesDifficulty
        }
    }

    val isFilterActive = state.exerciseSearchQuery.isNotBlank() ||
            state.exerciseMuscleFilter != "All" ||
            state.exerciseEquipmentFilter != "All" ||
            state.exerciseDifficultyFilter != "All"

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(
                value = state.exerciseSearchQuery,
                onValueChange = { viewModel.setExerciseSearchQuery(it) },
                placeholder = { Text("Search exercises by name or cue...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search exercises icon") },
                trailingIcon = {
                    if (state.exerciseSearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.setExerciseSearchQuery("") },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear exercise query")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Muscle Group:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(muscles) { muscle ->
                        FilterChip(
                            selected = state.exerciseMuscleFilter == muscle,
                            onClick = { viewModel.setExerciseMuscleFilter(muscle) },
                            label = { Text(muscle, fontSize = 11.sp) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Equipment:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(equipments) { equip ->
                        FilterChip(
                            selected = state.exerciseEquipmentFilter == equip,
                            onClick = { viewModel.setExerciseEquipmentFilter(equip) },
                            label = { Text(equip, fontSize = 11.sp) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Difficulty:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(difficulties) { diff ->
                        FilterChip(
                            selected = state.exerciseDifficultyFilter == diff,
                            onClick = { viewModel.setExerciseDifficultyFilter(diff) },
                            label = { Text(diff, fontSize = 11.sp) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${filtered.size} exercise${if (filtered.size != 1) "s" else ""} found",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isFilterActive) {
                    TextButton(
                        onClick = { viewModel.clearExerciseFilters() },
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    ) {
                        Text("Clear all", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "No exercises matching query",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Try resetting exercise filters or searching for another movement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { viewModel.clearExerciseFilters() },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        ) {
                            Text("Reset filters")
                        }
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { exercise ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(exercise.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        TagRow(listOf(exercise.primaryMuscle.label(), exercise.equipment.label(), exercise.difficulty.label()))
                        if (exercise.instructions.isNotBlank()) {
                            Text(exercise.instructions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        FilterChip(
                            selected = rest == it,
                            onClick = { rest = it },
                            label = { Text("${it}s") },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
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
                        TextButton(
                            onClick = { selected.clear() },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
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
                            IconButton(
                                onClick = { exerciseSearchQuery = "" },
                                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            ) {
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
                            label = { Text("All Muscles", fontSize = 11.sp) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                    items(muscles) { muscle ->
                        FilterChip(
                            selected = selectedMuscleFilter == muscle,
                            onClick = { selectedMuscleFilter = if (selectedMuscleFilter == muscle) null else muscle },
                            label = { Text(muscle, fontSize = 11.sp) },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
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
                        border = BorderStroke(
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
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
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
                        modifier = Modifier.weight(1.2f).defaultMinSize(minHeight = 48.dp),
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
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 0.5.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onApplySplit(splitItem) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
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
                border = BorderStroke(
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
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                                ) {
                                    Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFFF6F00), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Warm-Up (${routine.primaryMuscleGroup.name.lowercase().replaceFirstChar { it.titlecase() }})", color = Color(0xFFFF6F00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onStartWorkout(w.id) },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
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
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
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
                DropdownMenuItem(
                    text = { Text(it.name.lowercase().replace('_', ' ').replaceFirstChar { c -> c.titlecase() }) },
                    onClick = {
                        onChange(it)
                        expanded = false
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                )
            }
        }
    }
}
