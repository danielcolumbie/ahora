package com.ahora.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import com.ahora.app.data.TaskPriority
import com.ahora.app.data.TaskRecurrence
import com.ahora.app.ui.theme.Spacing

/**
 * Diálogo para editar una tarea: texto, prioridad, fecha límite
 * (ETAPA 10) y recurrencia (ETAPA 11). Devuelve los cuatro valores al
 * confirmar.
 */
@Composable
fun EditTaskDialog(
    initialText: String,
    initialPriority: TaskPriority = TaskPriority.NONE,
    initialDueAt: Long? = null,
    initialRecurrence: TaskRecurrence = TaskRecurrence.NONE,
    onDismiss: () -> Unit,
    onConfirm: (String, TaskPriority, Long?, TaskRecurrence) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var priority by remember { mutableStateOf(initialPriority) }
    var dueAt by remember { mutableStateOf(initialDueAt) }
    var recurrence by remember { mutableStateOf(initialRecurrence) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitle("Editar tarea") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.l)
            ) {
                // El título es la primera sección del diálogo, con la
                // etiqueta del sistema, como el resto de secciones.
                FormSection(title = "Título") {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (text.isNotBlank()) onConfirm(text, priority, dueAt, recurrence)
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Prioridad, fecha límite y recurrencia: el componente
                // compartido, que ya trae sus propias secciones.
                TaskFormFields(
                    priority = priority,
                    onPriorityChange = { priority = it },
                    dueAt = dueAt,
                    onDueAtChange = { dueAt = it },
                    recurrence = recurrence,
                    onRecurrenceChange = { recurrence = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text, priority, dueAt, recurrence) },
                enabled = text.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
