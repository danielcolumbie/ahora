package com.ahora.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import com.ahora.app.data.Task
import com.ahora.app.data.TaskPriority
import com.ahora.app.data.TaskRecurrence
import com.ahora.app.notifications.AlarmScheduler
import com.ahora.app.ui.theme.Haptics
import com.ahora.app.ui.theme.Motion
import com.ahora.app.ui.theme.Spacing
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * Lista de tareas con los diálogos de editar y de recordatorio integrados.
 * La reutilizan las pantallas Hoy y Todas.
 *
 * [alarmScheduler] es la instancia única del contenedor de la app: se pasa
 * desde fuera para no construir un programador nuevo (con su
 * `getSystemService`) en cada confirmación de recordatorio.
 *
 * Las filas se reordenan con suavidad (animateItemPlacement) y, solo la primera vez
 * que se muestra la lista, entran de forma escalonada. Cuando una fila sale
 * de la lista (eliminar, o completar en «Hoy») se desvanece y se colapsa
 * con [Motion.softExit]: discreta, sin pedir atención.
 *
 * [scrollToTopEvents]: al tocar la pestaña ya activa en la barra de
 * navegación, la lista visible sube al inicio con desplazamiento suave
 * (bloque H). La barra no conoce el scroll de cada pantalla; este flujo,
 * emitido por el ViewModel, lo conecta sin romper el estado restaurado.
 *
 * [onEditRequest]: en pantallas anchas (bloque J, dos paneles) tocar una
 * fila para editarla no abre el diálogo: selecciona la tarea para
 * mostrarla en el panel de detalle. En `null` (teléfonos) se conserva el
 * diálogo de edición de siempre.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksColumn(
    tasks: List<Task>,
    onToggleDone: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onUpdateDetails: (Task, String, TaskPriority, Long?, TaskRecurrence) -> Unit,
    onSetReminder: (Task, Long) -> Unit,
    onClearReminder: (Task) -> Unit,
    onPastReminder: () -> Unit,
    alarmScheduler: AlarmScheduler,
    scrollToTopEvents: SharedFlow<Unit>,
    modifier: Modifier = Modifier,
    onEditRequest: ((Task) -> Unit)? = null
) {
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var reminderTask by remember { mutableStateOf<Task?>(null) }
    val haptics = LocalHapticFeedback.current
    val listScope = rememberCoroutineScope()

    // Cada tarea anima su entrada una sola vez: las iniciales de forma
    // escalonada y las que se añadan después, al aparecer. El retardo
    // escalonado tiene tope (Motion.STAGGER_MAX_MILLIS = 360ms): en listas
    // largas la entrada no se siente mecánica ni tarda en arrancar, y las
    // filas que se componen al hacer scroll entran sin retardo.
    val shownIds = remember { mutableSetOf<Long>() }
    // El estado de la lista vive aquí (no se recrea en cada recomposición),
    // así la posición del scroll sobrevive a los cambios de la UI.
    val listState = rememberLazyListState()

    // Tocar la pestaña activa sube la lista visible al inicio (bloque H):
    // desplazamiento suave, sin tocar el estado restaurado de la otra
    // pantalla (cada TasksColumn tiene su propio listState y solo la
    // visible está compuesta).
    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents.collect {
            listScope.launch { listState.animateScrollToItem(0) }
        }
    }

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
                // Salida corta y discreta (bloque H): al eliminar o al
                // completar en «Hoy», la fila se desvanece y se colapsa en
                // 200ms en vez de desaparecer de golpe.
                exit = Motion.softExit(),
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
                    // Dos paneles (bloque J): editar selecciona la tarea
                    // para el panel de detalle en vez de abrir el diálogo.
                    onEdit = { onEditRequest?.invoke(it) ?: run { editingTask = it } },
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
        AnimatedVisibility(
            visible = true,
            // Los diálogos entran con fundido + escala sutil (bloque H);
            // al cerrar desaparecen al instante: salir es la acción.
            enter = Motion.dialogEnter()
        ) {
            EditTaskDialog(
                initialText = task.title,
                initialPriority = TaskPriority.fromLevel(task.priority),
                initialDueAt = task.dueAt,
                initialRecurrence = TaskRecurrence.fromCode(task.recurrence),
                onDismiss = { editingTask = null },
                onConfirm = { title, priority, dueAt, recurrence ->
                    // Tick de confirmación: el cambio quedó guardado.
                    Haptics.tick(haptics)
                    onUpdateDetails(task, title, priority, dueAt, recurrence)
                    editingTask = null
                }
            )
        }
    }

    // Flujo de recordatorio (diálogo + permiso + aviso de hora exacta):
    // el mismo host que usa el panel de detalle en pantallas anchas.
    ReminderFlowHost(
        task = reminderTask,
        onDismiss = { reminderTask = null },
        onSetReminder = onSetReminder,
        onPastReminder = onPastReminder,
        alarmScheduler = alarmScheduler
    )
}
