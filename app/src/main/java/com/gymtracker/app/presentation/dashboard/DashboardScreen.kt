package com.gymtracker.app.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WaterLogEntity
import com.gymtracker.app.data.local.entity.WeekDay
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.model.DashboardStats
import com.gymtracker.app.domain.model.DynamicWarmUpRoutine
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.DynamicWarmUpCatalog
import com.gymtracker.app.domain.usecase.WorkoutTemplateCatalog
import com.gymtracker.app.notification.WorkoutSoundPlayer
import com.gymtracker.app.presentation.components.EmptyState
import com.gymtracker.app.presentation.components.MetricCard
import com.gymtracker.app.presentation.components.PrimaryButton
import com.gymtracker.app.presentation.components.SectionTitle
import com.gymtracker.app.presentation.theme.AppSpacing
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

data class DaySchedulePlan(
    val day: WeekDay,
    val isToday: Boolean,
    val isCompleted: Boolean,
    val scheduledWorkout: WeeklyScheduleEntity?,
    val workoutEntity: WorkoutEntity?,
)

data class DashboardUiState(
    val profile: UserProfileEntity = UserProfileEntity(),
    val activeSession: WorkoutSessionEntity? = null,
    val todayWeekDay: WeekDay = WeekDay.MONDAY,
    val todayScheduleItem: WeeklyScheduleEntity? = null,
    val todayWorkout: WorkoutEntity? = null,
    val todayExerciseCount: Int = 0,
    val nextScheduledWorkout: WorkoutEntity? = null,
    val nextScheduledDayName: String = "",
    val weeklyPlan: List<DaySchedulePlan> = emptyList(),
    val splitWorkouts: List<WorkoutEntity> = emptyList(),
    val otherWorkouts: List<WorkoutEntity> = emptyList(),
    val history: List<WorkoutSessionEntity> = emptyList(),
    val weights: List<WeightLogEntity> = emptyList(),
    val todayWater: WaterLogEntity? = null,
    val reminders: List<ReminderEntity> = emptyList(),
    val todayWarmUpRoutine: DynamicWarmUpRoutine? = null,
    val exercises: List<ExerciseEntity> = emptyList(),
    val workoutExercises: List<WorkoutExerciseEntity> = emptyList(),
)

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
        @Suppress("UNCHECKED_CAST")
        val reminders = array[7] as? List<ReminderEntity> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val exercises = array[8] as? List<ExerciseEntity> ?: emptyList()
        @Suppress("UNCHECKED_CAST")
        val workoutExercises = array[9] as? List<WorkoutExerciseEntity> ?: emptyList()

        val profile = profileEntity ?: UserProfileEntity()
        val preferredSplit = profile.preferredSplit.ifBlank { "Push Pull Legs (PPL)" }
        val todayWeekDay = currentWeekDay()

        val todayScheduleItem = schedule.firstOrNull { it.weekDay == todayWeekDay }
        val todayWorkout = todayScheduleItem?.let { sched ->
            allWorkouts.firstOrNull { it.id == sched.workoutId }
                ?: allWorkouts.firstOrNull { it.name.equals(sched.workoutName, ignoreCase = true) }
        }

        val todayExCount = todayWorkout?.let { w ->
            workoutExercises.count { it.workoutId == w.id }
        } ?: 0

        val splitWorkouts = WorkoutTemplateCatalog.getWorkoutsForSplit(preferredSplit, allWorkouts, schedule)
        val otherWorkouts = allWorkouts.filter { w ->
            w.isTemplate && splitWorkouts.none { it.id == w.id || it.name.equals(w.name, ignoreCase = true) }
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

        val completedDates = history.map {
            java.time.Instant.ofEpochMilli(it.startedAt)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }.toSet()

        val todayDate = LocalDate.now()
        val mondayDate = todayDate.minusDays((todayDate.dayOfWeek.value - 1).toLong())

        val weeklyPlan = weekDaysOrdered.mapIndexed { idx, d ->
            val item = schedule.firstOrNull { it.weekDay == d }
            val workout = item?.let { sched ->
                allWorkouts.firstOrNull { it.id == sched.workoutId }
                    ?: allWorkouts.firstOrNull { it.name.equals(sched.workoutName, ignoreCase = true) }
            }
            val dateForDay = mondayDate.plusDays(idx.toLong())
            val isCompleted = completedDates.contains(dateForDay)

            DaySchedulePlan(
                day = d,
                isToday = d == todayWeekDay,
                isCompleted = isCompleted,
                scheduledWorkout = item,
                workoutEntity = workout,
            )
        }

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
            todayExerciseCount = todayExCount,
            nextScheduledWorkout = nextScheduled,
            nextScheduledDayName = nextDayName,
            weeklyPlan = weeklyPlan,
            splitWorkouts = splitWorkouts,
            otherWorkouts = otherWorkouts,
            history = history.take(5),
            weights = weights,
            todayWater = water,
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
    LaunchedEffect(state.history, state.activeSession) { viewModel.refreshStats() }

    val todayFormattedName = state.todayWeekDay.name.lowercase().replaceFirstChar { it.titlecase() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
        // Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text("Today", style = MaterialTheme.typography.headlineLarge)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            "$todayFormattedName",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Text(
                            state.profile.preferredSplit.ifBlank { "Push Pull Legs" },
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }

        // DOMINANT HERO ACTION CARD (Priority 1: Active, Priority 2: Scheduled, Priority 3: Choose/Browse)
        item {
            val activeSession = state.activeSession
            val todayWorkout = state.todayWorkout

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (activeSession != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(AppSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
                ) {
                    when {
                        activeSession != null -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Workout in Progress", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                ) {
                                    Text(
                                        "ACTIVE",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text(activeSession.workoutName.ifBlank { "Active Workout" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            PrimaryButton(
                                text = "Resume Active Workout",
                                onClick = { onOpenWorkout(activeSession.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        todayWorkout != null -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("Today's Workout ($todayFormattedName)", style = MaterialTheme.typography.titleMedium)
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text(
                                        todayWorkout.splitType,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text(todayWorkout.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            val detailsText = if (state.todayExerciseCount > 0) "${state.todayExerciseCount} exercises" else "Standard target sets"
                            Text(
                                detailsText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            PrimaryButton(
                                text = "Start Today's Workout",
                                onClick = { onStartWorkout(todayWorkout.id) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        else -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                                    Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Text("Today is Rest Day", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                            Text(
                                "No workout scheduled for today on your ${state.profile.preferredSplit} plan.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            PrimaryButton(
                                text = "Choose Workout or Browse Routines",
                                onClick = onOpenWorkouts,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

        // Stats Row (Up to 3 supporting metrics)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md), modifier = Modifier.fillMaxWidth()) {
                MetricCard("This Week", "${viewModel.stats.weeklySessions}", Modifier.weight(1f))
                MetricCard("Volume", "${viewModel.stats.weeklyVolume.toInt()} kg", Modifier.weight(1f))
                MetricCard("Streak", "${viewModel.stats.streakDays} d", Modifier.weight(1f))
            }
        }

        // WEEKLY CONSISTENCY STRIP
        item {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                SectionTitle("Weekly Consistency")

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(state.weeklyPlan) { plan ->
                        val dayAbbrev = plan.day.name.take(3)
                        val isToday = plan.isToday
                        val workoutName = plan.workoutEntity?.name ?: plan.scheduledWorkout?.workoutName
                        val statusLabel = when {
                            plan.isCompleted -> "Done"
                            isToday -> "Today"
                            workoutName != null -> "Scheduled"
                            else -> "Rest"
                        }

                        OutlinedCard(
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = when {
                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    plan.isCompleted -> MaterialTheme.colorScheme.surfaceVariant
                                    else -> MaterialTheme.colorScheme.surface
                                },
                            ),
                            border = BorderStroke(
                                width = if (isToday) 2.dp else 1.dp,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier.width(100.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(AppSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    text = dayAbbrev,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (plan.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Text(
                                        text = statusLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // SHORTCUTS: Nutrition and Body Log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
            ) {
                Card(
                    onClick = { onOpenNutrition?.invoke() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(AppSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Nutrition", style = MaterialTheme.typography.titleMedium)
                        }
                        val kcalText = if (state.profile.calorieGoal > 0) "Target: ${state.profile.calorieGoal} kcal" else "Not set"
                        Text(kcalText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Card(
                    onClick = { onOpenHistory?.invoke() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(
                        modifier = Modifier.padding(AppSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            Icon(Icons.Default.MonitorWeight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("Body Log", style = MaterialTheme.typography.titleMedium)
                        }
                        val weightText = if (state.profile.weightKg > 0) "${state.profile.weightKg} kg" else "Not set"
                        Text(weightText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // OPTIONAL COACH SUGGESTION
        onOpenGemini?.let { openGemini ->
            item {
                Card(
                    onClick = openGemini,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(AppSpacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text("Coach Suggestion", style = MaterialTheme.typography.titleMedium)
                            Text("Get personalized training advice or generate workout plans with AI.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // RECENT WORKOUT HISTORY
        item {
            SectionTitle(
                title = "Recent Workouts",
                action = {
                    if (onOpenHistory != null) {
                        TextButton(onClick = onOpenHistory) { Text("View All") }
                    }
                }
            )
        }

        if (state.history.isEmpty()) {
            item {
                EmptyState(
                    title = "No history yet",
                    detail = "Completed workouts will appear here.",
                )
            }
        } else {
            items(state.history, key = { it.id }) { session ->
                Card(
                    onClick = { onOpenHistory?.invoke() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(session.workoutName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("${session.totalVolume.toInt()} kg", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        val mins = session.durationSeconds / 60
                        Text("$mins min duration", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
