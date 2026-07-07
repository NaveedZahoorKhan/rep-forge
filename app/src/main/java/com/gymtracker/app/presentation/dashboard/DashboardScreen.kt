package com.gymtracker.app.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.model.DashboardStats
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.presentation.components.MetricCard
import com.gymtracker.app.presentation.components.SectionTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val activeSession: WorkoutSessionEntity? = null,
    val templates: List<WorkoutEntity> = emptyList(),
    val history: List<WorkoutSessionEntity> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: GymRepository,
) : ViewModel() {
    val state: StateFlow<DashboardUiState> = combine(
        repository.observeActiveSession(),
        repository.observeTemplates(),
        repository.observeHistory(),
    ) { active, templates, history ->
        DashboardUiState(activeSession = active, templates = templates.take(5), history = history.take(5))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    var stats by mutableStateOf(DashboardStats())
        private set

    init {
        refreshStats()
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
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.history, state.activeSession) { viewModel.refreshStats() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("This week", "${viewModel.stats.weeklySessions}", Modifier.weight(1f))
                MetricCard("Volume", "${viewModel.stats.weeklyVolume.toInt()} kg", Modifier.weight(1f), MaterialTheme.colorScheme.secondary)
                MetricCard("Streak", "${viewModel.stats.streakDays} d", Modifier.weight(1f), MaterialTheme.colorScheme.tertiary)
            }
        }
        state.activeSession?.let { session ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Active workout", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(session.workoutName)
                        Button(onClick = { onOpenWorkout(session.id) }, modifier = Modifier.fillMaxWidth()) { Text("Resume") }
                    }
                }
            }
        }
        item {
            SectionTitle("Quick start") {
                OutlinedButton(onClick = onOpenWorkouts) { Text("All") }
            }
        }
        items(state.templates, key = { it.id }) { workout ->
            Card(onClick = { onStartWorkout(workout.id) }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(workout.name, fontWeight = FontWeight.SemiBold)
                    Text(workout.splitType, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(workout.description, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { SectionTitle("Recent history") }
        items(state.history, key = { it.id }) { session ->
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(session.workoutName, fontWeight = FontWeight.SemiBold)
                    Text("${session.totalVolume.toInt()} kg volume - ${session.durationSeconds / 60} min")
                }
            }
        }
    }
}
