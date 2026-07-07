package com.gymtracker.app.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
enum class MuscleGroup {
    CHEST,
    BACK,
    SHOULDERS,
    BICEPS,
    TRICEPS,
    LEGS,
    GLUTES,
    ABS,
    CARDIO,
    FULL_BODY,
}

@Serializable
enum class Equipment {
    BARBELL,
    DUMBBELL,
    MACHINE,
    CABLE,
    BODYWEIGHT,
    KETTLEBELL,
    BAND,
    CARDIO_MACHINE,
}

@Serializable
enum class Difficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
}

@Serializable
enum class SetType {
    NORMAL,
    WARM_UP,
    DROP_SET,
    FAILURE,
}

@Serializable
enum class SessionStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED,
}

@Serializable
enum class UnitSystem {
    METRIC,
    IMPERIAL,
}

@Serializable
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

@Serializable
enum class Gender {
    MALE,
    FEMALE,
    OTHER,
}

@Serializable
enum class WeekDay {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY,
}

@Serializable
enum class PeriodizationType {
    NONE,
    LINEAR,
    DUP,
    BLOCK,
}

@Serializable
enum class OneRepMaxFormula {
    BRZYCKI,
    EPLEY,
    LOMBARDI,
    MAYHEW,
}

@Serializable
enum class BmrFormula {
    MIFFLIN_ST_JEOR,
    HARRIS_BENEDICT,
}

@Serializable
enum class BodyFatFormula {
    US_NAVY,
    JACKSON_POLLOCK_3,
    JACKSON_POLLOCK_7,
}

fun MuscleGroup.label(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }

fun Equipment.label(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.titlecase() }

fun Difficulty.label(): String = name.lowercase().replaceFirstChar { it.titlecase() }

fun SetType.label(): String = when (this) {
    SetType.NORMAL -> "Normal"
    SetType.WARM_UP -> "Warm-up"
    SetType.DROP_SET -> "Drop set"
    SetType.FAILURE -> "Failure"
}
