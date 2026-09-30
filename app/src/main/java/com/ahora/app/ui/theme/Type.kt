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
 * - [Typography.labelSmall]: metadatos pequeños (texto dentro de las pills).
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
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)

/**
 * Escala de fuente del sistema a partir de la cual la marca deja de crecer.
 *
 * Por encima de este valor (tamaños "Grande"/"Enorme" de accesibilidad), la
 * marca conservaría su tamaño diseñado en vez de escalar con el sistema.
 */
const val BRAND_FIXED_ABOVE_FONT_SCALE = 1.3f

/**
 * Estilo de la marca "AHORA" según el contexto.
 *
 * La marca es identidad, no contenido: cuando el encabezado es compacto
 * (pantallas bajas, p. ej. teléfono en horizontal) y la fuente del sistema
 * es muy grande (accesibilidad), la marca conserva sus 40dp visuales
 * diseñados en vez de crecer con la escala. El contenido sí escala; el
 * logo, no: la identidad se ve siempre igual.
 *
 * Función pura para poder probarla en JVM.
 */
fun brandTextStyle(
    typography: Typography,
    compactHeader: Boolean,
    fontScale: Float,
): TextStyle {
    val base = typography.displayLarge
    if (!compactHeader || fontScale <= BRAND_FIXED_ABOVE_FONT_SCALE) return base
    return base.copy(
        fontSize = base.fontSize / fontScale,
        letterSpacing = base.letterSpacing / fontScale,
    )
}
