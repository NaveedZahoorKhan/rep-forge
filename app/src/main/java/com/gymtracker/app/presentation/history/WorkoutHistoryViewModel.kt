package com.gymtracker.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.domain.model.ExerciseVolumePoint
import com.gymtracker.app.domain.model.PastWorkoutLogDraft
import com.gymtracker.app.domain.model.WorkoutSessionSummary
import com.gymtracker.app.domain.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class TimeRangeFilter(val label: String) {
    ALL("All Time"),
    LAST_7_DAYS("7 Days"),
    LAST_30_DAYS("30 Days"),
    LAST_90_DAYS("3 Months"),
}

enum class HistoryViewMode {
    SESSIONS,
    EXERCISE_VOLUME_TREND,
}

data class WorkoutHistoryUiState(
    val summaries: List<WorkoutSessionSummary> = emptyList(),
    val exercises: List<ExerciseEntity> = emptyList(),
    val filteredSummaries: List<WorkoutSessionSummary> = emptyList(),
    val selectedExerciseId: String? = null,
    val exerciseVolumePoints: List<ExerciseVolumePoint> = emptyList(),
    val timeFilter: TimeRangeFilter = TimeRangeFilter.ALL,
    val viewMode: HistoryViewMode = HistoryViewMode.SESSIONS,
    val searchQuery: String = "",
    val totalVolumeAllTimeKg: Double = 0.0,
    val totalWorkoutsCount: Int = 0,
    val averageVolumePerWorkoutKg: Double = 0.0,
    val expandedSessionId: String? = null,
    val showAddPastWorkoutDialog: Boolean = false,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutHistoryViewModel @Inject constructor(
    private val repository: GymRepository,
) : ViewModel() {

    private val _timeFilter = MutableStateFlow(TimeRangeFilter.ALL)
    private val _viewMode = MutableStateFlow(HistoryViewMode.SESSIONS)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedExerciseId = MutableStateFlow<String?>(null)
    private val _expandedSessionId = MutableStateFlow<String?>(null)
    private val _showAddDialog = MutableStateFlow(false)

    val exercises: StateFlow<List<ExerciseEntity>> = repository.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rawSummaries: StateFlow<List<WorkoutSessionSummary>> = repository.observeCompletedSessionSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exerciseVolumePoints: StateFlow<List<ExerciseVolumePoint>> = _selectedExerciseId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(emptyList())
            else repository.observeExerciseVolumeOverTime(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<WorkoutHistoryUiState> = combine(
        rawSummaries,
        exercises,
        _timeFilter,
        _viewMode,
        _searchQuery,
        _selectedExerciseId,
        exerciseVolumePoints,
        _expandedSessionId,
        _showAddDialog,
    ) { args: Array<Any?> ->
        @Suppress("UNCHECKED_CAST")
        val summaries = args[0] as List<WorkoutSessionSummary>
        @Suppress("UNCHECKED_CAST")
        val exList = args[1] as List<ExerciseEntity>
        val timeFilter = args[2] as TimeRangeFilter
        val viewMode = args[3] as HistoryViewMode
        val query = args[4] as String
        val selectedExId = args[5] as String?
        @Suppress("UNCHECKED_CAST")
        val exPoints = args[6] as List<ExerciseVolumePoint>
        val expandedId = args[7] as String?
        val showAdd = args[8] as Boolean

        val now = System.currentTimeMillis()
        val oneDayMs = 24L * 60 * 60 * 1000

        val filteredByTime = summaries.filter { summary ->
            val date = summary.completedAt
            when (timeFilter) {
                TimeRangeFilter.ALL -> true
                TimeRangeFilter.LAST_7_DAYS -> date >= (now - 7 * oneDayMs)
                TimeRangeFilter.LAST_30_DAYS -> date >= (now - 30 * oneDayMs)
                TimeRangeFilter.LAST_90_DAYS -> date >= (now - 90 * oneDayMs)
            }
        }

        val filtered = if (query.isBlank()) {
            filteredByTime
        } else {
            val q = query.trim().lowercase()
            filteredByTime.filter { summary ->
                summary.session.workoutName.lowercase().contains(q) ||
                    summary.session.notes.lowercase().contains(q) ||
                    summary.exerciseSummaries.any { it.exerciseName.lowercase().contains(q) }
            }
        }

        val totalVol = summaries.sumOf { it.totalVolumeKg }
        val avgVol = if (summaries.isNotEmpty()) totalVol / summaries.size else 0.0

        // Auto-select first exercise if not set
        val effectiveExId = selectedExId ?: exList.firstOrNull()?.id

        WorkoutHistoryUiState(
            summaries = summaries,
            exercises = exList,
            filteredSummaries = filtered,
            selectedExerciseId = effectiveExId,
            exerciseVolumePoints = exPoints,
            timeFilter = timeFilter,
            viewMode = viewMode,
            searchQuery = query,
            totalVolumeAllTimeKg = totalVol,
            totalWorkoutsCount = summaries.size,
            averageVolumePerWorkoutKg = avgVol,
            expandedSessionId = expandedId,
            showAddPastWorkoutDialog = showAdd,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutHistoryUiState())

    fun setTimeFilter(filter: TimeRangeFilter) {
        _timeFilter.value = filter
    }

    fun setViewMode(mode: HistoryViewMode) {
        _viewMode.value = mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectExercise(exerciseId: String) {
        _selectedExerciseId.value = exerciseId
    }

    fun toggleSessionExpanded(sessionId: String) {
        _expandedSessionId.update { if (it == sessionId) null else sessionId }
    }

    fun setShowAddDialog(show: Boolean) {
        _showAddDialog.value = show
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteWorkoutSession(sessionId)
        }
    }

    fun logPastWorkout(draft: PastWorkoutLogDraft) {
        viewModelScope.launch {
            repository.logPastWorkout(draft)
            _showAddDialog.value = false
        }
    }
}
