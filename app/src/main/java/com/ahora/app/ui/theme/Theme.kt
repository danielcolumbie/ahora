package com.ahora.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = ElectricBlueDark,
    onPrimary = DarkOnBackground,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    // Contenedores de superficie (auditoría 1.25.0): los diálogos
    // (AlertDialog, DatePickerDialog) y los menús (DropdownMenu) de M3
    // usan `surfaceContainerHigh` y `surfaceContainer` por defecto; sin
    // definirlos caían a un gris frío ajeno a la paleta de Ahora. El
    // design system dice que diálogos y menús viven en `surface`.
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurface,
    outlineVariant = DarkDivider,
    error = DarkError
)

private val LightColors = lightColorScheme(
    primary = ElectricBlue,
    onPrimary = LightBackground,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    // Ver el comentario en DarkColors: diálogos y menús en `surface`,
    // no en el gris frío por defecto de M3.
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurface,
    outlineVariant = LightDivider,
    error = LightError
)

/**
 * @param themeMode 0 = automática, 1 = claro, 2 = oscuro (ver [com.ahora.app.data.ThemeMode]).
 */
@Composable
fun AhoraTheme(
    themeMode: Int = 0,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AhoraTypography,
        content = content
    )
}
