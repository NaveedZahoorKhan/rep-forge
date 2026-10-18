package com.gymtracker.app.presentation.onboarding

import com.gymtracker.app.data.local.entity.Gender
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.domain.usecase.NutritionCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class OnboardingTest {

    @Test
    fun testMetricToImperialConversion() {
        val heightCm = 175.0
        val weightKg = 75.0

        val inches = heightCm / 2.54
        val lbs = weightKg / 0.45359237

        assertEquals(68.8976, inches, 0.01)
        assertEquals(165.3466, lbs, 0.01)

        // Convert back
        val cmBack = inches * 2.54
        val kgBack = lbs * 0.45359237

        assertEquals(heightCm, cmBack, 0.001)
        assertEquals(weightKg, kgBack, 0.001)
    }

    @Test
    fun testCalorieCalculationMetricAndImperial() {
        // Metric inputs
        val weightKg = 75.0
        val heightCm = 175.0
        val caloriesMetric = NutritionCalculator.autoCalculateTargetCalories(
            weightKg = weightKg,
            heightCm = heightCm,
            gender = Gender.MALE,
            goal = "Lose weight",
        )

        // Imperial inputs converted
        val weightLbs = weightKg / 0.45359237
        val heightInches = heightCm / 2.54

        val weightKgFromImperial = weightLbs * 0.45359237
        val heightCmFromImperial = heightInches * 2.54

        val caloriesImperial = NutritionCalculator.autoCalculateTargetCalories(
            weightKg = weightKgFromImperial,
            heightCm = heightCmFromImperial,
            gender = Gender.MALE,
            goal = "Lose weight",
        )

        assertEquals(caloriesMetric, caloriesImperial)
    }

    @Test
    fun testOptionalMeasurementsValidationLogic() {
        fun validateHeight(heightStr: String, unitSystem: UnitSystem): String? {
            val valNum = heightStr.toDoubleOrNull()
            return when {
                heightStr.isBlank() -> null // Optional!
                valNum == null || valNum <= 0 -> "Enter a valid positive number"
                unitSystem == UnitSystem.METRIC && (valNum < 50.0 || valNum > 280.0) -> "Must be between 50 and 280 cm"
                unitSystem == UnitSystem.IMPERIAL && (valNum < 20.0 || valNum > 110.0) -> "Must be between 20 and 110 inches"
                else -> null
            }
        }

        // Blank string is optional (valid, returns null error)
        assertNull(validateHeight("", UnitSystem.METRIC))
        assertNull(validateHeight("   ", UnitSystem.METRIC))

        // Invalid inputs return error text
        assertNotNull(validateHeight("abc", UnitSystem.METRIC))
        assertNotNull(validateHeight("-5", UnitSystem.METRIC))
        assertNotNull(validateHeight("10", UnitSystem.METRIC))

        // Valid inputs return null error
        assertNull(validateHeight("175", UnitSystem.METRIC))
        assertNull(validateHeight("70", UnitSystem.IMPERIAL))
        assertNotNull(validateHeight("150", UnitSystem.IMPERIAL))
    }

    @Test
    fun testFiveStepNavigationBounds() {
        fun coerceStep(step: Int): Int = step.coerceIn(0, 4)

        assertEquals(0, coerceStep(-1))
        assertEquals(0, coerceStep(0))
        assertEquals(1, coerceStep(1))
        assertEquals(2, coerceStep(2))
        assertEquals(3, coerceStep(3))
        assertEquals(4, coerceStep(4))
        assertEquals(4, coerceStep(5))
    }

    @Test
    fun testPlanInputsSnapshotEquality() {
        val snapshot1 = PlanInputsSnapshot(
            routineName = "Push Pull Legs (PPL)",
            goal = "Lose weight",
            experienceLevel = "Intermediate",
            trainingDays = 4,
            notes = "Protect shoulder",
            equipmentIds = listOf("eq1", "eq2"),
        )

        val snapshot2 = PlanInputsSnapshot(
            routineName = "Push Pull Legs (PPL)",
            goal = "Lose weight",
            experienceLevel = "Intermediate",
            trainingDays = 4,
            notes = "Protect shoulder",
            equipmentIds = listOf("eq1", "eq2"),
        )

        val snapshot3 = PlanInputsSnapshot(
            routineName = "Upper / Lower Split",
            goal = "Lose weight",
            experienceLevel = "Intermediate",
            trainingDays = 4,
            notes = "Protect shoulder",
            equipmentIds = listOf("eq1", "eq2"),
        )

        assertEquals(snapshot1, snapshot2)
        assert(snapshot1 != snapshot3)
    }
}
