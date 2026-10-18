package com.gymtracker.app.data.auth

import android.content.Context
import android.content.ContextWrapper
import com.gymtracker.app.data.backup.CloudBackupService
import com.gymtracker.app.data.backup.LocalBackupService
import com.gymtracker.app.data.health.HealthSyncService
import com.gymtracker.app.data.local.dao.GymDao
import com.gymtracker.app.data.local.dao.ManualWorkoutDao
import com.gymtracker.app.data.local.entity.BodyMeasurementEntity
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.GeminiSyncReportEntity
import com.gymtracker.app.data.local.entity.GymEquipmentEntity
import com.gymtracker.app.data.local.entity.HealthSyncLogEntity
import com.gymtracker.app.data.local.entity.ManualWorkoutSessionEntity
import com.gymtracker.app.data.local.entity.ManualWorkoutSessionWithSets
import com.gymtracker.app.data.local.entity.ManualWorkoutSetEntity
import com.gymtracker.app.data.local.entity.MealEntity
import com.gymtracker.app.data.local.entity.NutritionLogEntity
import com.gymtracker.app.data.local.entity.PerformedSetEntity
import com.gymtracker.app.data.local.entity.PersonalRecordEntity
import com.gymtracker.app.data.local.entity.ProgressPhotoEntity
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.SessionStatus
import com.gymtracker.app.data.local.entity.SetTemplateEntity
import com.gymtracker.app.data.local.entity.UserHealthProfileEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WaterLogEntity
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.data.repository.GymRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleAuthVerificationTest {

    @Test
    fun `GymRepositoryImpl updateUserGoogleAuth decouples Google Auth from Health Connect status`() = runTest {
        val fakeDao = FakeGymDao()
        val fakeManualDao = FakeManualWorkoutDao()
        val context = DummyContext()
        val repository = GymRepositoryImpl(
            dao = fakeDao,
            manualWorkoutDao = fakeManualDao,
            localBackupService = LocalBackupService(context),
            cloudBackupService = CloudBackupService(context),
            healthSyncService = HealthSyncService(context, fakeDao, fakeManualDao),
        )

        // Initial state
        var profile = fakeDao.getUserProfile()
        assertNull(profile)

        // Update Google Auth with googleLinked = true
        repository.updateUserGoogleAuth(
            googleLinked = true,
            googleEmail = "athlete@example.com",
            googleDisplayName = "Athlete User",
            googlePhotoUrl = "https://example.com/avatar.jpg",
            googleId = "google_12345",
        )

        profile = fakeDao.getUserProfile()
        assertNotNull(profile)
        assertTrue(profile!!.googleLinked)
        assertEquals("athlete@example.com", profile.googleEmail)
        assertEquals("Athlete User", profile.googleDisplayName)

        // Critical verification: Health Connect status MUST NOT automatically turn true!
        assertFalse(profile.healthConnectLinked)

        // Updating Health Connect status explicitly
        repository.updateHealthConnectStatus(linked = true)
        profile = fakeDao.getUserProfile()
        assertTrue(profile!!.healthConnectLinked)
        assertTrue(profile.googleLinked)
    }

    @Test
    fun `GymRepositoryImpl updateUserGoogleAuth handles fallback display name as Athlete`() = runTest {
        val fakeDao = FakeGymDao()
        val fakeManualDao = FakeManualWorkoutDao()
        val context = DummyContext()
        val repository = GymRepositoryImpl(
            dao = fakeDao,
            manualWorkoutDao = fakeManualDao,
            localBackupService = LocalBackupService(context),
            cloudBackupService = CloudBackupService(context),
            healthSyncService = HealthSyncService(context, fakeDao, fakeManualDao),
        )

        repository.updateUserGoogleAuth(
            googleLinked = true,
            googleEmail = "user@gmail.com",
            googleDisplayName = "",
            googlePhotoUrl = null,
            googleId = "123",
        )

        val profile = fakeDao.getUserProfile()
        assertEquals("Athlete", profile!!.displayName)
    }
}

private class DummyContext : ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
}

// Minimal Stubs and Fakes for Unit Testing
private class FakeGymDao : GymDao {
    private var userProfile: UserProfileEntity? = null

    override fun observeUserProfile(): Flow<UserProfileEntity?> = MutableStateFlow(userProfile)
    override suspend fun getUserProfile(): UserProfileEntity? = userProfile
    override suspend fun upsertUserProfile(profile: UserProfileEntity) {
        userProfile = profile
    }

