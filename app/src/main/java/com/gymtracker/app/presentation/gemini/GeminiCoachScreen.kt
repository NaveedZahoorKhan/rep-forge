package com.gymtracker.app.presentation.gemini

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gymtracker.app.data.local.entity.GeminiSyncReportEntity
import com.gymtracker.app.data.local.entity.UserHealthProfileEntity
import com.gymtracker.app.data.remote.gemini.GeminiExerciseItem
import com.gymtracker.app.data.remote.gemini.GeminiProgressSyncResult
import com.gymtracker.app.data.remote.gemini.GeminiWorkoutOption
import com.gymtracker.app.presentation.components.SectionTitle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiCoachScreen(
    onNavigateToActiveWorkout: (sessionId: String) -> Unit,
    onNavigateToWorkoutsList: () -> Unit,
    viewModel: GeminiCoachViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.statusMessage) {
        state.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text("Gemini Intelligence", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (state.isLiveApi) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = if (state.isLiveApi) "Gemini Live" else "AI Coach Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isLiveApi) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(
                selectedTabIndex = state.selectedTab,
                modifier = Modifier.fillMaxWidth().testTag("gemini_tabs"),
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Workout Builder") },
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Workout Builder") },
                    modifier = Modifier.testTag("tab_workout_builder"),
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Progress Sync") },
                    icon = { Icon(Icons.Default.Sync, contentDescription = "Progress Sync") },
                    modifier = Modifier.testTag("tab_progress_sync"),
                )
            }

            when (state.selectedTab) {
                0 -> WorkoutBuilderTab(
                    state = state,
                    onUpdateHealthProfile = viewModel::updateHealthProfile,
                    onUpdateNotes = viewModel::updateNotes,
                    onSetPhotoUri = viewModel::setMachinePhoto,
                    onSetCapturedBitmap = viewModel::setCapturedBitmap,
                    onRemovePhoto = viewModel::removePhoto,
                    onGenerate = viewModel::generateCustomWorkout,
                    onSelectOption = viewModel::selectOption,
                    onSaveOption = { option ->
                        viewModel.saveWorkoutOption(option) {
                            onNavigateToWorkoutsList()
                        }
                    },
                    onStartOption = { option ->
                        viewModel.startWorkoutFromOption(option) { sessionId ->
                            onNavigateToActiveWorkout(sessionId)
                        }
                    },
                )
                1 -> ProgressSyncTab(
                    state = state,
                    onSyncNow = viewModel::syncDailyProgressWithGemini,
                )
            }
        }
    }
}

