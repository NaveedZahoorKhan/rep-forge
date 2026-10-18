@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package com.gymtracker.app.presentation.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
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
import com.gymtracker.app.data.local.entity.HealthSyncLogEntity
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.ThemeMode
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.remote.gemini.GeminiApiClient
import com.gymtracker.app.data.remote.gemini.GeminiExportSyncPayload
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.NutritionCalculator
import com.gymtracker.app.notification.NotificationHelper
import com.gymtracker.app.presentation.components.GoogleAccountProfileCard
import com.gymtracker.app.presentation.components.GoogleLogoIcon
import com.gymtracker.app.presentation.components.GoogleSignInButton
import com.gymtracker.app.presentation.components.SectionTitle
import com.gymtracker.app.worker.WaterReminderWorker
import com.gymtracker.app.worker.WorkoutReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class ProfileSection {
    MAIN,
    ACCOUNT,
    TRAINING_PREFERENCES,
    UNITS_REMINDERS,
    EQUIPMENT,
    CONNECTIONS,
    DATA_PRIVACY,
}

enum class HealthConnectionState {
    UNAVAILABLE,
    PERMISSION_NEEDED,
    CONNECTED,
    SYNCING,
}

data class ProfileUiState(
    val profile: UserProfileEntity? = null,
    val reminders: List<ReminderEntity> = emptyList(),
    val equipments: List<GymEquipmentEntity> = emptyList(),
    val healthLogs: List<HealthSyncLogEntity> = emptyList(),
    val currentSection: ProfileSection = ProfileSection.MAIN,
    val equipmentSearchQuery: String = "",
    val equipmentLocationFilter: String = "All",
    val equipmentCategoryFilter: String = "All",
    val isSyncingHealth: Boolean = false,
    val notificationsEnabled: Boolean = true,
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
    private val _currentSection = MutableStateFlow(ProfileSection.MAIN)
    private val _equipmentSearchQuery = MutableStateFlow("")
    private val _equipmentLocationFilter = MutableStateFlow("All")
    private val _equipmentCategoryFilter = MutableStateFlow("All")
    private val _isSyncingHealth = MutableStateFlow(false)

    private data class InternalUiState(
        val section: ProfileSection,
        val searchQuery: String,
        val locationFilter: String,
        val categoryFilter: String,
        val syncingHealth: Boolean,
    )

    private val _internalUiState = combine(
        _currentSection,
        _equipmentSearchQuery,
        _equipmentLocationFilter,
        _equipmentCategoryFilter,
        _isSyncingHealth,
    ) { section, searchQuery, locationFilter, categoryFilter, syncingHealth ->
        InternalUiState(section, searchQuery, locationFilter, categoryFilter, syncingHealth)
    }

    val state: StateFlow<ProfileUiState> = combine(
        repository.observeUserProfile(),
        repository.observeReminders(),
        repository.observeGymEquipments(),
        repository.observeHealthSyncLogs(),
        _internalUiState,
    ) { profile, reminders, equipments, healthLogs, internal ->
        val notifEnabled = runCatching {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }.getOrDefault(true)
        ProfileUiState(
            profile = profile,
            reminders = reminders,
            equipments = equipments,
            healthLogs = healthLogs,
            currentSection = internal.section,
            equipmentSearchQuery = internal.searchQuery,
            equipmentLocationFilter = internal.locationFilter,
            equipmentCategoryFilter = internal.categoryFilter,
            isSyncingHealth = internal.syncingHealth,
            notificationsEnabled = notifEnabled,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    fun updateSection(section: ProfileSection) {
        _currentSection.value = section
    }

    fun setEquipmentSearchQuery(query: String) {
        _equipmentSearchQuery.value = query
    }

    fun setEquipmentLocationFilter(location: String) {
        _equipmentLocationFilter.value = location
    }

    fun setEquipmentCategoryFilter(category: String) {
        _equipmentCategoryFilter.value = category
    }

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
            val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", targetHour, targetMinute)
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
            _isSyncingHealth.value = true
            transient = transient.copy(status = "Syncing progress to Google Health...")
            repository.syncProgressToGoogleHealth().fold(
                onSuccess = {
                    _isSyncingHealth.value = false
                    transient = transient.copy(status = it.message)
                },
                onFailure = {
                    _isSyncingHealth.value = false
                    transient = transient.copy(status = it.message ?: "Health sync failed")
                }
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
    onOpenNutrition: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val baseState by viewModel.state.collectAsStateWithLifecycle()
    val profile = baseState.profile ?: UserProfileEntity()
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

    BackHandler(enabled = baseState.currentSection != ProfileSection.MAIN) {
        viewModel.updateSection(ProfileSection.MAIN)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (baseState.currentSection == ProfileSection.MAIN) {
            Text(
                text = "Profile & Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            MainSettingsList(
                profile = profile,
                baseState = baseState,
                onSelectSection = { viewModel.updateSection(it) },
                onOpenNutrition = onOpenNutrition
            )
        } else {
            DrillInHeader(
                section = baseState.currentSection,
                onBack = { viewModel.updateSection(ProfileSection.MAIN) }
            )

            Box(modifier = Modifier.weight(1f)) {
                when (baseState.currentSection) {
                    ProfileSection.MAIN -> {}
                    ProfileSection.ACCOUNT -> AccountSection(
                        profile = profile,
                        viewModel = viewModel,
                        onSignIn = { signInLauncher.launch(viewModel.googleSignInIntent()) },
                        onSignOut = { viewModel.signOutGoogle() }
                    )
                    ProfileSection.TRAINING_PREFERENCES -> TrainingPreferencesSection(
                        profile = profile,
                        viewModel = viewModel,
                        onDone = { viewModel.updateSection(ProfileSection.MAIN) }
                    )
                    ProfileSection.UNITS_REMINDERS -> UnitsAndRemindersSection(
                        profile = profile,
                        reminders = baseState.reminders,
                        notificationsEnabled = baseState.notificationsEnabled,
                        viewModel = viewModel
                    )
                    ProfileSection.EQUIPMENT -> EquipmentSection(
                        equipments = baseState.equipments,
                        searchQuery = baseState.equipmentSearchQuery,
                        locationFilter = baseState.equipmentLocationFilter,
                        categoryFilter = baseState.equipmentCategoryFilter,
                        viewModel = viewModel
                    )
                    ProfileSection.CONNECTIONS -> ConnectionsSection(
                        profile = profile,
                        baseState = baseState,
                        viewModel = viewModel,
                        onSignIn = { signInLauncher.launch(viewModel.googleSignInIntent()) },
                        onSignOut = { viewModel.signOutGoogle() },
                        onSyncHealth = { viewModel.syncProgressToHealth() },
                        onOpenHealthSettings = { viewModel.openHealthConnectSettings(context) }
                    )
                    ProfileSection.DATA_PRIVACY -> DataAndPrivacySection(
                        profile = profile,
                        baseState = baseState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun MainSettingsList(
    profile: UserProfileEntity,
    baseState: ProfileUiState,
    onSelectSection: (ProfileSection) -> Unit,
    onOpenNutrition: () -> Unit,
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Account Row
        item {
            val subtitle = if (profile.googleLinked && !profile.googleEmail.isNullOrBlank()) {
                "✓ Signed in as ${profile.googleEmail}"
            } else {
                "Sign in to synchronize & cloud backup"
            }
            SettingRowCard(
                title = "Account",
                subtitle = subtitle,
                icon = Icons.Default.Person,
                onClick = { onSelectSection(ProfileSection.ACCOUNT) }
            )
        }

        // 2. Training preferences Row
        item {
            val subtitle = "${profile.primaryGoal} • ${profile.preferredSplit}"
            SettingRowCard(
                title = "Training preferences",
                subtitle = subtitle,
                icon = Icons.Default.FitnessCenter,
                onClick = { onSelectSection(ProfileSection.TRAINING_PREFERENCES) }
            )
        }

        // 3. Units & reminders Row
        item {
            val activeRemindersCount = baseState.reminders.count { it.enabled }
            val unitText = if (profile.unitSystem == UnitSystem.METRIC) "Metric (kg, cm)" else "Imperial (lb, in)"
            val reminderText = if (activeRemindersCount > 0) "$activeRemindersCount active reminder(s)" else "Reminders off"
            SettingRowCard(
                title = "Units & reminders",
                subtitle = "$unitText • $reminderText",
                icon = Icons.Default.NotificationsActive,
                onClick = { onSelectSection(ProfileSection.UNITS_REMINDERS) }
            )
        }

        // 4. Equipment Row
        item {
            val count = baseState.equipments.size
            SettingRowCard(
                title = "Equipment",
                subtitle = "$count gym equipment(s) registered",
                icon = Icons.Default.HomeWork,
                onClick = { onSelectSection(ProfileSection.EQUIPMENT) }
            )
        }

        // 5. Connections Row
        item {
            val subtitle = if (profile.healthConnectLinked) {
                "Google Health Connected"
            } else if (profile.googleLinked) {
                "Google Account linked • Health Connect ready"
            } else {
                "Connect Google Account & Health"
            }
            SettingRowCard(
                title = "Connections",
                subtitle = subtitle,
                icon = Icons.Default.Sync,
                onClick = { onSelectSection(ProfileSection.CONNECTIONS) }
            )
        }

        // 6. Data & privacy Row
        item {
            SettingRowCard(
                title = "Data & privacy",
                subtitle = "Local export/import, Gemini AI sync & Reset",
                icon = Icons.Default.Shield,
                onClick = { onSelectSection(ProfileSection.DATA_PRIVACY) }
            )
        }

        // 7. Nutrition shortcut Row
        item {
            SettingRowCard(
                title = "Nutrition shortcut",
                subtitle = "Open canonical food & macro log screen",
                icon = Icons.Default.Restaurant,
                onClick = onOpenNutrition,
                isShortcut = true
            )
        }
    }
}

@Composable
private fun SettingRowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isShortcut: Boolean = false,
) {
    OutlinedCard(
        onClick = onClick,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isShortcut) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isShortcut) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isShortcut) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isShortcut) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = if (isShortcut) Icons.AutoMirrored.Filled.OpenInNew else Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DrillInHeader(
    section: ProfileSection,
    onBack: () -> Unit,
) {
    val title = when (section) {
        ProfileSection.ACCOUNT -> "Account"
        ProfileSection.TRAINING_PREFERENCES -> "Training Preferences"
        ProfileSection.UNITS_REMINDERS -> "Units & Reminders"
        ProfileSection.EQUIPMENT -> "Gym Equipment"
        ProfileSection.CONNECTIONS -> "Connections"
        ProfileSection.DATA_PRIVACY -> "Data & Privacy"
        else -> "Settings"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back to settings list"
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

// -------------------------------------------------------------
// 1. ACCOUNT SECTION DRILL-IN
// -------------------------------------------------------------
@Composable
private fun AccountSection(
    profile: UserProfileEntity,
    viewModel: ProfileViewModel,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
) {
    val status = viewModel.transient.status
    val isLoading = status.contains("Authenticating", ignoreCase = true)
    val isError = status.startsWith("Google sign-in failed") || status.startsWith("❌")

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("Identity & Authentication")

                    if (profile.googleLinked && !profile.googleEmail.isNullOrBlank()) {
                        GoogleAccountProfileCard(
                            email = profile.googleEmail,
                            displayName = profile.googleDisplayName ?: "Athlete",
                            photoUrl = profile.googlePhotoUrl,
                            healthLinked = profile.healthConnectLinked,
                            onSignOut = onSignOut
                        )
                    } else if (isLoading) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                Text("Authenticating Google Account...", fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        Text(
                            text = "Sign in with Google to synchronize your workout history, body measurements, and daily progress to Google Health Connect and cloud backup storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isError) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = status,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        GoogleSignInButton(
                            onClick = onSignIn,
                            modifier = Modifier.fillMaxWidth(),
                            isLoading = isLoading
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. TRAINING PREFERENCES SECTION DRILL-IN
// -------------------------------------------------------------
@Composable
private fun TrainingPreferencesSection(
    profile: UserProfileEntity,
    viewModel: ProfileViewModel,
    onDone: () -> Unit,
) {
    var name by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var gender by remember(profile.gender) { mutableStateOf(profile.gender) }
    var goal by remember(profile.primaryGoal) { mutableStateOf(profile.primaryGoal) }
    var unitSystem by remember(profile.unitSystem) { mutableStateOf(profile.unitSystem) }

    var heightStr by remember(profile.heightCm, profile.unitSystem) {
        val h = if (profile.unitSystem == UnitSystem.IMPERIAL) profile.heightCm / 2.54 else profile.heightCm
        mutableStateOf(String.format(Locale.US, "%.1f", h))
    }
    var weightStr by remember(profile.weightKg, profile.unitSystem) {
        val w = if (profile.unitSystem == UnitSystem.IMPERIAL) profile.weightKg * 2.20462262185 else profile.weightKg
        mutableStateOf(String.format(Locale.US, "%.1f", w))
    }

    var split by remember(profile.preferredSplit) { mutableStateOf(profile.preferredSplit.ifBlank { "Push Pull Legs (PPL)" }) }
    var rest by remember(profile.defaultRestSeconds) { mutableStateOf(profile.defaultRestSeconds.toString()) }
    var calories by remember(profile.calorieGoal) { mutableStateOf(profile.calorieGoal.toString()) }
    var water by remember(profile.waterGoalMl) { mutableStateOf(profile.waterGoalMl.toString()) }

    var settingsFeedback by remember { mutableStateOf("") }

    val goals = listOf("Lose weight", "Build muscle", "Gain strength", "Tone & Endurance")

    val availableSplits = listOf(
        "Push Pull Legs (PPL)" to "Mon: Push • Tue: Pull • Wed: Legs • Thu: Push • Fri: Pull • Sat: Legs • Sun: Rest",
        "PPLUL (Push Pull Legs Upper Lower)" to "Mon: Push • Tue: Pull • Wed: Legs • Thu: Rest • Fri: Upper • Sat: Lower • Sun: Rest",
        "Upper / Lower Split" to "Mon: Upper • Tue: Lower • Wed: Rest • Thu: Upper • Fri: Lower • Sat/Sun: Rest",
        "Bro Split (Body Part Split)" to "Mon: Chest • Tue: Back • Wed: Shoulders • Thu: Legs • Fri: Arms • Sat/Sun: Rest",
        "Full Body Circuit" to "Mon: Full Body • Tue: Rest • Wed: Full Body • Thu: Rest • Fri: Full Body • Sat/Sun: Rest",
    )

    fun autoCalculateCalories() {
        val rawW = weightStr.toDoubleOrNull() ?: 75.0
        val rawH = heightStr.toDoubleOrNull() ?: 175.0
        val wKg = if (unitSystem == UnitSystem.IMPERIAL) rawW / 2.20462262185 else rawW
        val hCm = if (unitSystem == UnitSystem.IMPERIAL) rawH * 2.54 else rawH
        val calculated = NutritionCalculator.autoCalculateTargetCalories(
            weightKg = wKg,
            heightCm = hCm,
            gender = gender,
            goal = goal,
        )
        calories = calculated.toString()
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Athlete Profile")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Gender", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Gender.entries.forEach { entry ->
                                FilterChip(
                                    selected = gender == entry,
                                    onClick = { gender = entry },
                                    label = { Text(entry.name.lowercase().replaceFirstChar { it.titlecase() }) }
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Primary Fitness Goal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    goal,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            goals.forEach { g ->
                                val isSelected = goal.equals(g, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        goal = g
                                        autoCalculateCalories()
                                    },
                                    label = { Text(g, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.height(40.dp)
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Workout Routine Split", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        for ((splitTitle, splitSchedule) in availableSplits) {
                            val isSelected = split.trim().equals(splitTitle.trim(), ignoreCase = true)
                            OutlinedCard(
                                onClick = { split = splitTitle },
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { split = splitTitle },
                                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                        )
                                        Text(splitTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Text(
                                        "📅 $splitSchedule",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = heightStr,
                            onValueChange = {
                                heightStr = it
                                if (it.toDoubleOrNull() != null) autoCalculateCalories()
                            },
                            label = { Text("Height (${if (unitSystem == UnitSystem.METRIC) "cm" else "in"})") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = weightStr,
                            onValueChange = {
                                weightStr = it
                                if (it.toDoubleOrNull() != null) autoCalculateCalories()
                            },
                            label = { Text("Weight (${if (unitSystem == UnitSystem.METRIC) "kg" else "lb"})") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    NumberField("Default rest seconds", rest, { rest = it }, Modifier.fillMaxWidth())

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Calorie Goal (kcal)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(
                                onClick = { autoCalculateCalories() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("⚡ Auto-calculate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedTextField(
                            value = calories,
                            onValueChange = { calories = it },
                            label = { Text("Daily Calories") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    NumberField("Water goal (ml)", water, { water = it }, Modifier.fillMaxWidth())

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = {
                                val parsedH = heightStr.toDoubleOrNull()
                                val parsedW = weightStr.toDoubleOrNull()

                                val heightCmValue = if (parsedH != null) {
                                    if (unitSystem == UnitSystem.IMPERIAL) parsedH * 2.54 else parsedH
                                } else profile.heightCm

                                val weightKgValue = if (parsedW != null) {
                                    if (unitSystem == UnitSystem.IMPERIAL) parsedW / 2.20462262185 else parsedW
                                } else profile.weightKg

                                viewModel.save(
                                    profile.copy(
                                        displayName = name,
                                        gender = gender,
                                        primaryGoal = goal,
                                        heightCm = heightCmValue,
                                        weightKg = weightKgValue,
                                        preferredSplit = split,
                                        defaultRestSeconds = rest.toIntOrNull() ?: profile.defaultRestSeconds,
                                        calorieGoal = calories.toIntOrNull() ?: profile.calorieGoal,
                                        waterGoalMl = water.toIntOrNull() ?: profile.waterGoalMl,
                                    )
                                )
                                settingsFeedback = "✅ Training preferences saved successfully!"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Training Preferences")
                        }

                        if (settingsFeedback.isNotBlank()) {
                            Text(
                                text = settingsFeedback,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. UNITS & REMINDERS SECTION DRILL-IN
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitsAndRemindersSection(
    profile: UserProfileEntity,
    reminders: List<ReminderEntity>,
    notificationsEnabled: Boolean,
    viewModel: ProfileViewModel,
) {
    val context = LocalContext.current
    var reminderTitle by remember { mutableStateOf("Workout reminder") }
    var selectedHour by remember { mutableIntStateOf(18) }
    var selectedMinute by remember { mutableIntStateOf(0) }
    var showTimePicker by remember { mutableStateOf(false) }

    var waterInterval by remember { mutableIntStateOf(2) }
    val isWaterReminderActive = reminders.any { it.id == "water-reminder" && it.enabled }

    var unitFeedback by remember { mutableStateOf("") }
    var reminderFeedback by remember { mutableStateOf("") }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        if (!notificationsEnabled) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text("Notifications Blocked", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Text(
                            "Notifications are currently disabled for GymTracker in system settings. Reminders will not fire until enabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Open System Notification Settings")
                        }
                    }
                }
            }
        }

        // Unit System Selection
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Unit System Preference")
                    Text("Select measurement units for weight, distance, and height.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        UnitSystem.entries.forEach { unit ->
                            FilterChip(
                                selected = profile.unitSystem == unit,
                                onClick = {
                                    if (profile.unitSystem != unit) {
                                        viewModel.save(profile.copy(unitSystem = unit))
                                        unitFeedback = "✅ Unit system changed to ${unit.name.lowercase().replaceFirstChar { it.titlecase() }}"
                                    }
                                },
                                label = { Text(if (unit == UnitSystem.METRIC) "Metric (kg, cm)" else "Imperial (lb, in)") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (unitFeedback.isNotBlank()) {
                        Text(unitFeedback, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Water Intake Reminders
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text("Water Intake Reminders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Periodic hydration alerts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Switch(
                            checked = isWaterReminderActive,
                            onCheckedChange = { on ->
                                if (on) {
                                    viewModel.scheduleWaterReminder(waterInterval)
                                } else {
                                    viewModel.cancelWaterReminder()
                                }
                            }
                        )
                    }

                    if (isWaterReminderActive) {
                        Text("Interval Frequency:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf(1, 2, 3, 4).forEach { hours ->
                                FilterChip(
                                    selected = waterInterval == hours,
                                    onClick = {
                                        waterInterval = hours
                                        viewModel.scheduleWaterReminder(hours)
                                    },
                                    label = { Text("Every ${hours}h") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.testWaterReminder(500, profile.waterGoalMl) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Test Hydration Alert Now")
                    }
                }
            }
        }

        // Workout Reminders
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Daily Workout Reminder Schedule")
                    OutlinedTextField(
                        value = reminderTitle,
                        onValueChange = { reminderTitle = it },
                        label = { Text("Reminder Title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    val displayTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Scheduled Local Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(displayTime, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        OutlinedButton(onClick = { showTimePicker = true }, shape = RoundedCornerShape(8.dp)) {
                            Text("Select Time")
                        }
                    }

                    Button(
                        onClick = {
                            val timeMinutes = selectedHour * 60 + selectedMinute
                            viewModel.createReminder(reminderTitle, timeMinutes)
                            reminderFeedback = "✅ Workout reminder scheduled for $displayTime local time!"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Schedule Daily Reminder")
                    }

                    if (reminderFeedback.isNotBlank()) {
                        Text(reminderFeedback, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active Reminders List
        items(reminders, key = { it.id }) { r ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(r.title, fontWeight = FontWeight.Bold)
                        Text(
                            "Time: ${String.format(Locale.getDefault(), "%02d:%02d", r.timeMinutes / 60, r.timeMinutes % 60)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "Active",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                Button(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            title = { Text("Set Reminder Time") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

// -------------------------------------------------------------
// 4. EQUIPMENT SECTION DRILL-IN
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentSection(
    equipments: List<GymEquipmentEntity>,
    searchQuery: String,
    locationFilter: String,
    categoryFilter: String,
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

    val filteredEquipments = remember(equipments, searchQuery, locationFilter, categoryFilter) {
        equipments.filter { eq ->
            val matchesSearch = searchQuery.isBlank() ||
                eq.name.contains(searchQuery, ignoreCase = true) ||
                eq.notes.contains(searchQuery, ignoreCase = true) ||
                eq.category.contains(searchQuery, ignoreCase = true)
            val matchesLocation = locationFilter == "All" || eq.location.equals(locationFilter, ignoreCase = true)
            val matchesCategory = categoryFilter == "All" || eq.category.equals(categoryFilter, ignoreCase = true)
            matchesSearch && matchesLocation && matchesCategory
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search Bar & Filter Chips
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setEquipmentSearchQuery(it) },
                            placeholder = { Text("Search equipment name, category...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { viewModel.setEquipmentSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("All", "Gym", "Home").forEach { loc ->
                                FilterChip(
                                    selected = locationFilter == loc,
                                    onClick = { viewModel.setEquipmentLocationFilter(loc) },
                                    label = { Text(if (loc == "Gym") "Commercial Gym" else loc) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (showAddForm) resetForm() else { resetForm(); showAddForm = true }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(if (showAddForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (showAddForm) "Cancel" else "+ Add Gym Machine or Equipment")
                }
            }

            if (showAddForm) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                if (editingEquipment != null) "Edit Machine / Gear" else "New Machine / Gear",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text("Location", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Gym", "Home").forEach { loc ->
                                    FilterChip(
                                        selected = location == loc,
                                        onClick = { location = loc },
                                        label = { Text(if (loc == "Gym") "Commercial Gym" else "Home") },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = machineName,
                                onValueChange = { machineName = it },
                                label = { Text("Machine Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val presetCategories = listOf("Machine", "Free Weight", "Cable", "Cardio", "Bodyweight", "Accessory")
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                presetCategories.forEach { cat ->
                                    FilterChip(
                                        selected = category.equals(cat, ignoreCase = true),
                                        onClick = { category = cat },
                                        label = { Text(cat) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = machineNotes,
                                onValueChange = { machineNotes = it },
                                label = { Text("Notes (seat height, attachments, resistance ratio)") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (selectedPhotoUri != null || !existingPhotoUriStr.isNullOrBlank()) "Change Photo" else "Upload Photo")
                                }

                                if (selectedPhotoUri != null || !existingPhotoUriStr.isNullOrBlank()) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                    ) {
                                        AsyncImage(
                                            model = selectedPhotoUri ?: existingPhotoUriStr,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(onClick = { resetForm() }, modifier = Modifier.weight(1f)) {
                                    Text("Cancel")
                                }

                                Button(
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
                                                coroutineScope.launch { snackbarHostState.showSnackbar("Updated \"$name\"") }
                                            } else {
                                                viewModel.addEquipment(
                                                    name = name,
                                                    category = category,
                                                    location = location,
                                                    photoUri = selectedPhotoUri,
                                                    notes = machineNotes
                                                )
                                                coroutineScope.launch { snackbarHostState.showSnackbar("Saved \"$name\"") }
                                            }
                                            resetForm()
                                        }
                                    },
                                    enabled = machineName.isNotBlank(),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Text(if (editingEquipment != null) "Update Equipment" else "Save Equipment")
                                }
                            }
                        }
                    }
                }
            }

            if (filteredEquipments.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Text("No matching equipment found", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(filteredEquipments, key = { it.id }) { eq ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!eq.photoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = eq.photoUri,
                                contentDescription = eq.name,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (eq.location == "Home") Icons.Default.Home else Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Column(Modifier.weight(1f)) {
                            Text(eq.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                    Text(eq.location, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Medium)
                                }
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(eq.category, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            if (eq.notes.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(eq.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        IconButton(onClick = {
                            editingEquipment = eq
                            machineName = eq.name
                            location = eq.location
                            category = eq.category
                            machineNotes = eq.notes
                            existingPhotoUriStr = eq.photoUri
                            selectedPhotoUri = eq.photoUri?.let { Uri.parse(it) }
                            showAddForm = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Equipment", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(onClick = { equipmentToDelete = eq }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Equipment", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }

    if (equipmentToDelete != null) {
        val target = equipmentToDelete!!
        AlertDialog(
            onDismissRequest = { equipmentToDelete = null },
            title = { Text("Delete Equipment?") },
            text = { Text("Are you sure you want to delete \"${target.name}\"? You can undo this action immediately after deletion.") },
            confirmButton = {
                Button(
                    onClick = {
                        val deleted = target
                        equipmentToDelete = null
                        viewModel.deleteEquipment(deleted.id)
                        coroutineScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Deleted \"${deleted.name}\"",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.restoreEquipment(deleted)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { equipmentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// 5. CONNECTIONS SECTION DRILL-IN
// -------------------------------------------------------------
@Composable
private fun ConnectionsSection(
    profile: UserProfileEntity,
    baseState: ProfileUiState,
    viewModel: ProfileViewModel,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncHealth: () -> Unit,
    onOpenHealthSettings: () -> Unit,
) {
    val context = LocalContext.current

    val healthState = remember(profile.healthConnectLinked, baseState.isSyncingHealth) {
        if (baseState.isSyncingHealth) {
            HealthConnectionState.SYNCING
        } else if (profile.healthConnectLinked) {
            HealthConnectionState.CONNECTED
        } else if (profile.googleLinked) {
            HealthConnectionState.PERMISSION_NEEDED
        } else {
            HealthConnectionState.UNAVAILABLE
        }
    }

    val lastSyncFormatted = remember(profile.healthLastSyncedAt, baseState.healthLogs) {
        val timestamp = profile.healthLastSyncedAt ?: baseState.healthLogs.firstOrNull { it.status == "SUCCESS" }?.syncedAt
        if (timestamp != null && timestamp > 0) {
            SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(timestamp))
        } else {
            "Never synced"
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        // Health Connect Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoogleLogoIcon(Modifier.size(20.dp))
                        SectionTitle("Google Health Connect")
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when (healthState) {
                            HealthConnectionState.CONNECTED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            HealthConnectionState.SYNCING -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                            HealthConnectionState.PERMISSION_NEEDED -> Color(0xFFFFF3E0)
                            HealthConnectionState.UNAVAILABLE -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                when (healthState) {
                                    HealthConnectionState.CONNECTED -> {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF34A853))
                                        Text("Status: Connected & Active", fontWeight = FontWeight.Bold, color = Color(0xFF1E4620))
                                    }
                                    HealthConnectionState.SYNCING -> {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        Text("Status: Syncing Progress...", fontWeight = FontWeight.Bold)
                                    }
                                    HealthConnectionState.PERMISSION_NEEDED -> {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100))
                                        Text("Status: Permission Required", fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                                    }
                                    HealthConnectionState.UNAVAILABLE -> {
                                        Text("Status: Account Not Linked", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            Text(
                                "Last Successful Sync: $lastSyncFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = onSyncHealth,
                            enabled = healthState != HealthConnectionState.SYNCING && profile.googleLinked,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (healthState == HealthConnectionState.SYNCING) "Syncing..." else "Sync / Retry")
                        }

                        OutlinedButton(onClick = onOpenHealthSettings, modifier = Modifier.weight(1f)) {
                            Text("Health Settings")
                        }
                    }
                }
            }
        }

        // Cloud Storage Backup
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Cloud Backup & Restore")
                    Text("Synchronize your database backup to Google Account cloud storage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = viewModel::cloudBackup, modifier = Modifier.weight(1f)) {
                            Text("Cloud Backup")
                        }
                        Button(onClick = viewModel::cloudRestore, modifier = Modifier.weight(1f)) {
                            Text("Cloud Restore")
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. DATA & PRIVACY SECTION DRILL-IN
// -------------------------------------------------------------
@Composable
private fun DataAndPrivacySection(
    profile: UserProfileEntity,
    baseState: ProfileUiState,
    viewModel: ProfileViewModel,
) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var userGeminiInstruction by remember { mutableStateOf("Calibrate my daily calories, water targets, and training volume based on my current progression.") }
    var formatForAppSync by remember { mutableStateOf(true) }
    var pasteSyncJson by remember { mutableStateOf("") }

    val transientState = viewModel.transient

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showImportConfirmation by remember { mutableStateOf(false) }

    val presetInstructions = listOf(
        "⚡ Calorie & Water Targets" to "Calibrate my daily calorie and water goals to optimize recovery.",
        "🏋️ Routine & Volume Optimization" to "Analyze my training split and volume, suggesting workout adjustments.",
        "📊 Full Health & Training Audit" to "Comprehensive audit of weight trends, workouts, and nutrition with sync adjustments."
    )

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete All Local Data?") },
            text = {
                Text(
                    "This will permanently delete ALL your local workout history, body measurements, nutrition logs, weight check-ins, water logs, and custom settings, and restore defaults. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.deleteAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Permanently Delete Data")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showImportConfirmation) {
        AlertDialog(
            onDismissRequest = { showImportConfirmation = false },
            title = { Text("Import Backup Data?") },
            text = { Text("Importing backup data will update and replace your current workout database with the contents of the backup JSON file. Are you sure you want to proceed?") },
            confirmButton = {
                Button(onClick = {
                    showImportConfirmation = false
                    viewModel.importJson(importText) { importText = "" }
                }) {
                    Text("Import Backup")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportConfirmation = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxSize()) {
        if (transientState.status.isNotBlank()) {
            item {
                val isError = transientState.status.startsWith("❌") || transientState.status.contains("failed", ignoreCase = true)
                Surface(
                    color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        transientState.status,
                        color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Export & Gemini AI Section
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Export Data to Gemini & App Sync", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }

                    HorizontalDivider()

                    Text("Prompt Objective:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetInstructions.forEach { (label, prompt) ->
                            FilterChip(
                                selected = userGeminiInstruction == prompt,
                                onClick = { userGeminiInstruction = prompt },
                                label = { Text(label, fontSize = 12.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = userGeminiInstruction,
                        onValueChange = { userGeminiInstruction = it },
                        label = { Text("Instructions for Gemini Coach") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = formatForAppSync,
                            onCheckedChange = { formatForAppSync = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text("Format response for direct sync back to app", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.exportAndAnalyzeWithGemini(userGeminiInstruction, formatForAppSync) },
                            enabled = !transientState.isExportingGemini,
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export to Gemini")
                        }

                        OutlinedButton(
                            onClick = { viewModel.copyDataToClipboard(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy Data")
                        }
                    }

                    transientState.geminiSyncPayload?.let { payload ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Gemini Recommendations Ready to Sync", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(payload.summary, style = MaterialTheme.typography.bodyMedium)
                                Button(
                                    onClick = { viewModel.syncGeminiToApp(payload) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Apply & Sync Back to App", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Local Export / Restore Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Local Export & Restore")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = viewModel::exportJson, modifier = Modifier.weight(1f)) { Text("Export JSON") }
                        Button(onClick = viewModel::exportCsv, modifier = Modifier.weight(1f)) { Text("Export CSV") }
                    }
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        label = { Text("Import JSON Backup") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Button(
                        onClick = {
                            if (importText.isBlank()) viewModel.importJson("") else showImportConfirmation = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import JSON Backup")
                    }
                }
            }
        }

        // Labeled Danger Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("Danger Zone", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                    }

                    Text(
                        "Irreversible database reset actions. Exercise caution before deleting.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )

                    Button(
                        onClick = { showDeleteConfirmation = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete All Local Data")
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
