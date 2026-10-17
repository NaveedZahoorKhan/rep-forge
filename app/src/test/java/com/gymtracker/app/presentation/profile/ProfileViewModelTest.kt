package com.gymtracker.app.presentation.profile

import android.content.Context
import android.content.ContextWrapper
import com.gymtracker.app.data.auth.GoogleAuthService
import com.gymtracker.app.data.backup.CloudBackupService
import com.gymtracker.app.data.health.HealthSyncService
import com.gymtracker.app.data.local.dao.GymDao
import com.gymtracker.app.data.local.dao.ManualWorkoutDao
import com.gymtracker.app.data.local.entity.GymEquipmentEntity
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.remote.gemini.GeminiApiClient
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.notification.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeContext(private val clipboard: Any? = null) : ContextWrapper(null) {
        override fun getSystemService(name: String): Any? {
            return if (name == Context.CLIPBOARD_SERVICE) clipboard else null
        }
        override fun getApplicationContext(): Context = this
    }

    private fun createMockRepository(
        exportJsonResult: String = "{\"userProfile\":{}}",
        importJsonException: Exception? = null,
    ): GymRepository {
        return Proxy.newProxyInstance(
            GymRepository::class.java.classLoader,
            arrayOf(GymRepository::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "observeUserProfile" -> flowOf(UserProfileEntity(displayName = "Test Athlete"))
                "observeReminders" -> flowOf(emptyList<ReminderEntity>())
                "observeGymEquipments" -> flowOf(emptyList<GymEquipmentEntity>())
                "exportJson" -> exportJsonResult
                "importJson" -> {
                    if (importJsonException != null) throw importJsonException
                    Unit
                }
                "deleteAllData" -> Unit
                "cloudRestore" -> Result.success(Unit)
                "createOrUpdateProfile" -> Unit
                "upsertReminder" -> Unit
                "toString" -> "MockGymRepository"
                "hashCode" -> 1
                "equals" -> false
                else -> {
                    val returnType = method.returnType
                    when {
                        returnType == Flow::class.java -> flowOf(emptyList<Any>())
                        returnType == Result::class.java -> Result.success(Unit)
                        returnType == List::class.java -> emptyList<Any>()
                        returnType == String::class.java -> ""
                        returnType == Void.TYPE || returnType == Unit::class.java -> Unit
                        returnType == Boolean::class.javaPrimitiveType || returnType == Boolean::class.javaObjectType -> false
                        returnType == Int::class.javaPrimitiveType || returnType == Int::class.javaObjectType -> 0
                        returnType == Long::class.javaPrimitiveType || returnType == Long::class.javaObjectType -> 0L
                        returnType == Double::class.javaPrimitiveType || returnType == Double::class.javaObjectType -> 0.0
                        else -> null
                    }
                }
            }
        } as GymRepository
    }

    private inline fun <reified T> createMockInterface(): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "toString" -> "MockInterface"
                "hashCode" -> 1
                "equals" -> false
                else -> {
                    val returnType = method.returnType
                    when {
                        returnType == Flow::class.java -> flowOf(emptyList<Any>())
                        returnType == Result::class.java -> Result.success(Unit)
                        returnType == List::class.java -> emptyList<Any>()
                        returnType == String::class.java -> ""
                        returnType == Void.TYPE || returnType == Unit::class.java -> Unit
                        returnType == Boolean::class.javaPrimitiveType || returnType == Boolean::class.javaObjectType -> false
                        else -> null
                    }
                }
            }
        } as T
    }

    private fun createViewModel(
        repo: GymRepository,
        context: Context = FakeContext(),
    ): ProfileViewModel {
        val googleAuthService = GoogleAuthService(context)
        val cloudBackupService = CloudBackupService(context)
        val dummyGymDao = createMockInterface<GymDao>()
        val dummyManualDao = createMockInterface<ManualWorkoutDao>()
        val healthSyncService = HealthSyncService(context, dummyGymDao, dummyManualDao)
        val notificationHelper = createMockInterface<NotificationHelper>()
        return ProfileViewModel(
            repository = repo,
            cloudBackupService = cloudBackupService,
            googleAuthService = googleAuthService,
            healthSyncService = healthSyncService,
            geminiApiClient = GeminiApiClient(),
            notificationHelper = notificationHelper,
            context = context,
        )
    }

    @Test
    fun `copyDataToClipboard handles missing clipboard service gracefully`() = runTest(testDispatcher) {
        val context = FakeContext(clipboard = null)
        val repo = createMockRepository(exportJsonResult = "{\"test\": 123}")
        val viewModel = createViewModel(repo, context)

        viewModel.copyDataToClipboard(context)
        runCurrent()

        assertTrue(viewModel.transient.status.startsWith("❌ Failed to copy data:"))
    }

    @Test
    fun `importJson with blank text sets error status and does not call onSuccess`() = runTest(testDispatcher) {
        var onSuccessCalled = false
        val repo = createMockRepository()
        val viewModel = createViewModel(repo)

        viewModel.importJson("") {
            onSuccessCalled = true
        }
        runCurrent()

        assertFalse(onSuccessCalled)
        assertEquals("❌ Import failed: JSON backup text cannot be empty", viewModel.transient.status)
    }

    @Test
    fun `importJson with invalid payload sets error status and does not call onSuccess`() = runTest(testDispatcher) {
        var onSuccessCalled = false
        val repo = createMockRepository(importJsonException = IllegalArgumentException("Malformed JSON string"))
        val viewModel = createViewModel(repo)

        viewModel.importJson("corrupted json data") {
            onSuccessCalled = true
        }
        runCurrent()

        assertFalse(onSuccessCalled)
        assertTrue(viewModel.transient.status.startsWith("❌ Import failed:"))
        assertTrue(viewModel.transient.status.contains("Malformed JSON string"))
    }

    @Test
    fun `importJson with valid payload calls onSuccess and sets success status`() = runTest(testDispatcher) {
        var onSuccessCalled = false
        val repo = createMockRepository()
        val viewModel = createViewModel(repo)

        viewModel.importJson("{\"userProfile\":{}}") {
            onSuccessCalled = true
        }
        runCurrent()

        assertTrue(onSuccessCalled)
        assertEquals("✅ Backup imported successfully!", viewModel.transient.status)
    }

    @Test
    fun `parseAndSyncRawGeminiJson with invalid json preserves text and reports error`() = runTest(testDispatcher) {
        var onSuccessCalled = false
        val repo = createMockRepository()
        val viewModel = createViewModel(repo)

        viewModel.parseAndSyncRawGeminiJson("invalid gemini response text") {
            onSuccessCalled = true
        }
        runCurrent()

        assertFalse(onSuccessCalled)
        assertTrue(viewModel.transient.status.startsWith("❌ Sync failed:"))
    }

    @Test
    fun `parseAndSyncRawGeminiJson with valid payload syncs to app and calls onSuccess`() = runTest(testDispatcher) {
        var onSuccessCalled = false
        val repo = createMockRepository()
        val viewModel = createViewModel(repo)

        val validJson = """
            {
                "summary": "Solid progress",
                "recommendedCalorieGoal": 2500,
                "recommendedWaterGoalMl": 3000,
                "recommendedPrimaryGoal": "Build muscle",
                "recommendedSplit": "Push Pull Legs (PPL)",
                "suggestedReminders": []
            }
        """.trimIndent()

        viewModel.parseAndSyncRawGeminiJson(validJson) {
            onSuccessCalled = true
        }
        runCurrent()

        assertTrue(onSuccessCalled)
        assertEquals("✅ Successfully synced Gemini recommendations to app!", viewModel.transient.status)
    }

    @Test
    fun `deleteAllData updates status message`() = runTest(testDispatcher) {
        val repo = createMockRepository()
        val viewModel = createViewModel(repo)

        viewModel.deleteAllData()
        runCurrent()

        assertEquals("All local data deleted and defaults restored", viewModel.transient.status)
    }

    @Test
    fun `cloudRestore updates status message`() = runTest(testDispatcher) {
        val repo = createMockRepository()
        val viewModel = createViewModel(repo)

        viewModel.cloudRestore()
        runCurrent()

        assertEquals("Cloud restore complete", viewModel.transient.status)
    }
}
