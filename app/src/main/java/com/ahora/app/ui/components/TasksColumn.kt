package com.ahora.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import com.ahora.app.data.Task
import com.ahora.app.data.TaskPriority
import com.ahora.app.data.TaskRecurrence
import com.ahora.app.notifications.AlarmScheduler
import com.ahora.app.ui.theme.Haptics
import com.ahora.app.ui.theme.Motion
import com.ahora.app.ui.theme.Sizes
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
    /** El usuario negó el permiso de notificaciones al guardar un recordatorio. */
    onNotifPermissionDenied: () -> Unit,
    alarmScheduler: AlarmScheduler,
    scrollToTopEvents: SharedFlow<Unit>,
    modifier: Modifier = Modifier,
    onEditRequest: ((Task) -> Unit)? = null,
    /**
     * En dos paneles: id de la tarea que muestra el panel de detalle, para
     * resaltar su fila ([TaskRow.selected]). En `null` no se resalta nada.
     */
    selectedTaskId: Long? = null,
    /**
     * Ronda 2 (2026-09-30, mockup aprobado por Daniel): las tareas
     * completadas se agrupan tras una fila táctil de ancho completo
     * ("Completadas" + pill con el contador), colapsada por defecto.
     * Las activas siempre visibles; las hechas, ocultas hasta expandir.
     * Solo Hoy lo activa; Todas conserva la lista plana.
     */
    collapsibleCompleted: Boolean = false
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

    // Instancias estables para todas las filas (bloque K, rendimiento):
    // sin lambdas recreadas por ítem en cada recomposición. `editingTask`
    // y `reminderTask` se leen/escriben vía su holder de estado, así que
    // la instancia no cambia cuando el diálogo se abre o se cierra y las
    // filas que no cambiaron pueden saltar la recomposición.
    val onEdit: (Task) -> Unit = remember(onEditRequest) {
        { task -> onEditRequest?.invoke(task) ?: run { editingTask = task } }
    }
    val onToggleReminder: (Task) -> Unit = remember(onClearReminder) {
        { task ->
            if (task.reminderAt == null) reminderTask = task
            else onClearReminder(task)
        }
    }

    // Tocar la pestaña activa sube la lista visible al inicio (bloque H):
    // desplazamiento suave, sin tocar el estado restaurado de la otra
    // pantalla (cada TasksColumn tiene su propio listState y solo la
    // visible está compuesta).
    LaunchedEffect(scrollToTopEvents) {
        scrollToTopEvents.collect {
            listScope.launch { listState.animateScrollToItem(0) }
        }
    }

    // Ronda 2 (2026-09-30): en Hoy las completadas se agrupan tras una
    // fila táctil ("Completadas" + pill con el contador), colapsada por
    // defecto. Las activas siempre visibles; las hechas, ocultas hasta
    // expandir. Sin la bandera, la lista queda plana como antes (Todas).
    val (activeTasks, completedTasks) = remember(tasks, collapsibleCompleted) {
        if (collapsibleCompleted) splitActiveCompleted(tasks)
        else tasks to emptyList()
    }
    // Colapsada por defecto, como en el mockup aprobado; el estado
    // sobrevive a la rotación.
    var completedExpanded by rememberSaveable { mutableStateOf(false) }
    // Post-auditoría (2026-09-30, B-1): al vaciarse la sección, el estado
    // vuelve a colapsado (el mockup la define colapsada por defecto). Sin
    // esto, al reaparecer lo hacía expandida si se había dejado abierta.
    LaunchedEffect(completedTasks.isEmpty()) {
        completedExpanded = resetCompletedExpanded(
            completedEmpty = completedTasks.isEmpty(),
            expanded = completedExpanded
        )
    }
    val hasCompletedSection = collapsibleCompleted && completedTasks.isNotEmpty()

    LazyColumn(
        state = listState,
        // Sin tarjetas: las filas se separan con un divisor sutil.
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
        contentPadding = PaddingValues(vertical = Spacing.xs),
        modifier = modifier
    ) {
        itemsIndexed(
            activeTasks,
            key = { _, task -> task.id }
        ) { index, task ->
            TaskListItem(
                task = task,
                index = index,
                shownIds = shownIds,
                onToggleDone = onToggleDone,
                onDelete = onDelete,
                onEdit = onEdit,
                onToggleReminder = onToggleReminder,
                selected = selectedTaskId != null && task.id == selectedTaskId,
                // El divisor también separa la última activa del
                // encabezado de completadas.
                showDivider = index < activeTasks.lastIndex || hasCompletedSection,
                // animateItemPlacement vive en el scope del LazyColumn:
                // se crea aquí y se pasa al ítem.
                modifier = Modifier.animateItemPlacement()
            )
        }
        if (hasCompletedSection) {
            item(key = "completed-header") {
                CompletedSectionHeader(
                    count = completedTasks.size,
                    expanded = completedExpanded,
                    onToggle = {
                        // Tick de confirmación (B-2, 2026-09-30): la
                        // sección se alterna como el checkbox de tarea.
                        Haptics.tick(haptics)
                        completedExpanded = !completedExpanded
                    }
                )
            }
            itemsIndexed(
                completedTasks,
                key = { _, task -> task.id }
            ) { index, task ->
                // Las hechas se revelan/ocultan con fundido + despliegue
                // en 150ms (Motion.completedExpand/Collapse): la misma
                // física que la selección de fila, sin física nueva.
                AnimatedVisibility(
                    visible = completedExpanded,
                    enter = Motion.completedExpand(),
                    exit = Motion.completedCollapse()
                ) {
                    TaskListItem(
                        task = task,
                        index = activeTasks.size + index,
                        shownIds = shownIds,
                        onToggleDone = onToggleDone,
                        onDelete = onDelete,
                        onEdit = onEdit,
                        onToggleReminder = onToggleReminder,
                        selected = selectedTaskId != null && task.id == selectedTaskId,
                        showDivider = index < completedTasks.lastIndex,
                        modifier = Modifier.animateItemPlacement()
                    )
                }
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
        onNotifPermissionDenied = onNotifPermissionDenied,
        alarmScheduler = alarmScheduler
    )
}

/**
 * Una fila de tarea dentro del LazyColumn, con su animación de entrada y
 * su divisor opcional. Extraída para reutilizarla en los dos grupos
 * (activas y completadas) sin duplicar el cuerpo.
 *
 * [modifier] se crea en el scope del LazyColumn (`animateItemPlacement`
 * solo existe ahí: en foundation 1.6.8 aún no existe `animateItem()`).
 */
@Composable
private fun TaskListItem(
    task: Task,
    index: Int,
    shownIds: MutableSet<Long>,
    onToggleDone: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onToggleReminder: (Task) -> Unit,
    selected: Boolean,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    // Cada tarea anima su entrada una sola vez: las iniciales de forma
    // escalonada y las que se añadan después, al aparecer. El retardo
    // escalonado tiene tope (Motion.STAGGER_MAX_MILLIS = 360ms).
    val isNew = task.id !in shownIds
    LaunchedEffect(task.id) { shownIds += task.id }
    val staggerDelay =
        Motion.staggerDelayMillis(index, isFirstShow = isNew && shownIds.isEmpty())
    AnimatedVisibility(
        visible = true,
        enter = if (isNew) Motion.taskEnter(staggerDelay) else EnterTransition.None,
        // Salida corta y discreta (bloque H): al eliminar o al completar
        // en «Hoy», la fila se desvanece y se colapsa en 200ms en vez de
        // desaparecer de golpe.
        exit = Motion.softExit(),
        modifier = modifier
    ) {
        TaskRow(
            task = task,
            // Se pasan las referencias estables: una sola instancia para
            // todas las filas, no una lambda nueva por fila (bloque K).
            onToggleDone = onToggleDone,
            onDelete = onDelete,
            // Dos paneles (bloque J): editar selecciona la tarea para el
            // panel de detalle en vez de abrir el diálogo.
            onEdit = onEdit,
            onToggleReminder = onToggleReminder,
            // La fila de la tarea en el detalle se resalta.
            selected = selected
        )
    }
    if (showDivider) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * Fila táctil de ancho completo que abre/cierra las tareas completadas
 * (ronda 2, 2026-09-30; mockup aprobado por Daniel tal cual): chevron,
 * texto "Completadas" en semibold gris oscuro y pill neutra con el
 * contador. Sin tarjetas ni sombras: la misma calma del resto de la
 * lista. El área táctil mide como mínimo [Sizes.minTouchRow] (48dp).
 *
 * TalkBack la anuncia como un solo botón: nombre + conteo + estado
 * ("Expandida"/"Contraída"); el chevron es decorativo.
 */
@Composable
private fun CompletedSectionHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    // El chevron gira al expandir/contraer con el fundido de 150ms de la
    // app: el mismo gesto visual que la selección de fila.
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = Motion.selectionFade(),
        label = "completedChevron"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.minTouchRow)
            // El clip acota el ripple al tocar: la fila no tiene fondo
            // propio (como el resto de la lista), pero el toque sí se ve.
            .clip(RoundedCornerShape(Spacing.s))
            .clickable(
                onClick = onToggle,
                role = Role.Button,
                onClickLabel = if (expanded) "Contraer" else "Expandir"
            )
            // Un solo anuncio: nombre + conteo + estado, como un botón.
            .semantics(mergeDescendants = true) {
                contentDescription = completedHeaderContentDescription(count)
                stateDescription = completedHeaderStateDescription(expanded)
            }
            .padding(horizontal = Spacing.s, vertical = Spacing.xs)
    ) {
        Icon(
            // Chevron de trazo, como en el mockup aprobado (B-3,
            // 2026-09-30): el filled del inicio se veía más pesado.
            imageVector = Icons.Outlined.KeyboardArrowDown,
            // Decorativo: el estado lo anuncia el `stateDescription`
            // del encabezado, no el icono.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(Sizes.sectionChevron)
                .graphicsLayer { rotationZ = chevronRotation }
        )
        Spacer(modifier = Modifier.width(Spacing.s))
        Text(
            text = "Completadas",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        CountPill(count = count)
    }
}

