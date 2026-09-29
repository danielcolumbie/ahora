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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ahora.app.data.Task
import com.ahora.app.data.TaskPriority
import com.ahora.app.data.TaskRecurrence
import com.ahora.app.ui.theme.Motion
import com.ahora.app.ui.theme.Spacing

/**
 * Fila de tarea: checkbox circular, texto y acciones sobre el fondo.
 *
 * Sin tarjeta: el contenido es protagonista y las filas se separan con un
 * divisor sutil (ver [TasksColumn]). La superficie solo aparece donde aporta
 * jerarquía; aquí no aporta nada.
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
            .alpha(alpha)
            .padding(vertical = Spacing.m)
    ) {
        CircularCheckButton(
            checked = task.isDone,
            onToggle = { onToggleDone(task) }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = { onEdit(task) })
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
        IconButton(onClick = { onToggleReminder(task) }) {
            Icon(
                imageVector = if (task.reminderAt == null) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                contentDescription = if (task.reminderAt == null) "Añadir recordatorio" else "Quitar recordatorio",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = { onDelete(task) }) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Eliminar tarea",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Pill pequeña de metadato (campana, bandera, calendario). Neutra a
 * propósito: el acento se reserva para acciones y estados, no para
 * metadatos que aparecen en cada fila. Solo la prioridad alta y la fecha
 * vencida usan el color de error, porque ahí sí hay algo que comunicar.
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
                modifier = Modifier.size(Spacing.m)
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

/** Bandera con la prioridad. Solo Alta usa el color de error. */
@Composable
private fun PriorityPill(priority: TaskPriority) {
    val color = when (priority) {
        TaskPriority.HIGH -> MaterialTheme.colorScheme.error
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.tertiary
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
 * Al marcar/desmarcar, el círculo rebota con un spring y el check entra con escala.
 * El acento aquí sí comunica: es el estado de la tarea.
 */
@Composable
private fun CircularCheckButton(
    checked: Boolean,
    onToggle: () -> Unit,
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
    // Rebote sutil del círculo al cambiar de estado.
    val circleSize by animateDpAsState(
        targetValue = if (checked) 27.dp else 24.dp,
        animationSpec = Motion.bouncySpring(),
        label = "checkCircleSize"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(Spacing.minTouchRow)
            .clip(CircleShape)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .background(background)
                .border(2.dp, borderColor, CircleShape)
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
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
