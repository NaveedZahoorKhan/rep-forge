package com.gymtracker.app.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.data.local.entity.NutritionLogEntity
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.SessionStatus
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WaterLogEntity
import com.gymtracker.app.data.local.entity.WeekDay
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.data.local.entity.label
import com.gymtracker.app.domain.model.DashboardStats
import com.gymtracker.app.domain.model.DynamicWarmUpRoutine
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.DynamicWarmUpCatalog
import com.gymtracker.app.domain.usecase.WorkoutTemplateCatalog
import com.gymtracker.app.notification.WorkoutSoundPlayer
import com.gymtracker.app.presentation.components.DynamicWarmUpCard
import com.gymtracker.app.presentation.components.EmptyState
import com.gymtracker.app.presentation.components.GoogleLogoIcon
import com.gymtracker.app.presentation.components.MetricCard
import com.gymtracker.app.presentation.components.WeightLogChartCard
import com.gymtracker.app.presentation.warmup.DynamicWarmUpPlayerModal
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DaySchedulePlan(
    val day: WeekDay,
    val isToday: Boolean,
    val scheduledWorkout: WeeklyScheduleEntity?,
    val workoutEntity: WorkoutEntity?,
    val isCompleted: Boolean = false,
    val completedWorkoutName: String? = null,
)

