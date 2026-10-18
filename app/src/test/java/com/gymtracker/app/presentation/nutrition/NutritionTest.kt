package com.gymtracker.app.presentation.nutrition

import com.gymtracker.app.data.local.entity.BmrFormula
import com.gymtracker.app.data.local.entity.Gender
import com.gymtracker.app.data.local.entity.MealEntity
import com.gymtracker.app.data.local.entity.NutritionLogEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WaterLogEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.NutritionCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeGymRepository : GymRepository {
        val mealsStore = mutableListOf<MealEntity>()
        val nutritionLogsStore = mutableMapOf<Long, NutritionLogEntity>()
        val waterLogsStore = mutableMapOf<Long, WaterLogEntity>()
        val userProfileFlow = MutableStateFlow(UserProfileEntity(calorieGoal = 2000, waterGoalMl = 3000))

        private val mealsFlow = MutableStateFlow<List<MealEntity>>(emptyList())
        private val nutritionLogFlow = MutableStateFlow<NutritionLogEntity?>(null)
        private val waterLogFlow = MutableStateFlow<WaterLogEntity?>(null)

        fun notifyFlows(dateEpochDay: Long) {
            mealsFlow.value = mealsStore.filter { it.dateEpochDay == dateEpochDay }
            nutritionLogFlow.value = nutritionLogsStore[dateEpochDay]
            waterLogFlow.value = waterLogsStore[dateEpochDay]
        }

        override fun observeUserProfile(): Flow<UserProfileEntity?> = userProfileFlow.asStateFlow()

        override fun observeNutritionLog(dateEpochDay: Long): Flow<NutritionLogEntity?> {
            notifyFlows(dateEpochDay)
            return nutritionLogFlow.asStateFlow()
        }

        override fun observeMeals(dateEpochDay: Long): Flow<List<MealEntity>> {
            notifyFlows(dateEpochDay)
            return mealsFlow.asStateFlow()
        }

        override fun observeWater(dateEpochDay: Long): Flow<WaterLogEntity?> {
            notifyFlows(dateEpochDay)
            return waterLogFlow.asStateFlow()
        }

        override fun observeWeightLogs(): Flow<List<WeightLogEntity>> = flowOf(emptyList())

        override suspend fun addMeal(meal: MealEntity) {
            mealsStore.add(meal)
            recomputeNutrition(meal.dateEpochDay)
            notifyFlows(meal.dateEpochDay)
        }

        override suspend fun updateMeal(meal: MealEntity) {
            val idx = mealsStore.indexOfFirst { it.id == meal.id }
            if (idx >= 0) {
                val oldDate = mealsStore[idx].dateEpochDay
                mealsStore[idx] = meal
                recomputeNutrition(meal.dateEpochDay)
                if (oldDate != meal.dateEpochDay) recomputeNutrition(oldDate)
            } else {
                mealsStore.add(meal)
                recomputeNutrition(meal.dateEpochDay)
            }
            notifyFlows(meal.dateEpochDay)
        }

        override suspend fun deleteMeal(id: String) {
            val meal = mealsStore.firstOrNull { it.id == id } ?: return
            mealsStore.removeIf { it.id == id }
            recomputeNutrition(meal.dateEpochDay)
            notifyFlows(meal.dateEpochDay)
        }

        private fun recomputeNutrition(dateEpochDay: Long) {
            val dayMeals = mealsStore.filter { it.dateEpochDay == dateEpochDay }
            val profile = userProfileFlow.value
            val existing = nutritionLogsStore[dateEpochDay]
            val updated = (existing ?: NutritionLogEntity(dateEpochDay = dateEpochDay)).copy(
                calories = dayMeals.sumOf { it.calories },
                proteinG = dayMeals.sumOf { it.proteinG },
                carbsG = dayMeals.sumOf { it.carbsG },
                fatG = dayMeals.sumOf { it.fatG },
                fiberG = dayMeals.sumOf { it.fiberG },
                calorieGoal = profile.calorieGoal,
                proteinGoalG = profile.proteinGoalG,
            )
            nutritionLogsStore[dateEpochDay] = updated
        }

        override suspend fun addWater(dateEpochDay: Long, deltaMl: Int, goalMl: Int) {
            val current = waterLogsStore[dateEpochDay] ?: WaterLogEntity(dateEpochDay = dateEpochDay, goalMl = goalMl)
            val updated = current.copy(milliliters = (current.milliliters + deltaMl).coerceAtLeast(0), goalMl = goalMl)
            waterLogsStore[dateEpochDay] = updated
            notifyFlows(dateEpochDay)
        }

        // Unused stub methods required by interface
        override fun observeExercises() = flowOf(emptyList<com.gymtracker.app.data.local.entity.ExerciseEntity>())
        override fun observeWorkouts() = flowOf(emptyList<com.gymtracker.app.data.local.entity.WorkoutEntity>())
        override fun observeTemplates() = flowOf(emptyList<com.gymtracker.app.data.local.entity.WorkoutEntity>())
        override fun observeWorkoutExercises(workoutId: String) = flowOf(emptyList<com.gymtracker.app.data.local.entity.WorkoutExerciseEntity>())
        override fun observeAllWorkoutExercises() = flowOf(emptyList<com.gymtracker.app.data.local.entity.WorkoutExerciseEntity>())
        override suspend fun getAllWorkoutExercises() = emptyList<com.gymtracker.app.data.local.entity.WorkoutExerciseEntity>()
        override fun observeActiveSession() = flowOf(null)
        override fun observeSessionSets(sessionId: String) = flowOf(emptyList<com.gymtracker.app.data.local.entity.PerformedSetEntity>())
        override fun observeHistory() = flowOf(emptyList<com.gymtracker.app.data.local.entity.WorkoutSessionEntity>())
        override fun observeCompletedSessionSummaries() = flowOf(emptyList<com.gymtracker.app.domain.model.WorkoutSessionSummary>())
        override fun observeExerciseVolumeOverTime(exerciseId: String) = flowOf(emptyList<com.gymtracker.app.domain.model.ExerciseVolumePoint>())
        override suspend fun getSessionSummary(sessionId: String) = null
        override suspend fun logPastWorkout(draft: com.gymtracker.app.domain.model.PastWorkoutLogDraft) = ""
        override suspend fun deleteWorkoutSession(sessionId: String) {}
        override fun observeManualWorkouts() = flowOf(emptyList<com.gymtracker.app.data.local.entity.ManualWorkoutSessionWithSets>())
        override fun observeManualWorkout(sessionId: String) = flowOf(null)
        override suspend fun getManualWorkout(sessionId: String) = null
        override suspend fun saveManualWorkout(session: com.gymtracker.app.data.local.entity.ManualWorkoutSessionEntity, sets: List<com.gymtracker.app.data.local.entity.ManualWorkoutSetEntity>) = ""
        override suspend fun deleteManualWorkout(sessionId: String) {}
        override fun observePersonalRecords() = flowOf(emptyList<com.gymtracker.app.data.local.entity.PersonalRecordEntity>())
        override fun observeMeasurements() = flowOf(emptyList<com.gymtracker.app.data.local.entity.BodyMeasurementEntity>())
        override fun observeProgressPhotos() = flowOf(emptyList<com.gymtracker.app.data.local.entity.ProgressPhotoEntity>())
        override fun observeWeeklySchedule() = flowOf(emptyList<com.gymtracker.app.data.local.entity.WeeklyScheduleEntity>())
        override fun observeReminders() = flowOf(emptyList<com.gymtracker.app.data.local.entity.ReminderEntity>())
        override fun observeGymEquipments() = flowOf(emptyList<com.gymtracker.app.data.local.entity.GymEquipmentEntity>())
        override suspend fun getGymEquipments() = emptyList<com.gymtracker.app.data.local.entity.GymEquipmentEntity>()
        override suspend fun addGymEquipment(equipment: com.gymtracker.app.data.local.entity.GymEquipmentEntity) {}
        override suspend fun deleteGymEquipment(id: String) {}
        override fun observeHealthProfile() = flowOf(null)
        override suspend fun getHealthProfile() = com.gymtracker.app.data.local.entity.UserHealthProfileEntity()
        override suspend fun updateHealthProfile(profile: com.gymtracker.app.data.local.entity.UserHealthProfileEntity) {}
        override fun observeGeminiSyncReports() = flowOf(emptyList<com.gymtracker.app.data.local.entity.GeminiSyncReportEntity>())
        override fun observeLatestGeminiSyncReport() = flowOf(null)
        override suspend fun saveGeminiSyncReport(report: com.gymtracker.app.data.local.entity.GeminiSyncReportEntity) {}
        override suspend fun saveGeminiWorkoutAsCustomWorkout(option: com.gymtracker.app.data.remote.gemini.GeminiWorkoutOption) = ""
        override suspend fun saveGeminiRoutinePlan(result: com.gymtracker.app.data.remote.gemini.GeminiWorkoutBuilderResult, setAsSchedule: Boolean) = emptyList<String>()
        override suspend fun seedInitialData() {}
        override suspend fun applyRoutineSplitSchedule(splitName: String) {}
        override suspend fun dashboardStats(todayEpochDay: Long) = com.gymtracker.app.domain.model.DashboardStats()
        override suspend fun createOrUpdateProfile(profile: UserProfileEntity) {}
        override suspend fun createCustomWorkout(draft: com.gymtracker.app.domain.model.WorkoutDraft) = ""
        override suspend fun startWorkout(workoutId: String) = ""
        override suspend fun completeSet(setId: String, reps: Int, weight: Double, rpe: Double?, rir: Int?, notes: String) = error("")
        override suspend fun updatePerformedSet(set: com.gymtracker.app.data.local.entity.PerformedSetEntity) {}
        override suspend fun finishSession(sessionId: String, rating: Int?, notes: String) {}
        override suspend fun cancelSession(sessionId: String) {}
        override suspend fun progressForExercise(exerciseId: String) = emptyList<com.gymtracker.app.domain.model.ExerciseProgressPoint>()
        override suspend fun addMeasurement(measurement: com.gymtracker.app.data.local.entity.BodyMeasurementEntity) {}
        override suspend fun addProgressPhoto(photo: com.gymtracker.app.data.local.entity.ProgressPhotoEntity) {}
        override suspend fun upsertNutrition(log: NutritionLogEntity) {}
        override suspend fun upsertWater(water: WaterLogEntity) {}
        override suspend fun addWeightLog(weight: WeightLogEntity) {}
        override suspend fun deleteWeightLog(id: String) {}
        override suspend fun upsertSchedule(schedule: com.gymtracker.app.data.local.entity.WeeklyScheduleEntity) {}
        override suspend fun deleteSchedule(id: String) {}
        override suspend fun deleteScheduleForDay(weekDay: com.gymtracker.app.data.local.entity.WeekDay) {}
        override suspend fun upsertReminder(reminder: com.gymtracker.app.data.local.entity.ReminderEntity) {}
        override suspend fun deleteReminder(id: String) {}
        override suspend fun exportJson() = ""
        override suspend fun exportCsv() = ""
        override suspend fun exportJsonFile() = error("")
        override suspend fun exportCsvFile() = error("")
        override suspend fun importJson(jsonText: String) {}
        override suspend fun cloudBackup() = Result.success(Unit)
        override suspend fun cloudRestore() = Result.success(Unit)
        override suspend fun deleteAllData() {}
        override fun observeHealthSyncLogs() = flowOf(emptyList<com.gymtracker.app.data.local.entity.HealthSyncLogEntity>())
        override suspend fun getHealthSyncLogs() = emptyList<com.gymtracker.app.data.local.entity.HealthSyncLogEntity>()
        override suspend fun updateUserGoogleAuth(googleLinked: Boolean, googleEmail: String?, googleDisplayName: String?, googlePhotoUrl: String?, googleId: String?) {}
        override suspend fun updateHealthConnectStatus(linked: Boolean) {}
        override suspend fun updateHealthSyncPreferences(linked: Boolean, syncWorkouts: Boolean, syncWeights: Boolean, syncHydration: Boolean, syncSteps: Boolean) {}
        override suspend fun syncProgressToGoogleHealth(syncWorkouts: Boolean, syncWeights: Boolean, syncHydration: Boolean, onProgressUpdate: (suspend (step: String, progress: Float) -> Unit)?) = Result.success(com.gymtracker.app.data.health.HealthSyncSummary(message = "OK"))
        override suspend fun recordHealthSyncLog(log: com.gymtracker.app.data.local.entity.HealthSyncLogEntity) {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `NutritionViewModel recomputes daily totals when adding and editing meals`() = runTest(testDispatcher) {
        val repo = FakeGymRepository()
        val viewModel = NutritionViewModel(repo)

        val job = launch(testDispatcher) {
            viewModel.state.collect {}
        }
        runCurrent()

        // Initially no meals
        assertEquals(0, viewModel.state.value.meals.size)
        assertEquals(0, viewModel.state.value.log?.calories ?: 0)

        // Add Meal 1
        viewModel.saveMeal(null, "Oatmeal", 350, 15.0, 50.0, 6.0, 4.0)
        runCurrent()

        assertEquals(1, viewModel.state.value.meals.size)
        assertEquals(350, viewModel.state.value.log?.calories)
        assertEquals(15.0, viewModel.state.value.log?.proteinG ?: 0.0, 0.01)

        // Add Meal 2
        viewModel.saveMeal(null, "Chicken Breast", 450, 50.0, 0.0, 10.0, 0.0)
        runCurrent()

        assertEquals(2, viewModel.state.value.meals.size)
        assertEquals(800, viewModel.state.value.log?.calories)
        assertEquals(65.0, viewModel.state.value.log?.proteinG ?: 0.0, 0.01)

        // Edit Meal 1
        val meal1 = viewModel.state.value.meals.first { it.name == "Oatmeal" }
        viewModel.saveMeal(meal1.id, "Oatmeal with berries", 400, 18.0, 60.0, 7.0, 5.0)
        runCurrent()

        assertEquals(2, viewModel.state.value.meals.size)
        assertEquals(850, viewModel.state.value.log?.calories)
        assertEquals(68.0, viewModel.state.value.log?.proteinG ?: 0.0, 0.01)

        // Delete Meal 2
        val meal2 = viewModel.state.value.meals.first { it.name == "Chicken Breast" }
        viewModel.deleteMeal(meal2.id)
        runCurrent()

        assertEquals(1, viewModel.state.value.meals.size)
        assertEquals(400, viewModel.state.value.log?.calories)
        assertEquals(18.0, viewModel.state.value.log?.proteinG ?: 0.0, 0.01)

        job.cancel()
    }

    @Test
    fun `NutritionViewModel manages date changes and keeps records isolated by date`() = runTest(testDispatcher) {
        val repo = FakeGymRepository()
        val viewModel = NutritionViewModel(repo)

        val job = launch(testDispatcher) {
            viewModel.state.collect {}
        }
        runCurrent()

        val todayEpoch = LocalDate.now().toEpochDay()
        val yesterdayEpoch = todayEpoch - 1

        // Add meal today
        viewModel.saveMeal(null, "Today Meal", 500, 30.0, 40.0, 10.0, 2.0)
        runCurrent()

        assertEquals(1, viewModel.state.value.meals.size)
        assertEquals("Today Meal", viewModel.state.value.meals[0].name)

        // Navigate to yesterday
        viewModel.setSelectedDate(yesterdayEpoch)
        runCurrent()

        assertEquals(0, viewModel.state.value.meals.size)
        assertEquals(0, viewModel.state.value.log?.calories ?: 0)

        // Add meal yesterday
        viewModel.saveMeal(null, "Yesterday Meal", 600, 40.0, 50.0, 12.0, 3.0)
        runCurrent()

        assertEquals(1, viewModel.state.value.meals.size)
        assertEquals("Yesterday Meal", viewModel.state.value.meals[0].name)
        assertEquals(600, viewModel.state.value.log?.calories)

        // Return to today
        viewModel.resetToToday()
        runCurrent()

        assertEquals(1, viewModel.state.value.meals.size)
        assertEquals("Today Meal", viewModel.state.value.meals[0].name)
        assertEquals(500, viewModel.state.value.log?.calories)

        job.cancel()
    }

    @Test
    fun `NutritionCalculator BMR and Auto-Calculate Target Plan`() {
        val profile = UserProfileEntity(
            gender = Gender.MALE,
            weightKg = 80.0,
            heightCm = 180.0,
            birthYear = 1995,
        )

        val age = LocalDate.now().year - 1995
        val bmr = NutritionCalculator.bmr(profile.gender, profile.weightKg, profile.heightCm, age, BmrFormula.MIFFLIN_ST_JEOR)

        // Mifflin-St Jeor: 10 * 80 + 6.25 * 180 - 5 * age + 5 = 800 + 1125 - 5*age + 5
        val expectedBmr = 800.0 + 1125.0 - (5.0 * age) + 5.0
        assertEquals(expectedBmr, bmr, 0.1)

        val plan = NutritionCalculator.autoCalculateNutritionPlan(
            weightKg = profile.weightKg,
            heightCm = profile.heightCm,
            gender = profile.gender,
            age = age,
            goal = "Lose weight",
        )

        assertEquals((bmr * 1.55 - 500).toInt(), plan.calorieGoal)
        assertEquals(160.0, plan.proteinGoalG, 0.1)
    }
}
