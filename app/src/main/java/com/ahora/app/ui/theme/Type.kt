package com.ahora.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Jerarquía tipográfica de Ahora: pocos tamaños, pocos pesos, cada uno con
 * un rol fijo. La sofisticación viene de la proporción y la consistencia,
 * no de variar el estilo en cada pantalla.
 *
 * Roles:
 * - [Typography.displayLarge]: marca "AHORA" en la pantalla principal.
 * - [Typography.titleLarge]: títulos de pantalla ("Todas", "Ajustes").
 * - [Typography.titleMedium]: etiquetas de sección en mayúsculas
 *   ("HOY", "APARIENCIA", "RESPALDO"). Siempre con texto en mayúsculas.
 * - [Typography.bodyLarge]: título de la tarea (el contenido protagonista).
 * - [Typography.bodyMedium]: información secundaria (subtítulos, descripciones).
 * - [Typography.labelLarge]: acciones y botones.
 *
 * Tipografía limpia del sistema, sin fuentes externas.
 */
val AhoraTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        letterSpacing = (-0.25).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 1.5.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)
