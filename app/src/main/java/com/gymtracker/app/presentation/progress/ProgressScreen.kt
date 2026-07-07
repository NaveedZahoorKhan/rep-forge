package com.gymtracker.app.presentation.progress

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.gymtracker.app.data.local.entity.BodyMeasurementEntity
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.PersonalRecordEntity
import com.gymtracker.app.data.local.entity.ProgressPhotoEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.model.ExerciseProgressPoint
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.StrengthStandardsUseCase
import com.gymtracker.app.presentation.components.LineChartCard
import com.gymtracker.app.presentation.components.MultiLineChartCard
import com.gymtracker.app.presentation.components.SectionTitle
import com.gymtracker.app.presentation.components.StreakHeatmap
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgressUiState(
    val exercises: List<ExerciseEntity> = emptyList(),
    val measurements: List<BodyMeasurementEntity> = emptyList(),
    val photos: List<ProgressPhotoEntity> = emptyList(),
    val records: List<PersonalRecordEntity> = emptyList(),
    val weights: List<WeightLogEntity> = emptyList(),
    val history: List<WorkoutSessionEntity> = emptyList(),
)

@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val repository: GymRepository,
) : ViewModel() {
    val state: StateFlow<ProgressUiState> = combine(
        repository.observeExercises(),
        repository.observeMeasurements(),
        repository.observeProgressPhotos(),
        repository.observePersonalRecords(),
        repository.observeWeightLogs(),
        repository.observeHistory(),
    ) { exercises, measurements, photos, records, weights, history ->
        ProgressUiState(exercises, measurements, photos, records, weights, history)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressUiState())

    var selectedExerciseId by mutableStateOf<String?>(null)
        private set
    var progressPoints by mutableStateOf<List<ExerciseProgressPoint>>(emptyList())
        private set

    fun selectExercise(id: String) {
        selectedExerciseId = id
        viewModelScope.launch { progressPoints = repository.progressForExercise(id) }
    }

    fun addMeasurement(measurement: BodyMeasurementEntity) {
        viewModelScope.launch { repository.addMeasurement(measurement) }
    }

    fun addPhoto(uri: Uri) {
        viewModelScope.launch { repository.addProgressPhoto(ProgressPhotoEntity(uri = uri.toString())) }
    }
}

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.exercises) {
        if (viewModel.selectedExerciseId == null && state.exercises.isNotEmpty()) {
            viewModel.selectExercise(state.exercises.first().id)
        }
    }
    val selectedExercise = state.exercises.firstOrNull { it.id == viewModel.selectedExerciseId }
    val standards = selectedExercise?.let { StrengthStandardsUseCase.standardFor(it.name) }
    val maxOneRm = viewModel.progressPoints.maxOfOrNull { it.estimatedOneRepMax } ?: 0.0
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(viewModel::addPhoto)
    }
    val workoutDays = state.history.map {
        Instant.ofEpochMilli(it.startedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    }.toSet()

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Progress", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
        item {
            SectionTitle("Exercise charts")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                state.exercises.take(4).forEach { exercise ->
                    FilterChip(
                        selected = exercise.id == viewModel.selectedExerciseId,
                        onClick = { viewModel.selectExercise(exercise.id) },
                        label = { Text(exercise.name.take(12)) },
                    )
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(selectedExercise?.name ?: "Exercise", fontWeight = FontWeight.SemiBold)
                    LineChartCard(viewModel.progressPoints.map { it.weight })
                    Text("Volume trend")
                    LineChartCard(viewModel.progressPoints.map { it.volume })
                    Text("Estimated 1RM: ${maxOneRm.toInt()} kg - ${standards?.levelFor(maxOneRm).orEmpty()}")
                }
            }
        }
        item {
            SectionTitle("Weight trend")
            val chronological = state.weights.sortedBy { it.loggedAt }
            MultiLineChartCard(
                series = listOf(
                    chronological.map { it.weightKg },
                    chronological.mapNotNull { it.movingAverageKg },
                )
            )
        }
        item {
            SectionTitle("Body measurements")
            MeasurementForm(onSave = viewModel::addMeasurement)
        }
        items(state.measurements.take(5), key = { it.id }) { measurement ->
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${measurement.weightKg ?: "-"} kg - ${measurement.bodyFatPercent ?: "-"}% body fat")
                    Text("Waist ${measurement.waistCm ?: "-"} cm, chest ${measurement.chestCm ?: "-"} cm, arms ${measurement.armsCm ?: "-"} cm")
                }
            }
        }
        item {
            SectionTitle("Progress photos") {
                Button(onClick = { photoPicker.launch("image/*") }) { Text("Add") }
            }
        }
        items(state.photos, key = { it.id }) { photo ->
            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AsyncImage(model = photo.uri, contentDescription = "Progress photo", modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, Uri.parse(photo.uri))
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share photo"))
                    }) { Text("Share photo") }
                }
            }
        }
        item {
            SectionTitle("Personal records")
        }
        items(state.records.take(8), key = { it.id }) { record ->
            Card {
                Column(Modifier.padding(14.dp)) {
                    Text("${record.exerciseName} - ${record.type}", fontWeight = FontWeight.SemiBold)
                    Text("${record.value.toInt()} kg at ${record.reps} reps")
                }
            }
        }
        item {
            SectionTitle("Workout streak")
            StreakHeatmap(workoutDays = workoutDays)
            Text("${workoutDays.count { it >= LocalDate.now().minusDays(41) }} training days in the last 6 weeks")
        }
    }
}

@Composable
private fun MeasurementForm(onSave: (BodyMeasurementEntity) -> Unit) {
    var weight by remember { mutableDoubleStateOf(75.0) }
    var bodyFat by remember { mutableDoubleStateOf(15.0) }
    var waist by remember { mutableDoubleStateOf(82.0) }
    var chest by remember { mutableDoubleStateOf(100.0) }
    var arms by remember { mutableDoubleStateOf(35.0) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeasurementField("Weight", weight, { weight = it }, Modifier.weight(1f))
            MeasurementField("Body fat", bodyFat, { bodyFat = it }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeasurementField("Waist", waist, { waist = it }, Modifier.weight(1f))
            MeasurementField("Chest", chest, { chest = it }, Modifier.weight(1f))
            MeasurementField("Arms", arms, { arms = it }, Modifier.weight(1f))
        }
        Button(onClick = {
            onSave(BodyMeasurementEntity(weightKg = weight, bodyFatPercent = bodyFat, waistCm = waist, chestCm = chest, armsCm = arms))
        }, modifier = Modifier.fillMaxWidth()) { Text("Save measurements") }
    }
}

@Composable
private fun MeasurementField(label: String, value: Double, onChange: (Double) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onChange(it.toDoubleOrNull() ?: value) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}
