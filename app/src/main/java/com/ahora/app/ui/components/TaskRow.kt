package com.ahora.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

/** Fila de tarea: checkbox circular, texto, hora/fecha y acciones. */
@Composable
fun TaskRow(
    task: Task,
    onToggleDone: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleReminder: () -> Unit,
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .alpha(alpha)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        CircularCheckButton(
            checked = task.isDone,
            onToggle = onToggleDone
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEdit)
                .padding(horizontal = 8.dp)
        ) {
            Text(text = task.title, style = textStyle)
            task.reminderAt?.let { at ->
                Text(
                    text = formatReminderLabel(at),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        IconButton(onClick = onToggleReminder) {
            Icon(
                imageVector = if (task.reminderAt == null) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                contentDescription = if (task.reminderAt == null) "Añadir recordatorio" else "Quitar recordatorio"
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Eliminar tarea"
            )
        }
    }
}

/** Botón circular de completado, con semántica de checkbox para accesibilidad. */
@Composable
private fun CircularCheckButton(
    checked: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor =
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val background = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent
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
                .size(24.dp)
                .clip(CircleShape)
                .background(background)
                .border(2.dp, borderColor, CircleShape)
        ) {
            if (checked) {
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
