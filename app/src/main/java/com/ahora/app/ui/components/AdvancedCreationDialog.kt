package com.ahora.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.ahora.app.data.Tag
import com.ahora.app.ui.theme.Spacing

/**
 * Configuración avanzada de la creación (bloque C): prioridad, fecha
 * límite, recordatorio y recurrencia en un diálogo dedicado, separado
 * claramente de la creación rápida. Reutiliza el componente compartido
 * [TaskFormFields] (el mismo que usa el diálogo de edición) y el
 * [ReminderDialog] de dos pasos para el recordatorio: nada nuevo que
 * aprender, nada duplicado.
 *
 * El diálogo edita el borrador en vivo, pero "Cancelar" (o tocar fuera)
 * restaura el estado que había al abrirlo: solo "Guardar" confirma los
 * cambios. Los valores quedan en el borrador y se aplican al enviar la
 * tarea, sin fricción extra.
 *
 * No toca la lógica de guardado: eso sigue en `repository.add` vía el
 * ViewModel cuando el usuario envía desde la barra rápida.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedCreationDialog(
    draft: CreationDraftState,
    /** Todas las etiquetas (1.28.0): el diálogo las elige sobre el borrador. */
    allTags: List<Tag> = emptyList(),
    onDismiss: () -> Unit,
    /** El usuario eligió una fecha/hora pasada: se avisa, no se aplica. */
    onPastReminder: () -> Unit
) {
    val snapshot = remember { draft.snapshot() }
    var showReminderPicker by remember { mutableStateOf(false) }

    fun cancel() {
        draft.restore(snapshot)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = ::cancel,
        title = { DialogTitle("Opciones de la tarea") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.m)
            ) {
                TaskFormFields(
                    priority = draft.priority,
                    onPriorityChange = { draft.setPriorityManual(it) },
                    dueAt = draft.dueAt,
                    onDueAtChange = { draft.setDueAtManual(it) },
                    recurrence = draft.recurrence,
                    onRecurrenceChange = { draft.setRecurrenceManual(it) },
                    allTags = allTags,
                    selectedTagIds = draft.tagIds,
                    onToggleTag = { draft.toggleTag(it) }
                )

                // Recordatorio manual: si el lenguaje natural ya detectó
                // uno se muestra con opción de quitarlo; si no, se ofrece
                // añadirlo con el diálogo de dos pasos.
                FormSection(title = "Recordatorio") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        val reminderAt = draft.reminderAt
                        if (reminderAt == null) {
                            Text(
                                text = "Sin recordatorio",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                                    IconButton(onClick = { draft.setReminderAtManual(null) }) {
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
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = ::cancel) { Text("Cancelar") }
        }
    )

    if (showReminderPicker) {
        ReminderDialog(
            onDismiss = { showReminderPicker = false },
            onConfirm = { atMillis ->
                showReminderPicker = false
                // Un recordatorio nunca nace en el pasado: se avisa en vez
                // de aplicarlo en silencio (misma regla que en "Todas").
                if (isFutureInstant(atMillis)) draft.setReminderAtManual(atMillis)
                else onPastReminder()
            }
        )
    }
}
