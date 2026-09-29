package com.ahora.app.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.ahora.app.ui.MainViewModel
import com.ahora.app.ui.UiEvent

/**
 * Recolecta los eventos de una sola vez del [MainViewModel] y los muestra
 * en el [SnackbarHostState] de la pantalla: "Tarea eliminada" con Deshacer
 * y mensajes de error. Usado por Hoy y por Todas.
 */
@Composable
fun CollectUiEvents(
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState
) {
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.TaskDeleted -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Tarea eliminada",
                        actionLabel = "Deshacer",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.task)
                }
                is UiEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }
}
