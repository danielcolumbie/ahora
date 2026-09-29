package com.ahora.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahora.app.ui.MainViewModel
import com.ahora.app.ui.components.CollectUiEvents
import com.ahora.app.ui.components.EmptyState
import com.ahora.app.ui.components.TasksColumn
import com.ahora.app.ui.theme.Spacing

/** Todas las tareas, con búsqueda por título. */
@Composable
fun AllTasksScreen(viewModel: MainViewModel) {
    // Un único flujo sirve los dos estados: sin texto equivale a la lista
    // completa, con texto a los resultados (consultados con debounce).
    val tasks by viewModel.searchResults.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val searching = query.isNotBlank()

    // El Deshacer también funciona aquí: antes el evento se perdía porque
    // solo la pantalla Hoy recolectaba los eventos del ViewModel.
    CollectUiEvents(viewModel, snackbarHostState)

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.screenHorizontal)
        ) {
            Spacer(modifier = Modifier.height(Spacing.xxl))
            Text(text = "Todas", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(Spacing.m))

            TextField(
                value = query,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar tareas…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searching) {
                        IconButton(onClick = viewModel::clearSearch) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Limpiar búsqueda",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                ),
                shape = RoundedCornerShape(Spacing.l),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(Spacing.s))

            if (tasks.isEmpty()) {
                if (searching) {
                    EmptyState(
                        onAdd = {},
                        showAddButton = false,
                        title = "Sin resultados",
                        subtitle = "Nada coincide con «$query».",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    EmptyState(onAdd = {}, showAddButton = false, modifier = Modifier.weight(1f))
                }
            } else {
                if (searching) {
                    Text(
                        text = if (tasks.size == 1) "1 resultado" else "${tasks.size} resultados",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                }
                TasksColumn(
                    tasks = tasks,
                    onToggleDone = viewModel::toggleDone,
                    onDelete = viewModel::deleteTask,
                    onUpdateDetails = viewModel::updateDetails,
                    onSetReminder = viewModel::setReminder,
                    onClearReminder = viewModel::clearReminder,
                    onPastReminder = viewModel::pastReminderSelected,
                    alarmScheduler = viewModel.scheduler,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
