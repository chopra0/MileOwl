package com.mileowl.tracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mileowl.tracker.ui.home.HomeScreen
import com.mileowl.tracker.ui.locations.SavedLocationsScreen
import com.mileowl.tracker.ui.report.ReportScreen
import com.mileowl.tracker.ui.settings.SettingsScreen
import com.mileowl.tracker.ui.tripdetail.TripDetailScreen
import com.mileowl.tracker.ui.trips.TripsScreen

object NavRoutes {
    const val HOME = "home"
    const val TRIPS = "trips"
    const val TRIP_DETAIL = "trip_detail/{tripId}"
    const val LOCATIONS = "locations"
    const val REPORT = "report"
    const val SETTINGS = "settings"

    fun tripDetail(tripId: Long) = "trip_detail/$tripId"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(NavRoutes.TRIPS, "Trips", Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar),
    BottomNavItem(NavRoutes.LOCATIONS, "Locations", Icons.Filled.LocationOn, Icons.Outlined.LocationOn),
    BottomNavItem(NavRoutes.REPORT, "Reports", Icons.Filled.Summarize, Icons.Outlined.Summarize),
    BottomNavItem(NavRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun MileOwlNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(NavRoutes.HOME) {
                HomeScreen(
                    onNavigateToTrips = {
                        navController.navigate(NavRoutes.TRIPS)
                    },
                    onNavigateToTripDetail = { tripId ->
                        navController.navigate(NavRoutes.tripDetail(tripId))
                    }
                )
            }

            composable(NavRoutes.TRIPS) {
                TripsScreen(
                    onTripClick = { tripId ->
                        navController.navigate(NavRoutes.tripDetail(tripId))
                    }
                )
            }

            composable(
                route = NavRoutes.TRIP_DETAIL,
                arguments = listOf(navArgument("tripId") { type = NavType.LongType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getLong("tripId") ?: return@composable
                TripDetailScreen(
                    tripId = tripId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(NavRoutes.LOCATIONS) {
                SavedLocationsScreen()
            }

            composable(NavRoutes.REPORT) {
                ReportScreen()
            }

            composable(NavRoutes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}
