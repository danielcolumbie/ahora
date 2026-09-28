package com.ahora.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ahora.app.ui.MainViewModel
import com.ahora.app.ui.screens.AllTasksScreen
import com.ahora.app.ui.screens.HomeScreen
import com.ahora.app.ui.settings.SettingsScreen
import com.ahora.app.ui.settings.SettingsViewModel

object Routes {
    const val HOY = "hoy"
    const val TODAS = "todas"
    const val AJUSTES = "ajustes"
}

private data class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val destinations = listOf(
    Destination(Routes.HOY, "Hoy", Icons.Filled.Today),
    Destination(Routes.TODAS, "Todas", Icons.AutoMirrored.Filled.List),
    Destination(Routes.AJUSTES, "Ajustes", Icons.Filled.Settings)
)

/** Navegación mínima: Hoy, Todas y Ajustes. Nada más. */
@Composable
fun NavGraph(
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val current = backStackEntry?.destination
                destinations.forEach { dest ->
                    NavigationBarItem(
                        selected = current?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOY,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOY) { HomeScreen(mainViewModel) }
            composable(Routes.TODAS) { AllTasksScreen(mainViewModel) }
            composable(Routes.AJUSTES) { SettingsScreen(settingsViewModel) }
        }
    }
}
