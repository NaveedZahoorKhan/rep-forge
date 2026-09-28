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
import com.gymtracker.app.presentation.components.GoogleSignInOptionsDialog
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

data class OnboardingUiState(
    val currentStep: Int = 0, // 0 = Preferences/Goal, 1 = Gym Machines & Equipment, 2 = Routine & Gemini Plan
    val isGeneratingPlan: Boolean = false,
    val generatedPlan: GeminiWorkoutBuilderResult? = null,
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
                    repository.updateHealthConnectStatus(linked = true)
                    onUserLoaded(user.displayName)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Google sign-in failed") }
                }
            )
        }
    }

    fun quickGoogleSignIn(email: String, name: String, onUserLoaded: (name: String) -> Unit) {
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
                    repository.updateHealthConnectStatus(linked = true)
                    onUserLoaded(user.displayName)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Sign-in failed") }
                }
            )
        }
    }

    fun setStep(step: Int) {
        _uiState.update { it.copy(currentStep = step.coerceIn(0, 2), errorMessage = null) }
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
        userNotes: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingPlan = true, errorMessage = null, statusMessage = "Gemini is building your tailored plan...") }
            try {
                val currentHealth = repository.getHealthProfile()
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

                val result = geminiApiClient.generateRoutinePlan(
                    routineName = routineName,
                    userGoal = goal,
                    machines = currentEquipments,
                    userNotes = userNotes,
                    healthProfile = currentHealth,
                    userProfile = currentProfile,
                    machineBitmaps = bitmaps,
                )

                _uiState.update {
                    it.copy(
                        isGeneratingPlan = false,
                        generatedPlan = result,
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
        savePlan: Boolean = true,
        onDone: () -> Unit,
    ) {
        viewModelScope.launch {
            val plan = _uiState.value.generatedPlan
            if (savePlan && plan != null && plan.options.isNotEmpty()) {
                repository.saveGeminiRoutinePlan(plan, setAsSchedule = true)
            }
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

    var showGoogleDialog by remember { mutableStateOf(false) }

    // Step 1 Profile & Preferences state
    // Default gender set explicitly to MALE as requested
    var name by remember(storedProfile?.displayName) { mutableStateOf(storedProfile?.displayName ?: "Athlete") }
    var gender by remember(storedProfile?.gender) { mutableStateOf(storedProfile?.gender ?: Gender.MALE) }
    var primaryGoal by remember(storedProfile?.primaryGoal) { mutableStateOf(storedProfile?.primaryGoal ?: "Lose weight") }
    var experienceLevel by remember { mutableStateOf("Intermediate") }
    var trainingDays by remember { mutableIntStateOf(4) }
    var unitSystem by remember(storedProfile?.unitSystem) { mutableStateOf(storedProfile?.unitSystem ?: UnitSystem.METRIC) }
    var height by remember(storedProfile?.heightCm) { mutableStateOf((storedProfile?.heightCm ?: 175.0).toString()) }
    var weight by remember(storedProfile?.weightKg) { mutableStateOf((storedProfile?.weightKg ?: 75.0).toString()) }
    var calories by remember(storedProfile?.calorieGoal) { mutableStateOf((storedProfile?.calorieGoal ?: 2100).toString()) }

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

    // Auto-compute baseline calories on initial appearance if default
    LaunchedEffect(Unit) {
        if (storedProfile == null || storedProfile?.calorieGoal == 2200) {
            calories = computeCalories().toString()
        }
    }

    // Step 2 & 3 state
    var selectedRoutine by remember(storedProfile?.preferredSplit) {
        mutableStateOf(storedProfile?.preferredSplit ?: "Push Pull Legs (PPL)")
    }
    var personalNotes by remember { mutableStateOf("") }

    val steps = listOf("1. Goals & Body", "2. Gym Machines", "3. Gemini Routine")

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
                                steps[uiState.currentStep],
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
                    progress = { (uiState.currentStep + 1) / 3f },
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
                0 -> StepOnePreferences(
                    name = name,
                    onNameChange = { name = it },
                    gender = gender,
                    onGenderChange = {
                        gender = it
                        calories = computeCalories(g = it).toString()
                    },
                    primaryGoal = primaryGoal,
                    onGoalChange = {
                        primaryGoal = it
                        calories = computeCalories(goal = it).toString()
                    },
                    experienceLevel = experienceLevel,
                    onExperienceChange = { experienceLevel = it },
                    trainingDays = trainingDays,
                    onTrainingDaysChange = { trainingDays = it },
                    unitSystem = unitSystem,
                    onUnitChange = {
                        unitSystem = it
                        calories = computeCalories(unit = it).toString()
                    },
                    height = height,
                    onHeightChange = {
                        height = it
                        if (it.toDoubleOrNull() != null) {
                            calories = computeCalories(hStr = it).toString()
                        }
                    },
                    weight = weight,
                    onWeightChange = {
                        weight = it
                        if (it.toDoubleOrNull() != null) {
                            calories = computeCalories(wStr = it).toString()
                        }
                    },
                    calories = calories,
                    onCaloriesChange = { calories = it },
                    onAutoCalculateCalories = { calories = computeCalories().toString() },
                    isGoogleLinked = storedProfile?.googleLinked == true,
                    googleEmail = storedProfile?.googleEmail,
                    onSignInGoogle = { showGoogleDialog = true },
                    onQuickConnectGoogle = { showGoogleDialog = true },
                    onNext = { viewModel.setStep(1) },
                )

                1 -> StepTwoEquipments(
                    equipments = equipments,
                    onAddEquipment = { eqName, cat, loc, uri, note ->
                        viewModel.addEquipment(eqName, cat, loc, uri, note)
                    },
                    onRemoveEquipment = { viewModel.removeEquipment(it) },
                    onNext = { viewModel.setStep(2) },
                )

                2 -> StepThreeRoutines(
                    selectedRoutine = selectedRoutine,
                    onRoutineChange = { selectedRoutine = it },
                    personalNotes = personalNotes,
                    onNotesChange = { personalNotes = it },
                    equipmentsCount = equipments.size,
                    primaryGoal = primaryGoal,
                    uiState = uiState,
                    onGeneratePlan = {
                        viewModel.generateGeminiPlan(
                            routineName = selectedRoutine,
                            goal = primaryGoal,
                            userNotes = personalNotes,
                            onSuccess = {}
                        )
                    },
                    onFinish = {
                        val base = storedProfile ?: UserProfileEntity()
                        val updated = base.copy(
                            displayName = name.ifBlank { "Athlete" },
                            gender = gender, // Guaranteed default male
                            unitSystem = unitSystem,
                            themeMode = ThemeMode.LIGHT, // App is in light mode
                            heightCm = height.toDoubleOrNull() ?: base.heightCm,
                            weightKg = weight.toDoubleOrNull() ?: base.weightKg,
                            calorieGoal = calories.toIntOrNull() ?: base.calorieGoal,
                            primaryGoal = primaryGoal,
                            preferredSplit = selectedRoutine,
                        )
                        viewModel.completeOnboarding(
                            updatedProfile = updated,
                            savePlan = uiState.generatedPlan != null,
                            onDone = onDone,
                        )
                    },
                )
            }
        }
    }

    if (showGoogleDialog) {
        GoogleSignInOptionsDialog(
            onDismiss = { showGoogleDialog = false },
            onLaunchPlayServices = {
                showGoogleDialog = false
                signInLauncher.launch(viewModel.getGoogleSignInIntent())
            },
            onQuickSignIn = { email, displayName ->
                showGoogleDialog = false
                viewModel.quickGoogleSignIn(email, displayName) { loadedName ->
                    name = loadedName
                }
            }
        )
    }
}

