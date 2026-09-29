package com.ahora.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import com.ahora.app.data.Task
import com.ahora.app.data.TaskPriority
import com.ahora.app.data.TaskRecurrence
import com.ahora.app.ui.theme.Haptics
import com.ahora.app.ui.theme.Motion
import com.ahora.app.ui.theme.Sizes
import com.ahora.app.ui.theme.Spacing

/**
 * Fila de tarea: checkbox circular, texto y un solo botón de opciones.
 *
 * Sin tarjeta: el contenido es protagonista y las filas se separan con un
 * divisor sutil (ver [TasksColumn]). La superficie solo aparece donde aporta
 * jerarquía; aquí no aporta nada.
 *
 * La fila mide como mínimo [Sizes.minTouchRow] (48dp) de alto, para que el
 * área táctil no dependa del contenido.
 *
 * Los callbacks reciben la tarea en vez de lambdas `() -> Unit` creadas por
 * fila: así son la misma instancia para todas las filas y Compose puede
 * saltar la recomposición de las que no cambiaron.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskRow(
    task: Task,
    onToggleDone: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onEdit: (Task) -> Unit,
    onToggleReminder: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    // Al completar: el texto se tacha y baja la opacidad con animación, sin desaparecer de golpe.
    val alpha by animateFloatAsState(
        targetValue = if (task.isDone) 0.55f else 1f,
        label = "taskAlpha"
    )
    val textStyle = if (task.isDone) {
        MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
    } else {
        MaterialTheme.typography.bodyLarge
    }
    val priority = TaskPriority.fromLevel(task.priority)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.minTouchRow)
            .alpha(alpha)
            .padding(vertical = Spacing.m)
    ) {
        CircularCheckButton(
            checked = task.isDone,
            onToggle = { onToggleDone(task) },
            // TalkBack (bloque I): el checkbox necesita etiqueta propia —
            // sin ella anunciaría "casilla de verificación" sin decir de
            // qué tarea se trata. El estado (marcada/no marcada) lo anuncia
            // el propio `toggleable` con `Role.Checkbox`.
            label = task.title
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = { onEdit(task) },
                    onClickLabel = "Editar",
                    role = Role.Button
                )
                // Un solo anuncio: título + pills como un botón "Editar".
                // Sin esto, TalkBack leería el nodo clicable vacío por un
                // lado y cada texto por otro, desconectados entre sí.
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = Spacing.s)
        ) {
            Text(
                text = task.title,
                style = textStyle,
                color = MaterialTheme.colorScheme.onBackground
            )
            // Metadatos en pills neutras (el color solo comunica: prioridad
            // alta o fecha vencida). Sin metadatos, la fila queda limpia.
            val recurrence = TaskRecurrence.fromCode(task.recurrence)
            val hasMeta = priority != TaskPriority.NONE ||
                task.dueAt != null || task.reminderAt != null ||
                recurrence != TaskRecurrence.NONE
            if (hasMeta) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    if (priority != TaskPriority.NONE) {
                        PriorityPill(priority = priority)
                    }
                    task.dueAt?.let { due ->
                        DuePill(dueAt = due)
                    }
                    task.reminderAt?.let { at ->
                        ReminderPill(label = formatReminderLabel(at))
                    }
                    if (recurrence != TaskRecurrence.NONE) {
                        RecurrencePill(recurrence = recurrence)
                    }
                }
            }
        }
        // Un solo botón de opciones (⋮) en vez de los dos iconos directos
        // de antes (campana + papelera): la fila respira y sigue siendo
        // fácil de escanear. Ver [RowOverflowMenu].
        RowOverflowMenu(
            task = task,
            onToggleReminder = onToggleReminder,
            onDelete = onDelete
        )
    }
}

/**
 * Menú de opciones de la fila: «Añadir/Quitar recordatorio» y «Eliminar».
 *
 * Sustituye a los dos botones de icono directos (campana + papelera), que
 * hacían la fila más densa de lo necesario. El menú es descubrible (un
 * botón visible, etiquetado «Más opciones»), accesible y no añade gestos
 * que aprender. Las acciones conservan su flujo: eliminar sigue pasando
 * por el Deshacer del snackbar, y el recordatorio abre el diálogo de
 * fecha y hora.
 */
