package com.ahora.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ahora.app.data.SettingsRepository
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
