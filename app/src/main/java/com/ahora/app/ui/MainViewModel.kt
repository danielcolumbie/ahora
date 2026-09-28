package com.ahora.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ahora.app.data.Task
import com.ahora.app.di.AppContainer
import com.ahora.app.domain.TaskRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Eventos de una sola vez para mostrar en el Snackbar. */
sealed interface UiEvent {
    data class TaskDeleted(val task: Task) : UiEvent
    data class Message(val text: String) : UiEvent
}

/** Estado y acciones compartidos por las pantallas Hoy y Todas. */
class MainViewModel(private val repository: TaskRepository) : ViewModel() {

    val pendingTasks: StateFlow<List<Task>> = repository.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTasks: StateFlow<List<Task>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.add(title) }
                .onFailure { _events.emit(UiEvent.Message("No se pudo guardar la tarea")) }
        }
    }

    fun toggleDone(task: Task) = viewModelScope.launch {
        repository.toggleDone(task)
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        repository.delete(task)
        _events.emit(UiEvent.TaskDeleted(task))
    }

    fun undoDelete(task: Task) = viewModelScope.launch {
        repository.restore(task)
    }

    fun updateTitle(task: Task, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.updateTitle(task, title) }
                .onFailure { _events.emit(UiEvent.Message("No se pudo guardar el cambio")) }
        }
    }

    fun setReminder(task: Task, atMillis: Long) = viewModelScope.launch {
        runCatching { repository.setReminder(task, atMillis) }
            .onFailure { _events.emit(UiEvent.Message("El recordatorio debe ser en el futuro")) }
    }

    fun clearReminder(task: Task) = viewModelScope.launch {
        repository.clearReminder(task)
    }
}

class MainViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MainViewModel(container.taskRepository) as T
}
