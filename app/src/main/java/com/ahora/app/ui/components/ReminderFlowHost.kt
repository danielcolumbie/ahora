package com.ahora.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import com.ahora.app.data.Task
import com.ahora.app.notifications.AlarmScheduler
import com.ahora.app.ui.theme.Haptics
import com.ahora.app.ui.theme.Motion

/**
 * Diálogo de recordatorio con su flujo completo (BLOQUE J del rediseño
 * premium): selector de fecha/hora, petición del permiso de
 * notificaciones justo al guardar y aviso de alarmas exactas en
 * Android 12+.
 *
 * Extraído de `TasksColumn` sin cambiar su conducta: la lista lo usa
 * igual que antes, y el panel de detalle de pantallas anchas lo
 * reutiliza para no duplicar el flujo (permiso, aviso de hora exacta).
 *
 * El host siempre está compuesto (aunque `task` sea null): el launcher
 * del permiso y el aviso de alarmas exactas deben sobrevivir al cierre
 * del diálogo — el resultado del permiso llega después de que el
 * diálogo ya se cerró.
 */
@Composable
fun ReminderFlowHost(
    task: Task?,
    onDismiss: () -> Unit,
    onSetReminder: (Task, Long) -> Unit,
    onPastReminder: () -> Unit,
    alarmScheduler: AlarmScheduler
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var pendingReminder by remember { mutableStateOf<Pair<Task, Long>?>(null) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }

    // El permiso de notificaciones se pide justo al guardar un
    // recordatorio: sin él, el aviso nunca llegaría a la barra de estado.
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingReminder?.let { (pendingTask, atMillis) ->
                onSetReminder(pendingTask, atMillis)
            }
        }
        pendingReminder = null
        onDismiss()
    }

    fun confirmReminder(forTask: Task, atMillis: Long) {
        onSetReminder(forTask, atMillis)
        onDismiss()
        // Android 12+: sin permiso de alarmas exactas el aviso puede llegar tarde.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmScheduler.hasExactAlarmPermission()
        ) {
            showExactAlarmDialog = true
        }
    }

    // El diálogo entra con fundido + escala sutil (bloque H); al cerrar
    // desaparece al instante: salir es la acción.
    if (task != null) {
        AnimatedVisibility(
            visible = true,
            enter = Motion.dialogEnter()
        ) {
            ReminderDialog(
                onDismiss = onDismiss,
                onConfirm = { atMillis ->
                    if (!isFutureInstant(atMillis)) {
                        // Fecha pasada: avisar con un mensaje claro en vez de
                        // descartar la elección en silencio.
                        onPastReminder()
                        onDismiss()
                        return@ReminderDialog
                    }
                    val needsPermission =
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                    if (needsPermission) {
                        // Se guarda al conceder el permiso; si lo niega, no hay aviso posible.
                        pendingReminder = task to atMillis
                        onDismiss()
                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        // Tick de confirmación: el recordatorio quedó activo.
                        Haptics.tick(haptics)
                        confirmReminder(task, atMillis)
                    }
                }
            )
        }
    }

    if (showExactAlarmDialog) {
        AnimatedVisibility(
            visible = true,
            enter = Motion.dialogEnter()
        ) {
            AlertDialog(
                onDismissRequest = { showExactAlarmDialog = false },
                title = { Text("Aviso a la hora exacta") },
                text = {
                    Text(
                        "Para que el recordatorio suene justo a la hora que elegiste, " +
                            "permite las alarmas exactas de Ahora en los ajustes del sistema."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showExactAlarmDialog = false
                        alarmScheduler.exactAlarmSettingsIntent()
                            ?.let { context.startActivity(it) }
                    }) { Text("Ir a ajustes") }
                },
                dismissButton = {
                    TextButton(onClick = { showExactAlarmDialog = false }) { Text("Ahora no") }
                }
            )
        }
    }
}
