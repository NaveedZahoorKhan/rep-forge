package com.gymtracker.app.domain.usecase

import com.gymtracker.app.data.local.entity.BmrFormula
import com.gymtracker.app.data.local.entity.BodyFatFormula
import com.gymtracker.app.data.local.entity.Gender
import com.gymtracker.app.data.local.entity.OneRepMaxFormula
import com.gymtracker.app.data.local.entity.PerformedSetEntity
import com.gymtracker.app.data.local.entity.PeriodizationType
import com.gymtracker.app.domain.model.PlateLoad
import com.gymtracker.app.domain.model.StrengthStandard
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.pow

object OneRepMaxCalculator {
    fun estimate(weight: Double, reps: Int, formula: OneRepMaxFormula = OneRepMaxFormula.EPLEY): Double {
        if (weight <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weight
        return when (formula) {
            OneRepMaxFormula.BRZYCKI -> weight * 36.0 / (37.0 - reps.coerceAtMost(36))
            OneRepMaxFormula.EPLEY -> weight * (1.0 + reps / 30.0)
            OneRepMaxFormula.LOMBARDI -> weight * reps.toDouble().pow(0.10)
            OneRepMaxFormula.MAYHEW -> (100.0 * weight) / (52.2 + 41.9 * exp(-0.055 * reps))
        }
    }
}

object PlateCalculator {
    fun calculate(
        targetWeight: Double,
        barWeight: Double = 20.0,
        availablePlates: List<Double> = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25),
    ): PlateLoad {
        var perSide = ((targetWeight - barWeight) / 2.0).coerceAtLeast(0.0)
        val plates = mutableListOf<Double>()
        availablePlates.sortedDescending().forEach { plate ->
            while (perSide + 0.0001 >= plate) {
                plates += plate
                perSide -= plate
            }
        }
        return PlateLoad(sidePlates = plates, remainingDelta = perSide * 2.0)
    }
}

object ProgressiveOverloadUseCase {
    fun suggestion(
        completedSets: List<PerformedSetEntity>,
        targetRepsMax: Int,
        lowerBodyLift: Boolean,
    ): String {
        val workingSets = completedSets.filter { it.completed && it.weight > 0.0 }
        if (workingSets.isEmpty()) return "Log all working sets to get a load suggestion."
        val hitTopRange = workingSets.all { it.reps >= targetRepsMax }
        val avgRir = workingSets.mapNotNull { it.rir }.average().takeIf { !it.isNaN() }
        val increment = if (lowerBodyLift) 5.0 else 2.5
        return when {
            hitTopRange && (avgRir == null || avgRir >= 1.0) -> "Add ${increment} kg next time."
            workingSets.any { it.reps < targetRepsMax - 2 } -> "Hold load or reduce 5% and rebuild reps."
            else -> "Repeat load until every set reaches the top of the rep range."
        }
    }
}

object NutritionCalculator {
    fun bmr(
        gender: Gender,
        weightKg: Double,
        heightCm: Double,
        age: Int,
        formula: BmrFormula,
    ): Double {
        return when (formula) {
            BmrFormula.MIFFLIN_ST_JEOR -> {
                val genderOffset = if (gender == Gender.MALE) 5.0 else -161.0
                10.0 * weightKg + 6.25 * heightCm - 5.0 * age + genderOffset
            }
            BmrFormula.HARRIS_BENEDICT -> {
                if (gender == Gender.MALE) {
                    88.362 + (13.397 * weightKg) + (4.799 * heightCm) - (5.677 * age)
                } else {
                    447.593 + (9.247 * weightKg) + (3.098 * heightCm) - (4.330 * age)
                }
            }
        }
    }

    fun tdee(bmr: Double, activityMultiplier: Double): Double = bmr * activityMultiplier
}

object BodyFatCalculator {
    fun estimateUsNavy(
        gender: Gender,
        heightCm: Double,
        neckCm: Double,
        waistCm: Double,
        hipsCm: Double? = null,
    ): Double {
        val heightIn = heightCm / 2.54
        val neckIn = neckCm / 2.54
        val waistIn = waistCm / 2.54
        val hipsIn = (hipsCm ?: waistCm) / 2.54
        return if (gender == Gender.MALE) {
            86.010 * log10(waistIn - neckIn) - 70.041 * log10(heightIn) + 36.76
        } else {
            163.205 * log10(waistIn + hipsIn - neckIn) - 97.684 * log10(heightIn) - 78.387
        }.coerceIn(2.0, 65.0)
    }

    fun estimateJacksonPollock(
        gender: Gender,
        age: Int,
        skinfoldsMm: List<Double>,
        formula: BodyFatFormula,
    ): Double {
        val sum = skinfoldsMm.sum()
        val density = when (formula) {
            BodyFatFormula.JACKSON_POLLOCK_3 -> if (gender == Gender.MALE) {
                1.10938 - 0.0008267 * sum + 0.0000016 * sum * sum - 0.0002574 * age
            } else {
                1.0994921 - 0.0009929 * sum + 0.0000023 * sum * sum - 0.0001392 * age
            }
            BodyFatFormula.JACKSON_POLLOCK_7 -> if (gender == Gender.MALE) {
                1.112 - 0.00043499 * sum + 0.00000055 * sum * sum - 0.00028826 * age
            } else {
                1.097 - 0.00046971 * sum + 0.00000056 * sum * sum - 0.00012828 * age
            }
            BodyFatFormula.US_NAVY -> return 0.0
        }
        return ((495.0 / density) - 450.0).coerceIn(2.0, 65.0)
    }
}

object StrengthStandardsUseCase {
    private val standards = mapOf(
        "Barbell Bench Press" to StrengthStandard(60.0, 100.0, 140.0, 180.0),
        "Back Squat" to StrengthStandard(80.0, 140.0, 200.0, 260.0),
        "Deadlift" to StrengthStandard(100.0, 180.0, 240.0, 320.0),
        "Overhead Press" to StrengthStandard(40.0, 70.0, 100.0, 130.0),
        "Barbell Row" to StrengthStandard(50.0, 90.0, 130.0, 170.0),
    )

    fun standardFor(exerciseName: String): StrengthStandard =
        standards[exerciseName] ?: StrengthStandard(40.0, 70.0, 100.0, 130.0)
}

object PeriodizationPlanner {
    fun adjustmentForWeek(type: PeriodizationType, week: Int): Double = when (type) {
        PeriodizationType.NONE -> 1.0
        PeriodizationType.LINEAR -> 1.0 + (week - 1).coerceAtLeast(0) * 0.025
        PeriodizationType.DUP -> when (week % 3) {
            1 -> 0.90
            2 -> 1.00
            else -> 1.05
        }
        PeriodizationType.BLOCK -> when {
            week <= 4 -> 0.85 + week * 0.025
            week <= 8 -> 0.95 + (week - 4) * 0.02
            else -> 1.02
        }
    }

    fun isDeloadWeek(week: Int, deloadEveryWeeks: Int): Boolean =
        deloadEveryWeeks > 0 && week > 0 && week % deloadEveryWeeks == 0

    fun isResetWeek(week: Int, resetEveryWeeks: Int): Boolean =
        resetEveryWeeks > 0 && week > 0 && week % resetEveryWeeks == 0
}
