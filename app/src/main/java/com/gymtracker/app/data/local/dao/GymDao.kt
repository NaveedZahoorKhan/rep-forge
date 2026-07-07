package com.gymtracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.gymtracker.app.data.local.entity.BodyMeasurementEntity
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.MealEntity
import com.gymtracker.app.data.local.entity.NutritionLogEntity
import com.gymtracker.app.data.local.entity.PerformedSetEntity
import com.gymtracker.app.data.local.entity.PersonalRecordEntity
import com.gymtracker.app.data.local.entity.ProgressPhotoEntity
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.SessionStatus
import com.gymtracker.app.data.local.entity.SetTemplateEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WaterLogEntity
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {
    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int

    @Query("SELECT * FROM exercises ORDER BY primaryMuscle, name")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name")
    suspend fun getExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExercise(id: String): ExerciseEntity?

    @Query("SELECT * FROM workouts WHERE isArchived = 0 ORDER BY isTemplate DESC, name")
    fun observeWorkouts(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE isTemplate = 1 AND isArchived = 0 ORDER BY name")
    fun observeTemplates(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun getWorkout(id: String): WorkoutEntity?

    @Query("SELECT * FROM workouts ORDER BY createdAt DESC")
    suspend fun getWorkouts(): List<WorkoutEntity>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex")
    fun observeWorkoutExercises(workoutId: String): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_exercises WHERE workoutId = :workoutId ORDER BY orderIndex")
    suspend fun getWorkoutExercises(workoutId: String): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM set_templates WHERE workoutExerciseId IN (:workoutExerciseIds) ORDER BY workoutExerciseId, orderIndex")
    suspend fun getSetTemplates(workoutExerciseIds: List<String>): List<SetTemplateEntity>

    @Query("SELECT * FROM set_templates WHERE workoutExerciseId = :workoutExerciseId ORDER BY orderIndex")
    fun observeSetTemplates(workoutExerciseId: String): Flow<List<SetTemplateEntity>>

    @Query("SELECT * FROM workout_sessions WHERE status = :status ORDER BY startedAt DESC LIMIT 1")
    fun observeSessionByStatus(status: SessionStatus): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getSession(id: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_sessions WHERE status != 'ACTIVE' ORDER BY startedAt DESC")
    fun observeHistory(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY startedAt DESC")
    suspend fun getSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM performed_sets WHERE sessionId = :sessionId ORDER BY workoutExerciseId, setNumber")
    fun observeSessionSets(sessionId: String): Flow<List<PerformedSetEntity>>

    @Query("SELECT * FROM performed_sets WHERE sessionId = :sessionId ORDER BY workoutExerciseId, setNumber")
    suspend fun getSessionSets(sessionId: String): List<PerformedSetEntity>

    @Query("SELECT * FROM performed_sets WHERE exerciseId = :exerciseId AND completed = 1 ORDER BY completedAt")
    fun observeExerciseSets(exerciseId: String): Flow<List<PerformedSetEntity>>

    @Query("SELECT * FROM performed_sets WHERE exerciseId = :exerciseId AND completed = 1")
    suspend fun getCompletedSetsForExercise(exerciseId: String): List<PerformedSetEntity>

    @Query("SELECT * FROM performed_sets ORDER BY completedAt DESC")
    suspend fun getPerformedSets(): List<PerformedSetEntity>

    @Query("SELECT * FROM personal_records ORDER BY achievedAt DESC")
    fun observePersonalRecords(): Flow<List<PersonalRecordEntity>>

    @Query("SELECT * FROM personal_records ORDER BY achievedAt DESC")
    suspend fun getPersonalRecords(): List<PersonalRecordEntity>

    @Query("SELECT * FROM body_measurements ORDER BY loggedAt DESC")
    fun observeMeasurements(): Flow<List<BodyMeasurementEntity>>

    @Query("SELECT * FROM body_measurements ORDER BY loggedAt DESC")
    suspend fun getMeasurements(): List<BodyMeasurementEntity>

    @Query("SELECT * FROM progress_photos ORDER BY takenAt DESC")
    fun observeProgressPhotos(): Flow<List<ProgressPhotoEntity>>

    @Query("SELECT * FROM progress_photos ORDER BY takenAt DESC")
    suspend fun getProgressPhotos(): List<ProgressPhotoEntity>

    @Query("SELECT * FROM nutrition_logs WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    fun observeNutritionLog(dateEpochDay: Long): Flow<NutritionLogEntity?>

    @Query("SELECT * FROM nutrition_logs ORDER BY dateEpochDay DESC")
    suspend fun getNutritionLogs(): List<NutritionLogEntity>

    @Query("SELECT * FROM meals WHERE dateEpochDay = :dateEpochDay ORDER BY loggedAt DESC")
    fun observeMeals(dateEpochDay: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals ORDER BY loggedAt DESC")
    suspend fun getMeals(): List<MealEntity>

    @Query("SELECT * FROM water_logs WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    fun observeWater(dateEpochDay: Long): Flow<WaterLogEntity?>

    @Query("SELECT * FROM water_logs ORDER BY dateEpochDay DESC")
    suspend fun getWaterLogs(): List<WaterLogEntity>

    @Query("SELECT * FROM weight_logs ORDER BY loggedAt DESC")
    fun observeWeightLogs(): Flow<List<WeightLogEntity>>

    @Query("SELECT * FROM weight_logs ORDER BY loggedAt DESC")
    suspend fun getWeightLogs(): List<WeightLogEntity>

    @Query("SELECT * FROM user_profile WHERE id = 'me' LIMIT 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 'me' LIMIT 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Query("SELECT * FROM weekly_schedule ORDER BY weekDay")
    fun observeWeeklySchedule(): Flow<List<WeeklyScheduleEntity>>

    @Query("SELECT * FROM weekly_schedule ORDER BY weekDay")
    suspend fun getWeeklySchedule(): List<WeeklyScheduleEntity>

    @Query("SELECT * FROM reminders ORDER BY timeMinutes")
    fun observeReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY timeMinutes")
    suspend fun getReminders(): List<ReminderEntity>

    @Upsert suspend fun upsertExercises(items: List<ExerciseEntity>)
    @Upsert suspend fun upsertExercise(item: ExerciseEntity)
    @Upsert suspend fun upsertWorkouts(items: List<WorkoutEntity>)
    @Upsert suspend fun upsertWorkout(item: WorkoutEntity)
    @Upsert suspend fun upsertWorkoutExercises(items: List<WorkoutExerciseEntity>)
    @Upsert suspend fun upsertWorkoutExercise(item: WorkoutExerciseEntity)
    @Upsert suspend fun upsertSetTemplates(items: List<SetTemplateEntity>)
    @Upsert suspend fun upsertSetTemplate(item: SetTemplateEntity)
    @Upsert suspend fun upsertSessions(items: List<WorkoutSessionEntity>)
    @Upsert suspend fun upsertSession(item: WorkoutSessionEntity)
    @Upsert suspend fun upsertPerformedSets(items: List<PerformedSetEntity>)
    @Upsert suspend fun upsertPerformedSet(item: PerformedSetEntity)
    @Upsert suspend fun upsertPersonalRecord(item: PersonalRecordEntity)
    @Upsert suspend fun upsertMeasurements(items: List<BodyMeasurementEntity>)
    @Upsert suspend fun upsertMeasurement(item: BodyMeasurementEntity)
    @Upsert suspend fun upsertPhotos(items: List<ProgressPhotoEntity>)
    @Upsert suspend fun upsertPhoto(item: ProgressPhotoEntity)
    @Upsert suspend fun upsertNutritionLogs(items: List<NutritionLogEntity>)
    @Upsert suspend fun upsertNutritionLog(item: NutritionLogEntity)
    @Upsert suspend fun upsertMeals(items: List<MealEntity>)
    @Upsert suspend fun upsertMeal(item: MealEntity)
    @Upsert suspend fun upsertWaterLogs(items: List<WaterLogEntity>)
    @Upsert suspend fun upsertWaterLog(item: WaterLogEntity)
    @Upsert suspend fun upsertWeightLogs(items: List<WeightLogEntity>)
    @Upsert suspend fun upsertWeightLog(item: WeightLogEntity)
    @Upsert suspend fun upsertUserProfile(item: UserProfileEntity)
    @Upsert suspend fun upsertSchedules(items: List<WeeklyScheduleEntity>)
    @Upsert suspend fun upsertSchedule(item: WeeklyScheduleEntity)
    @Upsert suspend fun upsertReminders(items: List<ReminderEntity>)
    @Upsert suspend fun upsertReminder(item: ReminderEntity)

    @Query("DELETE FROM workouts WHERE id = :id AND isTemplate = 0")
    suspend fun deleteCustomWorkout(id: String)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminder(id: String)

    @Query("DELETE FROM reminders")
    suspend fun clearReminders()

    @Query("DELETE FROM weekly_schedule")
    suspend fun clearSchedules()

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()

    @Query("DELETE FROM weight_logs")
    suspend fun clearWeightLogs()

    @Query("DELETE FROM water_logs")
    suspend fun clearWaterLogs()

    @Query("DELETE FROM meals")
    suspend fun clearMeals()

    @Query("DELETE FROM nutrition_logs")
    suspend fun clearNutritionLogs()

    @Query("DELETE FROM progress_photos")
    suspend fun clearProgressPhotos()

    @Query("DELETE FROM body_measurements")
    suspend fun clearBodyMeasurements()

    @Query("DELETE FROM personal_records")
    suspend fun clearPersonalRecords()

    @Query("DELETE FROM performed_sets")
    suspend fun clearPerformedSets()

    @Query("DELETE FROM workout_sessions")
    suspend fun clearWorkoutSessions()

    @Query("DELETE FROM set_templates")
    suspend fun clearSetTemplates()

    @Query("DELETE FROM workout_exercises")
    suspend fun clearWorkoutExercises()

    @Query("DELETE FROM workouts")
    suspend fun clearWorkouts()

    @Query("DELETE FROM exercises")
    suspend fun clearExercises()

    @Transaction
    suspend fun deleteAllUserData() {
        clearReminders()
        clearSchedules()
        clearUserProfile()
        clearWeightLogs()
        clearWaterLogs()
        clearMeals()
        clearNutritionLogs()
        clearProgressPhotos()
        clearBodyMeasurements()
        clearPersonalRecords()
        clearPerformedSets()
        clearWorkoutSessions()
        clearSetTemplates()
        clearWorkoutExercises()
        clearWorkouts()
        clearExercises()
    }
}
