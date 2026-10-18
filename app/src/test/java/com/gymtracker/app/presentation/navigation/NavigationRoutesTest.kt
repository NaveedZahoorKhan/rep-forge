package com.gymtracker.app.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NavigationRoutesTest {

    @Test
    fun testTopLevelRouteAliasesMatchNewDestinations() {
        assertEquals(TodayRoute, DashboardRoute)
        assertEquals(TrainRoute, WorkoutsRoute)
        assertEquals(YouRoute, ProfileRoute)
    }

    @Test
    fun testActiveWorkoutRouteDefaultArguments() {
        val defaultRoute = ActiveWorkoutRoute()
        assertEquals("", defaultRoute.sessionId)
        assertEquals("", defaultRoute.workoutId)

        val customRoute = ActiveWorkoutRoute(sessionId = "s123", workoutId = "w456")
        assertEquals("s123", customRoute.sessionId)
        assertEquals("w456", customRoute.workoutId)
    }

    @Test
    fun testRouteObjectsAreInstantiated() {
        assertNotNull(TodayRoute)
        assertNotNull(TrainRoute)
        assertNotNull(ProgressRoute)
        assertNotNull(YouRoute)
        assertNotNull(NutritionRoute)
        assertNotNull(GeminiCoachRoute)
        assertNotNull(HistoryRoute)
        assertNotNull(OnboardingRoute)
    }
}
