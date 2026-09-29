package com.ahora.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ahora.app.data.Task
import com.ahora.app.notifications.AlarmScheduler
import com.ahora.app.ui.theme.Motion
import com.ahora.app.ui.theme.Spacing

/**
 * Lista de tareas con los diálogos de editar y de recordatorio integrados.
 * La reutilizan las pantallas Hoy y Todas.
 *
 * [alarmScheduler] es la instancia única del contenedor de la app: se pasa
 * desde fuera para no construir un programador nuevo (con su
 * `getSystemService`) en cada confirmación de recordatorio.
 *
 * Las filas se reordenan con suavidad (animateItemPlacement) y, solo la primera vez
 * que se muestra la lista, entran de forma escalonada.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksColumn(
    tasks: List<Task>,
    onToggleDone: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onUpdateTitle: (Task, String) -> Unit,
    onSetReminder: (Task, Long) -> Unit,
    onClearReminder: (Task) -> Unit,
    onPastReminder: () -> Unit,
    alarmScheduler: AlarmScheduler,
    modifier: Modifier = Modifier
) {
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var reminderTask by remember { mutableStateOf<Task?>(null) }
    var pendingReminder by remember { mutableStateOf<Pair<Task, Long>?>(null) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // El permiso de notificaciones se pide justo al guardar un recordatorio:
    // sin él, el aviso nunca llegaría a la barra de estado.
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingReminder?.let { (task, atMillis) -> onSetReminder(task, atMillis) }
        }
        pendingReminder = null
        reminderTask = null
    }

    fun confirmReminder(task: Task, atMillis: Long) {
        onSetReminder(task, atMillis)
        reminderTask = null
        // Android 12+: sin permiso de alarmas exactas el aviso puede llegar tarde.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmScheduler.hasExactAlarmPermission()
        ) {
            showExactAlarmDialog = true
        }
    }

    // Cada tarea anima su entrada una sola vez: las iniciales de forma
    // escalonada y las que se añadan después, al aparecer.
    val shownIds = remember { mutableSetOf<Long>() }
    // El estado de la lista vive aquí (no se recrea en cada recomposición),
    // así la posición del scroll sobrevive a los cambios de la UI.
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        // Sin tarjetas: las filas se separan con un divisor sutil.
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
        contentPadding = PaddingValues(vertical = Spacing.xs),
        modifier = modifier
    ) {
        itemsIndexed(tasks, key = { _, task -> task.id }) { index, task ->
            val isNew = task.id !in shownIds
            LaunchedEffect(task.id) { shownIds += task.id }
            val staggerDelay =
                Motion.staggerDelayMillis(index, isFirstShow = isNew && shownIds.isEmpty())
            AnimatedVisibility(
                visible = true,
                enter = if (isNew) Motion.taskEnter(staggerDelay) else EnterTransition.None,
                // animateItemPlacement(): en foundation 1.6.8 aún no existe
                // animateItem() (llegó en 1.7); esta es la API vigente aquí.
                modifier = Modifier.animateItemPlacement()
            ) {
                TaskRow(
                    task = task,
                    // Se pasan las referencias estables: una sola instancia
                    // para todas las filas, no una lambda nueva por fila.
                    onToggleDone = onToggleDone,
                    onDelete = onDelete,
                    onEdit = { editingTask = it },
                    onToggleReminder = { t ->
                        if (t.reminderAt == null) reminderTask = t
                        else onClearReminder(t)
                    }
                )
            }
            if (index < tasks.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    editingTask?.let { task ->
        EditTaskDialog(
            initialText = task.title,
            onDismiss = { editingTask = null },
            onConfirm = {
                onUpdateTitle(task, it)
                editingTask = null
            }
        )
    }

    reminderTask?.let { task ->
        ReminderDialog(
            onDismiss = { reminderTask = null },
            onConfirm = { atMillis ->
                if (!isFutureInstant(atMillis)) {
                    // Fecha pasada: avisar con un mensaje claro en vez de
                    // descartar la elección en silencio.
                    onPastReminder()
                    reminderTask = null
                    return@ReminderDialog
                }
                val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    // Se guarda al conceder el permiso; si lo niega, no hay aviso posible.
                    pendingReminder = task to atMillis
                    reminderTask = null
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    confirmReminder(task, atMillis)
                }
            }
        )
    }

    if (showExactAlarmDialog) {
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
