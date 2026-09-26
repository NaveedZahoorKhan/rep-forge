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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WeekDay
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.model.DashboardStats
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.WorkoutTemplateCatalog
import com.gymtracker.app.presentation.components.EmptyState
import com.gymtracker.app.presentation.components.MetricCard
import com.gymtracker.app.presentation.components.SectionTitle
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
    val scheduledWorkout: WeeklyScheduleEntity?,
    val workoutEntity: WorkoutEntity?,
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
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: GymRepository,
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
    ) { profileEntity, active, allWorkouts, schedule, history ->
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

        val weeklyPlan = weekDaysOrdered.map { d ->
            val item = schedule.firstOrNull { it.weekDay == d }
            val workout = item?.let { sched ->
                allWorkouts.firstOrNull { it.id == sched.workoutId }
                    ?: allWorkouts.firstOrNull { it.name.equals(sched.workoutName, ignoreCase = true) }
            }
            DaySchedulePlan(
                day = d,
                isToday = d == todayWeekDay,
                scheduledWorkout = item,
                workoutEntity = workout,
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
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

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
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAllTemplates by remember { mutableStateOf(false) }
    LaunchedEffect(state.history, state.activeSession) { viewModel.refreshStats() }

    val todayFormattedName = state.todayWeekDay.name.lowercase().replaceFirstChar { it.titlecase() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            "Today: $todayFormattedName",
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
                            state.profile.preferredSplit,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }
            }
        }

        // Stats Row
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("This week", "${viewModel.stats.weeklySessions}", Modifier.weight(1f))
                MetricCard("Volume", "${viewModel.stats.weeklyVolume.toInt()} kg", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                MetricCard("Streak", "${viewModel.stats.streakDays} d", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
            }
        }

        // -------------------------------------------------------------
        // HERO CARD: WORKOUT ACCORDING TO TODAY & SELECTED SPLIT
        // -------------------------------------------------------------
        state.activeSession?.let { session ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Workout in progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                        Text(session.workoutName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Button(
                            onClick = { onOpenWorkout(session.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Text("⚡ Resume Active Workout", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } ?: run {
            if (state.todayWorkout != null) {
                // Scheduled Workout for Today!
                val todayWorkout = state.todayWorkout!!
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("Today's Workout ($todayFormattedName)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                            Button(
                                onClick = { onStartWorkout(todayWorkout.id) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("⚡ Start Today's Workout (${todayWorkout.name})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Rest & Recovery Day for Today!
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                    Text("Today is $todayFormattedName • Rest Day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                ) {
                                    Text(
                                        "REST & RECOVERY",
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text(
                                "Your ${state.profile.preferredSplit} schedule has today set as a recovery day. Muscles grow and repair during rest! Hydrate and focus on your daily nutrition.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (state.nextScheduledWorkout != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Next workout:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "${state.nextScheduledDayName} • ${state.nextScheduledWorkout!!.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Button(
                                    onClick = { onStartWorkout(state.nextScheduledWorkout!!.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(8.dp),
                                ) {
                                    Text("Start Next Workout Early (${state.nextScheduledWorkout!!.name})", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // WEEKLY DAY-BY-DAY SPLIT STRIP (Mon - Sun)
        // -------------------------------------------------------------
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Weekly Schedule by Day", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Routine: ${state.profile.preferredSplit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenWorkouts,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Planner", fontSize = 11.sp)
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(state.weeklyPlan) { plan ->
                        val dayAbbrev = plan.day.name.take(3)
                        val isToday = plan.isToday
                        val workoutName = plan.workoutEntity?.name ?: plan.scheduledWorkout?.workoutName
                        val isRest = workoutName == null

                        OutlinedCard(
                            onClick = {
                                plan.workoutEntity?.let { onStartWorkout(it.id) }
                            },
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = when {
                                    isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    isRest -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    else -> MaterialTheme.colorScheme.surface
                                },
                            ),
                            border = BorderStroke(
                                width = if (isToday) 2.dp else 1.dp,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            ),
                            modifier = Modifier.width(108.dp),
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        dayAbbrev,
                                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    )
                                    if (isToday) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(6.dp),
                                        ) {}
                                    }
                                }

                                if (isToday) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                    ) {
                                        Text(
                                            "TODAY",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        )
                                    }
                                }

                                if (!isRest && workoutName != null) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                    ) {
                                        Text(
                                            workoutName.replace(" Day", ""),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        )
                                    }
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

        // Gemini AI Coach Banner
        onOpenGemini?.let { openGemini ->
            item {
                Card(
                    onClick = openGemini,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
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
        // QUICK START: FILTERED STRICTLY TO USER'S SELECTED SPLIT
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
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("All Workouts", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        items(state.splitWorkouts, key = { it.id }) { workout ->
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
                    Text(workout.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(
                            onClick = { onStartWorkout(workout.id) },
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
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
                modifier = Modifier.fillMaxWidth().height(42.dp),
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
        // RECENT WORKOUT HISTORY
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
                        modifier = Modifier.height(34.dp),
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
}
