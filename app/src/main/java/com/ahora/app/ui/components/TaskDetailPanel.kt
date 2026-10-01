package com.ahora.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import com.ahora.app.data.Tag
import com.ahora.app.data.Task
import com.ahora.app.data.TaskPriority
import com.ahora.app.data.TaskRecurrence
import com.ahora.app.notifications.AlarmScheduler
import com.ahora.app.ui.adaptive.AdaptiveLayout
import com.ahora.app.ui.theme.Spacing

/**
 * Panel de detalle para el layout de dos paneles (BLOQUE J del rediseño
 * premium, FASE 15): en pantallas anchas (≥600dp) muestra y edita la
 * tarea seleccionada de la lista, sin diálogos.
 *
 * Sin selección, un estado vacío elegante que orienta (mismo lenguaje
 * que [EmptyState], sin botón: aquí no se crea nada).
 *
 * La edición es en vivo y sin estado pendiente: el título se guarda con
 * cada cambio (nunca en blanco — vaciar el campo solo lo muestra vacío
 * en local hasta escribir de nuevo) y los chips de prioridad/fecha/
 * recurrencia/etiquetas (1.28.0) se aplican al tocarlos, igual que el
 * diálogo de edición aplica al guardar. Así no hay nada que perder al
 * rotar, al cambiar de tarea ni al cerrar el panel: no existe el concepto
 * de "cambios sin guardar".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailPanel(
    task: Task?,
    onToggleDone: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    /**
     * El último parámetro (`tagIds`, null = no tocar) lleva las etiquetas
     * (1.28.0): aquí cada toque se aplica en vivo.
     */
    onUpdateDetails: (Task, String, TaskPriority, Long?, TaskRecurrence, Set<Long>?) -> Unit,
    onSetReminder: (Task, Long) -> Unit,
    onClearReminder: (Task) -> Unit,
    onPastReminder: () -> Unit,
    /** El usuario negó el permiso de notificaciones al guardar un recordatorio. */
    onNotifPermissionDenied: () -> Unit,
    alarmScheduler: AlarmScheduler,
    modifier: Modifier = Modifier,
    /** Todas las etiquetas (1.28.0): para elegirlas en la sección. */
    allTags: List<Tag> = emptyList(),
    /** Etiquetas de la tarea en detalle (1.28.0): se derivan del mapa. */
    selectedTags: List<Tag> = emptyList()
) {
    if (task == null) {
        DetailEmptyState(modifier = modifier)
        return
    }

    var showReminderPicker by remember(task.id) { mutableStateOf(false) }
    // Si el usuario vacía el título, no se guarda en blanco (igual que el
    // diálogo, que desactiva "Guardar"): se muestra vacío en local hasta
    // que escribe de nuevo. Todo lo demás se aplica en vivo.
    var clearedTitle by remember(task.id) { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun saveTitle(newTitle: String) {
        if (newTitle.isBlank()) {
            clearedTitle = true
        } else {
            clearedTitle = false
            onUpdateDetails(
                task,
                newTitle,
                TaskPriority.fromLevel(task.priority),
                task.dueAt,
                TaskRecurrence.fromCode(task.recurrence),
                null
            )
        }
    }

    // Cada toque se aplica en vivo: las etiquetas actuales de la tarea se
    // reconstruyen de la lista seleccionada para que ningún otro cambio las
    // borre sin querer (null = no tocar solo vale en el diálogo, que las
    // devuelve explícitas al confirmar).
    val currentTagIds: Set<Long> = selectedTags.map { it.id }.toSet()

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            // En tablets muy anchas el formulario no se estira hasta el
            // borde: columna de lectura cómoda, alineada al inicio.
            .widthIn(max = AdaptiveLayout.detailPaneMaxWidth)
            .padding(horizontal = Spacing.screenHorizontal)
    ) {
        // Etiqueta de sección, espejo de «HOY» en el panel de la lista.
        Text(
            text = "DETALLE",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(Spacing.s))

        OutlinedTextField(
            value = if (clearedTitle) "" else task.title,
            onValueChange = ::saveTitle,
            label = { Text("Título") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { keyboardController?.hide() }
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(Spacing.l))

        // Prioridad, fecha límite, recurrencia y etiquetas (1.28.0): el
        // componente compartido con los diálogos; aquí cada toque se aplica
        // al momento.
        TaskFormFields(
            priority = TaskPriority.fromLevel(task.priority),
            onPriorityChange = { priority ->
                onUpdateDetails(
                    task, task.title, priority, task.dueAt,
                    TaskRecurrence.fromCode(task.recurrence), currentTagIds
                )
            },
            dueAt = task.dueAt,
            onDueAtChange = { dueAt ->
                onUpdateDetails(
                    task, task.title, TaskPriority.fromLevel(task.priority),
                    dueAt, TaskRecurrence.fromCode(task.recurrence), currentTagIds
                )
            },
            recurrence = TaskRecurrence.fromCode(task.recurrence),
            onRecurrenceChange = { recurrence ->
                onUpdateDetails(
                    task, task.title, TaskPriority.fromLevel(task.priority),
                    task.dueAt, recurrence, currentTagIds
                )
            },
            allTags = allTags,
            selectedTagIds = currentTagIds,
            onToggleTag = { toggledId ->
                val newIds = if (toggledId in currentTagIds) currentTagIds - toggledId
                else currentTagIds + toggledId
                onUpdateDetails(
                    task, task.title, TaskPriority.fromLevel(task.priority),
                    task.dueAt, TaskRecurrence.fromCode(task.recurrence), newIds
                )
            }
        )
        Spacer(modifier = Modifier.height(Spacing.l))

        // Recordatorio: el mismo patrón que el diálogo de creación
        // avanzada (chip con opción de quitar, o botón de añadir).
        FormSection(title = "Recordatorio") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                val reminderAt = task.reminderAt
                if (reminderAt == null) {
                    TextButton(onClick = { showReminderPicker = true }) {
                        Text("Añadir…")
                    }
                } else {
                    InputChip(
                        selected = true,
                        onClick = { showReminderPicker = true },
                        label = { Text(formatReminderLabel(reminderAt)) },
                        // Seleccionado con el acento propio (auditoría
                        // 1.25.0): sin esto caía al `secondaryContainer`
                        // por defecto de M3.
                        colors = ahoraSelectedChipColors(),
                        trailingIcon = {
                            IconButton(onClick = { onClearReminder(task) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Quitar recordatorio",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(Spacing.l))

        SettingSwitchRow(
            title = "Completada",
            icon = Icons.Outlined.CheckCircle,
            checked = task.isDone,
            onCheckedChange = { onToggleDone(task) }
        )
        Spacer(modifier = Modifier.height(Spacing.s))

        // Eliminar es destructiva: el error comunica la acción (el
        // Deshacer del snackbar sigue funcionando igual).
        TextButton(onClick = { onDelete(task) }) {
            Text(
                text = "Eliminar tarea",
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(Spacing.xxl))
    }

    // Diálogo de fecha/hora con el flujo completo (permiso + aviso de
    // hora exacta): el mismo host que usa la lista.
    ReminderFlowHost(
        task = if (showReminderPicker) task else null,
        onDismiss = { showReminderPicker = false },
        onSetReminder = onSetReminder,
        onPastReminder = onPastReminder,
        onNotifPermissionDenied = onNotifPermissionDenied,
        alarmScheduler = alarmScheduler
    )
}

/**
 * Estado vacío del panel de detalle: sin selección no hay nada que
 * editar. Explica el estado y orienta, con la jerarquía tipográfica del
 * [EmptyState] de las listas y sin botón (aquí no se crea nada).
 */
@Composable
private fun DetailEmptyState(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = Spacing.xxl)
    ) {
        Text(
            text = "Sin selección",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(Spacing.m))
        Text(
            text = "Toca una tarea de la lista para verla y editarla aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
