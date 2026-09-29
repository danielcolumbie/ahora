package com.ahora.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ahora.app.ui.theme.Spacing
import java.util.Calendar

/**
 * Diálogo de recordatorio en una sola pantalla, con el mismo patrón de
 * [TaskFormFields]: atajos de día (Hoy / Mañana / En 1 hora) + selector
 * de fecha, y la hora en su propia sección. Devuelve el instante en
 * milisegundos o nada si se cancela.
 *
 * La fecha elegida en el DatePicker se convierte con
 * [reminderInstantMillis] (corrección de zona horaria: el picker devuelve
 * medianoche UTC; en Cuba sin corregir caía el día anterior). Los atajos
 * usan [reminderAtLocalDay], que ya parte del día local.
 *
 * Si el instante elegido ya pasó, también se devuelve: quien lo llama
 * valida con [isFutureInstant] y avisa al usuario en vez de descartarlo
 * en silencio.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    var day by remember { mutableStateOf<ReminderDay>(ReminderDay.Today) }
    var showPicker by remember { mutableStateOf(false) }
    // "En 1 hora" fija la hora sin recrear a mano el estado del picker:
    // al cambiar la clave, el estado se crea de nuevo con la hora dada.
    var timeOverride by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val now = Calendar.getInstance()
    val timeState = key(timeOverride) {
        rememberTimePickerState(
            initialHour = timeOverride?.first ?: now.get(Calendar.HOUR_OF_DAY),
            initialMinute = timeOverride?.second ?: now.get(Calendar.MINUTE),
            is24Hour = true
        )
    }

    fun currentInstant(): Long = when (val d = day) {
        is ReminderDay.Today -> reminderAtLocalDay(0, timeState.hour, timeState.minute)
        is ReminderDay.Tomorrow -> reminderAtLocalDay(1, timeState.hour, timeState.minute)
        is ReminderDay.Custom -> reminderInstantMillis(d.utcMillis, timeState.hour, timeState.minute)
    }

    // Chips seleccionados con el acento propio (auditoría 1.25.0): sin
    // esto caían al `secondaryContainer` por defecto de M3.
    val selectedChipColors = ahoraSelectedChipColors()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recordatorio") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.l)
            ) {
                // Fecha: atajos + calendario, igual que la fecha límite en
                // TaskFormFields. "En 1 hora" fija hoy y adelanta la hora.
                FormSection(title = "Fecha") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        FilterChip(
                            selected = day is ReminderDay.Today,
                            onClick = { day = ReminderDay.Today },
                            label = { Text("Hoy") },
                            colors = selectedChipColors
                        )
                        FilterChip(
                            selected = day is ReminderDay.Tomorrow,
                            onClick = { day = ReminderDay.Tomorrow },
                            label = { Text("Mañana") },
                            colors = selectedChipColors
                        )
                        val custom = day as? ReminderDay.Custom
                        if (custom != null) {
                            InputChip(
                                selected = true,
                                onClick = { showPicker = true },
                                colors = selectedChipColors,
                                // La fecha concreta se muestra como la fecha
                                // límite: "Hoy", "Mañana" o "3 oct".
                                label = {
                                    Text(
                                        formatDueLabel(
                                            reminderInstantMillis(custom.utcMillis, 0, 0)
                                        )
                                    )
                                }
                            )
                        } else {
                            FilterChip(
                                selected = false,
                                onClick = { showPicker = true },
                                label = { Text("Elegir…") },
                                colors = selectedChipColors
                            )
                        }
                        FilterChip(
                            selected = false,
                            onClick = {
                                val inOneHour = Calendar.getInstance()
                                    .apply { add(Calendar.HOUR_OF_DAY, 1) }
                                day = ReminderDay.Today
                                timeOverride = inOneHour.get(Calendar.HOUR_OF_DAY) to
                                    inOneHour.get(Calendar.MINUTE)
                            },
                            label = { Text("En 1 hora") },
                            colors = selectedChipColors
                        )
                    }
                }
                // Hora: entrada compacta de tiempo (24h), sin un segundo
                // paso de diálogo.
                FormSection(title = "Hora") {
                    TimeInput(state = timeState)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentInstant()) }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (showPicker) {
        val dateState = rememberDatePickerState(
            // El DatePicker trabaja en UTC: pasarle el día expresado en
            // UTC para que muestre el día correcto (ver
            // [dueAtToPickerMillis]).
            initialSelectedDateMillis = dueAtToPickerMillis(startOfDayMillis(0))
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let { utc ->
                            day = ReminderDay.Custom(utc)
                        }
                        showPicker = false
                    },
                    enabled = dateState.selectedDateMillis != null
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

/**
 * Día elegido para el recordatorio: un atajo (hoy/mañana) o la fecha
 * concreta del calendario (medianoche UTC, como la devuelve el
 * DatePicker).
 */
private sealed interface ReminderDay {
    data object Today : ReminderDay
    data object Tomorrow : ReminderDay
    data class Custom(val utcMillis: Long) : ReminderDay
}
