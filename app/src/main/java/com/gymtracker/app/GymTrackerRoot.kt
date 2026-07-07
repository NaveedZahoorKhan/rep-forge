package com.gymtracker.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.gymtracker.app.domain.repository.GymRepository
import com.gymtracker.app.presentation.active.ActiveWorkoutRouteScreen
import com.gymtracker.app.presentation.dashboard.DashboardScreen
import com.gymtracker.app.presentation.navigation.ActiveWorkoutRoute
import com.gymtracker.app.presentation.navigation.DashboardRoute
import com.gymtracker.app.presentation.navigation.OnboardingRoute
import com.gymtracker.app.presentation.navigation.ProfileRoute
import com.gymtracker.app.presentation.navigation.ProgressRoute
import com.gymtracker.app.presentation.navigation.WorkoutsRoute
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

    init {
        viewModelScope.launch { repository.seedInitialData() }
    }
}

@Composable
fun GymTrackerRoot(viewModel: AppViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    GymTrackerTheme(themeMode = profile?.themeMode ?: ThemeMode.SYSTEM) {
        val loadedProfile = profile
        if (loadedProfile == null) {
            Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            GymTrackerNav(profile = loadedProfile)
        }
    }
}

@Composable
private fun GymTrackerNav(profile: UserProfileEntity) {
    val navController = rememberNavController()
    val startDestination = if (profile.onboardingComplete) DashboardRoute else OnboardingRoute
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = destination.shouldShowBottomBar()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = destination?.route?.contains(item.routeName) == true,
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
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding),
        ) {
            composable<OnboardingRoute> {
                OnboardingScreen(onDone = {
                    navController.navigate(DashboardRoute) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                })
            }
            composable<DashboardRoute> {
                DashboardScreen(
                    onOpenWorkout = { sessionId -> navController.navigate(ActiveWorkoutRoute(sessionId = sessionId)) },
                    onStartWorkout = { workoutId -> navController.navigate(ActiveWorkoutRoute(workoutId = workoutId)) },
                    onOpenWorkouts = { navController.navigate(WorkoutsRoute) },
                )
            }
            composable<WorkoutsRoute> {
                WorkoutsScreen(
                    onStartWorkout = { workoutId -> navController.navigate(ActiveWorkoutRoute(workoutId = workoutId)) },
                )
            }
            composable<ActiveWorkoutRoute> { entry ->
                val route = entry.toRoute<ActiveWorkoutRoute>()
                ActiveWorkoutRouteScreen(
                    route = route,
                    onDone = {
                        navController.navigate(DashboardRoute) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable<ProgressRoute> { ProgressScreen() }
            composable<ProfileRoute> { ProfileScreen() }
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
    BottomItem("Dashboard", "DashboardRoute", DashboardRoute, Icons.Outlined.Home),
    BottomItem("Workouts", "WorkoutsRoute", WorkoutsRoute, Icons.Outlined.FitnessCenter),
    BottomItem("Progress", "ProgressRoute", ProgressRoute, Icons.Outlined.Analytics),
    BottomItem("Profile", "ProfileRoute", ProfileRoute, Icons.Outlined.Person),
)

private fun NavDestination?.shouldShowBottomBar(): Boolean {
    val route = this?.route.orEmpty()
    return route.isNotBlank() &&
        !route.contains("OnboardingRoute") &&
        !route.contains("ActiveWorkoutRoute")
}
