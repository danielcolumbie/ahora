package com.ahora.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.ahora.app.data.Task

/**
 * Lista de tareas con los diálogos de editar y de recordatorio integrados.
 * La reutilizan las pantallas Hoy y Todas.
 */
@Composable
fun TasksColumn(
    tasks: List<Task>,
    onToggleDone: (Task) -> Unit,
    onDelete: (Task) -> Unit,
    onUpdateTitle: (Task, String) -> Unit,
    onSetReminder: (Task, Long) -> Unit,
    onClearReminder: (Task) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var reminderTask by remember { mutableStateOf<Task?>(null) }

    LazyColumn(modifier = modifier) {
        items(tasks, key = { it.id }) { task ->
            // Las tareas nuevas entran con una animación suave.
            AnimatedVisibility(
                visible = true,
                enter = fadeIn() + expandVertically()
            ) {
                TaskRow(
                    task = task,
                    onToggleDone = { onToggleDone(task) },
                    onDelete = { onDelete(task) },
                    onEdit = { editingTask = task },
                    onToggleReminder = {
                        if (task.reminderAt == null) reminderTask = task
                        else onClearReminder(task)
                    }
                )
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
            onConfirm = {
                onSetReminder(task, it)
                reminderTask = null
            }
        )
    }
}
