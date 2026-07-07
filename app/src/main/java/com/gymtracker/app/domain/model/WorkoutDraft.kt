package com.gymtracker.app.domain.model

import com.gymtracker.app.data.local.entity.SetType

data class WorkoutExerciseDraft(
    val exerciseId: String,
    val notes: String = "",
    val restSeconds: Int = 90,
    val setCount: Int = 3,
    val repsMin: Int = 8,
    val repsMax: Int = 12,
    val weight: Double = 0.0,
    val setType: SetType = SetType.NORMAL,
    val supersetGroup: String? = null,
    val amrapLastSet: Boolean = false,
)

data class WorkoutDraft(
    val name: String,
    val description: String,
    val splitType: String = "Custom",
    val exercises: List<WorkoutExerciseDraft>,
)

data class ExerciseProgressPoint(
    val timestamp: Long,
    val weight: Double,
    val volume: Double,
    val estimatedOneRepMax: Double,
)

data class DashboardStats(
    val weeklySessions: Int = 0,
    val weeklyVolume: Double = 0.0,
    val streakDays: Int = 0,
    val latestWeightKg: Double? = null,
    val caloriesToday: Int = 0,
    val waterTodayMl: Int = 0,
)

data class PlateLoad(
    val sidePlates: List<Double>,
    val remainingDelta: Double,
)

data class StrengthStandard(
    val beginner: Double,
    val intermediate: Double,
    val advanced: Double,
    val elite: Double,
) {
    fun levelFor(weight: Double): String = when {
        weight >= elite -> "Elite"
        weight >= advanced -> "Advanced"
        weight >= intermediate -> "Intermediate"
        weight >= beginner -> "Beginner"
        else -> "Novice"
    }
}
