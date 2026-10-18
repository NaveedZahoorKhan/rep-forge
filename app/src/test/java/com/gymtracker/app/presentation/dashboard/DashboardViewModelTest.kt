package com.gymtracker.app.presentation.dashboard

import com.gymtracker.app.data.local.entity.Difficulty
import com.gymtracker.app.data.local.entity.Equipment
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.data.local.entity.NutritionLogEntity
import com.gymtracker.app.data.local.entity.SessionStatus
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WaterLogEntity
import com.gymtracker.app.data.local.entity.WeekDay
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.model.DashboardStats
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.notification.WorkoutSoundPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private inline fun <reified T> createMockInterface(
        crossinline handler: (methodName: String, args: Array<Any>?) -> Any? = { _, _ -> null }
    ): T {
        return Proxy.newProxyInstance(
            T::class.java.classLoader,
            arrayOf(T::class.java),
        ) { _, method, args ->
            val custom = handler(method.name, args)
            if (custom != null) return@newProxyInstance custom

            val currentDayOfWeek = when (LocalDate.now().dayOfWeek) {
                DayOfWeek.MONDAY -> WeekDay.MONDAY
                DayOfWeek.TUESDAY -> WeekDay.TUESDAY
                DayOfWeek.WEDNESDAY -> WeekDay.WEDNESDAY
                DayOfWeek.THURSDAY -> WeekDay.THURSDAY
                DayOfWeek.FRIDAY -> WeekDay.FRIDAY
                DayOfWeek.SATURDAY -> WeekDay.SATURDAY
                DayOfWeek.SUNDAY -> WeekDay.SUNDAY
            }

            when (method.name) {
                "observeUserProfile" -> flowOf(UserProfileEntity(preferredSplit = "Push Pull Legs (PPL)"))
                "observeActiveSession" -> flowOf(null as WorkoutSessionEntity?)
                "observeWorkouts" -> flowOf(listOf(
                    WorkoutEntity(id = "w1", name = "Push Day A", splitType = "Push"),
                    WorkoutEntity(id = "w2", name = "Pull Day A", splitType = "Pull"),
                    WorkoutEntity(id = "w3", name = "Leg Day A", splitType = "Legs"),
                ))
                "observeWeeklySchedule" -> flowOf(listOf(
                    WeeklyScheduleEntity(id = "s1", weekDay = currentDayOfWeek, workoutId = "w1", workoutName = "Push Day A")
                ))
                "observeHistory" -> flowOf(emptyList<WorkoutSessionEntity>())
                "observeWeightLogs" -> flowOf(emptyList<WeightLogEntity>())
                "observeWater" -> flowOf(WaterLogEntity(dateEpochDay = LocalDate.now().toEpochDay(), milliliters = 1500, goalMl = 3000))
                "observeNutritionLog" -> flowOf(NutritionLogEntity(dateEpochDay = LocalDate.now().toEpochDay(), calories = 1800, calorieGoal = 2200))
                "observeReminders" -> flowOf(emptyList<Any>())
                "observeExercises" -> flowOf(listOf(
                    ExerciseEntity(id = "e1", name = "Bench Press", primaryMuscle = MuscleGroup.CHEST, equipment = Equipment.BARBELL, difficulty = Difficulty.INTERMEDIATE, instructions = "Press bar")
                ))
                "observeAllWorkoutExercises" -> flowOf(listOf(
                    WorkoutExerciseEntity(id = "we1", workoutId = "w1", exerciseId = "e1", orderIndex = 0),
                    WorkoutExerciseEntity(id = "we2", workoutId = "w1", exerciseId = "e1", orderIndex = 1),
                ))
                "dashboardStats" -> DashboardStats(weeklySessions = 3, weeklyVolume = 4500.0, streakDays = 4)
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

    @Test
    fun `viewModel state emits active session when active workout exists`() = runTest(testDispatcher) {
        val repo = createMockInterface<GymRepository> { methodName, _ ->
            if (methodName == "observeActiveSession") {
                flowOf(WorkoutSessionEntity(id = "sess1", workoutId = "w1", workoutName = "Push Day A", status = SessionStatus.ACTIVE))
            } else null
        }
        val soundPlayer = createMockInterface<WorkoutSoundPlayer>()
        val viewModel = DashboardViewModel(repo, soundPlayer)

        val job = launch { viewModel.state.collect {} }
        runCurrent()

        val state = viewModel.state.value
        assertNotNull(state.activeSession)
        assertEquals("sess1", state.activeSession?.id)
        assertEquals("Push Day A", state.activeSession?.workoutName)

        job.cancel()
    }

    @Test
    fun `viewModel state emits scheduled workout for today when no active session`() = runTest(testDispatcher) {
        val repo = createMockInterface<GymRepository>()
        val soundPlayer = createMockInterface<WorkoutSoundPlayer>()
        val viewModel = DashboardViewModel(repo, soundPlayer)

        val job = launch { viewModel.state.collect {} }
        runCurrent()

        val state = viewModel.state.value
        assertNull(state.activeSession)
        assertNotNull(state.todayWorkout)
        assertEquals("Push Day A", state.todayWorkout?.name)

        job.cancel()
    }

    @Test
    fun `viewModel state computes weeklyPlan with completed day detection`() = runTest(testDispatcher) {
        val todayEpoch = LocalDate.now().toEpochDay()
        val todayMillis = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + 1000

        val repo = createMockInterface<GymRepository> { methodName, _ ->
            if (methodName == "observeHistory") {
                flowOf(listOf(
                    WorkoutSessionEntity(
                        id = "sess_completed",
                        workoutId = "w1",
                        workoutName = "Push Day A",
                        startedAt = todayMillis,
                        status = SessionStatus.COMPLETED
                    )
                ))
            } else null
        }
        val soundPlayer = createMockInterface<WorkoutSoundPlayer>()
        val viewModel = DashboardViewModel(repo, soundPlayer)

        val job = launch { viewModel.state.collect {} }
        runCurrent()

        val state = viewModel.state.value
        val todayPlan = state.weeklyPlan.firstOrNull { it.isToday }
        assertNotNull(todayPlan)
        assertTrue("Today should be marked as completed", todayPlan!!.isCompleted)
        assertEquals("Push Day A", todayPlan.completedWorkoutName)

        job.cancel()
    }

    @Test
    fun `viewModel state provides today nutrition and water logs`() = runTest(testDispatcher) {
        val repo = createMockInterface<GymRepository>()
        val soundPlayer = createMockInterface<WorkoutSoundPlayer>()
        val viewModel = DashboardViewModel(repo, soundPlayer)

        val job = launch { viewModel.state.collect {} }
        runCurrent()

        val state = viewModel.state.value
        assertNotNull(state.todayNutrition)
        assertEquals(1800, state.todayNutrition?.calories)
        assertEquals(2200, state.todayNutrition?.calorieGoal)

        assertNotNull(state.todayWater)
        assertEquals(1500, state.todayWater?.milliliters)

        job.cancel()
    }

    @Test
    fun `exercise count and expected duration calculations handle exercises accurately`() = runTest(testDispatcher) {
        val repo = createMockInterface<GymRepository>()
        val soundPlayer = createMockInterface<WorkoutSoundPlayer>()
        val viewModel = DashboardViewModel(repo, soundPlayer)

        val job = launch { viewModel.state.collect {} }
        runCurrent()

        val state = viewModel.state.value
        assertEquals(2, state.getExerciseCount("w1"))
        assertEquals(16, state.getExpectedDurationMinutes("w1")) // 2 exercises * 8 min
        assertEquals(0, state.getExerciseCount("non_existent"))
        assertEquals(0, state.getExpectedDurationMinutes("non_existent"))

        job.cancel()
    }

    @Test
    fun `addWater calls repository upsertWater with updated volume`() = runTest(testDispatcher) {
        var upsertedWaterVolume = 0
        val repo = createMockInterface<GymRepository> { methodName, args ->
            if (methodName == "upsertWater") {
                val entity = args?.get(0) as? WaterLogEntity
                upsertedWaterVolume = entity?.milliliters ?: 0
                Unit
            } else null
        }
        val soundPlayer = createMockInterface<WorkoutSoundPlayer>()
        val viewModel = DashboardViewModel(repo, soundPlayer)

        val job = launch { viewModel.state.collect {} }
        runCurrent()

        viewModel.addWater(250)
        runCurrent()

        assertEquals(1750, upsertedWaterVolume) // Initial 1500 + 250
        job.cancel()
    }
}
