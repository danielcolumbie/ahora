package com.ahora.app.ui.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Umbrales y decisiones de layout adaptable (BLOQUE J del rediseño
 * premium, FASE 15).
 *
 * Centraliza los números que antes estarían sueltos en las pantallas:
 * si el punto de corte de dos paneles cambia, se cambia aquí. Las
 * decisiones son funciones puras sobre [Dp] para poder probarlas en la
 * JVM sin un dispositivo (`AdaptiveLayoutTest`).
 *
 * Sin dependencias nuevas: se usa `BoxWithConstraints` en las pantallas
 * (mide el espacio real disponible, no el tamaño físico del equipo) en
 * vez de `WindowSizeClass` (que exigiría añadir el artefacto
 * `material3-window-size-class`).
 */
object AdaptiveLayout {

    /**
     * Ancho mínimo para el layout de dos paneles (lista + detalle).
     * 600dp es el corte clásico entre teléfono y tablet; por debajo,
     * una sola columna + diálogos como hasta ahora.
     */
    val twoPaneMinWidth: Dp = 600.dp

    /**
     * Altura máxima bajo la cual el encabezado de la pantalla se
     * compacta (se oculta el lema y se reducen los espacios). Cubre el
     * landscape en teléfonos (~360dp de alto): la lista y la creación
     * siguen visibles sin pelear por el espacio vertical.
     */
    val compactHeaderMaxHeight: Dp = 480.dp

    /**
     * Ancho máximo del contenido del panel de detalle en dos paneles.
     * En tablets muy anchas el formulario no se estira hasta el borde:
     * queda una columna de lectura cómoda, alineada al inicio.
     */
    val detailPaneMaxWidth: Dp = 560.dp

    /**
     * Ancho máximo del contenido de pantallas de una sola columna
     * (Ajustes). En tablets las filas no se estiran a lo ancho de la
     * pantalla: quedan centradas en una columna legible. En teléfonos
     * (< 720dp) no tiene efecto.
     */
    val singleColumnMaxWidth: Dp = 720.dp

    /**
     * `true` si el ancho disponible admite dos paneles (lista + detalle).
     * El punto de corte es inclusivo: a 600dp exactos ya hay dos paneles.
     */
    fun isTwoPane(maxWidth: Dp): Boolean = maxWidth >= twoPaneMinWidth

    /**
     * `true` si la altura disponible es tan baja que conviene compactar
     * el encabezado (típico landscape de teléfono).
     */
    fun isCompactHeader(maxHeight: Dp): Boolean = maxHeight < compactHeaderMaxHeight
}
