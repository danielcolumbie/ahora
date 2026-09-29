package com.ahora.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ahora.app.data.TaskPriority
import com.ahora.app.data.TaskRecurrence
import com.ahora.app.domain.parseNaturalLanguage
import java.time.ZoneId

/**
 * Valores listos para guardar una tarea nueva: salen de
 * [CreationDraftState.consumeForSave], que además vacía el borrador.
 */
data class CreationValues(
    val title: String,
    val priority: TaskPriority,
    val dueAt: Long?,
    val recurrence: TaskRecurrence,
    val reminderAt: Long?
)

/**
 * Foto del borrador para que el diálogo de opciones avanzadas pueda
 * cancelarse sin dejar rastro (bloque C): se restaura tal cual estaba.
 */
data class CreationSnapshot(
    val priority: TaskPriority,
    val dueAt: Long?,
    val recurrence: TaskRecurrence,
    val reminderAt: Long?,
    val manualPriority: Boolean,
    val manualDueAt: Boolean,
    val manualRecurrence: Boolean,
    val manualReminder: Boolean
)

/**
 * Borrador de la creación rápida: un solo holder de estado en vez de los
 * ~12 `rememberSaveable` que HomeScreen manejaba sueltos (bloque C).
 *
 * - [applyText]: cada cambio en el texto (escrito o dictado) se analiza
 *   con el parser de lenguaje natural y lo detectado alimenta los campos,
 *   salvo que el usuario ya hubiera tocado ese campo a mano: lo manual
 *   siempre gana. Al vaciar el texto se rearman las marcas manuales.
 * - Los cambios manuales (diálogo de opciones avanzadas) se hacen con los
 *   setters `set*Manual`, que marcan el campo para que el parser no lo pise.
 * - [consumeForSave]: devuelve los valores limpios y vacía el borrador.
 * - [snapshot]/[restore]: el diálogo de opciones puede cancelarse y dejar
 *   el borrador como estaba.
 *
 * Lógica pura salvo los holders de Compose: testeable en JVM.
 * [now] y [zone] son inyectables para tests deterministas.
 */
class CreationDraftState(
    private val now: () -> Long = System::currentTimeMillis,
    private val zone: ZoneId = ZoneId.systemDefault()
) {
    var text by mutableStateOf("")
    var priority by mutableStateOf(TaskPriority.NONE)
    var dueAt by mutableStateOf<Long?>(null)
    var recurrence by mutableStateOf(TaskRecurrence.NONE)
    var reminderAt by mutableStateOf<Long?>(null)
    var manualPriority by mutableStateOf(false)
    var manualDueAt by mutableStateOf(false)
    var manualRecurrence by mutableStateOf(false)
    var manualReminder by mutableStateOf(false)

    fun applyText(newText: String) {
        text = newText
        if (newText.isBlank()) {
            manualPriority = false
            manualDueAt = false
            manualRecurrence = false
            manualReminder = false
        }
        val parsed = parseNaturalLanguage(newText, now(), zone)
        if (!manualPriority) priority = parsed.priority
        if (!manualDueAt) dueAt = parsed.dueDate?.let { startOfLocalDateMillis(it, zone) }
        if (!manualRecurrence) recurrence = parsed.recurrence
        if (!manualReminder) reminderAt = parsed.reminderAt
    }

    fun setPriorityManual(value: TaskPriority) {
        priority = value
        manualPriority = true
    }

    fun setDueAtManual(value: Long?) {
        dueAt = value
        manualDueAt = true
    }

    fun setRecurrenceManual(value: TaskRecurrence) {
        recurrence = value
        manualRecurrence = true
    }

    fun setReminderAtManual(value: Long?) {
        reminderAt = value
        manualReminder = true
    }

    fun snapshot(): CreationSnapshot = CreationSnapshot(
        priority = priority,
        dueAt = dueAt,
        recurrence = recurrence,
        reminderAt = reminderAt,
        manualPriority = manualPriority,
        manualDueAt = manualDueAt,
        manualRecurrence = manualRecurrence,
        manualReminder = manualReminder
    )

    fun restore(snapshot: CreationSnapshot) {
        priority = snapshot.priority
        dueAt = snapshot.dueAt
        recurrence = snapshot.recurrence
        reminderAt = snapshot.reminderAt
        manualPriority = snapshot.manualPriority
        manualDueAt = snapshot.manualDueAt
        manualRecurrence = snapshot.manualRecurrence
        manualReminder = snapshot.manualReminder
    }

    /**
     * Título limpio para guardar: lo detectado ("mañana", "urgente"…)
     * no queda pegado al nombre. Si el texto era solo lenguaje natural
     * ("mañana"), se guarda tal cual se escribió.
     */
    fun cleanTitle(): String {
        val parsed = parseNaturalLanguage(text, now(), zone)
        return parsed.title.ifBlank { text.trim() }
    }

    fun consumeForSave(): CreationValues {
        val values = CreationValues(
            title = cleanTitle(),
            priority = priority,
            dueAt = dueAt,
            recurrence = recurrence,
            reminderAt = reminderAt
        )
        clear()
        return values
    }

    fun clear() {
        text = ""
        priority = TaskPriority.NONE
        dueAt = null
        recurrence = TaskRecurrence.NONE
        reminderAt = null
        manualPriority = false
        manualDueAt = false
        manualRecurrence = false
        manualReminder = false
    }

    /**
     * Chips de feedback bajo la barra rápida: lo que la app entendió del
     * lenguaje natural o lo que el usuario configuró. El recordatorio
     * conserva la frase original ("Se pondrá para hoy · 15:00").
     */
    fun feedbackChips(): List<String> = buildList {
        if (priority != TaskPriority.NONE) add(priority.label)
        dueAt?.let { add("Vence: ${formatDueLabel(it, now())}") }
        reminderAt?.let { add("Se pondrá para ${formatReminderLabel(it, now())}") }
        if (recurrence != TaskRecurrence.NONE) add(recurrence.label)
    }
}

/**
 * Serialización del borrador para `rememberSaveable`: un solo estado
 * guardable en vez de doce sueltos. El borrador sobrevive a cambios de
 * configuración y a la muerte del proceso.
 */
internal fun saveCreationDraft(state: CreationDraftState): List<Any?> = listOf(
    state.text,
    state.priority.level,
    state.dueAt,
    state.recurrence.code,
    state.reminderAt,
    state.manualPriority,
    state.manualDueAt,
    state.manualRecurrence,
    state.manualReminder
)

internal fun restoreCreationDraft(saved: List<Any?>): CreationDraftState =
    CreationDraftState().apply {
        text = saved[0] as String
        priority = TaskPriority.fromLevel(saved[1] as Int)
        dueAt = saved[2] as Long?
        recurrence = TaskRecurrence.fromCode(saved[3] as String?)
        reminderAt = saved[4] as Long?
        manualPriority = saved[5] as Boolean
        manualDueAt = saved[6] as Boolean
        manualRecurrence = saved[7] as Boolean
        manualReminder = saved[8] as Boolean
    }

/** Borrador de creación con un único `rememberSaveable` (bloque C). */
@Composable
fun rememberCreationDraft(): CreationDraftState =
    rememberSaveable(
        saver = listSaver(
            save = { saveCreationDraft(it) },
            restore = { restoreCreationDraft(it) }
        )
    ) { CreationDraftState() }
