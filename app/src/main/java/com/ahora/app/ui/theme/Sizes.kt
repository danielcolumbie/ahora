package com.ahora.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Dimensiones de componentes (BLOQUE A del rediseño premium).
 *
 * Igual que [Spacing], centraliza los tamaños que antes estaban como
 * valores `dp` sueltos en los componentes: si el círculo de completado
 * cambia de tamaño, se cambia aquí y no en cinco archivos.
 */
object Sizes {

    /** Círculo de completado sin marcar. */
    val checkCircle = 24.dp

    /** Círculo de completado marcado (ligeramente mayor: comunica el estado). */
    val checkCircleChecked = 27.dp

    /** Grosor del borde del círculo de completado. */
    val checkStroke = 2.dp

    /** Check dibujado dentro del círculo. */
    val checkIcon = 15.dp

    /** Área táctil mínima de la fila de tarea (accesibilidad). */
    val minTouchRow = 48.dp

    /** Icono dentro de las pills de metadatos. */
    val pillIcon = 12.dp
}
