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

        override suspend fun upsertWater(water: WaterLogEntity) {
            waterLogsStore[water.dateEpochDay] = water
            notifyFlows(water.dateEpochDay)
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
        override suspend fun deleteProgressPhoto(id: String) {}
        override suspend fun upsertNutrition(log: NutritionLogEntity) {}
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
    fun `NutritionViewModel recomputes daily totals when adding and editing meals with serving size`() = runTest(testDispatcher) {
        val repo = FakeGymRepository()
        val viewModel = NutritionViewModel(repo)

        val job = launch(testDispatcher) {
            viewModel.state.collect {}
        }
        runCurrent()

        // Initially no meals
        assertEquals(0, viewModel.state.value.meals.size)
        assertEquals(0, viewModel.state.value.log?.calories ?: 0)

        // Add Meal 1 (Breakfast)
        viewModel.saveMeal(null, "Oatmeal", 350, 15.0, 50.0, 6.0, 4.0, serving = "1 cup", mealCategory = "Breakfast")
        runCurrent()

        assertEquals(1, viewModel.state.value.meals.size)
        assertEquals("Breakfast", viewModel.state.value.meals[0].mealCategory)
        assertEquals("1 cup", viewModel.state.value.meals[0].serving)
        assertEquals(350, viewModel.state.value.log?.calories)

        // Add Meal 2 (Lunch)
        viewModel.saveMeal(null, "Chicken Breast", 450, 50.0, 0.0, 10.0, 0.0, serving = "200g", mealCategory = "Lunch")
        runCurrent()

        assertEquals(2, viewModel.state.value.meals.size)
        assertEquals(800, viewModel.state.value.log?.calories)

        // Edit Meal 1
        val meal1 = viewModel.state.value.meals.first { it.name == "Oatmeal" }
        viewModel.saveMeal(meal1.id, "Oatmeal with berries", 400, 18.0, 60.0, 7.0, 5.0, serving = "1.5 cups", mealCategory = "Breakfast")
        runCurrent()

        assertEquals(2, viewModel.state.value.meals.size)
        assertEquals(850, viewModel.state.value.log?.calories)

        job.cancel()
    }

    @Test
    fun `NutritionViewModel supports meal deletion and recovery restore`() = runTest(testDispatcher) {
        val repo = FakeGymRepository()
        val viewModel = NutritionViewModel(repo)

        val job = launch(testDispatcher) {
            viewModel.state.collect {}
        }
        runCurrent()

        // Add Meal
        viewModel.saveMeal(null, "Protein Shake", 250, 30.0, 5.0, 3.0, 1.0, serving = "1 scoop", mealCategory = "Snacks")
        runCurrent()

        assertEquals(1, viewModel.state.value.meals.size)
        val mealId = viewModel.state.value.meals[0].id

        // Delete meal
        viewModel.deleteMeal(mealId)
        runCurrent()

        assertEquals(0, viewModel.state.value.meals.size)
        assertNotNull(viewModel.recentlyDeletedMeal)

        // Undo deletion / Restore
        viewModel.restoreDeletedMeal()
        runCurrent()

        assertEquals(1, viewModel.state.value.meals.size)
        assertEquals("Protein Shake", viewModel.state.value.meals[0].name)

        job.cancel()
    }

    @Test
    fun `NutritionViewModel water increments and direct editable total`() = runTest(testDispatcher) {
        val repo = FakeGymRepository()
        val viewModel = NutritionViewModel(repo)

        val job = launch(testDispatcher) {
            viewModel.state.collect {}
        }
        runCurrent()

        // Rapid water increments
        viewModel.addWater(250, 3000)
        viewModel.addWater(250, 3000)
        viewModel.addWater(500, 3000)
        runCurrent()

        assertEquals(1000, viewModel.state.value.water?.milliliters)

        // Direct total edit
        viewModel.setWaterTotal(2200, 3000)
        runCurrent()

        assertEquals(2200, viewModel.state.value.water?.milliliters)

        job.cancel()
    }
}
