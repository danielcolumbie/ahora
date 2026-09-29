package com.ahora.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import com.ahora.app.ui.theme.Motion

object Routes {
    const val HOY = "hoy"
    const val TODAS = "todas"
    const val AJUSTES = "ajustes"
}

private data class Destination(
    val route: String,
    val label: String,
    /** Relleno: comunica "seleccionado" junto al indicador de acento. */
    val iconSelected: ImageVector,
    /** Outlined: el destino sin seleccionar no compite por atención. */
    val iconUnselected: ImageVector
)

private val destinations = listOf(
    Destination(Routes.HOY, "Hoy", Icons.Filled.Today, Icons.Outlined.Today),
    Destination(
        Routes.TODAS, "Todas",
        Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List
    ),
    Destination(
        Routes.AJUSTES, "Ajustes",
        Icons.Filled.Settings, Icons.Outlined.Settings
    )
)

/**
 * Navegación mínima: Hoy, Todas y Ajustes. Nada más.
 *
 * Patrón (bloque E): la barra inferior es un `NavigationBar` de Material 3
 * con el acento propio de Ahora. La selección se comunica con el indicador
 * `primaryContainer` + el icono relleno; las etiquetas quedan neutras
 * (`onSurface`/`onSurfaceVariant`): el color solo comunica la selección.
 * `singleTop` + `saveState`/`restoreState` conservan el scroll y el estado
 * de cada pestaña al volver. Las transiciones salen del objeto `Motion`
 * centralizado (FASE 13: sin valores sueltos).
 */
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
                val scheme = MaterialTheme.colorScheme
                // Un solo acento: el indicador comunica la selección; el
                // resto queda neutro. Sin esto, M3 usaría secondaryContainer
                // (tono por defecto ajeno a la paleta de Ahora).
                val itemColors = NavigationBarItemDefaults.colors(
                    selectedIconColor = scheme.onPrimaryContainer,
                    selectedTextColor = scheme.onSurface,
                    indicatorColor = scheme.primaryContainer,
                    unselectedIconColor = scheme.onSurfaceVariant,
                    unselectedTextColor = scheme.onSurfaceVariant
                )
                destinations.forEach { dest ->
                    val selected =
                        current?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        // El icono es decorativo: la etiqueta ya nombra el
                        // destino (evita que TalkBack lo anuncie dos veces).
                        icon = {
                            Icon(
                                imageVector = if (selected) dest.iconSelected
                                else dest.iconUnselected,
                                contentDescription = null
                            )
                        },
                        label = { Text(dest.label) },
                        colors = itemColors
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
            composable(
                route = Routes.HOY,
                enterTransition = { Motion.screenEnter() },
                exitTransition = { Motion.screenExit() },
                popEnterTransition = { Motion.screenEnter(reverse = true) },
                popExitTransition = { Motion.screenExit(reverse = true) }
            ) { HomeScreen(mainViewModel) }
            composable(
                route = Routes.TODAS,
                enterTransition = { Motion.screenEnter() },
                exitTransition = { Motion.screenExit() },
                popEnterTransition = { Motion.screenEnter(reverse = true) },
                popExitTransition = { Motion.screenExit(reverse = true) }
            ) { AllTasksScreen(mainViewModel) }
            composable(
                route = Routes.AJUSTES,
                enterTransition = { Motion.screenEnter() },
                exitTransition = { Motion.screenExit() },
                popEnterTransition = { Motion.screenEnter(reverse = true) },
                popExitTransition = { Motion.screenExit(reverse = true) }
            ) { SettingsScreen(settingsViewModel) }
        }
    }
}
