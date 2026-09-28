package com.ahora.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahora.app.ui.MainViewModel
import com.ahora.app.ui.components.EmptyState
import com.ahora.app.ui.components.TasksColumn

/** Todas las tareas: pendientes primero, completadas al final. */
@Composable
fun AllTasksScreen(viewModel: MainViewModel) {
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "Todas", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        if (tasks.isEmpty()) {
            EmptyState(onAdd = {}, showAddButton = false, modifier = Modifier.weight(1f))
        } else {
            TasksColumn(
                tasks = tasks,
                onToggleDone = viewModel::toggleDone,
                onDelete = viewModel::deleteTask,
                onUpdateTitle = viewModel::updateTitle,
                onSetReminder = viewModel::setReminder,
                onClearReminder = viewModel::clearReminder,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
