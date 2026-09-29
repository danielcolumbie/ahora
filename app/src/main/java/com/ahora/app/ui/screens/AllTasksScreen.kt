package com.ahora.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahora.app.data.Task
import com.ahora.app.ui.MainViewModel
import com.ahora.app.ui.adaptive.AdaptiveLayout
import com.ahora.app.ui.components.AdaptiveListDetail
import com.ahora.app.ui.components.CollectUiEvents
import com.ahora.app.ui.components.EmptyState
import com.ahora.app.ui.components.TaskDetailPanel
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Los insets del sistema ya los aplica el Scaffold de NavGraph:
        // no se reaplican aquí (bloque J: evita doble padding).
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        // BoxWithConstraints (bloque J): el layout se decide por el
        // espacio real disponible, sin dependencias nuevas.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val wide = AdaptiveLayout.isTwoPane(maxWidth)
            val compactHeader = AdaptiveLayout.isCompactHeader(maxHeight)
            // Tarea seleccionada para el panel de detalle (dos paneles).
            // Sobrevive a la rotación; se limpia si sale de la lista.
            var selectedTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
            LaunchedEffect(tasks, selectedTaskId) {
                if (selectedTaskId != null &&
                    tasks.none { it.id == selectedTaskId }
                ) {
                    selectedTaskId = null
                }
            }
            // Instancias estables (bloque K, rendimiento): sin esto, cada
            // recomposición de la pantalla (p. ej. cada tecla en la
            // búsqueda) creaba lambdas nuevas que forzaban la recomposición
            // de la lista, sus filas y el panel de detalle.
            val onEditRequest: ((Task) -> Unit)? = remember(wide) {
                if (wide) { { task -> selectedTaskId = task.id } } else null
            }
            val onDetailDelete: (Task) -> Unit = remember {
                { task ->
                    if (task.id == selectedTaskId) selectedTaskId = null
                    viewModel.deleteTask(task)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.screenHorizontal)
            ) {
                Spacer(modifier = Modifier.height(if (compactHeader) Spacing.s else Spacing.xxl))
                Text(
                    text = "Todas",
                    style = MaterialTheme.typography.titleLarge,
                    // Encabezado para la navegación de TalkBack (bloque I).
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(if (compactHeader) Spacing.s else Spacing.m))

                TextField(
                    value = query,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar tareas…") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            // Decorativo (bloque I): no tiene acción; el
                            // `placeholder` ya etiqueta el campo para TalkBack.
                            contentDescription = null,
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

                // Lista + detalle en pantallas anchas; solo la lista en
                // teléfonos (el detalle se abre como diálogo, como antes).
                AdaptiveListDetail(
                    wide = wide,
                    modifier = Modifier.weight(1f),
                    list = { listModifier ->
                        Box(modifier = listModifier) {
                            if (tasks.isEmpty()) {
                                if (searching) {
                                    EmptyState(
                                        onAdd = {},
                                        showAddButton = false,
                                        title = "Sin resultados",
                                        subtitle = "Nada coincide con «$query».",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    EmptyState(
                                        onAdd = {},
                                        showAddButton = false,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    if (searching) {
                                        Text(
                                            text = if (tasks.size == 1) "1 resultado"
                                            else "${tasks.size} resultados",
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
                                        scrollToTopEvents = viewModel.scrollToTopEvents,
                                        modifier = Modifier.weight(1f),
                                        onEditRequest = onEditRequest
                                    )
                                }
                            }
                        }
                    },
                    detail = { detailModifier ->
                        TaskDetailPanel(
                            task = tasks.firstOrNull { it.id == selectedTaskId },
                            onToggleDone = viewModel::toggleDone,
                            onDelete = onDetailDelete,
                            onUpdateDetails = viewModel::updateDetails,
                            onSetReminder = viewModel::setReminder,
                            onClearReminder = viewModel::clearReminder,
                            onPastReminder = viewModel::pastReminderSelected,
                            alarmScheduler = viewModel.scheduler,
                            modifier = detailModifier
                        )
                    }
                )
            }
        }
    }
}
