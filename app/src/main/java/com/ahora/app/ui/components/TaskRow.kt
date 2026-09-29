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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.ahora.app.data.Task
import com.ahora.app.ui.theme.Motion

/**
 * Tarjeta de tarea: checkbox circular, texto, pill de recordatorio y acciones.
 *
 * Los callbacks reciben la tarea en vez de lambdas `() -> Unit` creadas por
 * fila: así son la misma instancia para todas las filas y Compose puede
 * saltar la recomposición de las que no cambiaron.
 */
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

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            CircularCheckButton(
                checked = task.isDone,
                onToggle = { onToggleDone(task) }
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = { onEdit(task) })
                    .padding(horizontal = 8.dp)
            ) {
                Text(text = task.title, style = textStyle)
                task.reminderAt?.let { at ->
                    Spacer(modifier = Modifier.height(6.dp))
                    ReminderPill(label = formatReminderLabel(at))
                }
            }
            IconButton(onClick = { onToggleReminder(task) }) {
                Icon(
                    imageVector = if (task.reminderAt == null) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                    contentDescription = if (task.reminderAt == null) "Añadir recordatorio" else "Quitar recordatorio"
                )
            }
            IconButton(onClick = { onDelete(task) }) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Eliminar tarea"
                )
            }
        }
    }
}

/** Pill pequeña con campana para la etiqueta del recordatorio. */
@Composable
private fun ReminderPill(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Botón circular de completado, con semántica de checkbox para accesibilidad.
 * Al marcar/desmarcar, el círculo rebota con un spring y el check entra con escala.
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
            .size(44.dp)
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
