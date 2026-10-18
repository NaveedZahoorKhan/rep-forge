package com.gymtracker.app.presentation.onboarding

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.gymtracker.app.data.auth.GoogleAuthService
import com.gymtracker.app.data.local.entity.Gender
import com.gymtracker.app.data.local.entity.GymEquipmentEntity
import com.gymtracker.app.data.local.entity.ThemeMode
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.remote.gemini.GeminiApiClient
import com.gymtracker.app.data.remote.gemini.GeminiWorkoutBuilderResult
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.NutritionCalculator
import com.gymtracker.app.presentation.components.GoogleLogoIcon
import com.gymtracker.app.presentation.components.GoogleSignInButton
import com.gymtracker.app.presentation.components.PrimaryButton
import com.gymtracker.app.presentation.components.SecondaryButton
import com.gymtracker.app.presentation.theme.AppSpacing
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlanInputsSnapshot(
    val routineName: String,
    val goal: String,
    val experienceLevel: String,
    val trainingDays: Int,
    val notes: String,
    val equipmentIds: List<String>,
)

data class OnboardingUiState(
    val currentStep: Int = 0, // 0 = Welcome & Goals, 1 = Schedule & Equipment, 2 = Measurements (Optional), 3 = Review & Plan
    val isGeneratingPlan: Boolean = false,
    val generatedPlan: GeminiWorkoutBuilderResult? = null,
    val generatedPlanInputs: PlanInputsSnapshot? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: GymRepository,
    private val googleAuthService: GoogleAuthService,
    private val geminiApiClient: GeminiApiClient,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val profile: StateFlow<UserProfileEntity?> = repository.observeUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val equipments: StateFlow<List<GymEquipmentEntity>> = repository.observeGymEquipments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun getGoogleSignInIntent(): Intent = googleAuthService.getSignInIntent()

    fun handleGoogleSignIn(data: Intent?, onUserLoaded: (name: String) -> Unit) {
        viewModelScope.launch {
            googleAuthService.handleSignInResult(data).fold(
                onSuccess = { user ->
                    repository.updateUserGoogleAuth(
                        googleLinked = true,
                        googleEmail = user.email,
                        googleDisplayName = user.displayName,
                        googlePhotoUrl = user.photoUrl,
                        googleId = user.id,
                    )
                    _uiState.update { it.copy(errorMessage = null) }
                    onUserLoaded(user.displayName)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            errorMessage = "Google sign-in failed: ${error.message ?: "Please check Google Play Services and try again."}"
                        )
                    }
                }
            )
        }
    }

    fun setStep(step: Int) {
        _uiState.update { it.copy(currentStep = step.coerceIn(0, 3), errorMessage = null) }
    }

    fun addEquipment(
        name: String,
        category: String,
        location: String,
        photoUri: Uri?,
        notes: String = "",
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            var persistentUriStr: String? = null
            if (photoUri != null) {
                persistentUriStr = try {
                    saveImageToInternalStorage(context, photoUri)
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
        }
    }

    fun removeEquipment(id: String) {
        viewModelScope.launch {
            repository.deleteGymEquipment(id)
        }
    }

    fun generateGeminiPlan(
        routineName: String,
        goal: String,
        experienceLevel: String,
        trainingDays: Int,
        userNotes: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPlan = true, errorMessage = null, statusMessage = "Building your tailored plan...") }
            try {
                val existingHealth = repository.getHealthProfile()
                val updatedHealth = existingHealth.copy(
                    primaryGoal = goal,
                    experienceLevel = experienceLevel,
                    updatedAt = System.currentTimeMillis(),
                )
                repository.updateHealthProfile(updatedHealth)

                val currentProfile = profile.value
                val currentEquipments = repository.getGymEquipments()

                val bitmaps = mutableListOf<Bitmap>()
                for (eq in currentEquipments.take(3)) {
                    val uriStr = eq.photoUri
                    if (!uriStr.isNullOrBlank()) {
                        try {
                            val uri = Uri.parse(uriStr)
                            val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                            }
                            bitmaps.add(bmp)
                        } catch (_: Exception) {
                        }
                    }
                }

                val notesWithDays = if (userNotes.isNotBlank()) {
                    "$userNotes (Days available to train per week: $trainingDays)"
                } else {
                    "Days available to train per week: $trainingDays"
                }

                val result = geminiApiClient.generateRoutinePlan(
                    routineName = routineName,
                    userGoal = goal,
                    machines = currentEquipments,
                    userNotes = notesWithDays,
                    healthProfile = updatedHealth,
                    userProfile = currentProfile,
                    machineBitmaps = bitmaps,
                )

                val snapshot = PlanInputsSnapshot(
                    routineName = routineName,
                    goal = goal,
                    experienceLevel = experienceLevel,
                    trainingDays = trainingDays,
                    notes = userNotes,
                    equipmentIds = currentEquipments.map { it.id }.sorted(),
                )

                _uiState.update {
                    it.copy(
                        isGeneratingPlan = false,
                        generatedPlan = result,
                        generatedPlanInputs = snapshot,
                        statusMessage = "Plan generated!",
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGeneratingPlan = false,
                        errorMessage = "Could not generate plan: ${e.message}",
                    )
                }
            }
        }
    }

    fun completeOnboarding(
        updatedProfile: UserProfileEntity,
        experienceLevel: String,
        savePlan: Boolean = true,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            val plan = _uiState.value.generatedPlan
            if (savePlan && plan != null && plan.options.isNotEmpty()) {
                repository.saveGeminiRoutinePlan(plan, setAsSchedule = true)
            }

            val existingHealth = repository.getHealthProfile()
            repository.updateHealthProfile(
                existingHealth.copy(
                    primaryGoal = updatedProfile.primaryGoal,
                    experienceLevel = experienceLevel,
                    updatedAt = System.currentTimeMillis(),
                )
            )

            repository.createOrUpdateProfile(
                updatedProfile.copy(
                    onboardingComplete = true,
                    themeMode = ThemeMode.LIGHT,
                    gender = updatedProfile.gender,
                )
            )
            onDone()
        }
    }

    private fun saveImageToInternalStorage(context: Context, sourceUri: Uri): String {
        val folder = File(context.filesDir, "equipments").apply { mkdirs() }
        val targetFile = File(folder, "eq_${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return targetFile.toURI().toString()
    }
}

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val storedProfile by viewModel.profile.collectAsStateWithLifecycle()
    val equipments by viewModel.equipments.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var name by remember(storedProfile?.displayName) { mutableStateOf(storedProfile?.displayName ?: "Athlete") }
    var gender by remember(storedProfile?.gender) { mutableStateOf(storedProfile?.gender ?: Gender.MALE) }
    var primaryGoal by remember(storedProfile?.primaryGoal) { mutableStateOf(storedProfile?.primaryGoal ?: "Lose weight") }
    var experienceLevel by remember { mutableStateOf("Intermediate") }
    var trainingDays by remember { mutableIntStateOf(4) }
    var unitSystem by remember(storedProfile?.unitSystem) { mutableStateOf(storedProfile?.unitSystem ?: UnitSystem.METRIC) }

    var height by remember(storedProfile?.heightCm, storedProfile?.unitSystem) {
        val initialUnit = storedProfile?.unitSystem ?: UnitSystem.METRIC
        val rawCm = storedProfile?.heightCm ?: 175.0
        val displayVal = if (initialUnit == UnitSystem.IMPERIAL) rawCm / 2.54 else rawCm
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", displayVal).removeSuffix(".0"))
    }
    var weight by remember(storedProfile?.weightKg, storedProfile?.unitSystem) {
        val initialUnit = storedProfile?.unitSystem ?: UnitSystem.METRIC
        val rawKg = storedProfile?.weightKg ?: 75.0
        val displayVal = if (initialUnit == UnitSystem.IMPERIAL) rawKg / 0.45359237 else rawKg
        mutableStateOf(String.format(java.util.Locale.US, "%.1f", displayVal).removeSuffix(".0"))
    }
    var calories by remember(storedProfile?.calorieGoal) { mutableStateOf((storedProfile?.calorieGoal ?: 2100).toString()) }

    val heightVal = height.toDoubleOrNull()
    val weightVal = weight.toDoubleOrNull()
    val caloriesVal = calories.toIntOrNull()

    val heightError = when {
        height.isBlank() -> null // Optional
        heightVal == null || heightVal <= 0 -> "Enter a valid positive number"
        unitSystem == UnitSystem.METRIC && (heightVal < 50.0 || heightVal > 280.0) -> "Must be between 50 and 280 cm"
        unitSystem == UnitSystem.IMPERIAL && (heightVal < 20.0 || heightVal > 110.0) -> "Must be between 20 and 110 inches"
        else -> null
    }

    val weightError = when {
        weight.isBlank() -> null // Optional
        weightVal == null || weightVal <= 0 -> "Enter a valid positive number"
        unitSystem == UnitSystem.METRIC && (weightVal < 20.0 || weightVal > 300.0) -> "Must be between 20 and 300 kg"
        unitSystem == UnitSystem.IMPERIAL && (weightVal < 45.0 || weightVal > 660.0) -> "Must be between 45 and 660 lbs"
        else -> null
    }

    val caloriesError = when {
        calories.isBlank() -> null // Optional
        caloriesVal == null || caloriesVal <= 0 -> "Enter a valid positive number"
        caloriesVal < 800 || caloriesVal > 10000 -> "Must be between 800 and 10,000 kcal"
        else -> null
    }

    val isStepValid = heightError == null && weightError == null && caloriesError == null

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.handleGoogleSignIn(result.data) { loadedName ->
            name = loadedName
        }
    }

    fun computeCalories(): Int {
        val rawWeight = weight.toDoubleOrNull() ?: 75.0
        val rawHeight = height.toDoubleOrNull() ?: 175.0
        val weightKg = if (unitSystem == UnitSystem.IMPERIAL) rawWeight * 0.45359237 else rawWeight
        val heightCm = if (unitSystem == UnitSystem.IMPERIAL) rawHeight * 2.54 else rawHeight
        return NutritionCalculator.autoCalculateTargetCalories(
            weightKg = weightKg,
            heightCm = heightCm,
            gender = gender,
            goal = primaryGoal,
        )
    }

    var selectedRoutine by remember(storedProfile?.preferredSplit) {
        mutableStateOf(storedProfile?.preferredSplit ?: "Push Pull Legs (PPL)")
    }
    var personalNotes by remember { mutableStateOf("") }

    val stepsTitles = listOf("Welcome & Goals", "Schedule & Equipment", "Optional Measurements", "Plan Review")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = AppSpacing.gutter, vertical = AppSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text("Rep Forge Setup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Step ${uiState.currentStep + 1} of 4: ${stepsTitles[uiState.currentStep]}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    if (uiState.currentStep > 0) {
                        SecondaryButton(
                            text = "Back",
                            onClick = { viewModel.setStep(uiState.currentStep - 1) },
                            modifier = Modifier.height(36.dp)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { (uiState.currentStep + 1) / 4f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (uiState.currentStep) {
                0 -> StepOneWelcomeGoals(
                    name = name,
                    onNameChange = { name = it },
                    primaryGoal = primaryGoal,
                    onGoalChange = { primaryGoal = it },
                    experienceLevel = experienceLevel,
                    onExperienceChange = { experienceLevel = it },
                    isGoogleLinked = storedProfile?.googleLinked == true,
                    googleEmail = storedProfile?.googleEmail,
                    onSignInGoogle = { signInLauncher.launch(viewModel.getGoogleSignInIntent()) },
                    onNext = { viewModel.setStep(1) },
                )

                1 -> StepTwoScheduleEquipment(
                    trainingDays = trainingDays,
                    onTrainingDaysChange = { trainingDays = it },
                    selectedRoutine = selectedRoutine,
                    onRoutineChange = { selectedRoutine = it },
                    equipments = equipments,
                    onAddEquipment = { eqName, cat, loc, uri, note -> viewModel.addEquipment(eqName, cat, loc, uri, note) },
                    onRemoveEquipment = { viewModel.removeEquipment(it) },
                    onNext = { viewModel.setStep(2) },
                )

                2 -> StepThreeMeasurements(
                    gender = gender,
                    onGenderChange = { gender = it },
                    unitSystem = unitSystem,
                    onUnitChange = { unitSystem = it },
                    height = height,
                    onHeightChange = { height = it },
                    heightError = heightError,
                    weight = weight,
                    onWeightChange = { weight = it },
                    weightError = weightError,
                    calories = calories,
                    onCaloriesChange = { calories = it },
                    caloriesError = caloriesError,
                    onAutoCalculateCalories = { calories = computeCalories().toString() },
                    isNextEnabled = isStepValid,
                    onNext = { viewModel.setStep(3) },
                )

                3 -> {
                    val currentInputs = PlanInputsSnapshot(
                        routineName = selectedRoutine,
                        goal = primaryGoal,
                        experienceLevel = experienceLevel,
                        trainingDays = trainingDays,
                        notes = personalNotes,
                        equipmentIds = equipments.map { it.id }.sorted(),
                    )
                    val isPlanStale = uiState.generatedPlan != null && uiState.generatedPlanInputs != null && uiState.generatedPlanInputs != currentInputs

                    StepFourReviewPlan(
                        name = name,
                        primaryGoal = primaryGoal,
                        experienceLevel = experienceLevel,
                        trainingDays = trainingDays,
                        selectedRoutine = selectedRoutine,
                        equipmentsCount = equipments.size,
                        personalNotes = personalNotes,
                        onNotesChange = { personalNotes = it },
                        uiState = uiState,
                        isPlanStale = isPlanStale,
                        onEditStep = { step -> viewModel.setStep(step) },
                        onGeneratePlan = {
                            viewModel.generateGeminiPlan(
                                routineName = selectedRoutine,
                                goal = primaryGoal,
                                experienceLevel = experienceLevel,
                                trainingDays = trainingDays,
                                userNotes = personalNotes,
                                onSuccess = {}
                            )
                        },
                        onFinish = {
                            val base = storedProfile ?: UserProfileEntity()
                            val parsedH = height.toDoubleOrNull()
                            val parsedW = weight.toDoubleOrNull()
                            val canonicalH = if (parsedH != null) {
                                if (unitSystem == UnitSystem.IMPERIAL) parsedH * 2.54 else parsedH
                            } else base.heightCm

                            val canonicalW = if (parsedW != null) {
                                if (unitSystem == UnitSystem.IMPERIAL) parsedW * 0.45359237 else parsedW
                            } else base.weightKg

                            val updated = base.copy(
                                displayName = name.ifBlank { "Athlete" },
                                gender = gender,
                                unitSystem = unitSystem,
                                themeMode = ThemeMode.LIGHT,
                                heightCm = canonicalH,
                                weightKg = canonicalW,
                                calorieGoal = calories.toIntOrNull() ?: base.calorieGoal,
                                primaryGoal = primaryGoal,
                                preferredSplit = selectedRoutine,
                            )
                            viewModel.completeOnboarding(
                                updatedProfile = updated,
                                experienceLevel = experienceLevel,
                                savePlan = uiState.generatedPlan != null && !isPlanStale,
                                onDone = onDone,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun StepOneWelcomeGoals(
    name: String,
    onNameChange: (String) -> Unit,
    primaryGoal: String,
    onGoalChange: (String) -> Unit,
    experienceLevel: String,
    onExperienceChange: (String) -> Unit,
    isGoogleLinked: Boolean,
    googleEmail: String?,
    onSignInGoogle: () -> Unit,
    onNext: () -> Unit,
) {
    val goals = listOf(
        "Lose weight" to "Fat loss & metabolic conditioning",
        "Build muscle" to "Hypertrophy & machine volume",
        "Gain strength" to "Heavy compound strength",
        "Tone & Endurance" to "Athletic stamina & higher reps",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        GoogleLogoIcon(Modifier.size(20.dp))
                        Text(
                            if (isGoogleLinked) "Connected as $googleEmail" else "Sign in with Google (Optional)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!isGoogleLinked) {
                        GoogleSignInButton(onClick = onSignInGoogle, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text("Welcome to Rep Forge", style = MaterialTheme.typography.headlineLarge)
                Text("Let's tailor your experience.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Your Name or Nickname") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Text("Primary Goal", style = MaterialTheme.typography.titleMedium)
        }

        items(goals) { (goalTitle, goalDesc) ->
            val isSelected = primaryGoal.equals(goalTitle, ignoreCase = true)
            OutlinedCard(
                onClick = { onGoalChange(goalTitle) },
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(goalTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(goalDesc, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text("Experience Level", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    listOf("Beginner", "Intermediate", "Advanced").forEach { exp ->
                        FilterChip(
                            selected = experienceLevel == exp,
                            onClick = { onExperienceChange(exp) },
                            label = { Text(exp) }
                        )
                    }
                }
            }
        }

        item {
            PrimaryButton(
                text = "Continue to Schedule & Equipment",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepTwoScheduleEquipment(
    trainingDays: Int,
    onTrainingDaysChange: (Int) -> Unit,
    selectedRoutine: String,
    onRoutineChange: (String) -> Unit,
    equipments: List<GymEquipmentEntity>,
    onAddEquipment: (name: String, category: String, location: String, photoUri: Uri?, notes: String) -> Unit,
    onRemoveEquipment: (String) -> Unit,
    onNext: () -> Unit,
) {
    var machineName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Gym") }
    var category by remember { mutableStateOf("Machine") }

    val routines = listOf(
        "Push Pull Legs (PPL)",
        "PPLUL (Push Pull Legs Upper Lower)",
        "Bro Split (Body Part Split)",
        "Upper / Lower Split",
        "Full Body Circuit",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text("Schedule & Routine", style = MaterialTheme.typography.headlineLarge)
                Text("Choose your preferred split and available days.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text("Training Days per Week", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    listOf(3, 4, 5, 6).forEach { days ->
                        FilterChip(
                            selected = trainingDays == days,
                            onClick = { onTrainingDaysChange(days) },
                            label = { Text("$days Days") }
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text("Routine Split", style = MaterialTheme.typography.labelMedium)
                routines.forEach { routine ->
                    val isSelected = selectedRoutine == routine
                    OutlinedCard(
                        onClick = { onRoutineChange(routine) },
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AppSpacing.xs)
                    ) {
                        Row(
                            modifier = Modifier.padding(AppSpacing.lg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onRoutineChange(routine) }
                            )
                            Spacer(Modifier.width(AppSpacing.sm))
                            Text(routine, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                    Text("Add Gym Equipment (Optional)", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = machineName,
                        onValueChange = { machineName = it },
                        label = { Text("Machine / Equipment Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    PrimaryButton(
                        text = "Add Equipment",
                        onClick = {
                            if (machineName.isNotBlank()) {
                                onAddEquipment(machineName, category, location, null, "")
                                machineName = ""
                            }
                        },
                        enabled = machineName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        items(equipments, key = { it.id }) { eq ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(AppSpacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(eq.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { onRemoveEquipment(eq.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        item {
            PrimaryButton(
                text = "Continue to Measurements (Optional)",
                onClick = onNext,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepThreeMeasurements(
    gender: Gender,
    onGenderChange: (Gender) -> Unit,
    unitSystem: UnitSystem,
    onUnitChange: (UnitSystem) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    heightError: String?,
    weight: String,
    onWeightChange: (String) -> Unit,
    weightError: String?,
    calories: String,
    onCaloriesChange: (String) -> Unit,
    caloriesError: String?,
    onAutoCalculateCalories: () -> Unit,
    isNextEnabled: Boolean,
    onNext: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text("Body Measurements", style = MaterialTheme.typography.headlineLarge)
                Text("All fields in this step are optional.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Unit System", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                            UnitSystem.entries.forEach { unit ->
                                FilterChip(
                                    selected = unitSystem == unit,
                                    onClick = { onUnitChange(unit) },
                                    label = { Text(if (unit == UnitSystem.METRIC) "Metric (kg/cm)" else "Imperial (lb/in)") }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = height,
                        onValueChange = onHeightChange,
                        label = { Text("Height (${if (unitSystem == UnitSystem.METRIC) "cm" else "in"}) - Optional") },
                        isError = heightError != null,
                        supportingText = heightError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = weight,
                        onValueChange = onWeightChange,
                        label = { Text("Weight (${if (unitSystem == UnitSystem.METRIC) "kg" else "lb"}) - Optional") },
                        isError = weightError != null,
                        supportingText = weightError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = calories,
                        onValueChange = onCaloriesChange,
                        label = { Text("Target Daily Calories (kcal) - Optional") },
                        isError = caloriesError != null,
                        supportingText = caloriesError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        item {
            PrimaryButton(
                text = "Continue to Plan Review",
                onClick = onNext,
                enabled = isNextEnabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StepFourReviewPlan(
    name: String,
    primaryGoal: String,
    experienceLevel: String,
    trainingDays: Int,
    selectedRoutine: String,
    equipmentsCount: Int,
    personalNotes: String,
    onNotesChange: (String) -> Unit,
    uiState: OnboardingUiState,
    isPlanStale: Boolean,
    onEditStep: (Int) -> Unit,
    onGeneratePlan: () -> Unit,
    onFinish: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.gutter),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                Text("Plan Review", style = MaterialTheme.typography.headlineLarge)
                Text("Review your choices before starting.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Goal & Experience", style = MaterialTheme.typography.titleMedium)
                        SecondaryButton(text = "Edit", onClick = { onEditStep(0) }, modifier = Modifier.height(32.dp))
                    }
                    Text("Athlete: $name • Goal: $primaryGoal • Level: $experienceLevel", style = MaterialTheme.typography.bodyMedium)

                    HorizontalDivider()

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Schedule & Equipment", style = MaterialTheme.typography.titleMedium)
                        SecondaryButton(text = "Edit", onClick = { onEditStep(1) }, modifier = Modifier.height(32.dp))
                    }
                    Text("Routine: $selectedRoutine • $trainingDays Days/Week • $equipmentsCount Machines", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        item {
            PrimaryButton(
                text = "Complete Setup & Start Training",
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