    override suspend fun exerciseCount(): Int = 0
    override fun observeExercises(): Flow<List<ExerciseEntity>> = MutableStateFlow(emptyList())
    override suspend fun getExercises(): List<ExerciseEntity> = emptyList()
    override suspend fun getExercise(id: String): ExerciseEntity? = null
    override fun observeWorkouts(): Flow<List<WorkoutEntity>> = MutableStateFlow(emptyList())
    override fun observeTemplates(): Flow<List<WorkoutEntity>> = MutableStateFlow(emptyList())
    override suspend fun getWorkout(id: String): WorkoutEntity? = null
    override suspend fun getWorkouts(): List<WorkoutEntity> = emptyList()
    override fun observeWorkoutExercises(workoutId: String): Flow<List<WorkoutExerciseEntity>> = MutableStateFlow(emptyList())
    override suspend fun getWorkoutExercises(workoutId: String): List<WorkoutExerciseEntity> = emptyList()
    override fun observeAllWorkoutExercises(): Flow<List<WorkoutExerciseEntity>> = MutableStateFlow(emptyList())
    override suspend fun getAllWorkoutExercises(): List<WorkoutExerciseEntity> = emptyList()
    override suspend fun getSetTemplates(workoutExerciseIds: List<String>): List<SetTemplateEntity> = emptyList()
    override fun observeSetTemplates(workoutExerciseId: String): Flow<List<SetTemplateEntity>> = MutableStateFlow(emptyList())
    override fun observeSessionByStatus(status: SessionStatus): Flow<WorkoutSessionEntity?> = MutableStateFlow(null)
    override suspend fun getSession(id: String): WorkoutSessionEntity? = null
    override fun observeHistory(): Flow<List<WorkoutSessionEntity>> = MutableStateFlow(emptyList())
    override suspend fun getSessions(): List<WorkoutSessionEntity> = emptyList()
    override suspend fun deleteSession(sessionId: String) {}
    override suspend fun deleteSessionSets(sessionId: String) {}
    override fun observeAllCompletedSets(): Flow<List<PerformedSetEntity>> = MutableStateFlow(emptyList())
    override fun observeSessionSets(sessionId: String): Flow<List<PerformedSetEntity>> = MutableStateFlow(emptyList())
    override suspend fun getSessionSets(sessionId: String): List<PerformedSetEntity> = emptyList()
    override fun observeExerciseSets(exerciseId: String): Flow<List<PerformedSetEntity>> = MutableStateFlow(emptyList())
    override suspend fun getCompletedSetsForExercise(exerciseId: String): List<PerformedSetEntity> = emptyList()
    override suspend fun getPerformedSets(): List<PerformedSetEntity> = emptyList()
    override fun observePersonalRecords(): Flow<List<PersonalRecordEntity>> = MutableStateFlow(emptyList())
    override suspend fun getPersonalRecords(): List<PersonalRecordEntity> = emptyList()
    override fun observeMeasurements(): Flow<List<BodyMeasurementEntity>> = MutableStateFlow(emptyList())
    override suspend fun getMeasurements(): List<BodyMeasurementEntity> = emptyList()
    override fun observeProgressPhotos(): Flow<List<ProgressPhotoEntity>> = MutableStateFlow(emptyList())
    override suspend fun getProgressPhotos(): List<ProgressPhotoEntity> = emptyList()
    override fun observeNutritionLog(dateEpochDay: Long): Flow<NutritionLogEntity?> = MutableStateFlow(null)
    override suspend fun getNutritionLogs(): List<NutritionLogEntity> = emptyList()
    override fun observeMeals(dateEpochDay: Long): Flow<List<MealEntity>> = MutableStateFlow(emptyList())
    override suspend fun getMealsForDate(dateEpochDay: Long): List<MealEntity> = emptyList()
    override suspend fun getMeal(id: String): MealEntity? = null
    override suspend fun getMeals(): List<MealEntity> = emptyList()
    override suspend fun deleteMeal(id: String) {}
    override fun observeWater(dateEpochDay: Long): Flow<WaterLogEntity?> = MutableStateFlow(null)
    override suspend fun getWaterForDate(dateEpochDay: Long): WaterLogEntity? = null
    override suspend fun getWaterLogs(): List<WaterLogEntity> = emptyList()
    override fun observeWeightLogs(): Flow<List<WeightLogEntity>> = MutableStateFlow(emptyList())
    override suspend fun getWeightLogs(): List<WeightLogEntity> = emptyList()
    override fun observeWeeklySchedule(): Flow<List<WeeklyScheduleEntity>> = MutableStateFlow(emptyList())
    override suspend fun getWeeklySchedule(): List<WeeklyScheduleEntity> = emptyList()
    override fun observeReminders(): Flow<List<ReminderEntity>> = MutableStateFlow(emptyList())
    override suspend fun getReminders(): List<ReminderEntity> = emptyList()
    override suspend fun upsertExercises(items: List<ExerciseEntity>) {}
    override suspend fun upsertExercise(item: ExerciseEntity) {}
    override suspend fun upsertWorkouts(items: List<WorkoutEntity>) {}
    override suspend fun upsertWorkout(item: WorkoutEntity) {}
    override suspend fun upsertWorkoutExercises(items: List<WorkoutExerciseEntity>) {}
    override suspend fun upsertWorkoutExercise(item: WorkoutExerciseEntity) {}
    override suspend fun upsertSetTemplates(items: List<SetTemplateEntity>) {}
    override suspend fun upsertSetTemplate(item: SetTemplateEntity) {}
    override suspend fun upsertSessions(items: List<WorkoutSessionEntity>) {}
    override suspend fun upsertSession(item: WorkoutSessionEntity) {}
    override suspend fun upsertPerformedSets(items: List<PerformedSetEntity>) {}
    override suspend fun upsertPerformedSet(item: PerformedSetEntity) {}
    override suspend fun upsertPersonalRecord(item: PersonalRecordEntity) {}
    override suspend fun upsertMeasurements(items: List<BodyMeasurementEntity>) {}
    override suspend fun upsertMeasurement(item: BodyMeasurementEntity) {}
    override suspend fun upsertPhotos(items: List<ProgressPhotoEntity>) {}
    override suspend fun upsertPhoto(item: ProgressPhotoEntity) {}
    override suspend fun deleteProgressPhoto(id: String) {}
    override suspend fun upsertNutritionLogs(items: List<NutritionLogEntity>) {}
    override suspend fun upsertNutritionLog(item: NutritionLogEntity) {}
    override suspend fun upsertMeals(items: List<MealEntity>) {}
    override suspend fun upsertMeal(item: MealEntity) {}
    override suspend fun upsertWaterLogs(items: List<WaterLogEntity>) {}
    override suspend fun upsertWaterLog(item: WaterLogEntity) {}
    override suspend fun upsertWeightLogs(items: List<WeightLogEntity>) {}
    override suspend fun upsertWeightLog(item: WeightLogEntity) {}
    override suspend fun upsertSchedules(items: List<WeeklyScheduleEntity>) {}
    override suspend fun upsertSchedule(item: WeeklyScheduleEntity) {}
    override suspend fun upsertReminders(items: List<ReminderEntity>) {}
    override suspend fun upsertReminder(item: ReminderEntity) {}
    override fun observeGymEquipments(): Flow<List<GymEquipmentEntity>> = MutableStateFlow(emptyList())
    override suspend fun getGymEquipments(): List<GymEquipmentEntity> = emptyList()
    override suspend fun upsertGymEquipment(item: GymEquipmentEntity) {}
    override suspend fun upsertGymEquipments(items: List<GymEquipmentEntity>) {}
    override suspend fun deleteGymEquipment(id: String) {}
    override suspend fun clearGymEquipments() {}
    override fun observeHealthProfile(): Flow<UserHealthProfileEntity?> = MutableStateFlow(null)
    override suspend fun getHealthProfile(): UserHealthProfileEntity? = null
    override suspend fun upsertHealthProfile(item: UserHealthProfileEntity) {}
    override fun observeGeminiSyncReports(): Flow<List<GeminiSyncReportEntity>> = MutableStateFlow(emptyList())
    override fun observeLatestGeminiSyncReport(): Flow<GeminiSyncReportEntity?> = MutableStateFlow(null)
    override suspend fun upsertGeminiSyncReport(item: GeminiSyncReportEntity) {}
    override suspend fun clearGeminiSyncReports() {}
    override suspend fun deleteCustomWorkout(id: String) {}
    override suspend fun deleteReminder(id: String) {}
    override suspend fun clearReminders() {}
    override suspend fun insertHealthSyncLog(log: HealthSyncLogEntity) {}
    override fun observeHealthSyncLogs(): Flow<List<HealthSyncLogEntity>> = MutableStateFlow(emptyList())
    override suspend fun getHealthSyncLogs(): List<HealthSyncLogEntity> = emptyList()
    override suspend fun clearHealthSyncLogs() {}
    override suspend fun updateUserGoogleAuth(
        googleLinked: Boolean,
        googleEmail: String?,
        googleDisplayName: String?,
        googlePhotoUrl: String?,
        googleId: String?,
        now: Long
    ) {}
    override suspend fun updateHealthConnectStatus(linked: Boolean, syncedAt: Long) {}
    override suspend fun updateHealthSyncPreferences(
        linked: Boolean,
        syncWorkouts: Boolean,
        syncWeights: Boolean,
        syncHydration: Boolean,
        syncSteps: Boolean,
        now: Long
    ) {}
    override suspend fun clearSchedules() {}
    override suspend fun clearUserProfile() {}
    override suspend fun deleteWeightLog(id: String) {}
    override suspend fun clearWeightLogs() {}
    override suspend fun clearWaterLogs() {}
    override suspend fun clearMeals() {}
    override suspend fun clearNutritionLogs() {}
    override suspend fun clearProgressPhotos() {}
    override suspend fun clearBodyMeasurements() {}
    override suspend fun clearPersonalRecords() {}
    override suspend fun clearPerformedSets() {}
    override suspend fun clearWorkoutSessions() {}
    override suspend fun clearSetTemplates() {}
    override suspend fun clearWorkoutExercises() {}
    override suspend fun clearWorkouts() {}
    override suspend fun clearExercises() {}
}

