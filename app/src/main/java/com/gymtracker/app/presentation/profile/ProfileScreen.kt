@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gymtracker.app.presentation.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.compose.AsyncImage
import com.gymtracker.app.data.auth.GoogleAuthService
import com.gymtracker.app.data.backup.CloudBackupService
import com.gymtracker.app.data.health.HealthSyncService
import com.gymtracker.app.data.local.entity.Gender
import com.gymtracker.app.data.local.entity.GymEquipmentEntity
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.ThemeMode
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.remote.gemini.GeminiApiClient
import com.gymtracker.app.data.remote.gemini.GeminiExportSyncPayload
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.NutritionCalculator
import com.gymtracker.app.notification.NotificationHelper
import com.gymtracker.app.presentation.components.DestructiveButton
import com.gymtracker.app.presentation.components.GoogleAccountProfileCard
import com.gymtracker.app.presentation.components.GoogleLogoIcon
import com.gymtracker.app.presentation.components.GoogleSignInButton
import com.gymtracker.app.presentation.components.PrimaryButton
import com.gymtracker.app.presentation.components.SecondaryButton
import com.gymtracker.app.presentation.components.SectionTitle
import com.gymtracker.app.presentation.nutrition.NutritionContent
import com.gymtracker.app.presentation.nutrition.NutritionViewModel
import com.gymtracker.app.presentation.theme.AppSpacing
import com.gymtracker.app.worker.WaterReminderWorker
import com.gymtracker.app.worker.WorkoutReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Calendar
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