/**
 * Pill neutra con el contador (igual que las pills de metadatos de la
 * fila: `surfaceVariant`, sin icono). El acento se reserva para acciones
 * y estados, no para un conteo.
 */
@Composable
private fun CountPill(count: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(
                horizontal = Spacing.m,
                vertical = Spacing.xs
            )
        )
    }
}

/**
 * Post-auditoría (B-1, 2026-09-30): cuando la sección de completadas se
 * vacía, el estado vuelve a colapsado (el mockup la define colapsada por
 * defecto); con tareas, se conserva el estado actual. Función pura para
 * poder probarla.
 */
internal fun resetCompletedExpanded(completedEmpty: Boolean, expanded: Boolean): Boolean =
    if (completedEmpty) false else expanded

/**
 * Parte la lista en activas y completadas, conservando el orden de cada
 * grupo. Función pura para poder probarla.
 */
internal fun splitActiveCompleted(tasks: List<Task>): Pair<List<Task>, List<Task>> =
    tasks.partition { !it.isDone }

/**
 * Anuncio de TalkBack del encabezado de completadas (nombre + conteo).
 * Función pura para poder probarla (igual que `micButtonDescription`).
 */
internal fun completedHeaderContentDescription(count: Int): String =
    if (count == 1) "Completadas, 1 tarea" else "Completadas, $count tareas"

/**
 * Estado que TalkBack anuncia del encabezado de completadas.
 * Función pura para poder probarla.
 */
internal fun completedHeaderStateDescription(expanded: Boolean): String =
    if (expanded) "Expandida" else "Contraída"
