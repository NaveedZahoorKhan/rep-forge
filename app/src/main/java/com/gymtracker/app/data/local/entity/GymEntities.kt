package com.gymtracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "exercises", indices = [Index(value = ["name"], unique = true)])
data class ExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<String> = emptyList(),
    val equipment: Equipment,
    val difficulty: Difficulty,
    val instructions: String,
    val cues: String = "",
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "workouts", indices = [Index("isTemplate"), Index("name")])
data class WorkoutEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val splitType: String = "Custom",
    val isTemplate: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "workout_exercises",
    indices = [Index("workoutId"), Index("exerciseId"), Index("supersetGroup")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val notes: String = "",
    val restSeconds: Int = 90,
    val supersetGroup: String? = null,
    val amrapLastSet: Boolean = false,
)

@Serializable
@Entity(tableName = "set_templates", indices = [Index("workoutExerciseId")])
data class SetTemplateEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutExerciseId: String,
    val orderIndex: Int,
    val targetRepsMin: Int = 8,
    val targetRepsMax: Int = 12,
    val targetWeight: Double = 0.0,
    val setType: SetType = SetType.NORMAL,
    val targetRpe: Double? = null,
    val targetRir: Int? = null,
)

@Serializable
@Entity(tableName = "workout_sessions", indices = [Index("workoutId"), Index("status"), Index("startedAt")])
data class WorkoutSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutId: String,
    val workoutName: String,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val notes: String = "",
    val rating: Int? = null,
    val totalVolume: Double = 0.0,
    val durationSeconds: Long = 0,
)

@Serializable
@Entity(
    tableName = "performed_sets",
    indices = [Index("sessionId"), Index("workoutExerciseId"), Index("exerciseId"), Index("completedAt")],
)
data class PerformedSetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val workoutExerciseId: String,
    val exerciseId: String,
    val exerciseName: String,
    val templateSetId: String? = null,
    val setNumber: Int,
    val reps: Int = 0,
    val weight: Double = 0.0,
    val restSeconds: Int = 90,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val setType: SetType = SetType.NORMAL,
    val rpe: Double? = null,
    val rir: Int? = null,
    val isPr: Boolean = false,
    val notes: String = "",
)

@Serializable
@Entity(tableName = "personal_records", indices = [Index("exerciseId"), Index("achievedAt")])
data class PersonalRecordEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val exerciseName: String,
    val sessionId: String,
    val type: String,
    val value: Double,
    val reps: Int = 0,
    val weight: Double = 0.0,
    val achievedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "body_measurements", indices = [Index("loggedAt")])
data class BodyMeasurementEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val loggedAt: Long = System.currentTimeMillis(),
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val waistCm: Double? = null,
    val neckCm: Double? = null,
    val chestCm: Double? = null,
    val armsCm: Double? = null,
    val thighsCm: Double? = null,
    val calvesCm: Double? = null,
    val hipsCm: Double? = null,
    val notes: String = "",
)

@Serializable
@Entity(tableName = "progress_photos", indices = [Index("takenAt")])
data class ProgressPhotoEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val uri: String,
    val takenAt: Long = System.currentTimeMillis(),
    val bodyWeightKg: Double? = null,
    val notes: String = "",
    val tags: List<String> = emptyList(),
)

@Serializable
@Entity(tableName = "nutrition_logs", indices = [Index(value = ["dateEpochDay"], unique = true)])
data class NutritionLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dateEpochDay: Long,
    val calories: Int = 0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val calorieGoal: Int = 2200,
    val proteinGoalG: Double = 160.0,
    val carbsGoalG: Double = 240.0,
    val fatGoalG: Double = 70.0,
    val fiberGoalG: Double = 30.0,
)

@Serializable
@Entity(tableName = "meals", indices = [Index("dateEpochDay")])
data class MealEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dateEpochDay: Long,
    val name: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double = 0.0,
    val loggedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "water_logs", indices = [Index(value = ["dateEpochDay"], unique = true)])
data class WaterLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val dateEpochDay: Long,
    val milliliters: Int = 0,
    val goalMl: Int = 3000,
)

@Serializable
@Entity(tableName = "weight_logs", indices = [Index("loggedAt")])
data class WeightLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val loggedAt: Long = System.currentTimeMillis(),
    val weightKg: Double,
    val movingAverageKg: Double? = null,
)

@Serializable
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String = "me",
    val displayName: String = "Athlete",
    val gender: Gender = Gender.OTHER,
    val birthYear: Int? = null,
    val heightCm: Double = 175.0,
    val weightKg: Double = 75.0,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultRestSeconds: Int = 90,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val workoutReminderEnabled: Boolean = false,
    val calorieGoal: Int = 2200,
    val proteinGoalG: Double = 160.0,
    val carbsGoalG: Double = 240.0,
    val fatGoalG: Double = 70.0,
    val fiberGoalG: Double = 30.0,
    val waterGoalMl: Int = 3000,
    val onboardingComplete: Boolean = false,
    val cloudSyncEnabled: Boolean = false,
    val analyticsOptIn: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(tableName = "weekly_schedule", indices = [Index("weekDay"), Index("workoutId")])
data class WeeklyScheduleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val weekDay: WeekDay,
    val workoutId: String,
    val workoutName: String,
    val periodizationType: PeriodizationType = PeriodizationType.NONE,
    val deloadEveryWeeks: Int = 4,
    val resetEveryWeeks: Int = 12,
    val reminderTimeMinutes: Int? = null,
)

@Serializable
@Entity(tableName = "reminders", indices = [Index("enabled"), Index("nextFireAt")])
data class ReminderEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val weekDay: WeekDay? = null,
    val timeMinutes: Int,
    val enabled: Boolean = true,
    val nextFireAt: Long? = null,
)
