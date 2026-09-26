package com.gymtracker.app.domain.model

import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import kotlinx.serialization.Serializable

@Serializable
data class SetDetailSummary(
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val volumeKg: Double,
    val isPr: Boolean = false,
)

@Serializable
data class ExerciseVolumeSummary(
    val exerciseId: String,
    val exerciseName: String,
    val completedSets: Int,
    val totalReps: Int,
    val maxWeightKg: Double,
    val totalVolumeKg: Double,
    val setsDetail: List<SetDetailSummary> = emptyList(),
)

@Serializable
data class WorkoutSessionSummary(
    val session: WorkoutSessionEntity,
    val completedAt: Long,
    val durationFormatted: String,
    val totalVolumeKg: Double,
    val completedSetsCount: Int,
    val totalRepsCount: Int,
    val exerciseSummaries: List<ExerciseVolumeSummary>,
)

@Serializable
data class ExerciseVolumePoint(
    val sessionId: String,
    val workoutName: String,
    val dateEpochMilli: Long,
    val volumeKg: Double,
    val maxWeightKg: Double,
    val totalReps: Int,
    val setsCount: Int,
)

data class PastWorkoutLogDraft(
    val workoutName: String,
    val dateEpochMilli: Long,
    val durationMinutes: Long,
    val rating: Int? = null,
    val notes: String = "",
    val exercises: List<PastExerciseDraft> = emptyList(),
)

data class PastExerciseDraft(
    val exerciseId: String,
    val exerciseName: String,
    val sets: List<PastSetDraft> = emptyList(),
)

data class PastSetDraft(
    val weightKg: Double,
    val reps: Int,
)
