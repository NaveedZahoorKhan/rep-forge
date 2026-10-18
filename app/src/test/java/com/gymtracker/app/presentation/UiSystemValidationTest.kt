package com.gymtracker.app.presentation

import com.gymtracker.app.data.local.entity.BmrFormula
import com.gymtracker.app.data.local.entity.Gender
import com.gymtracker.app.data.local.entity.ThemeMode
import com.gymtracker.app.data.local.entity.UnitSystem
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.domain.usecase.NutritionCalculator
import com.gymtracker.app.presentation.onboarding.PlanInputsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UiSystemValidationTest {

    @Test
    fun `verify theme modes support Light, Dark, and System`() {
        val lightProfile = UserProfileEntity(themeMode = ThemeMode.LIGHT)
        val darkProfile = UserProfileEntity(themeMode = ThemeMode.DARK)
        val systemProfile = UserProfileEntity(themeMode = ThemeMode.SYSTEM)

        assertEquals(ThemeMode.LIGHT, lightProfile.themeMode)
        assertEquals(ThemeMode.DARK, darkProfile.themeMode)
        assertEquals(ThemeMode.SYSTEM, systemProfile.themeMode)
    }

    @Test
    fun `verify unit system conversion math for Metric and Imperial`() {
        val kg = 80.0
        val lb = kg / 0.45359237
        val cm = 180.0
        val inches = cm / 2.54

        assertEquals(176.3698, lb, 0.01)
        assertEquals(70.8661, inches, 0.01)

        val kgConvertedBack = lb * 0.45359237
        val cmConvertedBack = inches * 2.54

        assertEquals(kg, kgConvertedBack, 0.001)
        assertEquals(cm, cmConvertedBack, 0.001)
    }

    @Test
    fun `verify calorie calculation consistency across unit systems`() {
        val weightKg = 70.0
        val heightCm = 170.0
        val calMetric = NutritionCalculator.autoCalculateTargetCalories(
            weightKg = weightKg,
            heightCm = heightCm,
            gender = Gender.MALE,
            goal = "Lose weight"
        )

        val weightLb = weightKg / 0.45359237
        val heightInches = heightCm / 2.54
        val calImperial = NutritionCalculator.autoCalculateTargetCalories(
            weightKg = weightLb * 0.45359237,
            heightCm = heightInches * 2.54,
            gender = Gender.MALE,
            goal = "Lose weight"
        )

        assertEquals(calMetric, calImperial)
    }

    @Test
    fun `verify nutrition calculator plan BMR and macro bounds`() {
        val plan = NutritionCalculator.autoCalculateNutritionPlan(
            weightKg = 75.0,
            heightCm = 175.0,
            gender = Gender.MALE,
            age = 28,
            activityMultiplier = 1.55,
            goal = "Lose weight",
            formula = BmrFormula.MIFFLIN_ST_JEOR
        )

        assertTrue(plan.calorieGoal >= 1500)
        assertTrue(plan.proteinGoalG > 0.0)
        assertTrue(plan.carbsGoalG > 0.0)
        assertTrue(plan.fatGoalG > 0.0)
        assertTrue(plan.waterGoalMl >= 2000)
    }

    @Test
    fun `verify plan inputs snapshot stale plan detection`() {
        val plan1 = PlanInputsSnapshot(
            routineName = "Push Pull Legs (PPL)",
            goal = "Lose weight",
            experienceLevel = "Intermediate",
            trainingDays = 4,
            notes = "Protect knees",
            equipmentIds = listOf("eq1", "eq2"),
        )

        val plan2 = PlanInputsSnapshot(
            routineName = "Push Pull Legs (PPL)",
            goal = "Lose weight",
            experienceLevel = "Intermediate",
            trainingDays = 4,
            notes = "Protect knees",
            equipmentIds = listOf("eq1", "eq2"),
        )

        val plan3 = PlanInputsSnapshot(
            routineName = "Upper / Lower Split",
            goal = "Lose weight",
            experienceLevel = "Intermediate",
            trainingDays = 4,
            notes = "Protect knees",
            equipmentIds = listOf("eq1", "eq2"),
        )

        assertEquals(plan1, plan2)
        assertTrue(plan1 != plan3)
    }
}
