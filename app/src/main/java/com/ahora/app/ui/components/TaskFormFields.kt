package com.ahora.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ahora.app.data.TaskPriority
import com.ahora.app.data.TaskRecurrence
import com.ahora.app.ui.theme.Spacing

/**
 * Selector de prioridad, fecha límite y recurrencia de una tarea
 * (ETAPA 10 + ETAPA 11).
 *
 * Lo comparten la creación rápida (HomeScreen) y el diálogo de edición:
 * un solo componente, un solo comportamiento. Sin dependencias nuevas:
 * DatePicker de Material3 para elegir el día y [startOfDayMillis] /
 * [reminderInstantMillis] para convertirlo al inicio del día local
 * (la fecha límite no lleva hora).
 *
 * La fecha se guarda como inicio del día local en milisegundos, o null
 * si la tarea no tiene fecha límite. La recurrencia se elige con chips;
 * "No se repite" es el valor por defecto.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskFormFields(
    priority: TaskPriority,
    onPriorityChange: (TaskPriority) -> Unit,
    dueAt: Long?,
    onDueAtChange: (Long?) -> Unit,
    recurrence: TaskRecurrence,
    onRecurrenceChange: (TaskRecurrence) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    // Chips seleccionados con el acento propio (auditoría 1.25.0): sin
    // esto caían al `secondaryContainer` por defecto de M3.
    val selectedChipColors = ahoraSelectedChipColors()

    // Tres secciones con las etiquetas del sistema ([FormSection]): la misma
    // jerarquía que usan el diálogo de edición y el de recordatorio, para
    // que crear, editar y programar un aviso se sientan como lo mismo.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        // Prioridad: cuatro opciones excluyentes. La app es minimalista:
        // chips compactos, sin iconos ni colores chillones aquí.
        FormSection(title = "Prioridad") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                TaskPriority.entries.forEach { option ->
                    FilterChip(
                        selected = priority == option,
                        onClick = { onPriorityChange(option) },
                        label = { Text(option.label) },
                        colors = selectedChipColors
                    )
                }
            }
        }

        // Fecha límite: atajos Hoy/Mañana + selector de día. FlowRow en vez
        // de Row (bloque I): con la escala de fuente del sistema grande,
        // los atajos no caben en una línea y se recortarían.
        FormSection(title = "Fecha límite") {
            FlowRow(
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                if (dueAt == null) {
                    FilterChip(
                        selected = false,
                        onClick = { onDueAtChange(startOfDayMillis(0)) },
                        label = { Text("Hoy") },
                        colors = selectedChipColors
                    )
                    FilterChip(
                        selected = false,
                        onClick = { onDueAtChange(startOfDayMillis(1)) },
                        label = { Text("Mañana") },
                        colors = selectedChipColors
                    )
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Elegir…")
                    }
                } else {
                    InputChip(
                        selected = true,
                        onClick = { showDatePicker = true },
                        label = { Text(formatDueLabel(dueAt)) },
                        colors = selectedChipColors,
                        trailingIcon = {
                            IconButton(onClick = { onDueAtChange(null) }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Quitar fecha límite",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                }
            }
        }

        // Recurrencia: al completar una tarea recurrente se genera la
        // siguiente ocurrencia (ETAPA 11). Cinco opciones excluyentes.
        FormSection(title = "Recurrencia") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                TaskRecurrence.entries.forEach { option ->
                    FilterChip(
                        selected = recurrence == option,
                        onClick = { onRecurrenceChange(option) },
                        label = { Text(option.label) },
                        colors = selectedChipColors
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            // El DatePicker trabaja en UTC: convertir el inicio del día
            // local a medianoche UTC para que muestre el día correcto.
            initialSelectedDateMillis = dueAt?.let(::dueAtToPickerMillis)
                ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let { utc ->
                            onDueAtChange(reminderInstantMillis(utc, 0, 0))
                        }
                        showDatePicker = false
                    },
                    enabled = dateState.selectedDateMillis != null
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}
