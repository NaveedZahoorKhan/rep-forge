@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.gymtracker.app.presentation.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import com.gymtracker.app.domain.usecase.NutritionCalculator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.gymtracker.app.data.auth.GoogleAuthService
import com.gymtracker.app.data.health.HealthSyncService
import com.gymtracker.app.presentation.components.GoogleAccountProfileCard
import com.gymtracker.app.presentation.components.GoogleLogoIcon
import com.gymtracker.app.presentation.components.GoogleSignInButton
import com.gymtracker.app.presentation.components.GoogleSignInOptionsDialog
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.gymtracker.app.data.backup.CloudBackupService
import com.gymtracker.app.data.local.entity.Gender
import com.gymtracker.app.data.local.entity.GymEquipmentEntity
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.ThemeMode
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.remote.gemini.GeminiApiClient
import com.gymtracker.app.data.remote.gemini.GeminiExportSyncPayload
import com.gymtracker.app.data.remote.gemini.GeminiSuggestedReminder
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.notification.NotificationHelper
import com.gymtracker.app.presentation.components.SectionTitle
import com.gymtracker.app.presentation.nutrition.NutritionContent
import com.gymtracker.app.presentation.nutrition.NutritionViewModel
import com.gymtracker.app.worker.WaterReminderWorker
import com.gymtracker.app.worker.WorkoutReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
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
        viewModelScope.launch { repository.createOrUpdateProfile(profile) }
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

    fun parseAndSyncRawGeminiJson(rawJson: String) {
        viewModelScope.launch {
            try {
                val clean = cleanJson(rawJson)
                val payload = Json { ignoreUnknownKeys = true; isLenient = true }.decodeFromString(
                    GeminiExportSyncPayload.serializer(),
                    clean
                )
                syncGeminiToApp(payload)
            } catch (e: Exception) {
                transient = transient.copy(status = "Could not parse JSON: ${e.message}")
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
                category = category,
                location = location,
                photoUri = persistentUriStr,
                notes = notes.trim(),
            )
            repository.addGymEquipment(equipment)
            transient = transient.copy(status = "Machine saved to profile")
        }
    }

    fun deleteEquipment(id: String) {
        viewModelScope.launch {
            repository.deleteGymEquipment(id)
        }
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
                    transient = transient.copy(status = "Signed in as ${user.email} (Google Health Linked)")
                    repository.syncProgressToGoogleHealth()
                },
                onFailure = {
                    transient = transient.copy(status = it.message ?: "Google sign-in failed")
                }
            )
        }
    }

    fun quickSignIn(email: String, name: String) {
        viewModelScope.launch {
            googleAuthService.quickSignIn(email, name).fold(
                onSuccess = { user ->
                    repository.updateUserGoogleAuth(
                        googleLinked = true,
                        googleEmail = user.email,
                        googleDisplayName = user.displayName,
                        googlePhotoUrl = user.photoUrl,
                        googleId = user.id,
                    )
                    transient = transient.copy(status = "Connected as ${user.email}")
                    repository.syncProgressToGoogleHealth()
                },
                onFailure = {
                    transient = transient.copy(status = it.message ?: "Sign-in failed")
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
            repository.updateHealthConnectStatus(linked = false)
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
    var showGoogleDialog by remember { mutableStateOf(false) }
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
        Text("Profile & Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        TabRow(selectedTabIndex = tab) {
            listOf("Settings", "Equipments", "Nutrition", "Data").forEachIndexed { index, title ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(title) })
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
                    onSignIn = { showGoogleDialog = true },
                    onQuickSignIn = { showGoogleDialog = true },
                    onSignOut = { viewModel.signOutGoogle() },
                    onSyncHealth = { viewModel.syncProgressToHealth() },
                    onOpenHealthSettings = { viewModel.openHealthConnectSettings(context) }
                )
            }
        }
    }

    if (showGoogleDialog) {
        GoogleSignInOptionsDialog(
            onDismiss = { showGoogleDialog = false },
            onLaunchPlayServices = {
                showGoogleDialog = false
                signInLauncher.launch(viewModel.googleSignInIntent())
            },
            onQuickSignIn = { email, name ->
                showGoogleDialog = false
                viewModel.quickSignIn(email, name)
            }
        )
    }
}

