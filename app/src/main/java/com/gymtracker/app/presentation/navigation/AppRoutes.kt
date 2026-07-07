package com.gymtracker.app.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

@Serializable
data object DashboardRoute

@Serializable
data object WorkoutsRoute

@Serializable
data class ActiveWorkoutRoute(
    val sessionId: String = "",
    val workoutId: String = "",
)

@Serializable
data object ProgressRoute

@Serializable
data object ProfileRoute

@Serializable
data object NutritionRoute
