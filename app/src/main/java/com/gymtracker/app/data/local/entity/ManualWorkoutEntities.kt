package com.gymtracker.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * Local Room entity representing a manually entered workout session header.
 * Stores high-level session metadata such as date, duration, notes, and aggregate stats.
 */
@Serializable
@Entity(
    tableName = "manual_workout_sessions",
    indices = [
        Index("sessionDate"),
        Index("createdAt"),
        Index("workoutName"),
    ],
)
data class ManualWorkoutSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workoutName: String,
    val sessionDate: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 45,
    val notes: String = "",
    val rating: Int? = null,
    val totalVolumeKg: Double = 0.0,
    val totalReps: Int = 0,
    val totalSets: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Local Room entity representing a single performed set inside a manually entered workout session.
 * Stores exercise identification, set order, completed reps, and lifted weight.
 */
@Serializable
@Entity(
    tableName = "manual_workout_sets",
    indices = [
        Index("sessionId"),
        Index("exerciseName"),
        Index("exerciseId"),
        Index(value = ["sessionId", "exerciseName"]),
    ],
)
data class ManualWorkoutSetEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val exerciseName: String,
    val exerciseId: String = "",
    val muscleGroup: String = "",
    val setNumber: Int = 1,
    val reps: Int = 10,
    val weightKg: Double = 0.0,
    val rpe: Double? = null,
    val isWarmup: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/**
 * Relational Room representation that joins a manual workout session with all its recorded
 * exercises, sets, reps, and weights.
 */
data class ManualWorkoutSessionWithSets(
    @Embedded val session: ManualWorkoutSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId",
    )
    val sets: List<ManualWorkoutSetEntity> = emptyList(),
) {
    /**
     * Groups all recorded sets by exercise name, preserving insertion order.
     */
    val exercisesGrouped: Map<String, List<ManualWorkoutSetEntity>>
        get() = sets.groupBy { it.exerciseName }

    /**
     * Calculates total session volume (weight * reps for all sets).
     */
    fun calculateCalculatedVolumeKg(): Double = sets.sumOf { it.weightKg * it.reps }

    /**
     * Calculates total reps across all exercises.
     */
    fun calculateCalculatedReps(): Int = sets.sumOf { it.reps }

    /**
     * Count of distinct exercises performed in this manual session.
     */
    val distinctExercisesCount: Int
        get() = sets.map { it.exerciseName.trim().lowercase() }.distinct().size
}
