package com.gymtracker.app.presentation.gemini

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.app.data.local.entity.GeminiSyncReportEntity
import com.gymtracker.app.data.local.entity.NutritionLogEntity
import com.gymtracker.app.data.local.entity.UserHealthProfileEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WaterLogEntity
import com.gymtracker.app.data.remote.gemini.GeminiApiClient
import com.gymtracker.app.data.remote.gemini.GeminiProgressSyncResult
import com.gymtracker.app.data.remote.gemini.GeminiWorkoutBuilderResult
import com.gymtracker.app.data.remote.gemini.GeminiWorkoutOption
import com.gymtracker.app.domain.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class GeminiCoachUiState(
    val healthProfile: UserHealthProfileEntity = UserHealthProfileEntity(),
    val userProfile: UserProfileEntity? = null,
    val selectedTab: Int = 0, // 0 = Workout Builder, 1 = Progress Sync
    val machineBitmap: Bitmap? = null,
    val machineUri: Uri? = null,
    val userNotes: String = "",
    val isBuildingWorkout: Boolean = false,
    val builderResult: GeminiWorkoutBuilderResult? = null,
    val selectedOptionIndex: Int = 0,
    val isSyncing: Boolean = false,
    val latestSyncResult: GeminiProgressSyncResult? = null,
    val pastSyncReports: List<GeminiSyncReportEntity> = emptyList(),
    val equipments: List<com.gymtracker.app.data.local.entity.GymEquipmentEntity> = emptyList(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val isLiveApi: Boolean = false,
)

@HiltViewModel
class GeminiCoachViewModel @Inject constructor(
    private val repository: GymRepository,
    private val geminiApiClient: GeminiApiClient,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GeminiCoachUiState())
    val uiState: StateFlow<GeminiCoachUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(isLiveApi = geminiApiClient.isLiveApiConfigured()) }
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            repository.observeHealthProfile().collect { health ->
                if (health != null) {
                    _uiState.update { it.copy(healthProfile = health) }
                } else {
                    val defaultHealth = repository.getHealthProfile()
                    _uiState.update { it.copy(healthProfile = defaultHealth) }
                }
            }
        }

        viewModelScope.launch {
            repository.observeUserProfile().collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }

        viewModelScope.launch {
            repository.observeGeminiSyncReports().collect { reports ->
                _uiState.update { it.copy(pastSyncReports = reports) }
            }
        }

        viewModelScope.launch {
            repository.observeGymEquipments().collect { eqList ->
                _uiState.update { it.copy(equipments = eqList) }
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun updateHealthProfile(profile: UserHealthProfileEntity) {
        viewModelScope.launch {
            repository.updateHealthProfile(profile)
            _uiState.update { it.copy(healthProfile = profile) }
        }
    }

    fun updateNotes(notes: String) {
        _uiState.update { it.copy(userNotes = notes) }
    }

    fun setMachinePhoto(uri: Uri?) {
        if (uri == null) {
            _uiState.update { it.copy(machineBitmap = null, machineUri = null) }
            return
        }
        viewModelScope.launch {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                _uiState.update { it.copy(machineBitmap = bitmap, machineUri = uri) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Could not load image: ${e.message}") }
            }
        }
    }

    fun setCapturedBitmap(bitmap: Bitmap?) {
        _uiState.update { it.copy(machineBitmap = bitmap, machineUri = null) }
    }

    fun removePhoto() {
        _uiState.update { it.copy(machineBitmap = null, machineUri = null) }
    }

    fun generateCustomWorkout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBuildingWorkout = true, errorMessage = null, statusMessage = null) }
            try {
                val currentHealth = _uiState.value.healthProfile
                val currentProfile = _uiState.value.userProfile
                val bitmap = _uiState.value.machineBitmap
                val notes = _uiState.value.userNotes
                val equipmentNames = _uiState.value.equipments.joinToString(", ") { it.name }
                val effectiveNotes = if (equipmentNames.isNotBlank() && bitmap == null) {
                    if (notes.isBlank()) "Registered gym equipment: $equipmentNames" else "$notes (Registered gym equipment: $equipmentNames)"
                } else notes

                val result = geminiApiClient.generateCustomWorkout(
                    machineBitmap = bitmap,
                    userNotes = effectiveNotes,
                    healthProfile = currentHealth,
                    userProfile = currentProfile,
                )

                _uiState.update {
                    it.copy(
                        isBuildingWorkout = false,
                        builderResult = result,
                        selectedOptionIndex = 0,
                        statusMessage = if (geminiApiClient.isLiveApiConfigured()) {
                            "Custom workouts generated with Gemini 2.5 Flash!"
                        } else {
                            "Workouts generated with AI Coach! (Add Gemini API key in Secrets panel for live vision model)"
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBuildingWorkout = false,
                        errorMessage = "Error building workout: ${e.message ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun selectOption(index: Int) {
        _uiState.update { it.copy(selectedOptionIndex = index) }
    }

    fun saveWorkoutOption(option: GeminiWorkoutOption, onSuccess: (workoutId: String) -> Unit) {
        viewModelScope.launch {
            try {
                val workoutId = repository.saveGeminiWorkoutAsCustomWorkout(option)
                _uiState.update { it.copy(statusMessage = "Workout \"${option.title}\" saved to My Workouts!") }
                onSuccess(workoutId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to save workout: ${e.message}") }
            }
        }
    }

    fun startWorkoutFromOption(option: GeminiWorkoutOption, onStarted: (sessionId: String) -> Unit) {
        viewModelScope.launch {
            try {
                val workoutId = repository.saveGeminiWorkoutAsCustomWorkout(option)
                val sessionId = repository.startWorkout(workoutId)
                onStarted(sessionId)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed to start workout: ${e.message}") }
            }
        }
    }

    fun syncDailyProgressWithGemini() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null, statusMessage = null) }
            try {
                val todayEpoch = LocalDate.now().toEpochDay()
                val userProfile = repository.observeUserProfile().first()
                val healthProfile = repository.getHealthProfile()
                val recentSessions = repository.observeHistory().first()
                val recentRecords = repository.observePersonalRecords().first()
                val recentWeights = repository.observeWeightLogs().first()
                val todayNutrition = repository.observeNutritionLog(todayEpoch).first()
                val todayWater = repository.observeWater(todayEpoch).first()

                // Collect sets from recent sessions
                val allSets = mutableListOf<com.gymtracker.app.data.local.entity.PerformedSetEntity>()
                for (session in recentSessions.take(5)) {
                    val sets = repository.observeSessionSets(session.id).first()
                    allSets.addAll(sets)
                }

                val syncResult = geminiApiClient.syncDailyProgress(
                    userProfile = userProfile,
                    healthProfile = healthProfile,
                    recentSessions = recentSessions,
                    recentSets = allSets,
                    recentRecords = recentRecords,
                    recentWeights = recentWeights,
                    todayNutrition = todayNutrition,
                    todayWater = todayWater,
                )

                // Save report to Room database
                val reportEntity = GeminiSyncReportEntity(
                    id = UUID.randomUUID().toString(),
                    syncedAt = syncResult.syncTimestamp,
                    overallSummary = syncResult.overallSummary,
                    consistencyScore = syncResult.consistencyScore,
                    volumeAnalysis = syncResult.volumeAndLoadAnalysis,
                    healthReview = syncResult.healthAndRecoveryReview,
                    nutritionAudit = syncResult.nutritionAndHydrationAudit,
                    actionableSuggestionsJson = Json.encodeToString(syncResult.actionableSuggestions),
                    motivationalQuote = syncResult.motivationalQuote,
                    workloadSummary = syncResult.workloadSummary,
                )
                repository.saveGeminiSyncReport(reportEntity)

                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        latestSyncResult = syncResult,
                        statusMessage = "Daily progress successfully synced to Gemini Coach!",
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = "Failed to sync progress with Gemini: ${e.message ?: "Unknown error"}",
                    )
                }
            }
        }
    }

    fun clearStatus() {
        _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
    }
}
