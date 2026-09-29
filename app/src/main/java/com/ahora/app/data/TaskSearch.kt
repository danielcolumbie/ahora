package com.ahora.app.data

/**
 * Construye el patrón LIKE para buscar tareas por título.
 *
 * El texto del usuario puede contener los comodines de LIKE (`%`, `_`);
 * se escapan con `\` (la consulta SQL declara `ESCAPE '\'`) para que se
 * busquen literalmente y no actúen como comodines. Sin este escape, buscar
 * "100%" devolvería cualquier tarea con "100" seguido de lo que sea.
 *
 * La búsqueda es insensible a mayúsculas (LIKE de SQLite lo es para ASCII).
 * Limitación conocida: las tildes no se normalizan ("cafe" no encuentra
 * "café"); hacerlo exigiría FTS o una columna normalizada, un coste que no
 * se justifica para el volumen de tareas de esta app.
 */
fun buildSearchPattern(query: String): String {
    val escaped = query.trim()
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")
    return "%$escaped%"
}
