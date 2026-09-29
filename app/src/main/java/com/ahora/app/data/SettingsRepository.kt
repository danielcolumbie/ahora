package com.ahora.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ajustes")

/** Modos de apariencia: 0 = automática, 1 = claro, 2 = oscuro. */
object ThemeMode {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
}

/**
 * Ajustes de la app guardados localmente con DataStore.
 * Todo permanece en el dispositivo: no hay cuentas ni servidores.
 */
class SettingsRepository(private val context: Context) {

    private val themeModeKey = intPreferencesKey("theme_mode")
    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")

    val themeMode: Flow<Int> =
        context.dataStore.data.map { it[themeModeKey] ?: ThemeMode.SYSTEM }

    val notificationsEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[notificationsEnabledKey] ?: true }

    suspend fun setThemeMode(mode: Int) {
        context.dataStore.edit { it[themeModeKey] = mode }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[notificationsEnabledKey] = enabled }
    }
}
