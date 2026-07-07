package com.gymtracker.app.presentation.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.gymtracker.app.data.backup.CloudBackupService
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.ThemeMode
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.presentation.components.SectionTitle
import com.gymtracker.app.presentation.nutrition.NutritionContent
import com.gymtracker.app.presentation.nutrition.NutritionViewModel
import com.gymtracker.app.worker.WorkoutReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfileEntity? = null,
    val reminders: List<ReminderEntity> = emptyList(),
    val shareUri: Uri? = null,
    val shareMime: String = "text/plain",
    val status: String = "",
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: GymRepository,
    private val cloudBackupService: CloudBackupService,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val state: StateFlow<ProfileUiState> = combine(
        repository.observeUserProfile(),
        repository.observeReminders(),
    ) { profile, reminders ->
        ProfileUiState(profile = profile, reminders = reminders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    var transient by mutableStateOf(ProfileUiState())
        private set

    fun save(profile: UserProfileEntity) {
        viewModelScope.launch { repository.createOrUpdateProfile(profile) }
    }

    fun createReminder(title: String, timeMinutes: Int) {
        viewModelScope.launch {
            repository.upsertReminder(ReminderEntity(title = title, body = "Training time", timeMinutes = timeMinutes))
            val request = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(1, TimeUnit.DAYS)
                .setInputData(workDataOf(WorkoutReminderWorker.KEY_TITLE to title, WorkoutReminderWorker.KEY_BODY to "Training time"))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "gymtracker-workout-reminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            transient = transient.copy(shareUri = repository.exportJsonFile(), shareMime = "application/json", status = "JSON export ready")
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            transient = transient.copy(shareUri = repository.exportCsvFile(), shareMime = "text/csv", status = "CSV export ready")
        }
    }

    fun importJson(jsonText: String) {
        viewModelScope.launch {
            runCatching { repository.importJson(jsonText) }
                .onSuccess { transient = transient.copy(status = "Import complete") }
                .onFailure { transient = transient.copy(status = it.message ?: "Import failed") }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
            transient = transient.copy(status = "All local data deleted and defaults restored")
        }
    }

    fun googleSignInIntent(): Intent = cloudBackupService.googleSignInIntent()

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            val message = cloudBackupService.handleGoogleSignInResult(data).fold(
                onSuccess = { "Signed in as $it" },
                onFailure = { it.message ?: "Sign-in failed" },
            )
            transient = transient.copy(status = message)
        }
    }

    fun cloudBackup() {
        viewModelScope.launch {
            transient = transient.copy(status = repository.cloudBackup().fold({ "Cloud backup complete" }, { it.message ?: "Cloud backup failed" }))
        }
    }

    fun cloudRestore() {
        viewModelScope.launch {
            transient = transient.copy(status = repository.cloudRestore().fold({ "Cloud restore complete" }, { it.message ?: "Cloud restore failed" }))
        }
    }
}

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    nutritionViewModel: NutritionViewModel = hiltViewModel(),
) {
    val baseState by viewModel.state.collectAsStateWithLifecycle()
    val nutritionState by nutritionViewModel.state.collectAsStateWithLifecycle()
    val profile = baseState.profile ?: UserProfileEntity()
    var tab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.handleGoogleSignInResult(it.data)
    }

    LaunchedEffect(viewModel.transient.shareUri) {
        viewModel.transient.shareUri?.let { uri ->
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = viewModel.transient.shareMime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Share GymTracker export"))
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        TabRow(selectedTabIndex = tab) {
            listOf("Settings", "Nutrition", "Data").forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
            }
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> SettingsPanel(profile, baseState.reminders, viewModel)
                1 -> NutritionContent(nutritionState, nutritionViewModel)
                2 -> DataPanel(
                    status = viewModel.transient.status,
                    onJson = viewModel::exportJson,
                    onCsv = viewModel::exportCsv,
                    onImport = viewModel::importJson,
                    onDelete = viewModel::deleteAllData,
                    onSignIn = { signInLauncher.launch(viewModel.googleSignInIntent()) },
                    onCloudBackup = viewModel::cloudBackup,
                    onCloudRestore = viewModel::cloudRestore,
                )
            }
        }
    }
}

