package com.gymtracker.app.presentation.progress

import android.content.ContentResolver
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import com.gymtracker.app.data.auth.GoogleAuthService
import com.gymtracker.app.data.health.HealthSyncService
import com.gymtracker.app.data.local.dao.GymDao
import com.gymtracker.app.data.local.dao.ManualWorkoutDao
import com.gymtracker.app.data.local.entity.BodyMeasurementEntity
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.PersonalRecordEntity
import com.gymtracker.app.data.local.entity.ProgressPhotoEntity
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.model.ExerciseProgressPoint
import com.gymtracker.app.domain.repository.GymRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tempDir = File(System.getProperty("java.io.tmpdir"), "progress_test_${System.currentTimeMillis()}").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    private class TestContext(
        private val filesDirLocation: File,
        private val inputData: ByteArray = ByteArray(0)
    ) : ContextWrapper(null) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = filesDirLocation
        override fun getContentResolver(): ContentResolver {
            return Proxy.newProxyInstance(
                ContentResolver::class.java.classLoader,
                arrayOf(ContentResolver::class.java)
            ) { _, method, _ ->
                if (method.name == "openInputStream") {
                    ByteArrayInputStream(inputData)
                } else null
            } as? ContentResolver ?: super.getContentResolver()
        }
    }

    private inline fun <reified T> createMockInterface(
        crossinline handler: (methodName: String, args: Array<Any>?) -> Any? = { _, _ -> null }
    ): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java)
        ) { _, method, args ->
            val custom = handler(method.name, args)
            if (custom != null) return@newProxyInstance custom

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
        } as T
    }

    @Test
    fun `selectExercise loads progress points and updates state`() = runTest(testDispatcher) {
        val testPoints = listOf(
            ExerciseProgressPoint(timestamp = 1000L, weight = 100.0, volume = 500.0, estimatedOneRepMax = 115.0)
        )
        val historyFlow = MutableSharedFlow<List<WorkoutSessionEntity>>(replay = 1)
        historyFlow.emit(emptyList())

        val repo = createMockInterface<GymRepository> { methodName, args ->
            when (methodName) {
                "observeHistory" -> historyFlow
                "progressForExercise" -> {
                    val exId = args?.get(0) as? String
                    if (exId == "ex_bench") testPoints else emptyList<ExerciseProgressPoint>()
                }
                else -> null
            }
        }

        val context = TestContext(tempDir)
        val googleAuthService = GoogleAuthService(context)
        val healthSyncService = HealthSyncService(context, createMockInterface<GymDao>(), createMockInterface<ManualWorkoutDao>())
        val vm = ProgressViewModel(repo, googleAuthService, healthSyncService, context)

        runCurrent()
        vm.selectExercise("ex_bench")
        runCurrent()

        assertEquals("ex_bench", vm.selectedExerciseId)
        assertEquals(1, vm.progressPoints.size)
        assertEquals(100.0, vm.progressPoints[0].weight, 0.01)
    }

    @Test
    fun `history update auto-refreshes strength data for selected exercise`() = runTest(testDispatcher) {
        val historyFlow = MutableSharedFlow<List<WorkoutSessionEntity>>(replay = 1)
        historyFlow.emit(listOf(WorkoutSessionEntity(id = "s1", workoutId = "w1", workoutName = "Push A", startedAt = 1000L)))

        var queryCount = 0
        val repo = createMockInterface<GymRepository> { methodName, _ ->
            when (methodName) {
                "observeHistory" -> historyFlow
                "progressForExercise" -> {
                    queryCount++
                    listOf(ExerciseProgressPoint(timestamp = 1000L, weight = 100.0 * queryCount, volume = 500.0, estimatedOneRepMax = 115.0))
                }
                else -> null
            }
        }

        val context = TestContext(tempDir)
        val googleAuthService = GoogleAuthService(context)
        val healthSyncService = HealthSyncService(context, createMockInterface<GymDao>(), createMockInterface<ManualWorkoutDao>())
        val vm = ProgressViewModel(repo, googleAuthService, healthSyncService, context)

        runCurrent()
        vm.selectExercise("ex_squat")
        runCurrent()

        assertEquals(1, queryCount)
        assertEquals(100.0, vm.progressPoints[0].weight, 0.01)

        // New session completed -> emits to history
        historyFlow.emit(listOf(
            WorkoutSessionEntity(id = "s1", workoutId = "w1", workoutName = "Push A", startedAt = 1000L),
            WorkoutSessionEntity(id = "s2", workoutId = "w1", workoutName = "Push A", startedAt = 2000L)
        ))
        runCurrent()

        // Should automatically refresh progressPoints!
        assertEquals(2, queryCount)
        assertEquals(200.0, vm.progressPoints[0].weight, 0.01)
    }

    @Test
    fun `addWeightLog and deleteWeightLog invoke repository`() = runTest(testDispatcher) {
        var addedWeight: WeightLogEntity? = null
        var deletedWeightId: String? = null

        val repo = createMockInterface<GymRepository> { methodName, args ->
            when (methodName) {
                "addWeightLog" -> {
                    addedWeight = args?.get(0) as? WeightLogEntity
                    Unit
                }
                "deleteWeightLog" -> {
                    deletedWeightId = args?.get(0) as? String
                    Unit
                }
                else -> null
            }
        }

        val context = TestContext(tempDir)
        val googleAuthService = GoogleAuthService(context)
        val healthSyncService = HealthSyncService(context, createMockInterface<GymDao>(), createMockInterface<ManualWorkoutDao>())
        val vm = ProgressViewModel(repo, googleAuthService, healthSyncService, context)

        vm.addWeightLog(82.5)
        runCurrent()
        assertNotNull(addedWeight)
        assertEquals(82.5, addedWeight!!.weightKg, 0.01)

        vm.deleteWeightLog("weight_123")
        runCurrent()
        assertEquals("weight_123", deletedWeightId)
    }

    @Test
    fun `deletePhoto removes photo from repository and file system`() = runTest(testDispatcher) {
        var deletedPhotoId: String? = null

        val repo = createMockInterface<GymRepository> { methodName, args ->
            when (methodName) {
                "deleteProgressPhoto" -> {
                    deletedPhotoId = args?.get(0) as? String
                    Unit
                }
                else -> null
            }
        }

        val photoFile = File(tempDir, "test_pic.jpg").apply { writeText("dummy content") }
        assertTrue(photoFile.exists())

        val context = TestContext(tempDir)
        val googleAuthService = GoogleAuthService(context)
        val healthSyncService = HealthSyncService(context, createMockInterface<GymDao>(), createMockInterface<ManualWorkoutDao>())
        val vm = ProgressViewModel(repo, googleAuthService, healthSyncService, context)

        val photoEntity = ProgressPhotoEntity(
            id = "photo_abc",
            uri = photoFile.toURI().toString()
        )

        vm.deletePhoto(photoEntity)
        runCurrent()

        assertEquals("photo_abc", deletedPhotoId)
        org.junit.Assert.assertFalse(photoFile.exists())
    }
}
