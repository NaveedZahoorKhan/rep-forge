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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.gymtracker.app.domain.usecase.NutritionCalculator
import com.gymtracker.app.data.auth.GoogleAuthService
import com.gymtracker.app.presentation.components.GoogleAccountProfileCard
import com.gymtracker.app.presentation.components.GoogleLogoIcon
import com.gymtracker.app.presentation.components.GoogleSignInButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import coil.compose.AsyncImage
import com.gymtracker.app.data.local.entity.Gender
import com.gymtracker.app.data.local.entity.GymEquipmentEntity
import com.gymtracker.app.data.local.entity.ThemeMode
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.remote.gemini.GeminiApiClient
import com.gymtracker.app.data.remote.gemini.GeminiWorkoutBuilderResult
import com.gymtracker.app.domain.repository.GymRepository
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
    val currentStep: Int = 0, // 0 = Welcome, 1 = Goals & Experience, 2 = Schedule & Equipment, 3 = Optional Measurements, 4 = Plan Review
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
        _uiState.update { it.copy(currentStep = step.coerceIn(0, 4), errorMessage = null) }
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
            _uiState.update { it.copy(isGeneratingPlan = true, errorMessage = null, statusMessage = "Gemini is building your tailored plan...") }
            try {
                // First persist health profile with current goal & experience level
                val existingHealth = repository.getHealthProfile()
                val updatedHealth = existingHealth.copy(
                    primaryGoal = goal,
                    experienceLevel = experienceLevel,
                    updatedAt = System.currentTimeMillis(),
                )
                repository.updateHealthProfile(updatedHealth)

                val currentProfile = profile.value
                val currentEquipments = repository.getGymEquipments()

                // Load any bitmaps from saved equipment photos
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
                        statusMessage = "Plan generated by Gemini Coach!",
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
                    themeMode = ThemeMode.LIGHT, // App is in light mode
                    gender = updatedProfile.gender, // Default Male preserved or user choice
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
        val rawCm = storedProfile?.heightCm
        if (rawCm == null) {
            mutableStateOf("")
        } else {
            val displayVal = if (initialUnit == UnitSystem.IMPERIAL) rawCm / 2.54 else rawCm
            mutableStateOf(String.format(java.util.Locale.US, "%.1f", displayVal).removeSuffix(".0"))
        }
    }
    var weight by remember(storedProfile?.weightKg, storedProfile?.unitSystem) {
        val initialUnit = storedProfile?.unitSystem ?: UnitSystem.METRIC
        val rawKg = storedProfile?.weightKg
        if (rawKg == null) {
            mutableStateOf("")
        } else {
            val displayVal = if (initialUnit == UnitSystem.IMPERIAL) rawKg / 0.45359237 else rawKg
            mutableStateOf(String.format(java.util.Locale.US, "%.1f", displayVal).removeSuffix(".0"))
        }
    }
    var calories by remember(storedProfile?.calorieGoal) {
        mutableStateOf(storedProfile?.calorieGoal?.toString() ?: "")
    }

    val heightVal = height.toDoubleOrNull()
    val weightVal = weight.toDoubleOrNull()
    val caloriesVal = calories.toIntOrNull()

    val heightError = when {
        height.isBlank() -> null
        heightVal == null || heightVal <= 0 -> "Enter a valid positive number"
        unitSystem == UnitSystem.METRIC && (heightVal < 50.0 || heightVal > 280.0) -> "Must be between 50 and 280 cm"
        unitSystem == UnitSystem.IMPERIAL && (heightVal < 20.0 || heightVal > 110.0) -> "Must be between 20 and 110 inches"
        else -> null
    }

    val weightError = when {
        weight.isBlank() -> null
        weightVal == null || weightVal <= 0 -> "Enter a valid positive number"
        unitSystem == UnitSystem.METRIC && (weightVal < 20.0 || weightVal > 300.0) -> "Must be between 20 and 300 kg"
        unitSystem == UnitSystem.IMPERIAL && (weightVal < 45.0 || weightVal > 660.0) -> "Must be between 45 and 660 lbs"
        else -> null
    }

    val caloriesError = when {
        calories.isBlank() -> null
        caloriesVal == null || caloriesVal <= 0 -> "Enter a valid positive number"
        caloriesVal < 800 || caloriesVal > 10000 -> "Must be between 800 and 10,000 kcal"
        else -> null
    }

    val isStepMeasurementsValid = heightError == null && weightError == null && caloriesError == null

    val signInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.handleGoogleSignIn(result.data) { loadedName ->
            name = loadedName
        }
    }

    fun computeCalories(
        wStr: String = weight,
        hStr: String = height,
        g: Gender = gender,
        goal: String = primaryGoal,
        unit: UnitSystem = unitSystem,
    ): Int {
        val rawWeight = wStr.toDoubleOrNull() ?: 75.0
        val rawHeight = hStr.toDoubleOrNull() ?: 175.0
        val weightKg = if (unit == UnitSystem.IMPERIAL) rawWeight * 0.45359237 else rawWeight
        val heightCm = if (unit == UnitSystem.IMPERIAL) rawHeight * 2.54 else rawHeight
        return NutritionCalculator.autoCalculateTargetCalories(
            weightKg = weightKg,
            heightCm = heightCm,
            gender = g,
            goal = goal,
        )
    }

    fun handleUnitChange(newUnit: UnitSystem) {
        if (newUnit == unitSystem) return
        val currentH = height.toDoubleOrNull()
        val currentW = weight.toDoubleOrNull()

        if (newUnit == UnitSystem.IMPERIAL) {
            if (currentH != null) {
                val inches = currentH / 2.54
                height = String.format(java.util.Locale.US, "%.1f", inches).removeSuffix(".0")
            }
            if (currentW != null) {
                val lbs = currentW / 0.45359237
                weight = String.format(java.util.Locale.US, "%.1f", lbs).removeSuffix(".0")
            }
        } else {
            if (currentH != null) {
                val cm = currentH * 2.54
                height = String.format(java.util.Locale.US, "%.1f", cm).removeSuffix(".0")
            }
            if (currentW != null) {
                val kg = currentW * 0.45359237
                weight = String.format(java.util.Locale.US, "%.1f", kg).removeSuffix(".0")
            }
        }
        unitSystem = newUnit
    }

    var selectedRoutine by remember(storedProfile?.preferredSplit) {
        mutableStateOf(storedProfile?.preferredSplit ?: "Push Pull Legs (PPL)")
    }
    var personalNotes by remember { mutableStateOf("") }

    val steps = listOf("1. Welcome", "2. Goals & Experience", "3. Schedule & Equipment", "4. Optional Measurements", "5. Plan Review")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .navigationBarsPadding()
    ) {
        // Top Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            Text("GymTracker Setup", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                steps[uiState.currentStep.coerceIn(0, 4)],
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (uiState.currentStep > 0) {
                        OutlinedButton(
                            onClick = { viewModel.setStep(uiState.currentStep - 1) },
                            modifier = Modifier.height(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Back", fontSize = 13.sp)
                        }
                    }
                }

                // Step progress bar
                LinearProgressIndicator(
                    progress = { (uiState.currentStep + 1) / 5f },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }

        // Body Content per Step
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (uiState.currentStep) {
                0 -> StepWelcome(
                    isGoogleLinked = storedProfile?.googleLinked == true,
                    googleEmail = storedProfile?.googleEmail,
                    googleDisplayName = storedProfile?.googleDisplayName,
                    googlePhotoUrl = storedProfile?.googlePhotoUrl,
                    onSignInGoogle = { signInLauncher.launch(viewModel.getGoogleSignInIntent()) },
                    onNext = { viewModel.setStep(1) },
                )

                1 -> StepGoalsAndExperience(
                    primaryGoal = primaryGoal,
                    onGoalChange = { primaryGoal = it },
                    experienceLevel = experienceLevel,
                    onExperienceChange = { experienceLevel = it },
                    onNext = { viewModel.setStep(2) },
                )

                2 -> StepScheduleAndEquipment(
                    trainingDays = trainingDays,
                    onTrainingDaysChange = { trainingDays = it },
                    selectedRoutine = selectedRoutine,
                    onRoutineChange = { selectedRoutine = it },
                    equipments = equipments,
                    onAddEquipment = { eqName, cat, loc, uri, note ->
                        viewModel.addEquipment(eqName, cat, loc, uri, note)
                    },
                    onRemoveEquipment = { viewModel.removeEquipment(it) },
                    onNext = { viewModel.setStep(3) },
                )

                3 -> StepOptionalMeasurements(
                    name = name,
                    onNameChange = { name = it },
                    gender = gender,
                    onGenderChange = { gender = it },
                    unitSystem = unitSystem,
                    onUnitChange = { handleUnitChange(it) },
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
                    isValid = isStepMeasurementsValid,
                    onNext = { viewModel.setStep(4) },
                )

                4 -> {
                    val currentInputs = PlanInputsSnapshot(
                        routineName = selectedRoutine,
                        goal = primaryGoal,
                        experienceLevel = experienceLevel,
                        trainingDays = trainingDays,
                        notes = personalNotes,
                        equipmentIds = equipments.map { it.id }.sorted(),
                    )
                    val isPlanStale = uiState.generatedPlan != null && uiState.generatedPlanInputs != null && uiState.generatedPlanInputs != currentInputs

                    StepPlanReview(
                        primaryGoal = primaryGoal,
                        experienceLevel = experienceLevel,
                        trainingDays = trainingDays,
                        selectedRoutine = selectedRoutine,
                        equipments = equipments,
                        height = height,
                        weight = weight,
                        calories = calories,
                        unitSystem = unitSystem,
                        personalNotes = personalNotes,
                        onNotesChange = { personalNotes = it },
                        uiState = uiState,
                        isPlanStale = isPlanStale,
                        onEditGoal = { viewModel.setStep(1) },
                        onEditSchedule = { viewModel.setStep(2) },
                        onEditMeasurements = { viewModel.setStep(3) },
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

// -------------------------------------------------------------
// STEP 0: WELCOME & GOOGLE HEALTH CONNECT ACCOUNT LINK
// -------------------------------------------------------------
@Composable
private fun StepWelcome(
    isGoogleLinked: Boolean,
    googleEmail: String?,
    googleDisplayName: String?,
    googlePhotoUrl: String?,
    onSignInGoogle: () -> Unit,
    onNext: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Text(
                    "Welcome to GymTracker",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Your intelligent, flexible workout logger and AI training assistant. Build custom routines, track workouts, and monitor body composition.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isGoogleLinked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GoogleLogoIcon(Modifier.size(24.dp))
                        Column {
                            Text(
                                if (isGoogleLinked) "Connected as ${googleDisplayName ?: googleEmail}" else "Google Account & Health Sync",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Optional Integration",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Text(
                        text = if (isGoogleLinked) {
                            "Your workout logs, weight tracking, and calories will automatically sync to Google Health Connect."
                        } else {
                            "Connect Google Health Connect to seamlessly sync your workouts, strength gains, and body measurements across apps."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isGoogleLinked) {
                        GoogleSignInButton(
                            onClick = onSignInGoogle,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (!googleEmail.isNullOrBlank()) {
                        Text(
                            "Account email: $googleEmail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
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
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "What to expect in setup:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    val features = listOf(
                        "1. Goal & Experience Level" to "Calibrate training intensity and rep ranges",
                        "2. Schedule & Equipment" to "Pick training days and register your gym machines",
                        "3. Optional Measurements" to "Set body stats & target calories (or skip)",
                        "4. Plan Review" to "Review summary & generate your AI routine"
                    )

                    features.forEach { (stepTitle, stepDesc) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp).padding(top = 2.dp)
                            )
                            Column {
                                Text(stepTitle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(stepDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (isGoogleLinked) "Continue to Goals" else "Continue without account",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 1: GOALS & EXPERIENCE LEVEL
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepGoalsAndExperience(
    primaryGoal: String,
    onGoalChange: (String) -> Unit,
    experienceLevel: String,
    onExperienceChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    val goals = listOf(
        "Lose weight" to "Fat loss, high calorie expenditure & metabolic circuits",
        "Build muscle" to "Hypertrophy, machine volume & progressive tension",
        "Gain strength" to "Heavy compound strength & stable motor patterns",
        "Tone & Endurance" to "Athletic conditioning, high reps & stamina",
    )

    val experiences = listOf(
        "Beginner" to "0-1 years of regular lifting",
        "Intermediate" to "1-3 years with consistent technique",
        "Advanced" to "3+ years, advanced progressive overload"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Primary Fitness Goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Choose what matters most. Gemini and GymTracker will calibrate your training volume and rep ranges.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Goals List
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
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        Text(
                            goalTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            goalDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Experience Level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Helps estimate optimal set volume and recovery capacity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(experiences) { (expTitle, expDesc) ->
            val isSelected = experienceLevel.equals(expTitle, ignoreCase = true)
            OutlinedCard(
                onClick = { onExperienceChange(expTitle) },
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent)
                            .border(1.5.dp, if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            expTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            expDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue to Schedule & Equipment", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 2: SCHEDULE & EQUIPMENT
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepScheduleAndEquipment(
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
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var machineNotes by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) selectedPhotoUri = uri
    }

    val quickPresets = listOf(
        "Lat Pulldown", "Leg Press Machine", "Cable Crossover",
        "Chest Press Machine", "Smith Machine", "Seated Cable Row",
        "Leg Curl / Extension", "Pec Deck Fly", "Dumbbells & Bench",
        "Treadmill"
    )

    val routines = listOf(
        RoutineSuggestion(
            title = "Push Pull Legs (PPL)",
            tag = "Most Popular • 6 Days (or 3 Days)",
            description = "Separates pushing (chest, shoulders, triceps), pulling (back, biceps), and legs. Optimal balance of recovery and hypertrophy.",
            badge = "Balanced Growth",
            schedulePreview = "Mon: Push • Tue: Pull • Wed: Legs • Thu: Push • Fri: Pull • Sat: Legs • Sun: Rest",
        ),
        RoutineSuggestion(
            title = "PPLUL (Push Pull Legs Upper Lower)",
            tag = "Elite Split • 5 Days",
            description = "Combines 3-day PPL with 2-day Upper/Lower. Hits each muscle group twice a week with varied machine intensities.",
            badge = "Max Hypertrophy",
            schedulePreview = "Mon: Push • Tue: Pull • Wed: Legs • Thu: Rest • Fri: Upper • Sat: Lower • Sun: Rest",
        ),
        RoutineSuggestion(
            title = "Bro Split (Body Part Split)",
            tag = "Classic Bodybuilding • 5 Days",
            description = "Dedicated days for Chest, Back, Shoulders, Arms, and Legs. Complete muscle exhaustion per session with high volume.",
            badge = "Targeted Focus",
            schedulePreview = "Mon: Chest • Tue: Back • Wed: Shoulders • Thu: Legs • Fri: Arms • Sat/Sun: Rest",
        ),
        RoutineSuggestion(
            title = "Upper / Lower Split",
            tag = "Time Efficient • 4 Days",
            description = "Alternates upper and lower body workouts. Heavy compound progression and joint-friendly rest periods.",
            badge = "Strength & Recovery",
            schedulePreview = "Mon: Upper • Tue: Lower • Wed: Rest • Thu: Upper • Fri: Lower • Sat/Sun: Rest",
        ),
        RoutineSuggestion(
            title = "Full Body Circuit",
            tag = "High Frequency • 3 Days",
            description = "Full body workouts 3 times weekly. Maximizes metabolic burn and caloric expenditure, ideal for fat loss.",
            badge = "Fat Loss & Conditioning",
            schedulePreview = "Mon: Full Body • Tue: Rest • Wed: Full Body • Thu: Rest • Fri: Full Body • Sat/Sun: Rest",
        ),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Schedule & Equipment",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Set your weekly training availability, choose your routine split, and register machines available at your gym.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Days per week selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Days Available to Train per Week", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 4, 5, 6).forEach { days ->
                            FilterChip(
                                selected = trainingDays == days,
                                onClick = { onTrainingDaysChange(days) },
                                label = { Text("$days Days") },
                                leadingIcon = if (trainingDays == days) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }

        // Routine split choice group
        item {
            Text(
                "Routine Split Choice",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(routines) { item ->
            val isSelected = selectedRoutine.trim().equals(item.title.trim(), ignoreCase = true)
            OutlinedCard(
                onClick = { onRoutineChange(item.title) },
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onRoutineChange(item.title) },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Column {
                                Text(item.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(item.tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ) {
                            Text(
                                item.badge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "📅 Daily Schedule Preview:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                item.schedulePreview,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Add Gym Equipment Card (Optional)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Register Gym Equipment (Optional)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Add machines or equipment at your gym/home so Gemini can tailor every exercise to what you have.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Gym", "Home").forEach { loc ->
                            FilterChip(
                                selected = location == loc,
                                onClick = { location = loc },
                                label = { Text(if (loc == "Gym") "Commercial Gym" else "Home Gym") },
                                leadingIcon = {
                                    Icon(
                                        if (loc == "Gym") Icons.Default.FitnessCenter else Icons.Default.Home,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = machineName,
                        onValueChange = { machineName = it },
                        label = { Text("Machine Name (e.g. Lat Pulldown, Leg Press)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Quick Add Suggestions:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            quickPresets.forEach { preset ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable {
                                        machineName = preset
                                        category = when {
                                            preset.contains("Cable", ignoreCase = true) -> "Cable"
                                            preset.contains("Dumbbell", ignoreCase = true) -> "Free Weights"
                                            preset.contains("Treadmill", ignoreCase = true) -> "Cardio"
                                            else -> "Machine"
                                        }
                                    }
                                ) {
                                    Text(
                                        "+ $preset",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (selectedPhotoUri != null) "Change Photo" else "Upload Photo")
                        }

                        if (selectedPhotoUri != null) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            ) {
                                AsyncImage(
                                    model = selectedPhotoUri,
                                    contentDescription = "Selected machine",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { selectedPhotoUri = null },
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.TopEnd)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (machineName.isNotBlank()) {
                                onAddEquipment(machineName, category, location, selectedPhotoUri, machineNotes)
                                machineName = ""
                                selectedPhotoUri = null
                                machineNotes = ""
                            }
                        },
                        enabled = machineName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Add Machine to Equipment List")
                    }
                }
            }
        }

        if (equipments.isNotEmpty()) {
            item {
                Text("Registered Gym Equipment (${equipments.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }
            items(equipments, key = { it.id }) { eq ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
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
                                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.size(54.dp)
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
                            }
                        }

                        IconButton(onClick = { onRemoveEquipment(eq.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (equipments.isEmpty()) "Continue to Optional Measurements" else "Continue (${equipments.size} machines registered)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 3: OPTIONAL MEASUREMENTS & UNITS
// -------------------------------------------------------------
@Composable
private fun StepOptionalMeasurements(
    name: String,
    onNameChange: (String) -> Unit,
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
    isValid: Boolean,
    onNext: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Optional Measurements",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            "Optional",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    "Adding your body metrics enables baseline calorie recommendations. Fields can be left blank if you prefer to skip.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Athlete Profile & Body Stats", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Athlete Name / Nickname") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Gender Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Gender Selection", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Gender.entries.forEach { entry ->
                                val selected = gender == entry
                                FilterChip(
                                    selected = selected,
                                    onClick = { onGenderChange(entry) },
                                    label = { Text(entry.name.lowercase().replaceFirstChar { it.titlecase() }) },
                                    leadingIcon = if (selected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }

                    // Unit System Switcher
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Preferred Units", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnitSystem.entries.forEach { unit ->
                                FilterChip(
                                    selected = unitSystem == unit,
                                    onClick = { onUnitChange(unit) },
                                    label = { Text(if (unit == UnitSystem.METRIC) "Metric (kg, cm)" else "Imperial (lb, in)") }
                                )
                            }
                        }
                    }

                    // Height & Weight fields with explicit unit suffix indicators
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = height,
                            onValueChange = onHeightChange,
                            label = { Text("Height (Optional)") },
                            suffix = { Text(if (unitSystem == UnitSystem.METRIC) "cm" else "in") },
                            isError = heightError != null,
                            supportingText = heightError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = weight,
                            onValueChange = onWeightChange,
                            label = { Text("Weight (Optional)") },
                            suffix = { Text(if (unitSystem == UnitSystem.METRIC) "kg" else "lb") },
                            isError = weightError != null,
                            supportingText = weightError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Target Calories field with explicit unit suffix indicator
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Target Daily Calories (Optional)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(
                                onClick = onAutoCalculateCalories,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("⚡ Auto-calculate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = calories,
                            onValueChange = onCaloriesChange,
                            label = { Text("Daily Calories Goal (Optional)") },
                            suffix = { Text("kcal") },
                            isError = caloriesError != null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                if (caloriesError != null) {
                                    Text(caloriesError, color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("Calculated via Mifflin-St Jeor formula based on body stats and goal.")
                                }
                            }
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = onNext,
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    if (height.isBlank() && weight.isBlank() && calories.isBlank()) "Skip Measurements & Review Plan" else "Continue to Plan Review",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 4: PLAN REVIEW & AI ROUTINE GENERATOR
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepPlanReview(
    primaryGoal: String,
    experienceLevel: String,
    trainingDays: Int,
    selectedRoutine: String,
    equipments: List<GymEquipmentEntity>,
    height: String,
    weight: String,
    calories: String,
    unitSystem: UnitSystem,
    personalNotes: String,
    onNotesChange: (String) -> Unit,
    uiState: OnboardingUiState,
    isPlanStale: Boolean,
    onEditGoal: () -> Unit,
    onEditSchedule: () -> Unit,
    onEditMeasurements: () -> Unit,
    onGeneratePlan: () -> Unit,
    onFinish: () -> Unit,
) {
    val noteChips = listOf(
        "Low back friendly / no spinal load",
        "Short 45-min workouts",
        "Focus on chest & lats",
        "Gentle on knees",
        "High rep burnouts"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Plan Review & AI Routine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Review your setup choices. Gemini Coach will generate a custom workout split tailored specifically for you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Summary Card with Edit Links
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Your Program Summary", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    // Goal & Experience
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Goal & Experience", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Text("$primaryGoal • $experienceLevel", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = onEditGoal) {
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Schedule & Routine
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Schedule & Split", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            Text("$trainingDays days/week • $selectedRoutine", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = onEditSchedule) {
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Equipment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Registered Equipment", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            val eqText = if (equipments.isEmpty()) "Standard gym gear" else "${equipments.size} machines (${equipments.take(3).joinToString { it.name }})"
                            Text(eqText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = onEditSchedule) {
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Measurements
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Body Stats & Calories", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                            val statsText = buildString {
                                if (weight.isNotBlank()) append("$weight ${if (unitSystem == UnitSystem.METRIC) "kg" else "lb"}, ")
                                if (height.isNotBlank()) append("$height ${if (unitSystem == UnitSystem.METRIC) "cm" else "in"}, ")
                                if (calories.isNotBlank()) append("$calories kcal")
                            }.ifBlank { "Not specified (Optional)" }
                            Text(statsText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = onEditMeasurements) {
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Personal Notes Section for Gemini
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Personal Notes for Gemini (Optional)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }

                    Text(
                        "Tell Gemini any specific requests (e.g. feel low energy, substitute barbells, protect joint, focus on upper chest).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = personalNotes,
                        onValueChange = onNotesChange,
                        placeholder = { Text("e.g. Prefer machines over free weights, keep rest brisk, protect right shoulder") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        noteChips.forEach { chip ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    onNotesChange(if (personalNotes.isBlank()) chip else "$personalNotes, $chip")
                                }
                            ) {
                                Text(
                                    "+ $chip",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Gemini Plan Generation Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Gemini AI Workout Plan Generator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }

                    Text(
                        "Gemini will generate your complete ${selectedRoutine.substringBefore(" (")} routine, catering every exercise to your ${equipments.size} registered machines and $primaryGoal goal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (uiState.isGeneratingPlan) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Gemini is building your routine...",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Button(
                            onClick = onGeneratePlan,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (uiState.generatedPlan != null) "Regenerate Plan with Gemini" else "Let Gemini Generate Workout Plan",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (uiState.errorMessage != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                uiState.errorMessage,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedButton(onClick = onGeneratePlan, modifier = Modifier.height(32.dp)) {
                                Text("Retry", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Stale Plan Warning Banner
        if (isPlanStale) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(
                            "Plan inputs changed (routine, goal, machines, or notes). Please regenerate your plan above to apply changes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Preview of Generated Plan
        uiState.generatedPlan?.let { plan ->
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPlanStale) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isPlanStale) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (isPlanStale) Icons.Default.Close else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (isPlanStale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                if (isPlanStale) "Outdated Plan Preview (${plan.options.size} Workout Days)" else "Gemini Plan Ready: ${plan.options.size} Workout Days",
                                fontWeight = FontWeight.Bold,
                                color = if (isPlanStale) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        plan.healthSafetyCues.take(2).forEach { cue ->
                            Text("• $cue", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        plan.options.forEachIndexed { idx, option ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    "${option.title} (${option.exercises.size} exercises)",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                                option.exercises.forEach { ex ->
                                    Text(
                                        "  • ${ex.exerciseName} (${ex.sets} sets × ${ex.repsMin}-${ex.repsMax} reps) [${ex.equipment}]",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Finish Onboarding Button
        item {
            Button(
                onClick = onFinish,
                enabled = !uiState.isGeneratingPlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (uiState.generatedPlan != null) {
                        if (isPlanStale) "Save Default $selectedRoutine Plan & Start" else "Confirm Plan & Start Training"
                    } else "Start Training with $selectedRoutine",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class RoutineSuggestion(
    val title: String,
    val tag: String,
    val description: String,
    val badge: String,
    val schedulePreview: String,
)
