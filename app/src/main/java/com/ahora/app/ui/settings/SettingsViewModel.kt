package com.ahora.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ahora.app.data.SettingsRepository
import com.ahora.app.data.Tag
import com.ahora.app.data.ThemeMode
import com.ahora.app.di.AppContainer
import com.ahora.app.domain.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settings: SettingsRepository,
    private val tasks: TaskRepository
) : ViewModel() {

    val themeMode: StateFlow<Int> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    val notificationsEnabled: StateFlow<Boolean> = settings.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setThemeMode(mode: Int) = viewModelScope.launch {
        settings.setThemeMode(mode)
    }

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setNotificationsEnabled(enabled)
    }

    /**
     * Todas las etiquetas (1.28.0): el diálogo de gestión las lista en
     * vivo. El mapa tarea → etiquetas sirve para mostrar el conteo de
     * uso de cada una.
     */
    val tags: StateFlow<List<Tag>> = tasks.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val taskTags: StateFlow<Map<Long, List<Tag>>> = tasks.observeTaskTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /**
     * Gestión de etiquetas (1.28.0): llamadas suspendidas porque el
     * diálogo las usa con `scope.launch { runCatching { ... } }` y avisa
     * en el snackbar si algo falla (mismo patrón que el respaldo).
     */
    suspend fun createTag(name: String, colorIndex: Int): Long =
        tasks.createTag(name, colorIndex)

    suspend fun updateTag(tag: Tag, name: String, colorIndex: Int) =
        tasks.updateTag(tag, name, colorIndex)

    suspend fun deleteTag(tag: Tag) = tasks.deleteTag(tag)

    /** Cuántas tareas usan una etiqueta, para avisar al eliminarla. */
    suspend fun tagTaskCount(tag: Tag): Int = tasks.tagTaskCount(tag)

    /** Respaldo local: el JSON de todas las tareas, para guardar en un archivo. */
    suspend fun exportBackup(): String = tasks.exportTasks()

    /** Restaura un respaldo y reprograma los recordatorios. */
    suspend fun importBackup(json: String): TaskRepository.ImportResult =
        tasks.importTasks(json)
}

class SettingsViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(container.settingsRepository, container.taskRepository) as T
}