// -------------------------------------------------------------
// STEP 1: USER PREFERENCES, GOALS, GENDER (MALE DEFAULT), BODY STATS
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepOnePreferences(
    name: String,
    onNameChange: (String) -> Unit,
    gender: Gender,
    onGenderChange: (Gender) -> Unit,
    primaryGoal: String,
    onGoalChange: (String) -> Unit,
    experienceLevel: String,
    onExperienceChange: (String) -> Unit,
    trainingDays: Int,
    onTrainingDaysChange: (Int) -> Unit,
    unitSystem: UnitSystem,
    onUnitChange: (UnitSystem) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    weight: String,
    onWeightChange: (String) -> Unit,
    calories: String,
    onCaloriesChange: (String) -> Unit,
    onAutoCalculateCalories: () -> Unit,
    isGoogleLinked: Boolean = false,
    googleEmail: String? = null,
    onSignInGoogle: () -> Unit = {},
    onQuickConnectGoogle: () -> Unit = {},
    onNext: () -> Unit,
) {
    val goals = listOf(
        "Lose weight" to "Fat loss, high calorie expenditure & metabolic circuits",
        "Build muscle" to "Hypertrophy, machine volume & progressive tension",
        "Gain strength" to "Heavy compound strength & stable motor patterns",
        "Tone & Endurance" to "Athletic conditioning, high reps & stamina",
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Google Sign-In & Health Link Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isGoogleLinked) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GoogleLogoIcon(Modifier.size(20.dp))
                        Text(
                            if (isGoogleLinked) "Connected as $googleEmail" else "Sign in with Google",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (isGoogleLinked) {
                            "Your account is connected to Google Health Connect. Your workouts and weight logs will automatically link to Health."
                        } else {
                            "Link your workout progress, strength gains, and weight logs to Google Health Connect from day 1."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!isGoogleLinked) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            GoogleSignInButton(onClick = onSignInGoogle, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = onQuickConnectGoogle) { Text("Quick Connect") }
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "What's your primary fitness goal?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Gemini will calibrate rep ranges, rest intervals, and volume to match your goal.",
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
                        Text(goalTitle, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                        Text(goalDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Athlete Profile & Body Stats", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Your Name or Nickname") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Gender Selector (Default Male as requested)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Gender (Default: Male)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                    // Experience Level
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Experience Level", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Beginner", "Intermediate", "Advanced").forEach { exp ->
                                FilterChip(
                                    selected = experienceLevel == exp,
                                    onClick = { onExperienceChange(exp) },
                                    label = { Text(exp) }
                                )
                            }
                        }
                    }

                    // Training Days Available
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Days Available to Train per Week: $trainingDays days", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(3, 4, 5, 6).forEach { days ->
                                FilterChip(
                                    selected = trainingDays == days,
                                    onClick = { onTrainingDaysChange(days) },
                                    label = { Text("$days Days") }
                                )
                            }
                        }
                    }

                    // Units
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Units System", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            UnitSystem.entries.forEach { unit ->
                                FilterChip(
                                    selected = unitSystem == unit,
                                    onClick = { onUnitChange(unit) },
                                    label = { Text(if (unit == UnitSystem.METRIC) "Metric (kg/cm)" else "Imperial (lb/in)") }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = height,
                            onValueChange = onHeightChange,
                            label = { Text("Height ${if (unitSystem == UnitSystem.METRIC) "cm" else "in"}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = weight,
                            onValueChange = onWeightChange,
                            label = { Text("Weight ${if (unitSystem == UnitSystem.METRIC) "kg" else "lb"}") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Target Daily Calories", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                            label = { Text("Daily Calories (kcal)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            supportingText = {
                                Text("Auto-calculated via Mifflin-St Jeor formula based on $weight ${if (unitSystem == UnitSystem.METRIC) "kg" else "lb"}, $height ${if (unitSystem == UnitSystem.METRIC) "cm" else "in"}, and $primaryGoal")
                            }
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
                Text("Continue to Gym Equipment", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 2: REGISTER MACHINES AT HOME OR GYM & UPLOAD PHOTOS
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepTwoEquipments(
    equipments: List<GymEquipmentEntity>,
    onAddEquipment: (name: String, category: String, location: String, photoUri: Uri?, notes: String) -> Unit,
    onRemoveEquipment: (String) -> Unit,
    onNext: () -> Unit,
) {
    var machineName by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("Gym") } // "Gym" or "Home"
    var category by remember { mutableStateOf("Machine") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var machineNotes by remember { mutableStateOf("") }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
        }
    }

    val quickPresets = listOf(
        "Lat Pulldown", "Leg Press Machine", "Cable Crossover",
        "Chest Press Machine", "Smith Machine", "Seated Cable Row",
        "Leg Curl / Extension", "Pec Deck Fly", "Dumbbells & Bench",
        "Treadmill"
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
                    "Tell us what machines you have",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Tell Gemini what machines you have at your gym or home (or upload machine photos). Gemini will cater and personalize your workout routine specifically to these machines, and save them to your workout profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Add Equipment Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add Machine or Equipment", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)

                    // Location Toggle
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

                    // Machine Name
                    OutlinedTextField(
                        value = machineName,
                        onValueChange = { machineName = it },
                        label = { Text("Machine Name (e.g. Lat Pulldown, Leg Press)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Quick Tap Suggestions
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

                    // Photo Upload Row
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
                            Text(if (selectedPhotoUri != null) "Change Photo" else "Upload Machine Photo")
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

                    // Add Button
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
                        Text("Save Machine to Workout Profile")
                    }
                }
            }
        }

        // Saved Equipments List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Your Registered Gym Equipments (${equipments.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                if (equipments.isEmpty()) {
                    Text("None added yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Text("No machines added yet", fontWeight = FontWeight.Bold)
                        Text(
                            "Add at least one machine above (or continue with standard gym gear), and Gemini will cater every exercise to what you have.",
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
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(8.dp)),
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

                    IconButton(onClick = { onRemoveEquipment(eq.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
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
                    if (equipments.isEmpty()) "Continue to Workout Routines" else "Next: Suggest Routines (${equipments.size} machines)",
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
// STEP 3: SUGGEST ROUTINES & LET GEMINI GENERATE WORKOUT PLAN
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepThreeRoutines(
    selectedRoutine: String,
    onRoutineChange: (String) -> Unit,
    personalNotes: String,
    onNotesChange: (String) -> Unit,
    equipmentsCount: Int,
    primaryGoal: String,
    uiState: OnboardingUiState,
    onGeneratePlan: () -> Unit,
    onFinish: () -> Unit,
) {
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Choose your routine split",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            "Single select",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    "Select exactly one routine split for your program. Your weekly workout days, rest days, and dashboard recommendations will follow this routine.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Suggested Routines (Strict Single Select)
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

                    // Daily Schedule Preview Strip
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "📅 Schedule by Day:",
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
                        Text("Personal Notes for Gemini", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
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

        // Gemini Plan Generation Button & Preview
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
                        "Gemini will generate your complete ${selectedRoutine.substringBefore(" (")} routine, catering every exercise to your $equipmentsCount registered machines and $primaryGoal goal.",
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
                        Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Preview of Generated Plan
        uiState.generatedPlan?.let { plan ->
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Gemini Plan Ready: ${plan.options.size} Workout Days", fontWeight = FontWeight.Bold)
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
                    if (uiState.generatedPlan != null) "Confirm Plan & Start Training" else "Start Training with $selectedRoutine",
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