// -------------------------------------------------------------
// GYM EQUIPMENTS PANEL (SAVED TO USER'S WORKOUT PROFILE)
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EquipmentsPanel(
    equipments: List<GymEquipmentEntity>,
    viewModel: ProfileViewModel,
) {
    var showAddForm by remember { mutableStateOf(false) }
    var machineName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Gym") }
    var category by remember { mutableStateOf("Machine") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var machineNotes by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) selectedPhotoUri = uri
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Profile Gym Equipments (${equipments.size})", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "These uploaded images and machines are saved in your workout profile. Gemini automatically caters every workout routine to your available machines.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Button(
                onClick = { showAddForm = !showAddForm },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(if (showAddForm) Icons.Default.Close else Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (showAddForm) "Cancel" else "Add Gym Machine or Equipment")
            }
        }

        if (showAddForm) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("New Machine / Gear", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

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
                                Text(if (selectedPhotoUri != null) "Change Photo" else "Upload Photo")
                            }

                            if (selectedPhotoUri != null) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                ) {
                                    AsyncImage(
                                        model = selectedPhotoUri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (machineName.isNotBlank()) {
                                    viewModel.addEquipment(machineName, category, location, selectedPhotoUri, machineNotes)
                                    machineName = ""
                                    selectedPhotoUri = null
                                    showAddForm = false
                                }
                            },
                            enabled = machineName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Save Machine to Profile")
                        }
                    }
                }
            }
        }

        if (equipments.isEmpty()) {
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
                        Text("No gym equipments registered", fontWeight = FontWeight.Bold)
                        Text(
                            "Tap 'Add Gym Machine or Equipment' above to upload photos and machine names for Gemini personalization.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        items(equipments, key = { it.id }) { eq ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    eq.location,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Text(
                                    eq.category,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(onClick = { viewModel.deleteEquipment(eq.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SETTINGS PANEL
// -------------------------------------------------------------
@Composable
private fun SettingsPanel(profile: UserProfileEntity, reminders: List<ReminderEntity>, viewModel: ProfileViewModel) {
    var name by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var gender by remember(profile.gender) { mutableStateOf(profile.gender) }
    var goal by remember(profile.primaryGoal) { mutableStateOf(profile.primaryGoal) }
    var heightStr by remember(profile.heightCm) { mutableStateOf(profile.heightCm.toString()) }
    var weightStr by remember(profile.weightKg) { mutableStateOf(profile.weightKg.toString()) }
    var split by remember(profile.preferredSplit) { mutableStateOf(profile.preferredSplit.ifBlank { "Push Pull Legs (PPL)" }) }
    var rest by remember(profile.defaultRestSeconds) { mutableStateOf(profile.defaultRestSeconds.toString()) }
    var calories by remember(profile.calorieGoal) { mutableStateOf(profile.calorieGoal.toString()) }
    var water by remember(profile.waterGoalMl) { mutableStateOf(profile.waterGoalMl.toString()) }
    var reminderTitle by remember { mutableStateOf("Workout reminder") }
    var reminderTime by remember { mutableStateOf("1080") }

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
        val wKg = if (profile.unitSystem == UnitSystem.IMPERIAL) rawW * 0.45359237 else rawW
        val hCm = if (profile.unitSystem == UnitSystem.IMPERIAL) rawH * 2.54 else rawH
        val calculated = NutritionCalculator.autoCalculateTargetCalories(
            weightKg = wKg,
            heightCm = hCm,
            gender = gender,
            goal = goal,
        )
        calories = calculated.toString()
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Athlete Preferences")
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())

                    // Gender Selection (Default: Male)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Gender (Default: Male)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                    // Primary Goal Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Primary Goal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                        Text(
                            "Choose your fitness focus. Gemini calibrates target volume, nutrition, and rest to this goal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                                    label = {
                                        Text(
                                            g,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
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

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UnitSystem.entries.forEach {
                            FilterChip(selected = profile.unitSystem == it, onClick = { viewModel.save(profile.copy(unitSystem = it)) }, label = { Text(if (it == UnitSystem.METRIC) "kg/cm" else "lb/in") })
                        }
                    }

                    // Theme selector (Defaults to Light)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("App Theme (Default: Light Mode)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeMode.entries.forEach {
                                FilterChip(
                                    selected = profile.themeMode == it,
                                    onClick = { viewModel.save(profile.copy(themeMode = it)) },
                                    label = { Text(it.name.lowercase().replaceFirstChar { c -> c.titlecase() }) }
                                )
                            }
                        }
                    }

                    // Routine Split Selection (Single Select)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Workout Routine Split", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text("Single select", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            "Select one split. Changing this automatically reorganizes your weekly schedule and dashboard workouts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        for ((splitTitle, splitSchedule) in availableSplits) {
                            val isSelected = split.trim().equals(splitTitle.trim(), ignoreCase = true)
                            OutlinedCard(
                                onClick = { split = splitTitle },
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
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

                    // Height & Weight & Auto-Calculate Calories
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = heightStr,
                            onValueChange = {
                                heightStr = it
                                if (it.toDoubleOrNull() != null) autoCalculateCalories()
                            },
                            label = { Text("Height (${if (profile.unitSystem == UnitSystem.METRIC) "cm" else "in"})") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = weightStr,
                            onValueChange = {
                                weightStr = it
                                if (it.toDoubleOrNull() != null) autoCalculateCalories()
                            },
                            label = { Text("Weight (${if (profile.unitSystem == UnitSystem.METRIC) "kg" else "lb"})") },
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
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
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
                            singleLine = true,
                            supportingText = { Text("Calculated from $weightStr ${if (profile.unitSystem == UnitSystem.METRIC) "kg" else "lb"} & $heightStr ${if (profile.unitSystem == UnitSystem.METRIC) "cm" else "in"} for $goal") }
                        )
                    }

                    NumberField("Water goal (ml)", water, { water = it }, Modifier.fillMaxWidth())
                    ToggleRow("Sound cues", profile.soundEnabled) { viewModel.save(profile.copy(soundEnabled = it)) }
                    ToggleRow("Vibration feedback", profile.vibrationEnabled) { viewModel.save(profile.copy(vibrationEnabled = it)) }
                    ToggleRow("Daily workout reminders", profile.workoutReminderEnabled) { viewModel.save(profile.copy(workoutReminderEnabled = it)) }
                    Button(onClick = {
                        viewModel.save(
                            profile.copy(
                                displayName = name,
                                gender = gender,
                                primaryGoal = goal,
                                heightCm = heightStr.toDoubleOrNull() ?: profile.heightCm,
                                weightKg = weightStr.toDoubleOrNull() ?: profile.weightKg,
                                preferredSplit = split,
                                defaultRestSeconds = rest.toIntOrNull() ?: profile.defaultRestSeconds,
                                calorieGoal = calories.toIntOrNull() ?: profile.calorieGoal,
                                waterGoalMl = water.toIntOrNull() ?: profile.waterGoalMl,
                            )
                        )
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save settings & Update Split") }
                }
            }
        }
        // -------------------------------------------------------------
        // WATER REMINDERS & NOTIFICATIONS
        // -------------------------------------------------------------
        item {
            var waterInterval by remember { mutableIntStateOf(2) }
            val isWaterReminderActive = reminders.any { it.id == "water-reminder" && it.enabled }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Column {
                                Text("Water Intake Reminders", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Stay hydrated throughout training days", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("Reminder Frequency:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        onClick = {
                            viewModel.testWaterReminder(
                                currentMl = 500,
                                goalMl = profile.waterGoalMl
                            )
                        },
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

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Daily workout reminders")
                    OutlinedTextField(value = reminderTitle, onValueChange = { reminderTitle = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                    NumberField("Time minutes after midnight (e.g. 540 = 9:00 AM)", reminderTime, { reminderTime = it }, Modifier.fillMaxWidth())
                    Button(onClick = { viewModel.createReminder(reminderTitle, reminderTime.toIntOrNull() ?: 1080) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Schedule daily reminder")
                    }
                }
            }
        }
        items(reminders, key = { it.id }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
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
    viewModel: ProfileViewModel,
    profile: UserProfileEntity,
    onSignIn: () -> Unit,
    onQuickSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSyncHealth: () -> Unit,
    onOpenHealthSettings: () -> Unit,
) {
    val context = LocalContext.current
    var importText by remember { mutableStateOf("") }
    var userGeminiInstruction by remember { mutableStateOf("Calibrate my daily calories, water targets, and training volume based on my current progression.") }
    var formatForAppSync by remember { mutableStateOf(true) }
    var pasteSyncJson by remember { mutableStateOf("") }

    val transientState = viewModel.transient

    val presetInstructions = listOf(
        "⚡ Calorie & Water Targets" to "Calibrate my daily calorie and water goals to optimize recovery.",
        "🏋️ Routine & Volume Optimization" to "Analyze my training split and volume, suggesting workout adjustments.",
        "📊 Full Health & Training Audit" to "Comprehensive audit of weight trends, workouts, and nutrition with sync adjustments."
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Status banner if present
        if (transientState.status.isNotBlank()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        transientState.status,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // EXPORT TO GEMINI AI & DIRECT APP SYNC
        // -------------------------------------------------------------
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text("Export Data to Gemini & App Sync", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Elite sports science analysis with one-tap sync back into GymTracker", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    HorizontalDivider()

                    // Quick Preset Chips
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

                    // Editable custom instruction
                    OutlinedTextField(
                        value = userGeminiInstruction,
                        onValueChange = { userGeminiInstruction = it },
                        label = { Text("Instructions for Gemini Coach") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    // Sync Schema Toggle (Mandatory requirement)
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
                        Column(Modifier.weight(1f)) {
                            Text("Ask Gemini to format response for direct sync back to app", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("Returns structured parameters (calories, water, split, reminders) that sync immediately.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Action Buttons Row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                viewModel.exportAndAnalyzeWithGemini(userGeminiInstruction, formatForAppSync)
                            },
                            enabled = !transientState.isExportingGemini,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (transientState.isExportingGemini) {
                                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("Analyzing...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export to Gemini")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Gemini Prompt", userGeminiInstruction)
                                clipboard.setPrimaryClip(clip)
                                viewModel.exportJson()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy Data")
                        }
                    }

                    // Gemini Sync Card if payload is received
                    transientState.geminiSyncPayload?.let { payload ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Text("Gemini Recommendations Ready to Sync", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }

                                Text(payload.summary, style = MaterialTheme.typography.bodyMedium)

                                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))

                                Text("Proposed App Changes:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    payload.recommendedCalorieGoal?.let { cal ->
                                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.weight(1f)) {
                                            Column(Modifier.padding(8.dp)) {
                                                Text("Calories", style = MaterialTheme.typography.labelSmall)
                                                Text("$cal kcal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                    payload.recommendedWaterGoalMl?.let { water ->
                                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.weight(1f)) {
                                            Column(Modifier.padding(8.dp)) {
                                                Text("Water Goal", style = MaterialTheme.typography.labelSmall)
                                                Text("$water ml", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }

                                payload.recommendedSplit?.let { split ->
                                    Text("• Recommended Split: $split", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                }

                                if (payload.suggestedReminders.isNotEmpty()) {
                                    Text("• Suggested Reminders: ${payload.suggestedReminders.joinToString { it.title }}", style = MaterialTheme.typography.bodySmall)
                                }

                                Button(
                                    onClick = { viewModel.syncGeminiToApp(payload) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Apply & Sync Back to App", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Display raw response text if available
                    if (transientState.geminiExportResponse.isNotBlank() && transientState.geminiSyncPayload == null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Gemini Response:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text(transientState.geminiExportResponse, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // PASTE EXTERNAL GEMINI RESPONSE TO SYNC
        // -------------------------------------------------------------
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Sync External Gemini Response")
                    Text("Ran Gemini in browser or chat? Paste the JSON response here to sync into GymTracker.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = pasteSyncJson,
                        onValueChange = { pasteSyncJson = it },
                        label = { Text("Paste Gemini JSON response") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                    Button(
                        onClick = {
                            if (pasteSyncJson.isNotBlank()) {
                                viewModel.parseAndSyncRawGeminiJson(pasteSyncJson)
                                pasteSyncJson = ""
                            }
                        },
                        enabled = pasteSyncJson.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Parse & Sync to App")
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // EXPORT & GDPR BACKUPS
        // -------------------------------------------------------------
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Local Export & Restore")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = viewModel::exportJson, modifier = Modifier.weight(1f)) { Text("Export JSON") }
                        Button(onClick = viewModel::exportCsv, modifier = Modifier.weight(1f)) { Text("Export CSV") }
                    }
                    OutlinedTextField(value = importText, onValueChange = { importText = it }, label = { Text("Import JSON Backup") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Button(onClick = { viewModel.importJson(importText) }, modifier = Modifier.fillMaxWidth()) { Text("Import backup") }
                    OutlinedButton(onClick = viewModel::deleteAllData, modifier = Modifier.fillMaxWidth()) { Text("Delete local data") }
                }
            }
        }

        // -------------------------------------------------------------
        // GOOGLE ACCOUNT & HEALTH CONNECT CLOUD BACKUP
        // -------------------------------------------------------------
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GoogleLogoIcon(Modifier.size(20.dp))
                            SectionTitle("Google Account & Health Link")
                        }
                    }

                    if (profile.googleLinked && !profile.googleEmail.isNullOrBlank()) {
                        GoogleAccountProfileCard(
                            email = profile.googleEmail,
                            displayName = profile.googleDisplayName ?: "Athlete",
                            photoUrl = profile.googlePhotoUrl,
                            healthLinked = profile.healthConnectLinked,
                            onSignOut = onSignOut
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = onSyncHealth, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Sync Health")
                            }
                            OutlinedButton(onClick = onOpenHealthSettings, modifier = Modifier.weight(1f)) {
                                Text("Health Settings")
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = viewModel::cloudBackup, modifier = Modifier.weight(1f)) { Text("Cloud Backup") }
                            Button(onClick = viewModel::cloudRestore, modifier = Modifier.weight(1f)) { Text("Cloud Restore") }
                        }
                    } else {
                        Text(
                            text = "Sign in with Google to synchronize your workout history, body weight logs, and daily progress to Google Health Connect and cloud storage.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            GoogleSignInButton(onClick = onSignIn, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = onQuickSignIn) { Text("Quick Connect") }
                        }
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