data class ProfileUiState(
    val profile: UserProfileEntity? = null,
    val reminders: List<ReminderEntity> = emptyList(),
    val equipments: List<GymEquipmentEntity> = emptyList(),
    val shareUri: Uri? = null,
    val shareMime: String = "text/plain",
    val status: String = "",
    val isExportingGemini: Boolean = false,
    val geminiExportResponse: String = "",
    val geminiSyncPayload: GeminiExportSyncPayload? = null,
    val waterReminderIntervalHours: Int = 2,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: GymRepository,
    private val cloudBackupService: CloudBackupService,
    private val googleAuthService: GoogleAuthService,
    private val healthSyncService: HealthSyncService,
    private val geminiApiClient: GeminiApiClient,
    private val notificationHelper: NotificationHelper,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val state: StateFlow<ProfileUiState> = combine(
        repository.observeUserProfile(),
        repository.observeReminders(),
        repository.observeGymEquipments(),
    ) { profile, reminders, equipments ->
        ProfileUiState(profile = profile, reminders = reminders, equipments = equipments)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    var transient by mutableStateOf(ProfileUiState())
        private set

    fun save(profile: UserProfileEntity) {
        viewModelScope.launch {
            repository.createOrUpdateProfile(profile)
            transient = transient.copy(status = "Settings saved successfully")
        }
    }

    fun scheduleWaterReminder(intervalHours: Int) {
        viewModelScope.launch {
            val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(intervalHours.toLong(), TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WaterReminderWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
            repository.upsertReminder(
                ReminderEntity(
                    id = "water-reminder",
                    title = "💧 Daily Hydration Reminder",
                    body = "Time to drink water and reach your daily target",
                    timeMinutes = intervalHours * 60,
                    enabled = true,
                )
            )
            transient = transient.copy(status = "Water reminders scheduled every $intervalHours hours")
        }
    }

    fun cancelWaterReminder() {
        viewModelScope.launch {
            WorkManager.getInstance(context).cancelUniqueWork(WaterReminderWorker.WORK_NAME)
            repository.deleteReminder("water-reminder")
            transient = transient.copy(status = "Water reminders cancelled")
        }
    }

    fun testWaterReminder(currentMl: Int, goalMl: Int) {
        notificationHelper.showWaterReminder(currentMl, goalMl)
        transient = transient.copy(status = "Water reminder notification sent!")
    }

    fun exportAndAnalyzeWithGemini(userInstructions: String, requestSyncSchema: Boolean) {
        viewModelScope.launch {
            transient = transient.copy(isExportingGemini = true, status = "Exporting data to Gemini AI...")
            try {
                val dataJson = repository.exportJson()
                val (rawText, payload) = geminiApiClient.analyzeAndSyncData(dataJson, userInstructions, requestSyncSchema)
                transient = transient.copy(
                    isExportingGemini = false,
                    geminiExportResponse = rawText,
                    geminiSyncPayload = payload,
                    status = if (payload != null) "Gemini response ready to sync to app!" else "Gemini analysis generated!"
                )
            } catch (e: Exception) {
                transient = transient.copy(
                    isExportingGemini = false,
                    status = "Gemini export failed: ${e.message}"
                )
            }
        }
    }

    fun syncGeminiToApp(payload: GeminiExportSyncPayload) {
        viewModelScope.launch {
            try {
                val currentProfile = repository.observeUserProfile().first() ?: UserProfileEntity()
                var updated = currentProfile
                payload.recommendedCalorieGoal?.let { updated = updated.copy(calorieGoal = it) }
                payload.recommendedWaterGoalMl?.let { updated = updated.copy(waterGoalMl = it) }
                payload.recommendedPrimaryGoal?.let { if (it.isNotBlank()) updated = updated.copy(primaryGoal = it) }
                payload.recommendedSplit?.let { if (it.isNotBlank()) updated = updated.copy(preferredSplit = it) }
                repository.createOrUpdateProfile(updated)

                payload.suggestedReminders.forEach { r ->
                    repository.upsertReminder(
                        ReminderEntity(
                            title = r.title,
                            body = r.body,
                            timeMinutes = r.timeMinutes,
                            enabled = true
                        )
                    )
                }

                transient = transient.copy(
                    status = "✅ Successfully synced Gemini recommendations to app!",
                    geminiSyncPayload = null
                )
            } catch (e: Exception) {
                transient = transient.copy(status = "Sync failed: ${e.message}")
            }
        }
    }

    fun parseAndSyncRawGeminiJson(rawJson: String, onSuccess: () -> Unit = {}) {
        if (rawJson.isBlank()) {
            transient = transient.copy(status = "❌ Sync failed: Gemini JSON response cannot be empty")
            return
        }
        viewModelScope.launch {
            try {
                val clean = cleanJson(rawJson)
                val payload = Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString(
                    GeminiExportSyncPayload.serializer(),
                    clean
                )
                syncGeminiToApp(payload)
                onSuccess()
            } catch (e: Exception) {
                transient = transient.copy(status = "❌ Sync failed: Could not parse JSON (${e.message})")
            }
        }
    }

    private fun cleanJson(raw: String): String {
        var c = raw.trim()
        if (c.startsWith("```json")) c = c.removePrefix("```json")
        else if (c.startsWith("```")) c = c.removePrefix("```")
        if (c.endsWith("```")) c = c.removeSuffix("```")
        return c.trim()
    }

    fun addEquipment(name: String, category: String, location: String, photoUri: Uri?, notes: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            var persistentUriStr: String? = null
            if (photoUri != null) {
                persistentUriStr = try {
                    val folder = File(context.filesDir, "equipments").apply { mkdirs() }
                    val targetFile = File(folder, "eq_${UUID.randomUUID()}.jpg")
                    context.contentResolver.openInputStream(photoUri)?.use { input ->
                        targetFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    targetFile.toURI().toString()
                } catch (e: Exception) {
                    photoUri.toString()
                }
            }

            val equipment = GymEquipmentEntity(
                name = name.trim(),
                category = category.trim().ifBlank { "Machine" },
                location = location,
                photoUri = persistentUriStr,
                notes = notes.trim(),
            )
            repository.addGymEquipment(equipment)
            transient = transient.copy(status = "Machine saved to profile")
        }
    }

    fun updateEquipment(
        id: String,
        name: String,
        category: String,
        location: String,
        photoUri: Uri?,
        existingPhotoUriStr: String?,
        notes: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            var persistentUriStr: String? = existingPhotoUriStr
            if (photoUri != null && photoUri.toString() != existingPhotoUriStr) {
                persistentUriStr = try {
                    val folder = File(context.filesDir, "equipments").apply { mkdirs() }
                    val targetFile = File(folder, "eq_${UUID.randomUUID()}.jpg")
                    context.contentResolver.openInputStream(photoUri)?.use { input ->
                        targetFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    targetFile.toURI().toString()
                } catch (e: Exception) {
                    photoUri.toString()
                }
            } else if (photoUri == null && existingPhotoUriStr == null) {
                persistentUriStr = null
            }

            val equipment = GymEquipmentEntity(
                id = id,
                name = name.trim(),
                category = category.trim().ifBlank { "Machine" },
                location = location,
                photoUri = persistentUriStr,
                notes = notes.trim(),
            )
            repository.addGymEquipment(equipment)
            transient = transient.copy(status = "Machine updated successfully")
        }
    }

    fun restoreEquipment(equipment: GymEquipmentEntity) {
        viewModelScope.launch {
            repository.addGymEquipment(equipment)
            transient = transient.copy(status = "Restored ${equipment.name}")
        }
    }

    fun deleteEquipment(id: String) {
        viewModelScope.launch {
            repository.deleteGymEquipment(id)
            transient = transient.copy(status = "Equipment deleted")
        }
    }

    fun createReminder(title: String, timeMinutes: Int) {
        viewModelScope.launch {
            repository.upsertReminder(ReminderEntity(title = title, body = "Training time", timeMinutes = timeMinutes))
            val targetHour = timeMinutes / 60
            val targetMinute = timeMinutes % 60
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now)) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            val initialDelayMs = target.timeInMillis - now.timeInMillis
            val request = PeriodicWorkRequestBuilder<WorkoutReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(WorkoutReminderWorker.KEY_TITLE to title, WorkoutReminderWorker.KEY_BODY to "Training time"))
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "gymtracker-workout-reminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
            val formattedTime = String.format(java.util.Locale.getDefault(), "%02d:%02d", targetHour, targetMinute)
            transient = transient.copy(status = "Workout reminder scheduled for $formattedTime")
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            transient = transient.copy(shareUri = repository.exportJsonFile(), shareMime = "application/json", status = "JSON export ready")
        }
    }

    fun copyDataToClipboard(context: Context) {
        viewModelScope.launch {
            runCatching {
                val dataJson = repository.exportJson()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("GymTracker Export Data", dataJson)
                clipboard.setPrimaryClip(clip)
            }.onSuccess {
                transient = transient.copy(status = "✅ GymTracker JSON export copied to clipboard!")
            }.onFailure { e ->
                transient = transient.copy(status = "❌ Failed to copy data: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            transient = transient.copy(shareUri = repository.exportCsvFile(), shareMime = "text/csv", status = "CSV export ready")
        }
    }

    fun importJson(jsonText: String, onSuccess: () -> Unit = {}) {
        if (jsonText.isBlank()) {
            transient = transient.copy(status = "❌ Import failed: JSON backup text cannot be empty")
            return
        }
        viewModelScope.launch {
            runCatching { repository.importJson(jsonText) }
                .onSuccess {
                    transient = transient.copy(status = "✅ Backup imported successfully!")
                    onSuccess()
                }
                .onFailure {
                    transient = transient.copy(status = "❌ Import failed: ${it.message ?: "Invalid JSON payload"}")
                }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            repository.deleteAllData()
            transient = transient.copy(status = "All local data deleted and defaults restored")
        }
    }

    fun googleSignInIntent(): Intent = googleAuthService.getSignInIntent()

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            transient = transient.copy(status = "Authenticating Google Account...")
            googleAuthService.handleSignInResult(data).fold(
                onSuccess = { user ->
                    repository.updateUserGoogleAuth(
                        googleLinked = true,
                        googleEmail = user.email,
                        googleDisplayName = user.displayName,
                        googlePhotoUrl = user.photoUrl,
                        googleId = user.id,
                    )
                    transient = transient.copy(status = "Signed in as ${user.email}")
                },
                onFailure = {
                    transient = transient.copy(status = "Google sign-in failed: ${it.message ?: "Please check Google Play Services and try again."}")
                }
            )
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            googleAuthService.signOut()
            repository.updateUserGoogleAuth(
                googleLinked = false,
                googleEmail = null,
                googleDisplayName = null,
                googlePhotoUrl = null,
                googleId = null,
            )
            transient = transient.copy(status = "Signed out of Google Account")
        }
    }

    fun syncProgressToHealth() {
        viewModelScope.launch {
            transient = transient.copy(status = "Syncing progress to Google Health...")
            repository.syncProgressToGoogleHealth().fold(
                onSuccess = { transient = transient.copy(status = it.message) },
                onFailure = { transient = transient.copy(status = it.message ?: "Health sync failed") }
            )
        }
    }

    fun openHealthConnectSettings(ctx: Context) {
        runCatching {
            val intent = healthSyncService.getHealthConnectSettingsIntent()
            ctx.startActivity(intent)
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
            }, "Share Rep Forge export"))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        Text("You", style = MaterialTheme.typography.headlineLarge)

        TabRow(selectedTabIndex = tab) {
            listOf("Account & Settings", "Equipment", "Nutrition", "Data & Privacy").forEachIndexed { index, title ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        Box(Modifier.weight(1f)) {
            when (tab) {
                0 -> SettingsPanel(profile, baseState.reminders, viewModel)
                1 -> EquipmentsPanel(baseState.equipments, viewModel)
                2 -> NutritionContent(nutritionState, nutritionViewModel)
                3 -> DataPanel(
                    viewModel = viewModel,
                    profile = profile,
                    onSignIn = { signInLauncher.launch(viewModel.googleSignInIntent()) },
                    onSignOut = { viewModel.signOutGoogle() },
                    onSyncHealth = { viewModel.syncProgressToHealth() },
                    onOpenHealthSettings = { viewModel.openHealthConnectSettings(context) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentsPanel(
    equipments: List<GymEquipmentEntity>,
    viewModel: ProfileViewModel,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showAddForm by remember { mutableStateOf(false) }
    var editingEquipment by remember { mutableStateOf<GymEquipmentEntity?>(null) }
    var equipmentToDelete by remember { mutableStateOf<GymEquipmentEntity?>(null) }

    var machineName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Gym") }
    var category by remember { mutableStateOf("Machine") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var existingPhotoUriStr by remember { mutableStateOf<String?>(null) }
    var machineNotes by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedPhotoUri = uri
            existingPhotoUriStr = null
        }
    }

    fun resetForm() {
        machineName = ""
        location = "Gym"
        category = "Machine"
        selectedPhotoUri = null
        existingPhotoUriStr = null
        machineNotes = ""
        editingEquipment = null
        showAddForm = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Column(Modifier.padding(AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Registered Equipments (${equipments.size})", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            "These saved machines allow Gemini to adapt workout routines to your gear.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                PrimaryButton(
                    text = if (showAddForm) "Cancel" else "Add Equipment",
                    onClick = {
                        if (showAddForm) resetForm() else { resetForm(); showAddForm = true }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (showAddForm) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.padding(AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                            Text(
                                if (editingEquipment != null) "Edit Machine" else "New Machine",
                                style = MaterialTheme.typography.titleMedium
                            )

                            OutlinedTextField(
                                value = machineName,
                                onValueChange = { machineName = it },
                                label = { Text("Machine Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            PrimaryButton(
                                text = if (editingEquipment != null) "Update Equipment" else "Save Equipment",
                                onClick = {
                                    if (machineName.isNotBlank()) {
                                        val name = machineName
                                        val currentEditing = editingEquipment
                                        if (currentEditing != null) {
                                            viewModel.updateEquipment(
                                                id = currentEditing.id,
                                                name = name,
                                                category = category,
                                                location = location,
                                                photoUri = selectedPhotoUri,
                                                existingPhotoUriStr = existingPhotoUriStr,
                                                notes = machineNotes
                                            )
                                        } else {
                                            viewModel.addEquipment(
                                                name = name,
                                                category = category,
                                                location = location,
                                                photoUri = selectedPhotoUri,
                                                notes = machineNotes
                                            )
                                        }
                                        resetForm()
                                    }
                                },
                                enabled = machineName.isNotBlank(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            items(equipments, key = { it.id }) { eq ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(AppSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(eq.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            Text("${eq.location} • ${eq.category}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { equipmentToDelete = eq }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsPanel(profile: UserProfileEntity, reminders: List<ReminderEntity>, viewModel: ProfileViewModel) {
    var name by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var gender by remember(profile.gender) { mutableStateOf(profile.gender) }
    var goal by remember(profile.primaryGoal) { mutableStateOf(profile.primaryGoal) }
    var unitSystem by remember(profile.unitSystem) { mutableStateOf(profile.unitSystem) }

    var heightStr by remember(profile.heightCm, profile.unitSystem) {
        val h = if (profile.unitSystem == UnitSystem.IMPERIAL) profile.heightCm / 2.54 else profile.heightCm
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", h))
    }
    var weightStr by remember(profile.weightKg, profile.unitSystem) {
        val w = if (profile.unitSystem == UnitSystem.IMPERIAL) profile.weightKg * 2.20462262185 else profile.weightKg
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", w))
    }

    var split by remember(profile.preferredSplit) { mutableStateOf(profile.preferredSplit.ifBlank { "Push Pull Legs (PPL)" }) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                    SectionTitle("Account & Preferences")

                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())

                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = heightStr,
                            onValueChange = { heightStr = it },
                            label = { Text("Height (${if (unitSystem == UnitSystem.METRIC) "cm" else "in"})") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = weightStr,
                            onValueChange = { weightStr = it },
                            label = { Text("Weight (${if (unitSystem == UnitSystem.METRIC) "kg" else "lb"})") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PrimaryButton(
                        text = "Save Preferences",
                        onClick = {
                            val parsedH = heightStr.toDoubleOrNull()
                            val parsedW = weightStr.toDoubleOrNull()
                            val canonicalH = if (parsedH != null) {
                                if (unitSystem == UnitSystem.IMPERIAL) parsedH * 2.54 else parsedH
                            } else profile.heightCm
                            val canonicalW = if (parsedW != null) {
                                if (unitSystem == UnitSystem.IMPERIAL) parsedW / 2.20462262185 else parsedW
                            } else profile.weightKg

                            viewModel.save(
                                profile.copy(
                                    displayName = name,
                                    gender = gender,
                                    primaryGoal = goal,
                                    unitSystem = unitSystem,
                                    heightCm = canonicalH,
                                    weightKg = canonicalW,
                                    preferredSplit = split,
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun DataPanel(
    viewModel: ProfileViewModel,
    profile: UserProfileEntity,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncHealth: () -> Unit,
    onOpenHealthSettings: () -> Unit,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete All Local Data?") },
            text = { Text("This will permanently remove all your local workout history, body measurements, nutrition logs, and custom settings.") },
            confirmButton = {
                DestructiveButton(
                    text = "Delete All Data",
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteAllData()
                    }
                )
            },
            dismissButton = {
                SecondaryButton(text = "Cancel", onClick = { showDeleteConfirmation = false })
            }
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.lg)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                    SectionTitle("Data & Privacy")

                    PrimaryButton(text = "Export JSON Backup", onClick = viewModel::exportJson, modifier = Modifier.fillMaxWidth())
                    SecondaryButton(text = "Export CSV Data", onClick = viewModel::exportCsv, modifier = Modifier.fillMaxWidth())

                    HorizontalDivider()

                    DestructiveButton(
                        text = "Delete All Data",
                        onClick = { showDeleteConfirmation = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
