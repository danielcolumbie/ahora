package com.ahora.app.ui.theme

import androidx.compose.ui.graphics.Color

// Azul eléctrico: el único acento de color. Se usa con moderación.
// Bloque G (dark mode, 1.20.0): en claro pasó de #3D7BFF a #2F63E4. El
// anterior daba 3.49:1 con el papel encima (texto del botón «Añadir tarea»
// y «Escuchando…» bajo AA); con #2F63E4 da 4.74:1. Mismo tono eléctrico,
// un paso más profundo. En oscuro se conserva #5B93FF (6.61:1).
val ElectricBlue = Color(0xFF2F63E4)
val ElectricBlueDark = Color(0xFF5B93FF)

// Modo oscuro (apariencia principal): negro casi puro, superficies apenas más claras.
val DarkBackground = Color(0xFF0B0B0D)
val DarkSurface = Color(0xFF141417)
val DarkSurfaceVariant = Color(0xFF1D1D22)
val DarkOnBackground = Color(0xFFF5F3EE)
val DarkOnSurface = Color(0xFFF5F3EE)
val DarkOnSurfaceVariant = Color(0xFFA8A6A0)

// Modo claro.
val LightBackground = Color(0xFFF6F4EE)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEDEAE2)
val LightOnBackground = Color(0xFF171714)
val LightOnSurface = Color(0xFF171714)
// Bloque G (dark mode, 1.20.0): antes #6E6C66; el texto de las pills
// (surfaceVariant #EDEAE2) daba 4.37:1 y quedaba por debajo de AA. Con
// #625F58 da 5.30:1 sobre la pill y 5.79:1 sobre el fondo. Sigue siendo
// el mismo gris cálido, solo un paso más oscuro.
val LightOnSurfaceVariant = Color(0xFF625F58)

// Divisores sutiles: separan sin encerrar. En claro, tinta al 8%;
// en oscuro, papel al 10%. No son "bordes", son aire estructurado.
val LightDivider = Color(0x14171714)
val DarkDivider = Color(0x1AF5F3EE)

// Estado de error (snackbars y avisos críticos). Se usa poco y solo
// donde comunica un problema real.
val LightError = Color(0xFFB3261E)
val DarkError = Color(0xFFF2B8B5)

// Contenedor del acento: la superficie que comunica "seleccionado"
// (indicador de la barra de navegación, bloque E). Un solo acento
// también aquí: el color comunica estado, no decora.
val LightPrimaryContainer = Color(0xFFDCE6FF)
val LightOnPrimaryContainer = Color(0xFF0B2A6B)
val DarkPrimaryContainer = Color(0xFF2A4A8F)
val DarkOnPrimaryContainer = Color(0xFFDCE6FF)