@Composable
private fun RowOverflowMenu(
    task: Task,
    onToggleReminder: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Más opciones",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val hasReminder = task.reminderAt != null
            DropdownMenuItem(
                text = {
                    Text(if (hasReminder) "Quitar recordatorio" else "Añadir recordatorio")
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (hasReminder) {
                            Icons.Outlined.NotificationsOff
                        } else {
                            Icons.Outlined.Notifications
                        },
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onToggleReminder(task)
                }
            )
            DropdownMenuItem(
                text = { Text("Eliminar") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null
                    )
                },
                onClick = {
                    expanded = false
                    onDelete(task)
                }
            )
        }
    }
}

/**
 * Pill pequeña de metadato (campana, bandera, calendario). Neutra a
 * propósito: el acento se reserva para acciones y estados, no para
 * metadatos que aparecen en cada fila. Solo la prioridad alta y la fecha
 * vencida usan el color de error, porque ahí sí hay algo que comunicar.
 *
 * Solo usa tokens del tema propio (`surfaceVariant` / `onSurfaceVariant` /
 * `labelSmall` de [AhoraTheme]): nada depende de los defaults de M3.
 */
@Composable
private fun MetaPill(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Sizes.pillIcon)
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun ReminderPill(label: String) {
    MetaPill(icon = Icons.Outlined.Notifications, label = label)
}

/** Bandera con la prioridad. Solo Alta usa el color de error; el resto es neutro. */
@Composable
private fun PriorityPill(priority: TaskPriority) {
    val color = when (priority) {
        TaskPriority.HIGH -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    MetaPill(
        icon = Icons.Outlined.Flag,
        label = priority.label,
        contentColor = color
    )
}

/** Calendario con la fecha límite. En rojo solo si ya venció. */
@Composable
private fun DuePill(dueAt: Long) {
    val overdue = isOverdue(dueAt)
    MetaPill(
        icon = Icons.Outlined.CalendarToday,
        label = formatDueLabel(dueAt),
        contentColor = if (overdue) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Repetición con la frecuencia (ETAPA 11). Neutra como las demás pills. */
@Composable
private fun RecurrencePill(recurrence: TaskRecurrence) {
    MetaPill(icon = Icons.Outlined.Repeat, label = recurrence.label)
}

/**
 * Botón circular de completado, con semántica de checkbox para accesibilidad.
 * [label] identifica la tarea en el anuncio de TalkBack (el estado
 * marcada/no marcada lo anuncia el `toggleable` solo).
 * Al marcar/desmarcar, el círculo crece con un spring sutil (sin rebote) y
 * el check entra con escala; además se emite un tick háptico corto como
 * confirmación ([Haptics.tick]: sutil, solo cuando aporta; el sistema
 * decide si vibra según sus ajustes).
 * El acento aquí sí comunica: es el estado de la tarea.
 */
@Composable
private fun CircularCheckButton(
    checked: Boolean,
    onToggle: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline,
        label = "checkBorder"
    )
    val background by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary
        else Color.Transparent,
        label = "checkBackground"
    )
    // Crecimiento sutil del círculo al cambiar de estado (sin rebote).
    val circleSize by animateDpAsState(
        targetValue = if (checked) Sizes.checkCircleChecked else Sizes.checkCircle,
        animationSpec = Motion.checkSpring(),
        label = "checkCircleSize"
    )
    val haptics = LocalHapticFeedback.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(Sizes.minTouchRow)
            .clip(CircleShape)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = {
                    // Tick háptico corto como confirmación (FASE 9): sutil,
                    // solo cuando aporta; el sistema decide si vibra según
                    // sus ajustes. Ver [Haptics] para la revisión holística.
                    Haptics.tick(haptics)
                    onToggle()
                }
            )
            // La etiqueta identifica la tarea en el anuncio de TalkBack
            // ("Comprar pan, casilla de verificación, no marcada"): se
            // fusiona en el mismo nodo del `toggleable`, sin duplicar.
            .semantics { contentDescription = label }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(background)
                .border(Sizes.checkStroke, borderColor, CircleShape)
        ) {
            AnimatedVisibility(
                visible = checked,
                enter = scaleIn(animationSpec = Motion.checkSpring()) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Sizes.checkIcon)
                )
            }
        }
    }
}
