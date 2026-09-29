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
    const val VERSION = 1

    class BackupFormatException(message: String, cause: Throwable? = null) :
        Exception(message, cause)

    data class ParsedBackup(val tasks: List<Task>, val skipped: Int)

    fun tasksToJson(tasks: List<Task>, exportedAt: Long = System.currentTimeMillis()): String {
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", exportedAt)
        val array = JSONArray()
        tasks.forEach { array.put(taskToJson(it)) }
        root.put("tasks", array)
        return root.toString()
    }

    private fun taskToJson(task: Task): JSONObject = JSONObject()
        .put("id", task.id)
        .put("title", task.title)
        .put("createdAt", task.createdAt)
        .put("reminderAt", task.reminderAt ?: JSONObject.NULL)
        .put("isDone", task.isDone)
        .put("doneAt", task.doneAt ?: JSONObject.NULL)
        .put("recurrence", task.recurrence ?: JSONObject.NULL)

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
        val version = root.optInt("version", -1)
        if (version != VERSION) {
            throw BackupFormatException("Versión de respaldo no soportada: $version")
        }
        val array = root.optJSONArray("tasks")
            ?: throw BackupFormatException("El respaldo no contiene tareas")
        val tasks = mutableListOf<Task>()
        var skipped = 0
        for (i in 0 until array.length()) {
            val task = array.optJSONObject(i)?.let(::taskFromJson)
            if (task == null) skipped++ else tasks.add(task)
        }
        return ParsedBackup(tasks, skipped)
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
            recurrence = obj.optString("recurrence").ifBlank { null }
        )
    }
}
