package com.ahora.app.ui

import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ahora.app.data.SettingsRepository
import com.ahora.app.data.Task
import com.ahora.app.di.AppContainer
import com.ahora.app.domain.TaskRepository
import com.ahora.app.notifications.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Eventos de una sola vez para mostrar en el Snackbar. */
sealed interface UiEvent {
    data class TaskDeleted(val task: Task) : UiEvent
    data class Message(val text: String) : UiEvent
}

/**
 * ¿Mostrar el aviso de "permiso de alarmas exactas revocado"?
 * Función pura para poder probarla. Solo tiene sentido en Android 12+,
 * cuando hay recordatorios futuros pendientes (sin ellos el aviso sería
 * ruido) y si el usuario no lo descartó ya.
 */
internal fun shouldShowExactAlarmNudge(
    sdkAtLeastS: Boolean,
    hasExactAlarmPermission: Boolean,
    hasFutureReminders: Boolean,
    nudgeDismissed: Boolean
): Boolean = sdkAtLeastS && !hasExactAlarmPermission && hasFutureReminders && !nudgeDismissed

/** Estado y acciones compartidos por las pantallas Hoy y Todas. */
class MainViewModel(
    private val repository: TaskRepository,
    private val scheduler: AlarmScheduler,
    private val settings: SettingsRepository
) : ViewModel() {

    val pendingTasks: StateFlow<List<Task>> = repository.observePending()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allTasks: StateFlow<List<Task>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Buffer de 1: emitir nunca suspende aunque ninguna pantalla esté
    // recolectando (p. ej. durante una transición de navegación). Sin esto,
    // borrar desde "Todas" colgaba la corrutina y se perdía el Deshacer.
    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    /**
     * Aviso puntual si el sistema revocó el permiso de alarmas exactas
     * (Android 12+): sin él, los recordatorios pueden llegar tarde.
     * No insistente: se descarta y no vuelve hasta que el permiso se
     * conceda y se revoque de nuevo.
     */
    private val _exactAlarmNudge = MutableStateFlow(false)
    val exactAlarmNudge: StateFlow<Boolean> = _exactAlarmNudge.asStateFlow()

    init {
        refreshExactAlarmNudge()
    }

    fun refreshExactAlarmNudge() = viewModelScope.launch(Dispatchers.IO) {
        val hasPermission = scheduler.hasExactAlarmPermission()
        // Si el permiso volvió, se rearma el aviso para una futura revocación.
        if (hasPermission) settings.setExactAlarmNudgeDismissed(false)
        _exactAlarmNudge.value = shouldShowExactAlarmNudge(
            sdkAtLeastS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            hasExactAlarmPermission = hasPermission,
            hasFutureReminders = repository.hasFutureReminders(),
            nudgeDismissed = settings.exactAlarmNudgeDismissed.first()
        )
    }

    fun dismissExactAlarmNudge() = viewModelScope.launch {
        settings.setExactAlarmNudgeDismissed(true)
        _exactAlarmNudge.value = false
    }

    fun exactAlarmSettingsIntent(): Intent? = scheduler.exactAlarmSettingsIntent()

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            runCatching { repository.add(title) }
                .onFailure { _events.emit(UiEvent.Message("No se pudo guardar la tarea")) }
        }
    }

    fun toggleDone(task: Task) = viewModelScope.launch {
        runCatching { repository.toggleDone(task) }
            .onFailure { _events.emit(UiEvent.Message("No se pudo actualizar la tarea")) }
    }

    fun deleteTask(task: Task) = viewModelScope.launch {
        runCatching { repository.delete(task) }
            .onSuccess { _events.emit(UiEvent.TaskDeleted(task)) }
            .onFailure { _events.emit(UiEvent.Message("No se pudo eliminar la tarea")) }
    }

    fun undoDelete(task: Task) = viewModelScope.launch {
        runCatching { repository.restore(task) }
            .onFailure { _events.emit(UiEvent.Message("No se pudo deshacer")) }
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
            .onFailure { _events.emit(UiEvent.Message("Esa hora ya pasó, elige una futura")) }
    }

    /** El usuario eligió una fecha/hora pasada en el diálogo: avisar, no descartar en silencio. */
    fun pastReminderSelected() = viewModelScope.launch {
        _events.emit(UiEvent.Message("Esa hora ya pasó, elige una futura"))
    }

    fun clearReminder(task: Task) = viewModelScope.launch {
        runCatching { repository.clearReminder(task) }
            .onFailure { _events.emit(UiEvent.Message("No se pudo quitar el recordatorio")) }
    }
}

class MainViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MainViewModel(
            container.taskRepository,
            container.reminderScheduler,
            container.settingsRepository
        ) as T
}
