package com.gymtracker.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.gymtracker.app.data.local.entity.ThemeMode
import com.gymtracker.app.data.local.entity.UserProfileEntity
import com.gymtracker.app.data.local.entity.WorkoutSessionEntity
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.presentation.active.ActiveWorkoutRouteScreen
import com.gymtracker.app.presentation.dashboard.DashboardScreen
import com.gymtracker.app.presentation.gemini.GeminiCoachScreen
import com.gymtracker.app.presentation.history.WorkoutHistoryScreen
import com.gymtracker.app.presentation.navigation.ActiveWorkoutRoute
import com.gymtracker.app.presentation.navigation.GeminiCoachRoute
import com.gymtracker.app.presentation.navigation.HistoryRoute
import com.gymtracker.app.presentation.navigation.NutritionRoute
import com.gymtracker.app.presentation.navigation.OnboardingRoute
import com.gymtracker.app.presentation.navigation.ProgressRoute
import com.gymtracker.app.presentation.navigation.TodayRoute
import com.gymtracker.app.presentation.navigation.TrainRoute
import com.gymtracker.app.presentation.navigation.YouRoute
import com.gymtracker.app.presentation.nutrition.NutritionScreen
import com.gymtracker.app.presentation.onboarding.OnboardingScreen
import com.gymtracker.app.presentation.profile.ProfileScreen
import com.gymtracker.app.presentation.progress.ProgressScreen
import com.gymtracker.app.presentation.theme.GymTrackerTheme
import com.gymtracker.app.presentation.workouts.WorkoutsScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: GymRepository,
) : ViewModel() {
    val profile: StateFlow<UserProfileEntity?> = repository.observeUserProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeSession: StateFlow<WorkoutSessionEntity?> = repository.observeActiveSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            runCatching { repository.seedInitialData() }
                .onFailure { android.util.Log.e("AppViewModel", "Initial seeding failed", it) }
        }
    }
}

@Composable
fun GymTrackerRoot(viewModel: AppViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()

    GymTrackerTheme(themeMode = profile?.themeMode ?: ThemeMode.SYSTEM) {
        val loadedProfile = profile
        if (loadedProfile == null) {
            Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            GymTrackerNav(profile = loadedProfile, activeSession = activeSession)
        }
    }
}

@Composable
private fun GymTrackerNav(
    profile: UserProfileEntity,
    activeSession: WorkoutSessionEntity?,
) {
    val navController = rememberNavController()
    val startDestination = if (profile.onboardingComplete) TodayRoute else OnboardingRoute
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = destination.shouldShowBottomBar()

    Scaffold(
        bottomBar = {
            Column {
                if (showBottomBar && activeSession != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate(ActiveWorkoutRoute(sessionId = activeSession.id)) }
                            .testTag("active_session_resume_bar"),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                ) {
                                    Text(
                                        "ACTIVE",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    )
                                }
                                Column {
                                    Text(
                                        activeSession.workoutName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        "Workout in progress • Tap to resume",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    )
                                }
                            }
                            Button(
                                onClick = { navController.navigate(ActiveWorkoutRoute(sessionId = activeSession.id)) },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Resume", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (showBottomBar) {
                    NavigationBar {
                        bottomItems.forEach { item ->
                            val selected = destination?.route?.contains(item.routeName) == true ||
                                (item.routeName == "TodayRoute" && destination?.route?.contains("DashboardRoute") == true) ||
                                (item.routeName == "TrainRoute" && destination?.route?.contains("WorkoutsRoute") == true) ||
                                (item.routeName == "YouRoute" && destination?.route?.contains("ProfileRoute") == true)

                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(onDone = {
                    navController.navigate(TodayRoute) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                })
            }
            composable<TodayRoute> {
                DashboardScreen(
                    onOpenWorkout = { sessionId -> navController.navigate(ActiveWorkoutRoute(sessionId = sessionId)) },
                    onStartWorkout = { workoutId -> navController.navigate(ActiveWorkoutRoute(workoutId = workoutId)) },
                    onOpenWorkouts = { navController.navigate(TrainRoute) },
                    onOpenGemini = { navController.navigate(GeminiCoachRoute) },
                    onOpenHistory = { navController.navigate(HistoryRoute) },
                    onOpenNutrition = { navController.navigate(NutritionRoute) },
                )
            }
            composable<TrainRoute> {
                WorkoutsScreen(
                    onStartWorkout = { workoutId -> navController.navigate(ActiveWorkoutRoute(workoutId = workoutId)) },
                    onOpenGemini = { navController.navigate(GeminiCoachRoute) },
                )
            }
            composable<HistoryRoute> {
                WorkoutHistoryScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable<ActiveWorkoutRoute> { entry ->
                val route = entry.toRoute<ActiveWorkoutRoute>()
                ActiveWorkoutRouteScreen(
                    route = route,
                    onDone = {
                        navController.navigate(TodayRoute) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<ProgressRoute> {
                ProgressScreen(
                    onOpenNutrition = { navController.navigate(NutritionRoute) },
                )
            }
            composable<YouRoute> {
                ProfileScreen(
                    onOpenNutrition = { navController.navigate(NutritionRoute) },
                )
            }
            composable<NutritionRoute> {
                NutritionScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable<GeminiCoachRoute> {
                GeminiCoachScreen(
                    onNavigateToActiveWorkout = { sessionId ->
                        navController.navigate(ActiveWorkoutRoute(sessionId = sessionId))
                    },
                    onNavigateToWorkoutsList = {
                        navController.navigate(TrainRoute) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}

private data class BottomItem(
    val label: String,
    val routeName: String,
    val route: Any,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val bottomItems = listOf(
    BottomItem("Today", "TodayRoute", TodayRoute, Icons.Outlined.Today),
    BottomItem("Train", "TrainRoute", TrainRoute, Icons.Outlined.FitnessCenter),
    BottomItem("Progress", "ProgressRoute", ProgressRoute, Icons.Outlined.Analytics),
    BottomItem("You", "YouRoute", YouRoute, Icons.Outlined.Person),
)

private fun NavDestination?.shouldShowBottomBar(): Boolean {
    val route = this?.route.orEmpty()
    return route.isNotBlank() &&
        !route.contains("OnboardingRoute") &&
        !route.contains("ActiveWorkoutRoute")
}
