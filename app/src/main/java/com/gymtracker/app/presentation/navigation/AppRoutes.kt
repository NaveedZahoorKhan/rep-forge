package com.gymtracker.app.presentation.navigation

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingRoute

@Serializable
data object TodayRoute

typealias DashboardRoute = TodayRoute

@Serializable
data object TrainRoute

typealias WorkoutsRoute = TrainRoute

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
data object YouRoute

typealias ProfileRoute = YouRoute

@Serializable
data object NutritionRoute

@Serializable
data object GeminiCoachRoute

@Serializable
data object PreviewGalleryRoute
