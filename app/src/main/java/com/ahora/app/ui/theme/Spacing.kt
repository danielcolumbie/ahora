package com.ahora.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Escala de espaciado de la app: la única fuente de verdad para paddings,
 * separaciones y márgenes. Nada en la UI usa valores `dp` sueltos: todo
 * viene de aquí para que el ritmo visual sea consistente.
 *
 * La escala es estrictamente creciente (ver [SpacingTest]): si un valor
 * nuevo no respeta el orden, el test lo detecta.
 */
object Spacing {
    /** 2dp — respiro mínimo dentro de elementos compactos (pills, chips). */
    val xxs: Dp = 2.dp

    /** 4dp — separación mínima entre elementos relacionados. */
    val xs: Dp = 4.dp

    /** 8dp — separación base entre elementos de un mismo grupo. */
    val s: Dp = 8.dp

    /** 12dp — separación cómoda dentro de filas y tarjetas. */
    val m: Dp = 12.dp

    /** 16dp — separación entre bloques relacionados. */
    val l: Dp = 16.dp

    /** 20dp — margen horizontal de las pantallas y padding de contenido. */
    val xl: Dp = 20.dp

    /** 24dp — separación entre secciones distintas. */
    val xxl: Dp = 24.dp

    /** 32dp — separación generosa (estados vacíos, transiciones de contexto). */
    val xxxl: Dp = 32.dp

    /** Margen horizontal estándar del contenido de pantalla. */
    val screenHorizontal: Dp = xl

    /** Altura mínima de las filas táctiles de ajustes y diálogos. */
    val minTouchRow: Dp = 48.dp
}
