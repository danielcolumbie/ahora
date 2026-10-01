package com.ahora.app.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Respaldo local de tareas en JSON.
 *
 * La app no usa la nube de Android para respaldos ([allowBackup=false]):
 * el usuario guarda y restaura su propio archivo con "Exportar"/"Importar"
 * en Ajustes. El formato es deliberadamente simple y legible.
 *
 * Usa `org.json`, que viene incluido en Android: cero dependencias nuevas.
 * En los tests JVM se usa el artefacto `org.json:json` (solo tests),
 * porque el `android.jar` de la JVM trae stubs.
 *
 * La importación es idempotente: conserva los ids, así que importar el
 * mismo archivo dos veces no duplica tareas (el DAO hace REPLACE).
 */
object TaskBackup {

    const val FORMAT = "ahora-backup"

    /**
     * Versión del formato. La 2 (1.28.0) añade las etiquetas: un arreglo
     * opcional "tags" y, por tarea, un arreglo opcional "tags" con los
     * nombres. Los respaldos v1 se siguen leyendo (sin etiquetas).
     */
    const val VERSION = 2

    class BackupFormatException(message: String, cause: Throwable? = null) :
        Exception(message, cause)

    /**
     * [tags] y [tagAssignments] (taskId → nombres de etiqueta) solo vienen
     * en respaldos v2; en v1 quedan vacíos.
     */
    data class ParsedBackup(
        val tasks: List<Task>,
        val skipped: Int,
        val tags: List<Tag> = emptyList(),
        val tagAssignments: Map<Long, List<String>> = emptyMap()
    )

    /** Respaldo sin etiquetas (los tests y los llamadores que no las manejan). */
    fun tasksToJson(tasks: List<Task>, exportedAt: Long = System.currentTimeMillis()): String =
        tasksToJson(tasks, emptyList(), emptyMap(), exportedAt)

    /**
     * Respaldo completo: tareas + etiquetas. Las etiquetas se guardan por
     * nombre (legible y sin depender de los ids autogenerados); cada tarea
     * lista los nombres de sus etiquetas.
     */
    fun tasksToJson(
        tasks: List<Task>,
        tags: List<Tag>,
        taskTags: Map<Long, List<Tag>>,
        exportedAt: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", exportedAt)
        val tagArray = JSONArray()
        tags.forEach { tag ->
            tagArray.put(
                JSONObject()
                    .put("name", tag.name)
                    .put("colorIndex", tag.colorIndex)
            )
        }
        root.put("tags", tagArray)
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(taskToJson(task, taskTags[task.id].orEmpty().map { it.name }))
        }
        root.put("tasks", array)
        return root.toString()
    }

    private fun taskToJson(task: Task, tagNames: List<String> = emptyList()): JSONObject {
        val obj = JSONObject()
            .put("id", task.id)
            .put("title", task.title)
            .put("createdAt", task.createdAt)
            .put("reminderAt", task.reminderAt ?: JSONObject.NULL)
            .put("isDone", task.isDone)
            .put("doneAt", task.doneAt ?: JSONObject.NULL)
            .put("recurrence", task.recurrence ?: JSONObject.NULL)
            // ETAPA 10: prioridad y fecha límite. Se siguen leyendo con `opt`
            // para que los respaldos hechos antes de la 1.9.0 (sin estos campos)
            // se importen sin errores, con valores por defecto.
            .put("priority", task.priority)
            .put("dueAt", task.dueAt ?: JSONObject.NULL)
        // 1.28.0 (formato v2): etiquetas por nombre. En v1 no existían y el
        // arreglo simplemente no se escribe.
        if (tagNames.isNotEmpty()) {
            val names = JSONArray()
            tagNames.forEach { names.put(it) }
            obj.put("tags", names)
        }
        return obj
    }

    /**
     * Lee un respaldo. Lanza [BackupFormatException] si el archivo no es
     * un respaldo válido de Ahora. Las tareas individuales inválidas
     * (p. ej. título vacío tras editar el archivo a mano) se omiten y se
     * cuentan en [ParsedBackup.skipped] en vez de abortar todo.
     */
    fun tasksFromJson(json: String): ParsedBackup {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw BackupFormatException("El archivo no es un JSON válido", e)
        }
        if (root.optString("format") != FORMAT) {
            throw BackupFormatException("No es un archivo de respaldo de Ahora")
        }
        // v1 y v2 son legibles: la v1 simplemente no trae etiquetas.
        val version = root.optInt("version", -1)
        if (version != 1 && version != VERSION) {
            throw BackupFormatException("Versión de respaldo no soportada: $version")
        }
        val array = root.optJSONArray("tasks")
            ?: throw BackupFormatException("El respaldo no contiene tareas")
        val tags = parseTags(root.optJSONArray("tags"))
        val tasks = mutableListOf<Task>()
        val assignments = mutableMapOf<Long, MutableList<String>>()
        var skipped = 0
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i)
            val task = obj?.let(::taskFromJson)
            if (task == null) {
                skipped++
            } else {
                tasks.add(task)
                obj.optJSONArray("tags")?.let { names ->
                    val list = mutableListOf<String>()
                    for (j in 0 until names.length()) {
                        names.optString(j).takeIf { it.isNotBlank() }?.let { list.add(it) }
                    }
                    if (list.isNotEmpty()) assignments[task.id] = list
                }
            }
        }
        return ParsedBackup(tasks, skipped, tags, assignments)
    }

    /**
     * Lee el arreglo "tags" de un respaldo v2. Los nombres se limpian con
     * [sanitizeTagName] y se deduplican (insensible a mayúsculas, se queda
     * la primera): un archivo editado a mano no debe crear etiquetas
     * duplicadas ni sin nombre. Los ids se ignoran (se regeneran al
     * importar).
     */
    private fun parseTags(array: JSONArray?): List<Tag> {
        if (array == null) return emptyList()
        val seen = mutableSetOf<String>()
        val tags = mutableListOf<Tag>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val name = sanitizeTagName(obj.optString("name")) ?: continue
            if (!seen.add(name.lowercase())) continue
            tags.add(Tag(name = name, colorIndex = TagPalette.sanitizeColorIndex(obj.optInt("colorIndex", 0))))
        }
        return tags
    }

    private fun taskFromJson(obj: JSONObject): Task? {
        val title = obj.optString("title").trim()
        if (title.isEmpty()) return null
        return Task(
            id = obj.optLong("id", 0),
            title = title,
            createdAt = obj.optLong("createdAt", -1).takeIf { it > 0 }
                ?: System.currentTimeMillis(),
            reminderAt = obj.optLong("reminderAt", -1).takeIf { it > 0 },
            isDone = obj.optBoolean("isDone", false),
            doneAt = obj.optLong("doneAt", -1).takeIf { it > 0 },
            recurrence = obj.optString("recurrence").ifBlank { null },
            priority = TaskPriority.fromLevel(obj.optInt("priority", 0)).level,
            dueAt = obj.optLong("dueAt", -1).takeIf { it > 0 }
        )
    }
}