private class FakeManualWorkoutDao : ManualWorkoutDao {
    override fun observeAllManualSessionsWithSets(): Flow<List<ManualWorkoutSessionWithSets>> = MutableStateFlow(emptyList())
    override fun observeManualSessions(): Flow<List<ManualWorkoutSessionEntity>> = MutableStateFlow(emptyList())
    override fun observeManualSessionWithSets(sessionId: String): Flow<ManualWorkoutSessionWithSets?> = MutableStateFlow(null)
    override fun observeSetsForSession(sessionId: String): Flow<List<ManualWorkoutSetEntity>> = MutableStateFlow(emptyList())
    override fun observeSetsForExercise(exerciseName: String): Flow<List<ManualWorkoutSetEntity>> = MutableStateFlow(emptyList())
    override suspend fun getManualSessionWithSets(sessionId: String): ManualWorkoutSessionWithSets? = null
    override suspend fun getManualSession(sessionId: String): ManualWorkoutSessionEntity? = null
    override suspend fun getAllManualSessions(): List<ManualWorkoutSessionEntity> = emptyList()
    override suspend fun getSetsForSession(sessionId: String): List<ManualWorkoutSetEntity> = emptyList()
    override suspend fun getBestSetForExercise(exerciseName: String): ManualWorkoutSetEntity? = null
    override suspend fun getManualSessionCount(): Int = 0
    override suspend fun upsertSession(session: ManualWorkoutSessionEntity) {}
    override suspend fun upsertSessions(sessions: List<ManualWorkoutSessionEntity>) {}
    override suspend fun upsertSet(set: ManualWorkoutSetEntity) {}
    override suspend fun upsertSets(sets: List<ManualWorkoutSetEntity>) {}
    override suspend fun insertSession(session: ManualWorkoutSessionEntity) {}
    override suspend fun insertSets(sets: List<ManualWorkoutSetEntity>) {}
    override suspend fun updateSession(session: ManualWorkoutSessionEntity) {}
    override suspend fun updateSet(set: ManualWorkoutSetEntity) {}
    override suspend fun deleteSetById(setId: String) {}
    override suspend fun deleteSetsForSession(sessionId: String) {}
    override suspend fun deleteSessionById(sessionId: String) {}
    override suspend fun clearAllSets() {}
    override suspend fun clearAllSessions() {}
    override suspend fun saveManualWorkoutSessionWithSets(
        session: ManualWorkoutSessionEntity,
        sets: List<ManualWorkoutSetEntity>
    ) {}
    override suspend fun deleteManualWorkoutWithSets(sessionId: String) {}
}