data class DashboardUiState(
    val profile: UserProfileEntity = UserProfileEntity(),
    val activeSession: WorkoutSessionEntity? = null,
    val todayWeekDay: WeekDay = WeekDay.MONDAY,
    val todayScheduleItem: WeeklyScheduleEntity? = null,
    val todayWorkout: WorkoutEntity? = null,
    val nextScheduledWorkout: WorkoutEntity? = null,
    val nextScheduledDayName: String = "",
    val weeklyPlan: List<DaySchedulePlan> = emptyList(),
    val splitWorkouts: List<WorkoutEntity> = emptyList(),
    val otherWorkouts: List<WorkoutEntity> = emptyList(),
    val history: List<WorkoutSessionEntity> = emptyList(),
    val weights: List<WeightLogEntity> = emptyList(),
    val todayWater: WaterLogEntity? = null,
    val todayNutrition: NutritionLogEntity? = null,
    val reminders: List<ReminderEntity> = emptyList(),
    val todayWarmUpRoutine: DynamicWarmUpRoutine? = null,
    val exercises: List<ExerciseEntity> = emptyList(),
    val workoutExercises: List<WorkoutExerciseEntity> = emptyList(),
) {
    fun getExerciseCount(workoutId: String?): Int {
        if (workoutId == null) return 0
        return workoutExercises.count { it.workoutId == workoutId }
    }

    fun getExpectedDurationMinutes(workoutId: String?): Int {
        if (workoutId == null) return 0
        val count = getExerciseCount(workoutId)
        if (count == 0) return 0
        val pastSessions = history.filter { it.workoutId == workoutId && it.durationSeconds > 0 }
        if (pastSessions.isNotEmpty()) {
            val avgMins = (pastSessions.map { it.durationSeconds }.average() / 60.0).toInt()
            if (avgMins > 0) return avgMins
        }
        return count * 8
    }
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: GymRepository,
    val soundPlayer: WorkoutSoundPlayer,
) : ViewModel() {

    private fun currentWeekDay(): WeekDay = when (LocalDate.now().dayOfWeek) {
        DayOfWeek.MONDAY -> WeekDay.MONDAY
        DayOfWeek.TUESDAY -> WeekDay.TUESDAY
        DayOfWeek.WEDNESDAY -> WeekDay.WEDNESDAY
        DayOfWeek.THURSDAY -> WeekDay.THURSDAY
        DayOfWeek.FRIDAY -> WeekDay.FRIDAY
        DayOfWeek.SATURDAY -> WeekDay.SATURDAY
        DayOfWeek.SUNDAY -> WeekDay.SUNDAY
    }

    val state: StateFlow<DashboardUiState> = combine(
        repository.observeUserProfile(),
        repository.observeActiveSession(),
        repository.observeWorkouts(),
        repository.observeWeeklySchedule(),
        repository.observeHistory(),
        repository.observeWeightLogs(),
        repository.observeWater(LocalDate.now().toEpochDay()),
        repository.observeNutritionLog(LocalDate.now().toEpochDay()),
        repository.observeReminders(),
        repository.observeExercises(),
        repository.observeAllWorkoutExercises(),
    ) { array ->
        val profileEntity = array[0] as? UserProfileEntity
        val active = array[1] as? WorkoutSessionEntity
        @Suppress("UNCHECKED_CAST")
        val allWorkouts = array[2] as? List<WorkoutEntity> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val schedule = array[3] as? List<WeeklyScheduleEntity> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val history = array[4] as? List<WorkoutSessionEntity> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val weights = array[5] as? List<WeightLogEntity> ?: emptyList()
        val water = array[6] as? WaterLogEntity
        val nutrition = array[7] as? NutritionLogEntity
        @Suppress("UNCHECKED_CAST")
        val reminders = array[8] as? List<ReminderEntity> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val exercises = array[9] as? List<ExerciseEntity> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val workoutExercises = array[10] as? List<WorkoutExerciseEntity> ?: emptyList()

        val profile = profileEntity ?: UserProfileEntity()
        val preferredSplit = profile.preferredSplit.ifBlank { "Push Pull Legs (PPL)" }
        val todayWeekDay = currentWeekDay()

        // Today's scheduled workout
        val todayScheduleItem = schedule.firstOrNull { it.weekDay == todayWeekDay }
        val todayWorkout = todayScheduleItem?.let { sched ->
            allWorkouts.firstOrNull { it.id == sched.workoutId }
                ?: allWorkouts.firstOrNull { it.name.equals(sched.workoutName, ignoreCase = true) }
        }

        // Workouts belonging to the active split (e.g. Push, Pull, Legs for PPL)
        val splitWorkouts = WorkoutTemplateCatalog.getWorkoutsForSplit(preferredSplit, allWorkouts, schedule)
        val otherWorkouts = allWorkouts.filter { w ->
            w.isTemplate && splitWorkouts.none { it.id == w.id || it.name.equals(w.name, ignoreCase = true) }
        }

        // Build 7-day calendar strip (Monday to Sunday)
        val weekDaysOrdered = listOf(
            WeekDay.MONDAY,
            WeekDay.TUESDAY,
            WeekDay.WEDNESDAY,
            WeekDay.THURSDAY,
            WeekDay.FRIDAY,
            WeekDay.SATURDAY,
            WeekDay.SUNDAY,
        )

        val todayLocalDate = LocalDate.now()
        val currentDayOfWeekValue = todayLocalDate.dayOfWeek.value // 1 (Mon) to 7 (Sun)
        val mondayLocalDate = todayLocalDate.minusDays((currentDayOfWeekValue - 1).toLong())

        val weeklyPlan = weekDaysOrdered.mapIndexed { idx, d ->
            val item = schedule.firstOrNull { it.weekDay == d }
            val workout = item?.let { sched ->
                allWorkouts.firstOrNull { it.id == sched.workoutId }
                    ?: allWorkouts.firstOrNull { it.name.equals(sched.workoutName, ignoreCase = true) }
            }
            val dayLocalDate = mondayLocalDate.plusDays(idx.toLong())
            val dayEpoch = dayLocalDate.toEpochDay()

            val completedSession = history.firstOrNull { session ->
                session.status == SessionStatus.COMPLETED &&
                    Instant.ofEpochMilli(session.startedAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .toEpochDay() == dayEpoch
            }

            DaySchedulePlan(
                day = d,
                isToday = d == todayWeekDay,
                scheduledWorkout = item,
                workoutEntity = workout,
                isCompleted = completedSession != null,
                completedWorkoutName = completedSession?.workoutName,
            )
        }

        // Find next scheduled workout if today is rest or finished
        val todayIndex = weekDaysOrdered.indexOf(todayWeekDay)
        var nextScheduled: WorkoutEntity? = null
        var nextDayName = ""
        for (i in 1..6) {
            val nextIdx = (todayIndex + i) % 7
            val plan = weeklyPlan[nextIdx]
            if (plan.scheduledWorkout != null && plan.workoutEntity != null) {
                nextScheduled = plan.workoutEntity
                nextDayName = plan.day.name.lowercase().replaceFirstChar { it.titlecase() }
                break
            }
        }

        val warmUpTargetWorkout = todayWorkout ?: nextScheduled
        val todayWarmUpRoutine = DynamicWarmUpCatalog.suggestRoutineForWorkout(
            workout = warmUpTargetWorkout,
            exercises = exercises,
            workoutExercises = workoutExercises,
        )

        DashboardUiState(
            profile = profile,
            activeSession = active,
            todayWeekDay = todayWeekDay,
            todayScheduleItem = todayScheduleItem,
            todayWorkout = todayWorkout,
            nextScheduledWorkout = nextScheduled,
            nextScheduledDayName = nextDayName,
            weeklyPlan = weeklyPlan,
            splitWorkouts = splitWorkouts,
            otherWorkouts = otherWorkouts,
            history = history.take(5),
            weights = weights,
            todayWater = water,
            todayNutrition = nutrition,
            reminders = reminders,
            todayWarmUpRoutine = todayWarmUpRoutine,
            exercises = exercises,
            workoutExercises = workoutExercises,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun addWater(deltaMl: Int) {
        viewModelScope.launch {
            val todayEpochDay = LocalDate.now().toEpochDay()
            val current = state.value.todayWater ?: WaterLogEntity(dateEpochDay = todayEpochDay, goalMl = state.value.profile.waterGoalMl)
            val updated = current.copy(milliliters = (current.milliliters + deltaMl).coerceAtLeast(0))
            repository.upsertWater(updated)
        }
    }

    fun addWeight(weightKg: Double) {
        viewModelScope.launch { repository.addWeightLog(WeightLogEntity(weightKg = weightKg)) }
    }

    fun deleteWeight(id: String) {
        viewModelScope.launch { repository.deleteWeightLog(id) }
    }

    var stats by mutableStateOf(DashboardStats())
        private set

    init {
        refreshStats()
        // Ensure weekly schedule matches the user's preferred split
        viewModelScope.launch {
            val sched = repository.observeWeeklySchedule().first()
            val profile = repository.observeUserProfile().first()
            if (sched.isEmpty() && profile != null && profile.preferredSplit.isNotBlank()) {
                repository.applyRoutineSplitSchedule(profile.preferredSplit)
            }
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            stats = repository.dashboardStats(LocalDate.now().toEpochDay())
        }
    }
}

@Composable
fun DashboardScreen(
    onOpenWorkout: (String) -> Unit,
    onStartWorkout: (String) -> Unit,
    onOpenWorkouts: () -> Unit,
    onOpenGemini: (() -> Unit)? = null,
    onOpenHistory: (() -> Unit)? = null,
    onOpenNutrition: (() -> Unit)? = null,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAllTemplates by remember { mutableStateOf(false) }
    var activeWarmUpRoutine by remember { mutableStateOf<DynamicWarmUpRoutine?>(null) }
    LaunchedEffect(state.history, state.activeSession) { viewModel.refreshStats() }

    val todayFormattedName = state.todayWeekDay.name.lowercase().replaceFirstChar { it.titlecase() }
    val todayDateFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // -------------------------------------------------------------
        // 1. DATE / HEADER
        // -------------------------------------------------------------
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Today", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            todayDateFormatted,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            state.profile.preferredSplit.ifBlank { "Push Pull Legs (PPL)" },
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    if (state.profile.googleLinked && !state.profile.googleEmail.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE8F5E9),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                GoogleLogoIcon(Modifier.size(12.dp))
                                Text(
                                    if (state.profile.healthConnectLinked) "Health Linked" else "Google Connected",
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. ACTIVE OR PLANNED WORKOUT HERO (DOMINANT NEXT ACTION)
        // -------------------------------------------------------------
        item {
            TodayWorkoutHeroCard(
                state = state,
                onOpenWorkout = onOpenWorkout,
                onStartWorkout = onStartWorkout,
                onOpenWorkouts = onOpenWorkouts,
                onStartWarmUp = { routine -> activeWarmUpRoutine = routine },
            )
        }

        // -------------------------------------------------------------
        // 3. COMPACT WEEKLY CONSISTENCY ROW (Mon - Sun)
        // -------------------------------------------------------------
        item {
            WeeklyConsistencyRow(
                weeklyPlan = state.weeklyPlan,
                preferredSplit = state.profile.preferredSplit,
                onStartWorkout = onStartWorkout,
                onOpenWorkouts = onOpenWorkouts,
            )
        }

        // -------------------------------------------------------------
        // 4. NUTRITION AND BODY-LOG SHORTCUTS
        // -------------------------------------------------------------
        item {
            NutritionAndBodyLogShortcutsCard(
                state = state,
                onAddWater = viewModel::addWater,
                onOpenNutrition = onOpenNutrition,
                onLogWeight = viewModel::addWeight,
            )
        }

        // -------------------------------------------------------------
        // 5. OPTIONAL COACH SUGGESTION (DYNAMIC WARM-UP / GEMINI AI COACH)
        // -------------------------------------------------------------
        state.todayWarmUpRoutine?.let { warmUpRoutine ->
            item {
                DynamicWarmUpCard(
                    suggestedRoutine = warmUpRoutine,
                    scheduledWorkoutName = state.todayWorkout?.name ?: state.nextScheduledWorkout?.name.orEmpty(),
                    onStartWarmUp = { routineToRun ->
                        activeWarmUpRoutine = routineToRun
                    },
                )
            }
        }

        onOpenGemini?.let { openGemini ->
            item {
                Card(
                    onClick = openGemini,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.semantics {
                        role = Role.Button
                        contentDescription = "Gemini AI Coach & Workout Builder. Tap to open AI coach."
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Gemini AI Coach & Workout Builder", fontWeight = FontWeight.Bold)
                            Text(
                                "Snap a gym machine & tailor routines to your health background, or sync daily progress",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 6. SUPPORTING METRIC SUMMARIES (MAX 3 ABOVE SECONDARY DETAIL)
        // -------------------------------------------------------------
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("This week", "${viewModel.stats.weeklySessions}", Modifier.weight(1f))
                MetricCard("Volume", "${viewModel.stats.weeklyVolume.toInt()} kg", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                MetricCard("Streak", "${viewModel.stats.streakDays} d", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
            }
        }

        // -------------------------------------------------------------
        // SECONDARY DETAILS: SPLIT WORKOUTS LIBRARY
        // -------------------------------------------------------------
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        "Split Workouts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        state.profile.preferredSplit.ifBlank { "Push Pull Legs (PPL)" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onOpenWorkouts,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(36.dp).defaultMinSize(minWidth = 48.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("All Workouts", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        items(state.splitWorkouts, key = { it.id }) { workout ->
            val exerciseCount = state.getExerciseCount(workout.id)
            val durationMins = state.getExpectedDurationMinutes(workout.id)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text(workout.name, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Text(
                                workout.splitType,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (workout.description.isNotBlank()) {
                        Text(workout.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        if (durationMins > 0) "$exerciseCount exercises • ~$durationMins min" else "$exerciseCount exercises",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        OutlinedButton(
                            onClick = { onStartWorkout(workout.id) },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 36.dp),
                        ) {
                            Text("Start", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Full-width quick action to browse all workouts and custom workout builder
        item {
            OutlinedButton(
                onClick = onOpenWorkouts,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Browse All Workouts in Library", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Expandable / Optional section to explore other routines
        if (state.otherWorkouts.isNotEmpty()) {
            item {
                OutlinedCard(
                    onClick = { showAllTemplates = !showAllTemplates },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("Explore Other Workout Routines", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                if (showAllTemplates) "Showing other splits in library" else "Tap to view Arnold Split, Upper/Lower, Bro Split...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { showAllTemplates = !showAllTemplates }) {
                            Text(if (showAllTemplates) "Hide" else "Show All (${state.otherWorkouts.size})")
                        }
                    }
                }
            }

            if (showAllTemplates) {
                items(state.otherWorkouts, key = { "other_${it.id}" }) { workout ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(workout.name, fontWeight = FontWeight.SemiBold)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Text(workout.splitType, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                }
                            }
                            Text(workout.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                OutlinedButton(
                                    onClick = { onStartWorkout(workout.id) },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                ) {
                                    Text("Start", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // BODYWEIGHT TRACKING CHART
        // -------------------------------------------------------------
        item {
            WeightLogChartCard(
                weights = state.weights,
                unitSystem = state.profile.unitSystem,
                onAddWeight = viewModel::addWeight,
                onDeleteWeight = viewModel::deleteWeight,
            )
        }

        // -------------------------------------------------------------
        // SECONDARY DETAILS: RECENT WORKOUT HISTORY
        // -------------------------------------------------------------
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Recent history",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (onOpenHistory != null) {
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onOpenHistory,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp).defaultMinSize(minWidth = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("View All", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        if (state.history.isEmpty()) {
            item {
                EmptyState(
                    title = "No history yet",
                    detail = "Your completed workouts will appear here",
                )
            }
        } else {
            items(state.history, key = { it.id }) { session ->
                Card(
                    onClick = { onOpenHistory?.invoke() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(session.workoutName, fontWeight = FontWeight.SemiBold)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    "${session.totalVolume.toInt()} kg",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        val mins = session.durationSeconds / 60
                        Text(
                            "$mins min • Tap to view exercise breakdown & volume",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
            targetWorkoutId = state.todayWorkout?.id ?: state.nextScheduledWorkout?.id.orEmpty(),
        )
    }
}

/**
 * Dominant Next Action Hero Card with strict single primary action button.
 */
@Composable
fun TodayWorkoutHeroCard(
    state: DashboardUiState,
    onOpenWorkout: (String) -> Unit,
    onStartWorkout: (String) -> Unit,
    onOpenWorkouts: () -> Unit,
    onStartWarmUp: (DynamicWarmUpRoutine) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeSession = state.activeSession
    val todayWorkout = state.todayWorkout
    val nextScheduledWorkout = state.nextScheduledWorkout
    val hasSplitWorkouts = state.splitWorkouts.isNotEmpty() || state.otherWorkouts.isNotEmpty()

    when {
        // PRIORITY 1: Active Workout in Progress
        activeSession != null -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Active Workout In Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary,
                        ) {
                            Text(
                                "ACTIVE",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(activeSession.workoutName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { onOpenWorkout(activeSession.id) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("⚡ Resume Active Workout", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // PRIORITY 2: Start Today's Scheduled Workout
        todayWorkout != null -> {
            val exerciseCount = state.getExerciseCount(todayWorkout.id)
            val durationMins = state.getExpectedDurationMinutes(todayWorkout.id)
            val infoStr = if (durationMins > 0) "$exerciseCount exercises • ~$durationMins min" else "$exerciseCount exercises"

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text("Today's Scheduled Workout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                todayWorkout.splitType,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(todayWorkout.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (todayWorkout.description.isNotBlank()) {
                        Text(
                            todayWorkout.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        infoStr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    Button(
                        onClick = { onStartWorkout(todayWorkout.id) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("⚡ Start Today's Workout", fontWeight = FontWeight.Bold)
                    }

                    state.todayWarmUpRoutine?.let { routine ->
                        OutlinedButton(
                            onClick = { onStartWarmUp(routine) },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFF6F00),
                            ),
                            border = BorderStroke(1.dp, Color(0xFFFF6F00)),
                        ) {
                            Icon(Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFFF6F00))
                            Spacer(Modifier.width(6.dp))
                            Text("🔥 Warm-Up First (${routine.primaryMuscleGroup.label()} • ~${routine.estimatedDurationMinutes}m)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // PRIORITY 3: Rest & Recovery Day State (Schedule configured but today is rest day)
        hasSplitWorkouts -> {
            val splitName = state.profile.preferredSplit.ifBlank { "Push Pull Legs (PPL)" }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(20.dp),
                            )
                            Text("Rest & Recovery Day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                "REST DAY",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                    Text(
                        "Your $splitName schedule has today set as a recovery day. Focus on rest, hydration, and nutrition.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (nextScheduledWorkout != null) {
                        val nextExCount = state.getExerciseCount(nextScheduledWorkout.id)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Next workout:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${state.nextScheduledDayName} • ${nextScheduledWorkout.name} ($nextExCount exercises)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Button(
                        onClick = onOpenWorkouts,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    ) {
                        Text("Browse Routines", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // PRIORITY 4: New User State (No routines / splits setup yet)
        else -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Welcome to GymTracker", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Choose a workout routine or build a custom routine to start logging your progress.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onOpenWorkouts,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Choose Workout", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Compact Weekly Consistency Row showing completed, scheduled, today, and rest states with semantics and text labels.
 */
@Composable
fun WeeklyConsistencyRow(
    weeklyPlan: List<DaySchedulePlan>,
    preferredSplit: String,
    onStartWorkout: (String) -> Unit,
    onOpenWorkouts: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Weekly Schedule", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Routine: ${preferredSplit.ifBlank { "Push Pull Legs (PPL)" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(
                onClick = onOpenWorkouts,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(32.dp).defaultMinSize(minWidth = 48.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Planner", fontSize = 11.sp)
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(weeklyPlan) { plan ->
                val dayAbbrev = plan.day.name.take(3)
                val isToday = plan.isToday
                val isCompleted = plan.isCompleted
                val workoutName = plan.workoutEntity?.name ?: plan.scheduledWorkout?.workoutName
                val isRest = workoutName == null

                val statusLabel = when {
                    isCompleted -> "Completed"
                    isToday -> "Today"
                    !isRest -> "Scheduled"
                    else -> "Rest"
                }

                val talkBackText = buildString {
                    append("${plan.day.name}: ")
                    if (isToday) append("Today. ")
                    if (isCompleted) {
                        append("Completed ${plan.completedWorkoutName ?: workoutName.orEmpty()}")
                    } else if (!isRest) {
                        append("Scheduled $workoutName")
                    } else {
                        append("Rest Day")
                    }
                }

                OutlinedCard(
                    onClick = {
                        plan.workoutEntity?.let { onStartWorkout(it.id) } ?: onOpenWorkouts()
                    },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = when {
                            isCompleted -> Color(0xFFE8F5E9)
                            isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            isRest -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                    ),
                    border = BorderStroke(
                        width = if (isToday) 2.dp else 1.dp,
                        color = when {
                            isCompleted -> Color(0xFF2E7D32)
                            isToday -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                    modifier = Modifier
                        .width(108.dp)
                        .defaultMinSize(minHeight = 48.dp)
                        .semantics {
                            role = Role.Button
                            contentDescription = talkBackText
                        },
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                dayAbbrev,
                                fontWeight = if (isToday || isCompleted) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = when {
                                    isCompleted -> Color(0xFF1B5E20)
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            )
                            if (isCompleted) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(14.dp),
                                )
                            } else if (isToday) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(6.dp),
                                ) {}
                            }
                        }

                        // Badge / Status text label
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when {
                                isCompleted -> Color(0xFFC8E6C9)
                                isToday -> MaterialTheme.colorScheme.primary
                                !isRest -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        ) {
                            Text(
                                statusLabel,
                                color = when {
                                    isCompleted -> Color(0xFF1B5E20)
                                    isToday -> Color.White
                                    !isRest -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }

                        if (!isRest && workoutName != null) {
                            Text(
                                workoutName.replace(" Day", ""),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                        } else {
                            Text(
                                "Rest",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Combined Nutrition & Weight Log shortcuts with units and clear missing entry vs zero distinction.
 */
@Composable
fun NutritionAndBodyLogShortcutsCard(
    state: DashboardUiState,
    onAddWater: (Int) -> Unit,
    onOpenNutrition: (() -> Unit)?,
    onLogWeight: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showWeightInput by remember { mutableStateOf(false) }
    var inputWeightStr by remember { mutableStateOf("") }

    val unitSystem = state.profile.unitSystem
    val weightUnitLabel = if (unitSystem == UnitSystem.METRIC) "kg" else "lb"
    val multiplier = if (unitSystem == UnitSystem.METRIC) 1.0 else 2.20462

    // Determine weight log status
    val todayEpoch = LocalDate.now().toEpochDay()
    val todayWeightLog = state.weights.firstOrNull { log ->
        Instant.ofEpochMilli(log.loggedAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay() == todayEpoch
    }
    val latestWeightLog = state.weights.maxByOrNull { it.loggedAt }

    val weightDisplayStr = when {
        todayWeightLog != null -> String.format(Locale.US, "%.1f %s", todayWeightLog.weightKg * multiplier, weightUnitLabel)
        latestWeightLog != null -> String.format(Locale.US, "Last: %.1f %s (No log today)", latestWeightLog.weightKg * multiplier, weightUnitLabel)
        else -> "No entry today"
    }

    // Determine water log status
    val currentWater = state.todayWater?.milliliters
    val waterGoal = state.profile.waterGoalMl.coerceAtLeast(1000)
    val waterDisplayStr = if (currentWater != null) "$currentWater / $waterGoal ml" else "No entry today"
    val waterProgress = if (currentWater != null) (currentWater.toFloat() / waterGoal.toFloat()).coerceIn(0f, 1f) else 0f

    // Determine nutrition log status
    val todayNut = state.todayNutrition
    val caloriesDisplayStr = if (todayNut != null) "${todayNut.calories} / ${todayNut.calorieGoal} kcal" else "No entry today"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Daily Check-in & Shortcuts",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )

            // Nutrition & Water Shortcut Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        role = Role.Button
                        contentDescription = "Water and Nutrition Summary. Water: $waterDisplayStr. Calories: $caloriesDisplayStr."
                    },
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Color(0xFF29B6F6),
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "Hydration & Nutrition",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                modifier = if (onOpenNutrition != null) Modifier.clickable { onOpenNutrition() } else Modifier,
                            )
                        }
                        Text(
                            waterDisplayStr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentWater != null && currentWater >= waterGoal) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                        )
                    }

                    if (currentWater != null) {
                        LinearProgressIndicator(
                            progress = { waterProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Calories: $caloriesDisplayStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                        )
                        if (onOpenNutrition != null) {
                            TextButton(
                                onClick = onOpenNutrition,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 32.dp),
                            ) {
                                Text("Open Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = { onAddWater(250) },
                            modifier = Modifier.weight(1f).height(32.dp).defaultMinSize(minWidth = 48.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text("+250 ml", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onAddWater(500) },
                            modifier = Modifier.weight(1f).height(32.dp).defaultMinSize(minWidth = 48.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text("+500 ml", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Body Weight Shortcut Box
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        role = Role.Button
                        contentDescription = "Weight check-in. Current status: $weightDisplayStr."
                    },
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Default.MonitorWeight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp),
                            )
                            Column {
                                Text("Weight Check-in", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(weightDisplayStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                if (inputWeightStr.isBlank()) {
                                    val currentW = (latestWeightLog?.weightKg ?: state.profile.weightKg) * multiplier
                                    inputWeightStr = String.format(Locale.US, "%.1f", currentW)
                                }
                                showWeightInput = !showWeightInput
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp).defaultMinSize(minWidth = 48.dp),
                        ) {
                            Text(if (showWeightInput) "Cancel" else "Log Weight", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    AnimatedVisibility(visible = showWeightInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = inputWeightStr,
                                onValueChange = { inputWeightStr = it },
                                label = { Text("Weight ($weightUnitLabel)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = {
                                    val entered = inputWeightStr.replace(',', '.').toDoubleOrNull()
                                    if (entered != null && entered > 0.0) {
                                        val weightInKg = if (unitSystem == UnitSystem.METRIC) {
                                            entered
                                        } else {
                                            entered / 2.20462
                                        }
                                        onLogWeight(weightInKg)
                                        showWeightInput = false
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
