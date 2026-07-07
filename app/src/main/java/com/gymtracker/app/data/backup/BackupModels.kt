package com.gymtracker.app.data.backup

import com.gymtracker.app.data.local.entity.BodyMeasurementEntity
import com.gymtracker.app.data.local.entity.ExerciseEntity
import com.gymtracker.app.data.local.entity.MealEntity
import com.gymtracker.app.data.local.entity.NutritionLogEntity
import com.gymtracker.app.data.local.entity.PerformedSetEntity
import com.gymtracker.app.data.local.entity.PersonalRecordEntity
import com.gymtracker.app.data.local.entity.ProgressPhotoEntity
import com.gymtracker.app.data.local.entity.ReminderEntity
import com.gymtracker.app.data.local.entity.SetTemplateEntity
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WaterLogEntity
import com.gymtracker.app.data.local.entity.WeeklyScheduleEntity
import com.gymtracker.app.data.local.entity.WeightLogEntity
import com.gymtracker.app.data.local.entity.WorkoutEntity
import com.gymtracker.app.data.local.entity.WorkoutExerciseEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import kotlinx.serialization.Serializable

@Serializable
data class ExportSnapshot(
    val exportedAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val exercises: List<ExerciseEntity>,
    val workouts: List<WorkoutEntity>,
    val workoutExercises: List<WorkoutExerciseEntity>,
    val setTemplates: List<SetTemplateEntity>,
    val sessions: List<WorkoutSessionEntity>,
    val performedSets: List<PerformedSetEntity>,
    val personalRecords: List<PersonalRecordEntity>,
    val bodyMeasurements: List<BodyMeasurementEntity>,
    val progressPhotos: List<ProgressPhotoEntity>,
    val nutritionLogs: List<NutritionLogEntity>,
    val meals: List<MealEntity>,
    val waterLogs: List<WaterLogEntity>,
    val weightLogs: List<WeightLogEntity>,
    val userProfile: UserProfileEntity?,
    val weeklySchedule: List<WeeklyScheduleEntity>,
    val reminders: List<ReminderEntity>,
)