@Composable
private fun SettingsPanel(profile: UserProfileEntity, reminders: List<ReminderEntity>, viewModel: ProfileViewModel) {
    var name by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var rest by remember(profile.defaultRestSeconds) { mutableStateOf(profile.defaultRestSeconds.toString()) }
    var calories by remember(profile.calorieGoal) { mutableStateOf(profile.calorieGoal.toString()) }
    var water by remember(profile.waterGoalMl) { mutableStateOf(profile.waterGoalMl.toString()) }
    var reminderTitle by remember { mutableStateOf("Workout reminder") }
    var reminderTime by remember { mutableStateOf("1080") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Preferences")
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UnitSystem.entries.forEach {
                            FilterChip(selected = profile.unitSystem == it, onClick = { viewModel.save(profile.copy(unitSystem = it)) }, label = { Text(if (it == UnitSystem.METRIC) "kg/cm" else "lb/in") })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach {
                            FilterChip(selected = profile.themeMode == it, onClick = { viewModel.save(profile.copy(themeMode = it)) }, label = { Text(it.name.lowercase().replaceFirstChar { c -> c.titlecase() }) })
                        }
                    }
                    NumberField("Default rest seconds", rest, { rest = it }, Modifier.fillMaxWidth())
                    NumberField("Calorie goal", calories, { calories = it }, Modifier.fillMaxWidth())
                    NumberField("Water goal ml", water, { water = it }, Modifier.fillMaxWidth())
                    ToggleRow("Sound", profile.soundEnabled) { viewModel.save(profile.copy(soundEnabled = it)) }
                    ToggleRow("Vibration", profile.vibrationEnabled) { viewModel.save(profile.copy(vibrationEnabled = it)) }
                    ToggleRow("Workout reminders", profile.workoutReminderEnabled) { viewModel.save(profile.copy(workoutReminderEnabled = it)) }
                    Button(onClick = {
                        viewModel.save(
                            profile.copy(
                                displayName = name,
                                defaultRestSeconds = rest.toIntOrNull() ?: profile.defaultRestSeconds,
                                calorieGoal = calories.toIntOrNull() ?: profile.calorieGoal,
                                waterGoalMl = water.toIntOrNull() ?: profile.waterGoalMl,
                            )
                        )
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save settings") }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Reminder notifications")
                    OutlinedTextField(value = reminderTitle, onValueChange = { reminderTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    NumberField("Time minutes after midnight", reminderTime, { reminderTime = it }, Modifier.fillMaxWidth())
                    Button(onClick = { viewModel.createReminder(reminderTitle, reminderTime.toIntOrNull() ?: 1080) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Schedule reminder")
                    }
                }
            }
        }
        items(reminders, key = { it.id }) {
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text(it.title, fontWeight = FontWeight.SemiBold)
                    Text("At ${it.timeMinutes / 60}:${(it.timeMinutes % 60).toString().padStart(2, '0')}")
                }
            }
        }
    }
}

@Composable
private fun DataPanel(
    status: String,
    onJson: () -> Unit,
    onCsv: () -> Unit,
    onImport: (String) -> Unit,
    onDelete: () -> Unit,
    onSignIn: () -> Unit,
    onCloudBackup: () -> Unit,
    onCloudRestore: () -> Unit,
) {
    var importText by remember { mutableStateOf("") }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Export and GDPR")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onJson, modifier = Modifier.weight(1f)) { Text("Export JSON") }
                        Button(onClick = onCsv, modifier = Modifier.weight(1f)) { Text("Export CSV") }
                    }
                    OutlinedTextField(value = importText, onValueChange = { importText = it }, label = { Text("Import JSON") }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                    Button(onClick = { onImport(importText) }, modifier = Modifier.fillMaxWidth()) { Text("Import backup") }
                    OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Delete local data") }
                }
            }
        }
        item {
            Card {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Cloud backup")
                    Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text("Google Sign-In") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = onCloudBackup, modifier = Modifier.weight(1f)) { Text("Backup") }
                        Button(onClick = onCloudRestore, modifier = Modifier.weight(1f)) { Text("Restore") }
                    }
                    if (status.isNotBlank()) Text(status)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NumberField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
        singleLine = true,
    )
}
