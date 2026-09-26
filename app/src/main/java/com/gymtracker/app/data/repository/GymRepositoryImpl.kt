package com.gymtracker.app.data.repository

import android.net.Uri
import com.gymtracker.app.data.backup.CloudBackupService
import com.gymtracker.app.data.backup.LocalBackupService
import com.gymtracker.app.data.local.dao.GymDao
import com.gymtracker.app.data.local.dao.ManualWorkoutDao
import com.gymtracker.app.data.local.entity.BodyMeasurementEntity
import com.gymtracker.app.data.local.entity.Difficulty
import com.gymtracker.app.data.local.entity.Equipment
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.GeminiSyncReportEntity
import com.gymtracker.app.data.local.entity.ManualWorkoutSessionEntity
import com.gymtracker.app.data.local.entity.ManualWorkoutSessionWithSets
import com.gymtracker.app.data.local.entity.ManualWorkoutSetEntity
import com.gymtracker.app.data.local.entity.MealEntity
import com.gymtracker.app.data.local.entity.MuscleGroup
import com.gymtracker.app.data.local.entity.NutritionLogEntity
import com.gymtracker.app.data.local.entity.PerformedSetEntity
import com.gymtracker.app.data.local.entity.PersonalRecordEntity
import com.gymtracker.app.data.local.entity.ProgressPhotoEntity
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.SessionStatus
import com.gymtracker.app.data.local.entity.SetTemplateEntity
import com.gymtracker.app.data.local.entity.SetType
import com.gymtracker.app.data.local.entity.UserHealthProfileEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WaterLogEntity
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.data.remote.gemini.GeminiWorkoutBuilderResult
import com.gymtracker.app.data.remote.gemini.GeminiWorkoutOption
import com.gymtracker.app.domain.model.DashboardStats
import com.gymtracker.app.domain.model.ExerciseProgressPoint
import com.gymtracker.app.domain.model.ExerciseVolumePoint
import com.gymtracker.app.domain.model.ExerciseVolumeSummary
import com.gymtracker.app.domain.model.PastWorkoutLogDraft
import com.gymtracker.app.domain.model.SetDetailSummary
import com.gymtracker.app.domain.model.WorkoutDraft
import com.gymtracker.app.domain.model.WorkoutExerciseDraft
import com.gymtracker.app.domain.model.WorkoutSessionSummary
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.domain.usecase.ExerciseCatalog
import com.gymtracker.app.domain.usecase.OneRepMaxCalculator
import com.gymtracker.app.domain.usecase.WorkoutTemplateCatalog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class GymRepositoryImpl @Inject constructor(
    private val dao: GymDao,
    private val manualWorkoutDao: ManualWorkoutDao,
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

    override fun observeManualWorkouts(): Flow<List<ManualWorkoutSessionWithSets>> =
        manualWorkoutDao.observeAllManualSessionsWithSets()

    override fun observeManualWorkout(sessionId: String): Flow<ManualWorkoutSessionWithSets?> =
        manualWorkoutDao.observeManualSessionWithSets(sessionId)

    override suspend fun getManualWorkout(sessionId: String): ManualWorkoutSessionWithSets? =
        manualWorkoutDao.getManualSessionWithSets(sessionId)

    override suspend fun saveManualWorkout(session: ManualWorkoutSessionEntity, sets: List<ManualWorkoutSetEntity>): String {
        manualWorkoutDao.saveManualWorkoutSessionWithSets(session, sets)
        return session.id
    }

    override suspend fun deleteManualWorkout(sessionId: String) {
        manualWorkoutDao.deleteManualWorkoutWithSets(sessionId)
    }

    override fun observeCompletedSessionSummaries(): Flow<List<WorkoutSessionSummary>> =
        combine(
            dao.observeHistory(),
            dao.observeAllCompletedSets(),
        ) { sessions, completedSets ->
            val setsBySession = completedSets.groupBy { it.sessionId }
            sessions.map { session ->
                val sessionSets = setsBySession[session.id].orEmpty()
                mapToSummary(session, sessionSets)
            }
        }

    override fun observeExerciseVolumeOverTime(exerciseId: String): Flow<List<ExerciseVolumePoint>> =
        combine(
            dao.observeHistory(),
            dao.observeAllCompletedSets(),
        ) { sessions, completedSets ->
            val sessionMap = sessions.associateBy { it.id }
            completedSets
                .filter { it.exerciseId == exerciseId }
                .groupBy { it.sessionId }
                .mapNotNull { (sessionId, sets) ->
                    val session = sessionMap[sessionId] ?: return@mapNotNull null
                    val date = session.endedAt ?: session.startedAt
                    val totalVol = sets.sumOf { it.weight * it.reps }
                    val maxWt = sets.maxOfOrNull { it.weight } ?: 0.0
                    val totalReps = sets.sumOf { it.reps }
                    ExerciseVolumePoint(
                        sessionId = sessionId,
                        workoutName = session.workoutName,
                        dateEpochMilli = date,
                        volumeKg = totalVol,
                        maxWeightKg = maxWt,
                        totalReps = totalReps,
                        setsCount = sets.size,
                    )
                }
                .sortedBy { it.dateEpochMilli }
        }

    override suspend fun getSessionSummary(sessionId: String): WorkoutSessionSummary? {
        val session = dao.getSession(sessionId) ?: return null
        val sets = dao.getSessionSets(sessionId).filter { it.completed }
        return mapToSummary(session, sets)
    }

    override suspend fun deleteWorkoutSession(sessionId: String) {
        dao.deleteWorkoutSessionWithSets(sessionId)
        manualWorkoutDao.deleteManualWorkoutWithSets(sessionId)
    }

    override suspend fun logPastWorkout(draft: PastWorkoutLogDraft): String {
        val sessionId = UUID.randomUUID().toString()
        val totalVolume = draft.exercises.sumOf { ex ->
            ex.sets.sumOf { it.weightKg * it.reps }
        }
        val durationSec = (draft.durationMinutes.coerceAtLeast(1)) * 60
        val session = WorkoutSessionEntity(
            id = sessionId,
            workoutId = "manual_history",
            workoutName = draft.workoutName.ifBlank { "Workout Session" },
            startedAt = draft.dateEpochMilli - (durationSec * 1000),
            endedAt = draft.dateEpochMilli,
            status = SessionStatus.COMPLETED,
            notes = draft.notes,
            rating = draft.rating,
            totalVolume = totalVolume,
            durationSeconds = durationSec,
        )
        val performedSets = draft.exercises.flatMapIndexed { exIdx, exercise ->
            exercise.sets.mapIndexed { setIdx, set ->
                PerformedSetEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    workoutExerciseId = "manual:${exercise.exerciseId}:$exIdx",
                    exerciseId = exercise.exerciseId,
                    exerciseName = exercise.exerciseName,
                    setNumber = setIdx + 1,
                    reps = set.reps,
                    weight = set.weightKg,
                    completed = true,
                    completedAt = draft.dateEpochMilli,
                )
            }
        }
        dao.upsertSession(session)
        if (performedSets.isNotEmpty()) {
            dao.upsertPerformedSets(performedSets)
        }

        // Save into dedicated ManualWorkout Room entities & DAO
        val manualSession = ManualWorkoutSessionEntity(
            id = sessionId,
            workoutName = draft.workoutName.ifBlank { "Workout Session" },
            sessionDate = draft.dateEpochMilli,
            durationMinutes = draft.durationMinutes.toInt().coerceAtLeast(1),
            notes = draft.notes,
            rating = draft.rating,
            totalVolumeKg = totalVolume,
            totalReps = draft.exercises.sumOf { ex -> ex.sets.sumOf { it.reps } },
            totalSets = draft.exercises.sumOf { it.sets.size },
            createdAt = System.currentTimeMillis(),
        )
        val manualSets = draft.exercises.flatMapIndexed { _, exercise ->
            exercise.sets.mapIndexed { setIdx, set ->
                ManualWorkoutSetEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    exerciseName = exercise.exerciseName,
                    exerciseId = exercise.exerciseId,
                    muscleGroup = "",
                    setNumber = setIdx + 1,
                    reps = set.reps,
                    weightKg = set.weightKg,
                    notes = "",
                )
            }
        }
        manualWorkoutDao.saveManualWorkoutSessionWithSets(manualSession, manualSets)

        return sessionId
    }

    private fun mapToSummary(session: WorkoutSessionEntity, completedSets: List<PerformedSetEntity>): WorkoutSessionSummary {
        val completedAt = session.endedAt ?: session.startedAt
        val durationMins = (session.durationSeconds / 60).coerceAtLeast(1)
        val durationFormatted = if (durationMins >= 60) {
            "${durationMins / 60}h ${durationMins % 60}m"
        } else {
            "${durationMins}m"
        }
        val exerciseSummaries = completedSets
            .groupBy { it.exerciseId to it.exerciseName }
            .map { (pair, sets) ->
                val (exId, exName) = pair
                val totalVol = sets.sumOf { it.weight * it.reps }
                val maxWt = sets.maxOfOrNull { it.weight } ?: 0.0
                val totalReps = sets.sumOf { it.reps }
                val setDetails = sets.map { s ->
                    SetDetailSummary(
                        setNumber = s.setNumber,
                        weightKg = s.weight,
                        reps = s.reps,
                        volumeKg = s.weight * s.reps,
                        isPr = s.isPr,
                    )
                }
                ExerciseVolumeSummary(
                    exerciseId = exId,
                    exerciseName = exName,
                    completedSets = sets.size,
                    totalReps = totalReps,
                    maxWeightKg = maxWt,
                    totalVolumeKg = totalVol,
                    setsDetail = setDetails,
                )
            }
        val totalVolume = if (session.totalVolume > 0.0) session.totalVolume else completedSets.sumOf { it.weight * it.reps }
        return WorkoutSessionSummary(
            session = session,
            completedAt = completedAt,
            durationFormatted = durationFormatted,
            totalVolumeKg = totalVolume,
            completedSetsCount = completedSets.size,
            totalRepsCount = completedSets.sumOf { it.reps },
            exerciseSummaries = exerciseSummaries,
        )
    }

    override fun observePersonalRecords(): Flow<List<PersonalRecordEntity>> = dao.observePersonalRecords()
    override fun observeMeasurements(): Flow<List<BodyMeasurementEntity>> = dao.observeMeasurements()
    override fun observeProgressPhotos(): Flow<List<ProgressPhotoEntity>> = dao.observeProgressPhotos()
    override fun observeNutritionLog(dateEpochDay: Long): Flow<NutritionLogEntity?> = dao.observeNutritionLog(dateEpochDay)
    override fun observeMeals(dateEpochDay: Long): Flow<List<MealEntity>> = dao.observeMeals(dateEpochDay)
    override fun observeWater(dateEpochDay: Long): Flow<WaterLogEntity?> = dao.observeWater(dateEpochDay)
    override fun observeWeightLogs(): Flow<List<WeightLogEntity>> = dao.observeWeightLogs()
    override fun observeWeeklySchedule(): Flow<List<WeeklyScheduleEntity>> = dao.observeWeeklySchedule()
    override fun observeReminders(): Flow<List<ReminderEntity>> = dao.observeReminders()
    override fun observeGymEquipments(): Flow<List<com.gymtracker.app.data.local.entity.GymEquipmentEntity>> = dao.observeGymEquipments()
    override suspend fun getGymEquipments(): List<com.gymtracker.app.data.local.entity.GymEquipmentEntity> = dao.getGymEquipments()
    override suspend fun addGymEquipment(equipment: com.gymtracker.app.data.local.entity.GymEquipmentEntity) = dao.upsertGymEquipment(equipment)
    override suspend fun deleteGymEquipment(id: String) = dao.deleteGymEquipment(id)
    override fun observeHealthProfile(): Flow<UserHealthProfileEntity?> =
        dao.observeHealthProfile().map { profile ->
            profile?.let { sanitizeLegacyHealthDefaults(it) }
        }

    override suspend fun getHealthProfile(): UserHealthProfileEntity {
        val existing = dao.getHealthProfile()
        if (existing != null) {
            val sanitized = sanitizeLegacyHealthDefaults(existing)
            if (sanitized != existing) {
                dao.upsertHealthProfile(sanitized)
            }
            return sanitized
        }
        return UserHealthProfileEntity().also {
            dao.upsertHealthProfile(it)
        }
    }

    private fun sanitizeLegacyHealthDefaults(profile: UserHealthProfileEntity): UserHealthProfileEntity {
        val legacyHealth = "Lower back sensitivity (avoid excessive axial loading), occasional knee discomfort"
        val legacyInjuries = "Keep neutral spine; substitute barbell back squats with chest-supported machines or leg press"
        val legacyMuscles = "Chest, Shoulders, Upper Back"
        val needsSanitizing = profile.healthConditions == legacyHealth ||
                profile.injuriesAndLimitations == legacyInjuries ||
                profile.targetMuscles == legacyMuscles
        return if (needsSanitizing) {
            profile.copy(
                healthConditions = if (profile.healthConditions == legacyHealth) "" else profile.healthConditions,
                injuriesAndLimitations = if (profile.injuriesAndLimitations == legacyInjuries) "" else profile.injuriesAndLimitations,
                targetMuscles = if (profile.targetMuscles == legacyMuscles) "" else profile.targetMuscles,
            )
        } else {
            profile
        }
    }

    override suspend fun updateHealthProfile(profile: UserHealthProfileEntity) {
        dao.upsertHealthProfile(profile)
    }
    override fun observeGeminiSyncReports(): Flow<List<GeminiSyncReportEntity>> = dao.observeGeminiSyncReports()
    override fun observeLatestGeminiSyncReport(): Flow<GeminiSyncReportEntity?> = dao.observeLatestGeminiSyncReport()
    override suspend fun saveGeminiSyncReport(report: GeminiSyncReportEntity) {
        dao.upsertGeminiSyncReport(report)
    }

    override suspend fun saveGeminiWorkoutAsCustomWorkout(option: GeminiWorkoutOption): String {
        val allExercises = dao.getExercises().toMutableList()
        val draftItems = option.exercises.map { geminiExercise ->
            val existing = allExercises.firstOrNull { it.name.equals(geminiExercise.exerciseName, ignoreCase = true) }
            val exerciseId = if (existing != null) {
                existing.id
            } else {
                val muscle = runCatching { MuscleGroup.valueOf(geminiExercise.primaryMuscle.uppercase().replace(" ", "_")) }.getOrDefault(MuscleGroup.CHEST)
                val equip = runCatching { Equipment.valueOf(geminiExercise.equipment.uppercase().replace(" ", "_")) }.getOrDefault(Equipment.MACHINE)
                val newEntity = ExerciseEntity(
                    name = geminiExercise.exerciseName,
                    primaryMuscle = muscle,
                    equipment = equip,
                    difficulty = Difficulty.INTERMEDIATE,
                    instructions = geminiExercise.instructions.ifBlank { "Perform with controlled cadence and focus on mind-muscle connection." } +
                            if (geminiExercise.healthModification.isNotBlank()) "\n[Health safety note: ${geminiExercise.healthModification}]" else "",
                    isCustom = true,
                )
                dao.upsertExercise(newEntity)
                allExercises.add(newEntity)
                newEntity.id
            }

            WorkoutExerciseDraft(
                exerciseId = exerciseId,
                notes = if (geminiExercise.targetRpe != null) "Target RPE: ${geminiExercise.targetRpe}" else "",
                restSeconds = geminiExercise.restSeconds.coerceIn(15, 300),
                setCount = geminiExercise.sets.coerceIn(1, 10),
                repsMin = geminiExercise.repsMin.coerceIn(1, 100),
                repsMax = geminiExercise.repsMax.coerceIn(1, 100),
                weight = 0.0,
                setType = SetType.NORMAL,
            )
        }

        val draft = WorkoutDraft(
            name = option.title,
            description = "${option.splitType} • ${option.estimatedMinutes} min. ${option.healthFocusNote}",
            splitType = option.splitType,
            exercises = draftItems,
        )
        return createCustomWorkout(draft)
    }

    override suspend fun saveGeminiRoutinePlan(
        result: GeminiWorkoutBuilderResult,
        setAsSchedule: Boolean,
    ): List<String> {
        val createdIds = mutableListOf<String>()
        val weekDays = listOf(
            com.gymtracker.app.data.local.entity.WeekDay.MONDAY,
            com.gymtracker.app.data.local.entity.WeekDay.TUESDAY,
            com.gymtracker.app.data.local.entity.WeekDay.WEDNESDAY,
            com.gymtracker.app.data.local.entity.WeekDay.THURSDAY,
            com.gymtracker.app.data.local.entity.WeekDay.FRIDAY,
            com.gymtracker.app.data.local.entity.WeekDay.SATURDAY,
        )
        if (setAsSchedule && result.options.isNotEmpty()) {
            dao.clearSchedules()
        }
        for ((index, option) in result.options.withIndex()) {
            val workoutId = saveGeminiWorkoutAsCustomWorkout(option)
            createdIds.add(workoutId)
            if (setAsSchedule && index < weekDays.size) {
                dao.upsertSchedule(
                    WeeklyScheduleEntity(
                        weekDay = weekDays[index],
                        workoutId = workoutId,
                        workoutName = option.title,
                    )
                )
            }
        }
        return createdIds
    }

    override suspend fun seedInitialData() {
        runCatching {
            if (dao.exerciseCount() == 0) {
                val exercises = ExerciseCatalog.defaultExercises()
                dao.upsertExercises(exercises)
                val graph = WorkoutTemplateCatalog.defaultTemplates(exercises)
                dao.upsertWorkouts(graph.workouts)
                dao.upsertWorkoutExercises(graph.workoutExercises)
                dao.upsertSetTemplates(graph.setTemplates)
                dao.upsertSchedules(graph.schedules)
                dao.upsertUserProfile(UserProfileEntity())
                val splitSchedules = WorkoutTemplateCatalog.createScheduleForSplit(UserProfileEntity().preferredSplit, graph.workouts)
                if (splitSchedules.isNotEmpty()) {
                    dao.upsertSchedules(splitSchedules)
                }
            }
            val profile = dao.getUserProfile()
            if (profile != null && profile.preferredSplit.isNotBlank()) {
                val currentSchedules = dao.getWeeklySchedule()
                if (currentSchedules.isEmpty() || currentSchedules.size <= 3) {
                    applyRoutineSplitSchedule(profile.preferredSplit)
                }
            }

            // Remove any dummy/sample workout history sessions previously seeded
            val dummyNames = setOf(
                "Push Power Routine",
                "Leg Day Heavy Squats",
                "Upper Body Hypertrophy",
            )
            val dummyWorkoutIds = setOf("template:push", "template:legs", "template:upper")
            val existingSessions = dao.getSessions()
            for (s in existingSessions) {
                if (s.workoutName in dummyNames || s.workoutId in dummyWorkoutIds) {
                    dao.deleteWorkoutSessionWithSets(s.id)
                    manualWorkoutDao.deleteManualWorkoutWithSets(s.id)
                }
            }
        }.onFailure { e ->
            android.util.Log.e("GymRepository", "Error seeding initial data", e)
        }
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

    override suspend fun applyRoutineSplitSchedule(splitName: String) {
        val workouts = dao.getWorkouts()
        val schedules = WorkoutTemplateCatalog.createScheduleForSplit(splitName, workouts)
        if (schedules.isNotEmpty()) {
            dao.clearSchedules()
            dao.upsertSchedules(schedules)
        }
    }

    override suspend fun createOrUpdateProfile(profile: UserProfileEntity) {
        dao.upsertUserProfile(profile.copy(updatedAt = System.currentTimeMillis()))
        if (profile.preferredSplit.isNotBlank()) {
            applyRoutineSplitSchedule(profile.preferredSplit)
        }
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
