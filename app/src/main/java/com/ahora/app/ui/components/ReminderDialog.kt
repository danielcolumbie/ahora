package com.ahora.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.util.Calendar

/**
 * Diálogo en dos pasos (fecha → hora) para fijar un recordatorio local.
 * Devuelve el instante en milisegundos o nada si se cancela.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    val now = Calendar.getInstance()
    val timeState = rememberTimePickerState(
        initialHour = now.get(Calendar.HOUR_OF_DAY),
        initialMinute = now.get(Calendar.MINUTE),
        is24Hour = true
    )

    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { step = 1 }, enabled = dateState.selectedDateMillis != null) {
                    Text("Siguiente")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = dateState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        val dateMillis = dateState.selectedDateMillis ?: return@TextButton
                        // El DatePicker devuelve medianoche UTC: convertir a fecha
                        // local ANTES de fijar la hora (si no, en UTC−x cae el día anterior).
                        val atMillis = reminderInstantMillis(
                            dateMillisUtc = dateMillis,
                            hour = timeState.hour,
                            minute = timeState.minute
                        )
                        if (atMillis > System.currentTimeMillis()) {
                            onConfirm(atMillis)
                        } else {
                            onDismiss()
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}
