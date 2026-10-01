package com.ahora.app.data

/**
 * Paleta fija de colores de etiqueta (8 tonos).
 *
 * Los colores son fijos y no configurables a propósito: son tonos medios
 * que se distinguen bien tanto sobre el fondo claro como sobre el oscuro
 * (el punto de color de la etiqueta es decorativo — el nombre lleva el
 * significado — así que no se les exige contraste de texto). Con una
 * paleta cerrada la app conserva su identidad visual en vez de volverse
 * un arcoíris arbitrario.
 *
 * Todo en Kotlin puro (Int ARGB), sin Compose: se puede probar en JVM.
 */
object TagPalette {

    /** Colores como ARGB (0xAARRGGBB). */
    val COLORS: List<Int> = listOf(
        0xFFD84A3A.toInt(), // rojo
        0xFFE8830C.toInt(), // naranja
        0xFFC9A227.toInt(), // ámbar
        0xFF43A047.toInt(), // verde
        0xFF26A69A.toInt(), // turquesa
        0xFF1E88E5.toInt(), // azul
        0xFFAB47BC.toInt(), // violeta
        0xFFEC407A.toInt(), // rosa
    )

    val COUNT: Int get() = COLORS.size

    /**
     * Nombres de los colores para TalkBack (el selector de color del
     * diálogo de etiquetas los usa como etiquetas de accesibilidad).
     */
    val COLOR_NAMES: List<String> = listOf(
        "Rojo", "Naranja", "Ámbar", "Verde",
        "Turquesa", "Azul", "Violeta", "Rosa"
    )

    fun colorName(index: Int): String = COLOR_NAMES[sanitizeColorIndex(index)]

    fun isValid(index: Int): Boolean = index in COLORS.indices

    /**
     * Un índice fuera de rango cae al primero: una etiqueta siempre tiene
     * un color válido, nunca un índice roto.
     */
    fun sanitizeColorIndex(index: Int): Int = if (isValid(index)) index else 0

    fun colorFor(index: Int): Int = COLORS[sanitizeColorIndex(index)]
}

/** Máximo de caracteres del nombre de una etiqueta. */
const val TAG_NAME_MAX_LENGTH = 24

/**
 * Limpia el nombre de una etiqueta: recorta espacios y limita el largo.
 * Devuelve null si queda vacío (el llamador debe rechazarlo, no guardar
 * una etiqueta sin nombre). Función pura para poder probarla.
 */
fun sanitizeTagName(name: String): String? {
    val clean = name.trim().take(TAG_NAME_MAX_LENGTH).trim()
    return clean.ifEmpty { null }
}