@Composable
private fun WorkoutBuilderTab(
    state: GeminiCoachUiState,
    onUpdateHealthProfile: (UserHealthProfileEntity) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onSetPhotoUri: (android.net.Uri?) -> Unit,
    onSetCapturedBitmap: (Bitmap?) -> Unit,
    onRemovePhoto: () -> Unit,
    onGenerate: () -> Unit,
    onSelectOption: (Int) -> Unit,
    onSaveOption: (GeminiWorkoutOption) -> Unit,
    onStartOption: (GeminiWorkoutOption) -> Unit,
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> onSetPhotoUri(uri) },
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                onSetCapturedBitmap(bitmap)
            }
        },
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // Intro banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                    Column {
                        Text(
                            "AI Custom Workout Builder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Tell Gemini your health background, upload a photo of gym equipment, and get custom workouts tailored to your body.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // Section 1: Athlete Health Background & Preferences
        item {
            HealthBackgroundCard(
                healthProfile = state.healthProfile,
                onSaveProfile = onUpdateHealthProfile,
            )
        }

        // Section 2: Machine Photo Upload & Equipment Info
        item {
            MachinePhotoUploadCard(
                bitmap = state.machineBitmap,
                onPickPhoto = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onCapturePhoto = { cameraLauncher.launch(null) },
                onRemovePhoto = onRemovePhoto,
            )
        }

        // Section 3: Personal Notes & Custom Requests
        item {
            PersonalNotesCard(
                userNotes = state.userNotes,
                onUpdateNotes = onUpdateNotes,
            )
        }

        // Action Button: Generate
        item {
            Button(
                onClick = onGenerate,
                enabled = !state.isBuildingWorkout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_gemini_workout_button"),
                shape = RoundedCornerShape(12.dp),
            ) {
                if (state.isBuildingWorkout) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Gemini is designing custom workouts...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Tailored Workouts with Gemini", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section 3: Results from Gemini
        state.builderResult?.let { result ->
            item {
                SectionTitle("Gemini Analysis & Options")
            }

            // Machine Identification & Safety Cues
            item {
                IdentifiedMachineCard(result = result, bitmap = state.machineBitmap)
            }

            // Options Tabs / Selector
            item {
                Text(
                    "Select a Workout Option (${result.options.size} available):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    result.options.forEachIndexed { index, option ->
                        FilterChip(
                            selected = state.selectedOptionIndex == index,
                            onClick = { onSelectOption(index) },
                            label = { Text(option.title) },
                            leadingIcon = if (state.selectedOptionIndex == index) {
                                { Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.testTag("workout_option_chip_$index"),
                        )
                    }
                }
            }

            // Selected Option Details
            val selectedOption = result.options.getOrNull(state.selectedOptionIndex)
                ?: result.options.firstOrNull()

            selectedOption?.let { option ->
                item {
                    WorkoutOptionDetailCard(
                        option = option,
                        onSave = { onSaveOption(option) },
                        onStart = { onStartOption(option) },
                    )
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthBackgroundCard(
    healthProfile: UserHealthProfileEntity,
    onSaveProfile: (UserHealthProfileEntity) -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var goal by remember(healthProfile.primaryGoal) { mutableStateOf(healthProfile.primaryGoal) }
    var experience by remember(healthProfile.experienceLevel) { mutableStateOf(healthProfile.experienceLevel) }
    var healthConditions by remember(healthProfile.healthConditions) { mutableStateOf(healthProfile.healthConditions) }
    var limitations by remember(healthProfile.injuriesAndLimitations) { mutableStateOf(healthProfile.injuriesAndLimitations) }
    var targetMuscles by remember(healthProfile.targetMuscles) { mutableStateOf(healthProfile.targetMuscles) }
    var durationMinutes by remember(healthProfile.preferredDurationMinutes) { mutableStateOf(healthProfile.preferredDurationMinutes) }

    val fitnessGoals = listOf("Muscle Hypertrophy", "Strength & Power", "Fat Loss & Tone", "Joint Mobility & Rehab", "Endurance")
    val experienceLevels = listOf("Beginner", "Intermediate", "Advanced")
    val durationOptions = listOf(25, 35, 45, 60)
    val healthPresets = listOf("Lower Back Safe", "Knee Friendly", "Shoulder Safe", "Cardiac Conscious", "No Limitations")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("My Health Background & Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (healthConditions.isNotBlank() || limitations.isNotBlank()) {
                                "$goal • $experience • ${durationMinutes}m • ${listOf(healthConditions, limitations).filter { it.isNotBlank() }.joinToString(", ")}"
                            } else {
                                "$goal • $experience • ${durationMinutes}m • (Tap to add precautions)"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand health background",
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider()

                    Text("Primary Fitness Goal", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        fitnessGoals.forEach { g ->
                            FilterChip(
                                selected = goal == g,
                                onClick = { goal = g },
                                label = { Text(g) },
                            )
                        }
                    }

                    Text("Experience Level", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        experienceLevels.forEach { exp ->
                            FilterChip(
                                selected = experience == exp,
                                onClick = { experience = exp },
                                label = { Text(exp) },
                            )
                        }
                    }

                    Text("Preferred Session Duration", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        durationOptions.forEach { d ->
                            FilterChip(
                                selected = durationMinutes == d,
                                onClick = { durationMinutes = d },
                                label = { Text("${d}m") },
                            )
                        }
                    }

                    Text("Quick Health Presets (Optional)", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        healthPresets.forEach { preset ->
                            AssistChip(
                                onClick = {
                                    if (preset == "No Limitations") {
                                        healthConditions = ""
                                        limitations = ""
                                    } else {
                                        healthConditions = if (healthConditions.isBlank()) preset else "$healthConditions, $preset"
                                    }
                                },
                                label = { Text(if (preset == "No Limitations") "Clear All" else "+ $preset") },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = healthConditions,
                        onValueChange = { healthConditions = it },
                        label = { Text("Health conditions, joint issues, or spine precautions") },
                        placeholder = { Text("Type your health conditions or joint precautions here...") },
                        trailingIcon = if (healthConditions.isNotEmpty()) {
                            {
                                IconButton(onClick = { healthConditions = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear health conditions")
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth().testTag("input_health_conditions"),
                        minLines = 2,
                    )

                    OutlinedTextField(
                        value = limitations,
                        onValueChange = { limitations = it },
                        label = { Text("Injuries & exercises/angles to avoid") },
                        placeholder = { Text("Type your injuries, precautions, or exercises to avoid here...") },
                        trailingIcon = if (limitations.isNotEmpty()) {
                            {
                                IconButton(onClick = { limitations = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear injuries and limitations")
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth().testTag("input_injuries_limitations"),
                        minLines = 2,
                    )

                    OutlinedTextField(
                        value = targetMuscles,
                        onValueChange = { targetMuscles = it },
                        label = { Text("Target Muscles for This Workout") },
                        placeholder = { Text("Type target muscles (e.g. Chest & Shoulders, Back, Full Body)") },
                        trailingIcon = if (targetMuscles.isNotEmpty()) {
                            {
                                IconButton(onClick = { targetMuscles = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear target muscles")
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth().testTag("input_target_muscles"),
                        singleLine = true,
                    )

                    Button(
                        onClick = {
                            onSaveProfile(
                                healthProfile.copy(
                                    primaryGoal = goal,
                                    experienceLevel = experience,
                                    healthConditions = healthConditions,
                                    injuriesAndLimitations = limitations,
                                    targetMuscles = targetMuscles,
                                    preferredDurationMinutes = durationMinutes,
                                    updatedAt = System.currentTimeMillis(),
                                )
                            )
                            isExpanded = false
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_health_profile_button"),
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Health Background")
                    }
                }
            }
        }
    }
}

@Composable
private fun MachinePhotoUploadCard(
    bitmap: Bitmap?,
    onPickPhoto: () -> Unit,
    onCapturePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Gym Machine / Equipment Photo (Optional)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            Text(
                "Take a photo or upload an image of the machine you want to use. Gemini will identify it and design a safe routine.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Workout machine photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    IconButton(
                        onClick = onRemovePhoto,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove photo")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onPickPhoto,
                        modifier = Modifier.weight(1f).testTag("pick_machine_photo_button"),
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Gallery")
                    }
                    OutlinedButton(
                        onClick = onCapturePhoto,
                        modifier = Modifier.weight(1f).testTag("take_machine_photo_button"),
                    ) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Camera")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonalNotesCard(
    userNotes: String,
    onUpdateNotes: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Personal Workout Notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (userNotes.isNotBlank()) {
                    IconButton(
                        onClick = { onUpdateNotes("") },
                        modifier = Modifier.size(32.dp).testTag("clear_personal_notes_button"),
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear personal notes",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                "Tell Gemini specifically how to modify the workout to your personal needs (e.g. equipment available, focus area, supersets, low energy, tempo, or specific exercise preferences).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = userNotes,
                onValueChange = onUpdateNotes,
                label = { Text("Personal notes for Gemini") },
                placeholder = { Text("e.g. Feeling low energy today, keep it to 3 exercises; prefer dumbbells over barbells; focus heavily on lateral delts.") },
                modifier = Modifier.fillMaxWidth().testTag("input_machine_notes"),
                singleLine = false,
                minLines = 3,
                maxLines = 6,
            )

            // Quick suggestion chips
            val suggestions = listOf(
                "Keep intensity light today",
                "Focus on upper chest & triceps",
                "Dumbbells only (no barbells)",
                "Low back friendly / no spinal load",
                "Short 30-sec rest circuits",
            )

            Text(
                "Quick Add to Notes:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                suggestions.forEach { suggestion ->
                    AssistChip(
                        onClick = {
                            val newNotes = if (userNotes.isBlank()) {
                                suggestion
                            } else if (userNotes.contains(suggestion)) {
                                userNotes
                            } else {
                                "$userNotes, $suggestion"
                            }
                            onUpdateNotes(newNotes)
                        },
                        label = { Text(suggestion, style = MaterialTheme.typography.bodySmall) },
                    )
                }
            }
        }
    }
}

@Composable
private fun IdentifiedMachineCard(
    result: com.gymtracker.app.data.remote.gemini.GeminiWorkoutBuilderResult,
    bitmap: Bitmap?,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    "Detected: ${result.identifiedMachine.ifBlank { "Gym Equipment Station" }}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (result.machineDescription.isNotBlank()) {
                Text(
                    result.machineDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (result.healthSafetyCues.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                    Text("Health & Biomechanical Safety Cues:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                result.healthSafetyCues.forEach { cue ->
                    Row(
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(cue, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutOptionDetailCard(
    option: GeminiWorkoutOption,
    onSave: () -> Unit,
    onStart: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(option.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${option.splitType} • ${option.estimatedMinutes} minutes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            if (option.healthFocusNote.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f))
                        .padding(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                        Text(
                            "Why this fits your health background: ${option.healthFocusNote}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }

            HorizontalDivider()

            Text("Exercises (${option.exercises.size}):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

            option.exercises.forEachIndexed { idx, ex ->
                ExerciseItemRow(index = idx + 1, item = ex)
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f).testTag("save_gemini_workout_button"),
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save Workout")
                }
                Button(
                    onClick = onStart,
                    modifier = Modifier.weight(1f).testTag("start_gemini_workout_button"),
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Start Now")
                }
            }
        }
    }
}

@Composable
private fun ExerciseItemRow(
    index: Int,
    item: GeminiExerciseItem,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$index. ${item.exerciseName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        "${item.sets} sets × ${item.repsMin}-${item.repsMax} reps",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Target: ${item.primaryMuscle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Rest: ${item.restSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.targetRpe?.let { rpe ->
                    Text(
                        "RPE: $rpe",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (item.healthModification.isNotBlank()) {
                Text(
                    "Health Cue: ${item.healthModification}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium,
                )
            }

            if (item.instructions.isNotBlank()) {
                Text(
                    item.instructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ProgressSyncTab(
    state: GeminiCoachUiState,
    onSyncNow: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Spacer(Modifier.height(4.dp)) }

        // Sync Action Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                ),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(
                                "Daily Progress Sync with Gemini",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Sync workouts, sets, volume, and nutrition with your health background for smart AI coach feedback.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Button(
                        onClick = onSyncNow,
                        enabled = !state.isSyncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("sync_progress_with_gemini_button"),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        if (state.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Syncing progress with Gemini...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Sync Daily Progress to Gemini", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Display Latest Sync Report (either from live state or first in past reports)
        val report = state.latestSyncResult
        if (report != null) {
            item {
                SectionTitle("Latest Gemini Sync Report")
            }
            item {
                GeminiSyncReportCard(report = report)
            }
        } else if (state.pastSyncReports.isNotEmpty()) {
            val latestPast = state.pastSyncReports.first()
            item {
                SectionTitle("Recent Gemini Sync Report")
            }
            item {
                PastReportCard(entity = latestPast, dateFormat = dateFormat)
            }
        }

        // Past Sync History Section
        if (state.pastSyncReports.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Past Sync History (${state.pastSyncReports.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            items(state.pastSyncReports, key = { it.id }) { pastReport ->
                PastReportCard(entity = pastReport, dateFormat = dateFormat)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun GeminiSyncReportCard(report: GeminiProgressSyncResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Synced Successfully", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        report.consistencyScore,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Text(
                report.overallSummary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            HorizontalDivider()

            // Health & Recovery Review
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                    Text("Health & Recovery Safety Audit", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
                Text(
                    report.healthAndRecoveryReview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Volume & Load Analysis
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Progressive Overload & Volume Analysis", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    report.volumeAndLoadAnalysis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Nutrition & Hydration Audit
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Nutrition & Hydration Alignment", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    report.nutritionAndHydrationAudit,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Actionable Steps
            if (report.actionableSuggestions.isNotEmpty()) {
                Text("Actionable Coaching Recommendations:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                report.actionableSuggestions.forEach { suggestion ->
                    Row(
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(suggestion, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (report.motivationalQuote.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(10.dp),
                ) {
                    Text(
                        "“${report.motivationalQuote}”",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun PastReportCard(
    entity: GeminiSyncReportEntity,
    dateFormat: SimpleDateFormat,
) {
    var isExpanded by remember { mutableStateOf(false) }
    val suggestions: List<String> = remember(entity.actionableSuggestionsJson) {
        runCatching {
            Json.decodeFromString<List<String>>(entity.actionableSuggestionsJson)
        }.getOrDefault(emptyList())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        dateFormat.format(Date(entity.syncedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        entity.consistencyScore,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle report details",
                    )
                }
            }

            Text(
                entity.overallSummary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
            )

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()

                    Text("Health & Recovery:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(entity.healthReview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Text("Volume & Progression:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(entity.volumeAnalysis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Text("Nutrition & Fuel:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Text(entity.nutritionAudit, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (suggestions.isNotEmpty()) {
                        Text("Recommendations:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        suggestions.forEach { s ->
                            Text("• $s", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
