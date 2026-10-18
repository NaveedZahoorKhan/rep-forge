package com.gymtracker.app.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Primary App Navigation destinations:
 * - Today (DashboardRoute)
 * - Train (WorkoutsRoute)
 * - Progress (ProgressRoute)
 * - You (ProfileRoute)
 */
typealias TodayRoute = DashboardRoute
typealias TrainRoute = WorkoutsRoute
typealias YouRoute = ProfileRoute

@Serializable
data object OnboardingRoute

@Serializable
data object DashboardRoute

@Serializable
data object WorkoutsRoute

@Serializable
data object HistoryRoute

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

@Serializable
data object GeminiCoachRoute
