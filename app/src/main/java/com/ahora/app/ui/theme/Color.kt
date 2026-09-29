package com.ahora.app.ui.theme

import androidx.compose.ui.graphics.Color

// Azul eléctrico: el único acento de color. Se usa con moderación.
val ElectricBlue = Color(0xFF3D7BFF)
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
val LightOnSurfaceVariant = Color(0xFF6E6C66)

// Divisores sutiles: separan sin encerrar. En claro, tinta al 8%;
// en oscuro, papel al 10%. No son "bordes", son aire estructurado.
val LightDivider = Color(0x14171714)
val DarkDivider = Color(0x1AF5F3EE)

// Estado de error (snackbars y avisos críticos). Se usa poco y solo
// donde comunica un problema real.
val LightError = Color(0xFFB3261E)
val DarkError = Color(0xFFF2B8B5)
