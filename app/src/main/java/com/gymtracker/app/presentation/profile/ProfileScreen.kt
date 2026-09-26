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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.presentation.components.SectionTitle
import com.gymtracker.app.presentation.nutrition.NutritionContent
import com.gymtracker.app.presentation.nutrition.NutritionViewModel
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfileEntity? = null,
    val reminders: List<ReminderEntity> = emptyList(),
    val equipments: List<GymEquipmentEntity> = emptyList(),
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
        repository.observeGymEquipments(),
    ) { profile, reminders, equipments ->
        ProfileUiState(profile = profile, reminders = reminders, equipments = equipments)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())

    var transient by mutableStateOf(ProfileUiState())
        private set

    fun save(profile: UserProfileEntity) {
        viewModelScope.launch { repository.createOrUpdateProfile(profile) }
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
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Primary Goal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            goals.forEach { g ->
                                FilterChip(
                                    selected = goal == g,
                                    onClick = { goal = g },
                                    label = { Text(g, fontSize = 12.sp) }
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
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
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
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
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
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
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
