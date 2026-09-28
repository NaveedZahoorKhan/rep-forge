package com.gymtracker.app.domain.model

import com.gymtracker.app.data.local.entity.MuscleGroup
import kotlinx.serialization.Serializable

@Serializable
enum class WarmUpBadgeType {
    MOBILITY,
    ACTIVATION,
    JOINT_PREP,
    POTENTIATION,
    CNS_WAKE,
}

@Serializable
data class WarmUpMovement(
    val id: String,
    val name: String,
    val targetMuscleGroup: MuscleGroup,
    val jointMobilityFocus: String,
    val durationSeconds: Int? = null,
    val reps: Int? = null,
    val isPerSide: Boolean = false,
    val tempoOrPacing: String = "Smooth & controlled",
    val instructions: String,
    val coachingCue: String,
    val equipment: String = "Bodyweight",
    val badgeType: WarmUpBadgeType = WarmUpBadgeType.MOBILITY,
)

@Serializable
data class DynamicWarmUpRoutine(
    val id: String,
    val title: String,
    val primaryMuscleGroup: MuscleGroup,
    val secondaryMuscleGroups: List<MuscleGroup> = emptyList(),
    val estimatedDurationMinutes: Int = 6,
    val movementsCount: Int = 5,
    val benefitsSummary: String,
    val targetWorkoutName: String = "",
    val movements: List<WarmUpMovement>,
)
