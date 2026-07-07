package com.gymtracker.app.data.repository

import android.net.Uri
import com.gymtracker.app.data.backup.CloudBackupService
import com.gymtracker.app.data.backup.LocalBackupService
import com.gymtracker.app.data.local.dao.GymDao
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
import com.gymtracker.app.data.local.entity.SetType
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
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.ExerciseCatalog
import com.gymtracker.app.domain.usecase.OneRepMaxCalculator
import com.gymtracker.app.domain.usecase.WorkoutTemplateCatalog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

@Singleton
class GymRepositoryImpl @Inject constructor(
    private val dao: GymDao,
    private val localBackupService: LocalBackupService,
    private val cloudBackupService: CloudBackupService,
) : GymRepository {
    override fun observeUserProfile(): Flow<UserProfileEntity?> = dao.observeUserProfile()
    override fun observeExercises(): Flow<List<ExerciseEntity>> = dao.observeExercises()
    override fun observeWorkouts(): Flow<List<WorkoutEntity>> = dao.observeWorkouts()
    override fun observeTemplates(): Flow<List<WorkoutEntity>> = dao.observeTemplates()
    override fun observeWorkoutExercises(workoutId: String): Flow<List<WorkoutExerciseEntity>> = dao.observeWorkoutExercises(workoutId)
    override fun observeActiveSession(): Flow<WorkoutSessionEntity?> = dao.observeSessionByStatus(SessionStatus.ACTIVE)
    override fun observeSessionSets(sessionId: String): Flow<List<PerformedSetEntity>> = dao.observeSessionSets(sessionId)
    override fun observeHistory(): Flow<List<WorkoutSessionEntity>> = dao.observeHistory()
    override fun observePersonalRecords(): Flow<List<PersonalRecordEntity>> = dao.observePersonalRecords()
    override fun observeMeasurements(): Flow<List<BodyMeasurementEntity>> = dao.observeMeasurements()
    override fun observeProgressPhotos(): Flow<List<ProgressPhotoEntity>> = dao.observeProgressPhotos()
    override fun observeNutritionLog(dateEpochDay: Long): Flow<NutritionLogEntity?> = dao.observeNutritionLog(dateEpochDay)
    override fun observeMeals(dateEpochDay: Long): Flow<List<MealEntity>> = dao.observeMeals(dateEpochDay)
    override fun observeWater(dateEpochDay: Long): Flow<WaterLogEntity?> = dao.observeWater(dateEpochDay)
    override fun observeWeightLogs(): Flow<List<WeightLogEntity>> = dao.observeWeightLogs()
    override fun observeWeeklySchedule(): Flow<List<WeeklyScheduleEntity>> = dao.observeWeeklySchedule()
    override fun observeReminders(): Flow<List<ReminderEntity>> = dao.observeReminders()

    override suspend fun seedInitialData() {
        if (dao.exerciseCount() > 0) return
        val exercises = ExerciseCatalog.defaultExercises()
        dao.upsertExercises(exercises)
        val graph = WorkoutTemplateCatalog.defaultTemplates(exercises)
        dao.upsertWorkouts(graph.workouts)
        dao.upsertWorkoutExercises(graph.workoutExercises)
        dao.upsertSetTemplates(graph.setTemplates)
        dao.upsertSchedules(graph.schedules)
        dao.upsertUserProfile(UserProfileEntity())
    }

    override suspend fun dashboardStats(todayEpochDay: Long): DashboardStats {
        val zone = ZoneId.systemDefault()
        val weekStart = LocalDate.ofEpochDay(todayEpochDay).minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()
        val sessions = dao.getSessions().filter { it.startedAt >= weekStart && it.status == SessionStatus.COMPLETED }
        val days = sessions.map { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }.toSet()
        val streak = generateSequence(LocalDate.ofEpochDay(todayEpochDay)) { it.minusDays(1) }
            .takeWhile { it in days }
            .count()
        return DashboardStats(
            weeklySessions = sessions.size,
            weeklyVolume = sessions.sumOf { it.totalVolume },
            streakDays = streak,
            latestWeightKg = dao.getWeightLogs().maxByOrNull { it.loggedAt }?.weightKg,
            caloriesToday = dao.getNutritionLogs().firstOrNull { it.dateEpochDay == todayEpochDay }?.calories ?: 0,
            waterTodayMl = dao.getWaterLogs().firstOrNull { it.dateEpochDay == todayEpochDay }?.milliliters ?: 0,
        )
    }

    override suspend fun createOrUpdateProfile(profile: UserProfileEntity) {
        dao.upsertUserProfile(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun createCustomWorkout(draft: WorkoutDraft): String {
        require(draft.name.isNotBlank()) { "Workout name is required." }
        require(draft.exercises.isNotEmpty()) { "Add at least one exercise." }
        val workout = WorkoutEntity(
            name = draft.name.trim(),
            description = draft.description.trim(),
            splitType = draft.splitType.ifBlank { "Custom" },
            isTemplate = false,
        )
        val workoutExercises = draft.exercises.mapIndexed { index, item ->
            WorkoutExerciseEntity(
                workoutId = workout.id,
                exerciseId = item.exerciseId,
                orderIndex = index,
                notes = item.notes,
                restSeconds = item.restSeconds,
                supersetGroup = item.supersetGroup?.takeIf { it.isNotBlank() },
                amrapLastSet = item.amrapLastSet,
            )
        }
        val setTemplates = workoutExercises.flatMapIndexed { index, workoutExercise ->
            val draftItem = draft.exercises[index]
            List(draftItem.setCount.coerceAtLeast(1)) { setIndex ->
                SetTemplateEntity(
                    workoutExerciseId = workoutExercise.id,
                    orderIndex = setIndex,
                    targetRepsMin = draftItem.repsMin,
                    targetRepsMax = draftItem.repsMax,
                    targetWeight = draftItem.weight,
                    setType = if (setIndex == 0 && draftItem.setType == SetType.WARM_UP) SetType.WARM_UP else draftItem.setType,
                )
            }
        }
        dao.upsertWorkout(workout)
        dao.upsertWorkoutExercises(workoutExercises)
        dao.upsertSetTemplates(setTemplates)
        return workout.id
    }

    override suspend fun startWorkout(workoutId: String): String {
        val workout = dao.getWorkout(workoutId) ?: error("Workout not found.")
        val existing = dao.observeSessionByStatus(SessionStatus.ACTIVE).first()
        if (existing != null) return existing.id
        val exercises = dao.getExercises().associateBy { it.id }
        val workoutExercises = dao.getWorkoutExercises(workout.id)
        val templates = dao.getSetTemplates(workoutExercises.map { it.id }).groupBy { it.workoutExerciseId }
        val session = WorkoutSessionEntity(
            workoutId = workout.id,
            workoutName = workout.name,
        )
        val performedSets = workoutExercises.flatMap { workoutExercise ->
            val exercise = exercises[workoutExercise.exerciseId] ?: return@flatMap emptyList()
            templates[workoutExercise.id].orEmpty().mapIndexed { index, template ->
                PerformedSetEntity(
                    sessionId = session.id,
                    workoutExerciseId = workoutExercise.id,
                    exerciseId = exercise.id,
                    exerciseName = exercise.name,
                    templateSetId = template.id,
                    setNumber = index + 1,
                    reps = template.targetRepsMax,
                    weight = template.targetWeight,
                    restSeconds = workoutExercise.restSeconds,
                    setType = template.setType,
                    rpe = template.targetRpe,
                    rir = template.targetRir,
                )
            }
        }
        dao.upsertSession(session)
        dao.upsertPerformedSets(performedSets)
        return session.id
    }

    override suspend fun completeSet(
        setId: String,
        reps: Int,
        weight: Double,
        rpe: Double?,
        rir: Int?,
        notes: String,
    ): PerformedSetEntity {
        val allSets = dao.getPerformedSets()
        val current = allSets.firstOrNull { it.id == setId } ?: error("Set not found.")
        val estimatedOneRepMax = OneRepMaxCalculator.estimate(weight, reps)
        val historical = dao.getCompletedSetsForExercise(current.exerciseId).filter { it.id != current.id }
        val isWeightPr = weight > (historical.maxOfOrNull { it.weight } ?: 0.0)
        val isOneRmPr = estimatedOneRepMax > (historical.maxOfOrNull { OneRepMaxCalculator.estimate(it.weight, it.reps) } ?: 0.0)
        val completed = current.copy(
            reps = reps.coerceAtLeast(0),
            weight = weight.coerceAtLeast(0.0),
            rpe = rpe,
            rir = rir,
            notes = notes,
            completed = true,
            completedAt = System.currentTimeMillis(),
            isPr = isWeightPr || isOneRmPr,
        )
        dao.upsertPerformedSet(completed)
        if (isWeightPr) {
            dao.upsertPersonalRecord(
                PersonalRecordEntity(
                    exerciseId = completed.exerciseId,
                    exerciseName = completed.exerciseName,
                    sessionId = completed.sessionId,
                    type = "Weight",
                    value = weight,
                    reps = reps,
                    weight = weight,
                )
            )
        }
        if (isOneRmPr) {
            dao.upsertPersonalRecord(
                PersonalRecordEntity(
                    exerciseId = completed.exerciseId,
                    exerciseName = completed.exerciseName,
                    sessionId = completed.sessionId,
                    type = "Estimated 1RM",
                    value = estimatedOneRepMax,
                    reps = reps,
                    weight = weight,
                )
            )
        }
        return completed
    }

    override suspend fun updatePerformedSet(set: PerformedSetEntity) {
        dao.upsertPerformedSet(set)
    }

    override suspend fun finishSession(sessionId: String, rating: Int?, notes: String) {
        val session = dao.getSession(sessionId) ?: return
        val sets = dao.getSessionSets(sessionId)
        val now = System.currentTimeMillis()
        dao.upsertSession(
            session.copy(
                endedAt = now,
                status = SessionStatus.COMPLETED,
                rating = rating,
                notes = notes,
                totalVolume = sets.filter { it.completed }.sumOf { it.weight * it.reps },
                durationSeconds = (now - session.startedAt) / 1000,
            )
        )
    }

    override suspend fun cancelSession(sessionId: String) {
        dao.getSession(sessionId)?.let {
            dao.upsertSession(it.copy(status = SessionStatus.CANCELLED, endedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun progressForExercise(exerciseId: String): List<ExerciseProgressPoint> {
        return dao.getCompletedSetsForExercise(exerciseId)
            .sortedBy { it.completedAt ?: 0L }
            .map {
                ExerciseProgressPoint(
                    timestamp = it.completedAt ?: 0L,
                    weight = it.weight,
                    volume = it.weight * it.reps,
                    estimatedOneRepMax = OneRepMaxCalculator.estimate(it.weight, it.reps),
                )
            }
    }

    override suspend fun addMeasurement(measurement: BodyMeasurementEntity) {
        dao.upsertMeasurement(measurement)
    }

    override suspend fun addProgressPhoto(photo: ProgressPhotoEntity) {
        dao.upsertPhoto(photo)
    }

    override suspend fun upsertNutrition(log: NutritionLogEntity) {
        dao.upsertNutritionLog(log)
    }

    override suspend fun addMeal(meal: MealEntity) {
        dao.upsertMeal(meal)
        val existing = dao.getNutritionLogs().firstOrNull { it.dateEpochDay == meal.dateEpochDay }
            ?: NutritionLogEntity(dateEpochDay = meal.dateEpochDay)
        dao.upsertNutritionLog(
            existing.copy(
                calories = existing.calories + meal.calories,
                proteinG = existing.proteinG + meal.proteinG,
                carbsG = existing.carbsG + meal.carbsG,
                fatG = existing.fatG + meal.fatG,
                fiberG = existing.fiberG + meal.fiberG,
            )
        )
    }

    override suspend fun upsertWater(water: WaterLogEntity) {
        dao.upsertWaterLog(water)
    }

    override suspend fun addWeightLog(weight: WeightLogEntity) {
        val previous = dao.getWeightLogs().sortedByDescending { it.loggedAt }.take(6).map { it.weightKg }
        val average = (previous + weight.weightKg).average()
        dao.upsertWeightLog(weight.copy(movingAverageKg = average))
    }

    override suspend fun upsertSchedule(schedule: WeeklyScheduleEntity) {
        dao.upsertSchedule(schedule)
    }

    override suspend fun upsertReminder(reminder: ReminderEntity) {
        dao.upsertReminder(reminder)
    }

    override suspend fun deleteReminder(id: String) {
        dao.deleteReminder(id)
    }

    override suspend fun exportJson(): String = localBackupService.exportJson(dao)

    override suspend fun exportCsv(): String = localBackupService.exportCsv(dao)

    override suspend fun exportJsonFile(): Uri =
        localBackupService.writeShareFile("gymtracker-export.json", exportJson())

    override suspend fun exportCsvFile(): Uri =
        localBackupService.writeShareFile("gymtracker-workouts.csv", exportCsv())

    override suspend fun importJson(jsonText: String) {
        localBackupService.restore(jsonText, dao)
    }

    override suspend fun cloudBackup(): Result<Unit> = cloudBackupService.backup(exportJson())

    override suspend fun cloudRestore(): Result<Unit> = cloudBackupService.restore().mapCatching {
        importJson(it)
    }

    override suspend fun deleteAllData() {
        dao.deleteAllUserData()
        seedInitialData()
    }
}
