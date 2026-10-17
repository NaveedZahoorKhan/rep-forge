package com.gymtracker.app.presentation.profile

import android.content.Context
import com.gymtracker.app.data.auth.GoogleAuthService
import com.gymtracker.app.data.backup.CloudBackupService
import com.gymtracker.app.data.health.HealthSyncService
import com.gymtracker.app.data.local.entity.GymEquipmentEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.remote.gemini.GeminiApiClient
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val equipmentsFlow = MutableStateFlow<List<GymEquipmentEntity>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): Pair<ProfileViewModel, MutableList<GymEquipmentEntity>> {
        val savedEquipments = mutableListOf<GymEquipmentEntity>()

        val dummyRepo = Proxy.newProxyInstance(
            GymRepository::class.java.classLoader,
            arrayOf(GymRepository::class.java),
        ) { _, method, args ->
            when (method.name) {
                "observeUserProfile" -> flowOf(UserProfileEntity())
                "observeReminders" -> flowOf(emptyList<Any>())
                "observeGymEquipments" -> equipmentsFlow
                "addGymEquipment" -> {
                    val eq = args[0] as GymEquipmentEntity
                    savedEquipments.removeAll { it.id == eq.id }
                    savedEquipments.add(eq)
                    equipmentsFlow.value = savedEquipments.toList()
                    null
                }
                "deleteGymEquipment" -> {
                    val id = args[0] as String
                    savedEquipments.removeAll { it.id == id }
                    equipmentsFlow.value = savedEquipments.toList()
                    null
                }
                else -> null
            }
        } as GymRepository

        val dummyContext = Proxy.newProxyInstance(
            Context::class.java.classLoader,
            arrayOf(Context::class.java),
        ) { _, _, _ -> null } as Context

        val dummyCloudBackup = Proxy.newProxyInstance(
            CloudBackupService::class.java.classLoader,
            arrayOf(CloudBackupService::class.java),
        ) { _, _, _ -> null } as CloudBackupService

        val dummyGoogleAuth = Proxy.newProxyInstance(
            GoogleAuthService::class.java.classLoader,
            arrayOf(GoogleAuthService::class.java),
        ) { _, _, _ -> null } as GoogleAuthService

        val dummyHealthSync = Proxy.newProxyInstance(
            HealthSyncService::class.java.classLoader,
            arrayOf(HealthSyncService::class.java),
        ) { _, _, _ -> null } as HealthSyncService

        val dummyGeminiApi = Proxy.newProxyInstance(
            GeminiApiClient::class.java.classLoader,
            arrayOf(GeminiApiClient::class.java),
        ) { _, _, _ -> null } as GeminiApiClient

        val dummyNotificationHelper = Proxy.newProxyInstance(
            NotificationHelper::class.java.classLoader,
            arrayOf(NotificationHelper::class.java),
        ) { _, _, _ -> null } as NotificationHelper

        val viewModel = ProfileViewModel(
            repository = dummyRepo,
            cloudBackupService = dummyCloudBackup,
            googleAuthService = dummyGoogleAuth,
            healthSyncService = dummyHealthSync,
            geminiApiClient = dummyGeminiApi,
            notificationHelper = dummyNotificationHelper,
            context = dummyContext,
        )

        return viewModel to savedEquipments
    }

    @Test
    fun `addEquipment saves new equipment with category and notes`() = runTest(testDispatcher) {
        val (viewModel, savedEquipments) = createViewModel()
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        runCurrent()

        viewModel.addEquipment(
            name = "Leg Press Machine",
            category = "Machine",
            location = "Gym",
            photoUri = null,
            notes = "Pin setting 4 for quad focus"
        )
        runCurrent()

        assertEquals(1, savedEquipments.size)
        val added = savedEquipments.first()
        assertEquals("Leg Press Machine", added.name)
        assertEquals("Machine", added.category)
        assertEquals("Gym", added.location)
        assertEquals("Pin setting 4 for quad focus", added.notes)
        assertEquals("Machine saved to profile", viewModel.transient.status)

        collectJob.cancel()
    }

    @Test
    fun `updateEquipment modifies existing equipment attributes`() = runTest(testDispatcher) {
        val (viewModel, savedEquipments) = createViewModel()
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        runCurrent()

        // First add an equipment
        viewModel.addEquipment("Bench", "Free Weight", "Gym", null, "Initial note")
        runCurrent()
        val original = savedEquipments.first()

        // Update equipment
        viewModel.setEditingEquipment(original)
        assertEquals(original, viewModel.transient.editingEquipment)

        viewModel.updateEquipment(
            id = original.id,
            name = "Incline Bench Press",
            category = "Barbell",
            location = "Home",
            photoUri = null,
            notes = "Set angle to 30 degrees",
            existingPhotoUriStr = null
        )
        runCurrent()

        assertEquals(1, savedEquipments.size)
        val updated = savedEquipments.first()
        assertEquals(original.id, updated.id)
        assertEquals("Incline Bench Press", updated.name)
        assertEquals("Barbell", updated.category)
        assertEquals("Home", updated.location)
        assertEquals("Set angle to 30 degrees", updated.notes)
        assertNull(viewModel.transient.editingEquipment)
        assertEquals("Equipment updated", viewModel.transient.status)

        collectJob.cancel()
    }

    @Test
    fun `deleteEquipment removes item and allows recoverable deletion via undoDeleteEquipment`() = runTest(testDispatcher) {
        val (viewModel, savedEquipments) = createViewModel()
        val collectJob = launch(testDispatcher) { viewModel.state.collect {} }
        runCurrent()

        viewModel.addEquipment("Cable Crossover", "Cable", "Gym", null, "Heavy stack")
        runCurrent()
        val item = savedEquipments.first()

        // Delete equipment
        viewModel.deleteEquipment(item.id)
        runCurrent()

        assertEquals(0, savedEquipments.size)
        assertEquals(item, viewModel.transient.recentlyDeletedEquipment)
        assertEquals("Deleted Cable Crossover", viewModel.transient.status)

        // Undo deletion
        viewModel.undoDeleteEquipment()
        runCurrent()

        assertEquals(1, savedEquipments.size)
        assertEquals("Cable Crossover", savedEquipments.first().name)
        assertNull(viewModel.transient.recentlyDeletedEquipment)
        assertEquals("Restored Cable Crossover", viewModel.transient.status)

        collectJob.cancel()
    }
}
