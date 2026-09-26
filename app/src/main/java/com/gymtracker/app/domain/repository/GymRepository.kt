package com.gymtracker.app.domain.repository

import android.net.Uri
import com.gymtracker.app.data.local.entity.BodyMeasurementEntity
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.ManualWorkoutSessionEntity
import com.gymtracker.app.data.local.entity.ManualWorkoutSessionWithSets
import com.gymtracker.app.data.local.entity.ManualWorkoutSetEntity
import com.gymtracker.app.data.local.entity.MealEntity
import com.gymtracker.app.data.local.entity.NutritionLogEntity
import com.gymtracker.app.data.local.entity.PerformedSetEntity
import com.gymtracker.app.data.local.entity.PersonalRecordEntity
import com.gymtracker.app.data.local.entity.ProgressPhotoEntity
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WaterLogEntity
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.model.DashboardStats
import com.gymtracker.app.domain.model.ExerciseProgressPoint
import com.gymtracker.app.domain.model.WorkoutDraft
import kotlinx.coroutines.flow.Flow

interface GymRepository {
    fun observeUserProfile(): Flow<UserProfileEntity?>
    fun observeExercises(): Flow<List<ExerciseEntity>>
    fun observeWorkouts(): Flow<List<WorkoutEntity>>
    fun observeTemplates(): Flow<List<WorkoutEntity>>
    fun observeWorkoutExercises(workoutId: String): Flow<List<WorkoutExerciseEntity>>
    fun observeActiveSession(): Flow<WorkoutSessionEntity?>
    fun observeSessionSets(sessionId: String): Flow<List<PerformedSetEntity>>
    fun observeHistory(): Flow<List<WorkoutSessionEntity>>
    fun observeCompletedSessionSummaries(): Flow<List<com.gymtracker.app.domain.model.WorkoutSessionSummary>>
    fun observeExerciseVolumeOverTime(exerciseId: String): Flow<List<com.gymtracker.app.domain.model.ExerciseVolumePoint>>
    suspend fun getSessionSummary(sessionId: String): com.gymtracker.app.domain.model.WorkoutSessionSummary?
    suspend fun logPastWorkout(draft: com.gymtracker.app.domain.model.PastWorkoutLogDraft): String
    suspend fun deleteWorkoutSession(sessionId: String)

    // Manual Workout Entry
    fun observeManualWorkouts(): Flow<List<ManualWorkoutSessionWithSets>>
    fun observeManualWorkout(sessionId: String): Flow<ManualWorkoutSessionWithSets?>
    suspend fun getManualWorkout(sessionId: String): ManualWorkoutSessionWithSets?
    suspend fun saveManualWorkout(session: ManualWorkoutSessionEntity, sets: List<ManualWorkoutSetEntity>): String
    suspend fun deleteManualWorkout(sessionId: String)
    fun observePersonalRecords(): Flow<List<PersonalRecordEntity>>
    fun observeMeasurements(): Flow<List<BodyMeasurementEntity>>
    fun observeProgressPhotos(): Flow<List<ProgressPhotoEntity>>
    fun observeNutritionLog(dateEpochDay: Long): Flow<NutritionLogEntity?>
    fun observeMeals(dateEpochDay: Long): Flow<List<MealEntity>>
    fun observeWater(dateEpochDay: Long): Flow<WaterLogEntity?>
    fun observeWeightLogs(): Flow<List<WeightLogEntity>>
    fun observeWeeklySchedule(): Flow<List<WeeklyScheduleEntity>>
    fun observeReminders(): Flow<List<ReminderEntity>>
    fun observeGymEquipments(): Flow<List<com.gymtracker.app.data.local.entity.GymEquipmentEntity>>
    suspend fun getGymEquipments(): List<com.gymtracker.app.data.local.entity.GymEquipmentEntity>
    suspend fun addGymEquipment(equipment: com.gymtracker.app.data.local.entity.GymEquipmentEntity)
    suspend fun deleteGymEquipment(id: String)
    fun observeHealthProfile(): Flow<com.gymtracker.app.data.local.entity.UserHealthProfileEntity?>
    suspend fun getHealthProfile(): com.gymtracker.app.data.local.entity.UserHealthProfileEntity
    suspend fun updateHealthProfile(profile: com.gymtracker.app.data.local.entity.UserHealthProfileEntity)
    fun observeGeminiSyncReports(): Flow<List<com.gymtracker.app.data.local.entity.GeminiSyncReportEntity>>
    fun observeLatestGeminiSyncReport(): Flow<com.gymtracker.app.data.local.entity.GeminiSyncReportEntity?>
    suspend fun saveGeminiSyncReport(report: com.gymtracker.app.data.local.entity.GeminiSyncReportEntity)
    suspend fun saveGeminiWorkoutAsCustomWorkout(option: com.gymtracker.app.data.remote.gemini.GeminiWorkoutOption): String
    suspend fun saveGeminiRoutinePlan(result: com.gymtracker.app.data.remote.gemini.GeminiWorkoutBuilderResult, setAsSchedule: Boolean = true): List<String>

    suspend fun seedInitialData()
    suspend fun applyRoutineSplitSchedule(splitName: String)
    suspend fun dashboardStats(todayEpochDay: Long): DashboardStats
    suspend fun createOrUpdateProfile(profile: UserProfileEntity)
    suspend fun createCustomWorkout(draft: WorkoutDraft): String
    suspend fun startWorkout(workoutId: String): String
    suspend fun completeSet(setId: String, reps: Int, weight: Double, rpe: Double?, rir: Int?, notes: String = ""): PerformedSetEntity
    suspend fun updatePerformedSet(set: PerformedSetEntity)
    suspend fun finishSession(sessionId: String, rating: Int? = null, notes: String = "")
    suspend fun cancelSession(sessionId: String)
    suspend fun progressForExercise(exerciseId: String): List<ExerciseProgressPoint>
    suspend fun addMeasurement(measurement: BodyMeasurementEntity)
    suspend fun addProgressPhoto(photo: ProgressPhotoEntity)
    suspend fun upsertNutrition(log: NutritionLogEntity)
    suspend fun addMeal(meal: MealEntity)
    suspend fun upsertWater(water: WaterLogEntity)
    suspend fun addWeightLog(weight: WeightLogEntity)
    suspend fun upsertSchedule(schedule: WeeklyScheduleEntity)
    suspend fun upsertReminder(reminder: ReminderEntity)
    suspend fun deleteReminder(id: String)
    suspend fun exportJson(): String
    suspend fun exportCsv(): String
    suspend fun exportJsonFile(): Uri
    suspend fun exportCsvFile(): Uri
    suspend fun importJson(jsonText: String)
    suspend fun cloudBackup(): Result<Unit>
    suspend fun cloudRestore(): Result<Unit>
    suspend fun deleteAllData()
}
