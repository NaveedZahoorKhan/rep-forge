package com.gymtracker.app.presentation.gemini

import android.content.Context
import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class FakeGeminiApiClient(
    private val liveConfigured: Boolean,
    private val syncResultToReturn: GeminiProgressSyncResult?,
) : GeminiApiClient() {
    override fun isLiveApiConfigured(): Boolean = liveConfigured

    override suspend fun syncDailyProgress(
        userProfile: UserProfileEntity?,
        healthProfile: UserHealthProfileEntity,
        recentSessions: List<com.gymtracker.app.data.local.entity.WorkoutSessionEntity>,
        recentSets: List<com.gymtracker.app.data.local.entity.PerformedSetEntity>,
        recentRecords: List<com.gymtracker.app.data.local.entity.PersonalRecordEntity>,
        recentWeights: List<com.gymtracker.app.data.local.entity.WeightLogEntity>,
        todayNutrition: NutritionLogEntity?,
        todayWater: WaterLogEntity?,
    ): GeminiProgressSyncResult {
        return syncResultToReturn ?: super.syncDailyProgress(
            userProfile, healthProfile, recentSessions, recentSets, recentRecords, recentWeights, todayNutrition, todayWater
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GeminiCoachViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial ui state has empty results and not stale`() {
        val (viewModel, _) = createViewModel()

        val state = viewModel.uiState.value
        assertNull(state.builderResult)
        assertFalse(state.isResultStale)
        assertNull(state.lastGeneratedInputs)
    }

    @Test
    fun `generateCustomWorkout creates builderResult and captures lastGeneratedInputs`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()

        viewModel.updateNotes("Heavy leg day focus")
        runCurrent()

        viewModel.generateCustomWorkout()
        runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.builderResult)
        assertFalse(state.isResultStale)
        assertNotNull(state.lastGeneratedInputs)
        assertEquals("Heavy leg day focus", state.lastGeneratedInputs?.userNotes)
    }

    @Test
    fun `updating notes after workout generation marks result stale`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()

        viewModel.updateNotes("Initial note")
        runCurrent()

        viewModel.generateCustomWorkout()
        runCurrent()

        assertFalse(viewModel.uiState.value.isResultStale)

        // Modify notes
        viewModel.updateNotes("Initial note modified")
        runCurrent()

        assertTrue(viewModel.uiState.value.isResultStale)

        // Reverting notes back clears staleness
        viewModel.updateNotes("Initial note")
        runCurrent()

        assertFalse(viewModel.uiState.value.isResultStale)
    }

    @Test
    fun `updating health profile after workout generation marks result stale`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()

        val initialHealth = UserHealthProfileEntity(primaryGoal = "Hypertrophy", healthConditions = "None")
        viewModel.updateHealthProfile(initialHealth)
        runCurrent()

        viewModel.generateCustomWorkout()
        runCurrent()

        assertFalse(viewModel.uiState.value.isResultStale)

        val updatedHealth = initialHealth.copy(healthConditions = "Lower Back Sensitivity")
        viewModel.updateHealthProfile(updatedHealth)
        runCurrent()

        assertTrue(viewModel.uiState.value.isResultStale)
    }

    @Test
    fun `regenerating custom workout resets staleness to false with new inputs`() = runTest(testDispatcher) {
        val (viewModel, _) = createViewModel()

        viewModel.updateNotes("Note 1")
        runCurrent()
        viewModel.generateCustomWorkout()
        runCurrent()

        viewModel.updateNotes("Note 2")
        runCurrent()
        assertTrue(viewModel.uiState.value.isResultStale)

        viewModel.generateCustomWorkout()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isResultStale)
        assertEquals("Note 2", state.lastGeneratedInputs?.userNotes)
    }

    @Test
    fun `live API failure surfaces error message and sets fallback result`() = runTest(testDispatcher) {
        val fallbackResult = GeminiWorkoutBuilderResult(
            identifiedMachine = "Incline Leg Press",
            isFallback = true,
            fallbackReason = "Live API error (HTTP 500)",
            options = listOf(GeminiWorkoutOption(title = "Option A", splitType = "Legs")),
        )

        val (viewModel, _) = createViewModel(
            isLiveApiConfigured = true,
            mockResult = fallbackResult,
        )

        viewModel.generateCustomWorkout()
        runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.builderResult)
        assertTrue(state.builderResult?.isFallback == true)
        assertEquals("Live API error (HTTP 500)", state.builderResult?.fallbackReason)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage?.contains("HTTP 500") == true)
    }

    @Test
    fun `syncDailyProgressWithGemini successful live sync updates state with Gemini Coach source`() = runTest(testDispatcher) {
        val mockHealthProfile = UserHealthProfileEntity(id = "me", primaryGoal = "Muscle Hypertrophy")
        val mockUserProfile = UserProfileEntity(id = "user1", displayName = "Test Athlete")
        val mockPastReports = mutableListOf<GeminiSyncReportEntity>()

        val liveSyncResult = GeminiProgressSyncResult(
            syncTimestamp = 1000000L,
            overallSummary = "Excellent training progress from Gemini 2.5 Flash",
            consistencyScore = "High Cadence",
            isLocalFallback = false,
        )

        val dummyRepo = Proxy.newProxyInstance(
            GymRepository::class.java.classLoader,
            arrayOf(GymRepository::class.java),
        ) { _, method, args ->
            when (method.name) {
                "observeHealthProfile" -> flowOf(mockHealthProfile)
                "getHealthProfile" -> mockHealthProfile
                "observeUserProfile" -> flowOf(mockUserProfile)
                "observeGeminiSyncReports" -> flowOf(mockPastReports)
                "observeGymEquipments" -> flowOf(emptyList<Any>())
                "observeHistory" -> flowOf(emptyList<Any>())
                "observePersonalRecords" -> flowOf(emptyList<Any>())
                "observeWeightLogs" -> flowOf(emptyList<Any>())
                "observeNutritionLog" -> flowOf(null)
                "observeWater" -> flowOf(null)
                "saveGeminiSyncReport" -> {
                    val report = args[0] as GeminiSyncReportEntity
                    mockPastReports.add(report)
                    null
                }
                else -> null
            }
        } as GymRepository

        val fakeApiClient = FakeGeminiApiClient(
            liveConfigured = true,
            syncResultToReturn = liveSyncResult,
        )

        val dummyContext = object : android.content.ContextWrapper(null) {}

        val viewModel = GeminiCoachViewModel(dummyRepo, fakeApiClient, dummyContext)
        val collectJob = launch(testDispatcher) {
            viewModel.uiState.collect { }
        }
        runCurrent()

        // Trigger Sync
        viewModel.syncDailyProgressWithGemini()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isSyncing)
        assertNotNull(state.latestSyncResult)
        assertEquals("Excellent training progress from Gemini 2.5 Flash", state.latestSyncResult?.overallSummary)
        assertFalse(state.latestSyncResult!!.isLocalFallback)
        assertEquals("Daily progress successfully synced to Gemini Coach!", state.statusMessage)
        assertNull(state.errorMessage)

        collectJob.cancel()
    }

    @Test
    fun `syncDailyProgressWithGemini fallback calculation marks source as local fallback`() = runTest(testDispatcher) {
        val mockHealthProfile = UserHealthProfileEntity(id = "me")
        val mockUserProfile = UserProfileEntity(id = "user1")

        val fallbackSyncResult = GeminiProgressSyncResult(
            syncTimestamp = 2000000L,
            overallSummary = "Offline calculated summary",
            consistencyScore = "Local Calculation",
            isLocalFallback = true,
        )

        val dummyRepo = Proxy.newProxyInstance(
            GymRepository::class.java.classLoader,
            arrayOf(GymRepository::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "observeHealthProfile" -> flowOf(mockHealthProfile)
                "getHealthProfile" -> mockHealthProfile
                "observeUserProfile" -> flowOf(mockUserProfile)
                "observeGeminiSyncReports" -> flowOf(emptyList<GeminiSyncReportEntity>())
                "observeGymEquipments" -> flowOf(emptyList<Any>())
                "observeHistory" -> flowOf(emptyList<Any>())
                "observePersonalRecords" -> flowOf(emptyList<Any>())
                "observeWeightLogs" -> flowOf(emptyList<Any>())
                "observeNutritionLog" -> flowOf(null)
                "observeWater" -> flowOf(null)
                "saveGeminiSyncReport" -> null
                else -> null
            }
        } as GymRepository

        val fakeApiClient = FakeGeminiApiClient(
            liveConfigured = false,
            syncResultToReturn = fallbackSyncResult,
        )

        val dummyContext = object : android.content.ContextWrapper(null) {}

        val viewModel = GeminiCoachViewModel(dummyRepo, fakeApiClient, dummyContext)
        val collectJob = launch(testDispatcher) {
            viewModel.uiState.collect { }
        }
        runCurrent()

        viewModel.syncDailyProgressWithGemini()
        runCurrent()

        val state = viewModel.uiState.value
        assertNotNull(state.latestSyncResult)
        assertTrue(state.latestSyncResult!!.isLocalFallback)
        assertEquals("Daily progress report calculated locally (offline mode).", state.statusMessage)

        collectJob.cancel()
    }

    @Test
    fun `syncDailyProgressWithGemini error sets errorMessage and clears syncing state`() = runTest(testDispatcher) {
        val mockHealthProfile = UserHealthProfileEntity(id = "me")

        val dummyRepo = Proxy.newProxyInstance(
            GymRepository::class.java.classLoader,
            arrayOf(GymRepository::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "observeHealthProfile" -> flowOf(mockHealthProfile)
                "getHealthProfile" -> mockHealthProfile
                "observeUserProfile" -> flowOf(null)
                "observeGeminiSyncReports" -> flowOf(emptyList<GeminiSyncReportEntity>())
                "observeGymEquipments" -> flowOf(emptyList<Any>())
                "observeHistory" -> throw RuntimeException("Database error during sync fetch")
                else -> null
            }
        } as GymRepository

        val fakeApiClient = FakeGeminiApiClient(
            liveConfigured = false,
            syncResultToReturn = null,
        )

        val dummyContext = object : android.content.ContextWrapper(null) {}

        val viewModel = GeminiCoachViewModel(dummyRepo, fakeApiClient, dummyContext)
        val collectJob = launch(testDispatcher) {
            viewModel.uiState.collect { }
        }
        runCurrent()

        viewModel.syncDailyProgressWithGemini()
        runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isSyncing)
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("Failed to sync progress with Gemini"))

        collectJob.cancel()
    }

    private fun createViewModel(
        isLiveApiConfigured: Boolean = false,
        mockResult: GeminiWorkoutBuilderResult? = null,
    ): Pair<GeminiCoachViewModel, GymRepository> {
        val dummyRepo = Proxy.newProxyInstance(
            GymRepository::class.java.classLoader,
            arrayOf(GymRepository::class.java)
        ) { _, method, args ->
            when (method.name) {
                "observeHealthProfile" -> flowOf(UserHealthProfileEntity())
                "getHealthProfile" -> UserHealthProfileEntity()
                "observeUserProfile" -> flowOf(UserProfileEntity())
                "observeGeminiSyncReports" -> flowOf(emptyList<Any>())
                "observeGymEquipments" -> flowOf(emptyList<Any>())
                "updateHealthProfile" -> null
                else -> null
            }
        } as GymRepository

        val dummyContext = object : android.content.ContextWrapper(null) {}

        val dummyApiClient = object : GeminiApiClient() {
            override fun isLiveApiConfigured(): Boolean = isLiveApiConfigured

            override suspend fun generateCustomWorkout(
                machineBitmap: android.graphics.Bitmap?,
                userNotes: String,
                healthProfile: UserHealthProfileEntity,
                userProfile: UserProfileEntity?,
            ): GeminiWorkoutBuilderResult {
                return mockResult ?: GeminiWorkoutBuilderResult(
                    identifiedMachine = "Test Station",
                    options = listOf(GeminiWorkoutOption(title = "Option 1", splitType = "Push")),
                    isFallback = !isLiveApiConfigured,
                )
            }
        }

        val vm = GeminiCoachViewModel(dummyRepo, dummyApiClient, dummyContext)
        return Pair(vm, dummyRepo)
    }
}
