package com.ahora.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.dataStore by preferencesDataStore(name = "ajustes")

/**
 * Ajustes de la app guardados localmente con DataStore.
 * Todo permanece en el dispositivo: no hay cuentas ni servidores.
 *
 * Recibe el [DataStore] ya construido (en vez del [Context]) para poder
 * probarlo en JVM con un DataStore en memoria.
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private val themeModeKey = intPreferencesKey("theme_mode")
    private val notificationsEnabledKey = booleanPreferencesKey("notifications_enabled")
    private val exactAlarmNudgeDismissedKey = booleanPreferencesKey("exact_alarm_nudge_dismissed")

    val themeMode: Flow<Int> =
        dataStore.data.map { it[themeModeKey] ?: ThemeMode.SYSTEM }

    val notificationsEnabled: Flow<Boolean> =
        dataStore.data.map { it[notificationsEnabledKey] ?: true }

    /**
     * Si el usuario ya descartó el aviso de "permiso de alarmas exactas
     * revocado". El aviso no debe ser insistente: se muestra una vez y, si
     * el permiso vuelve a concederse, se rearma para una futura revocación.
     */
    val exactAlarmNudgeDismissed: Flow<Boolean> =
        dataStore.data.map { it[exactAlarmNudgeDismissedKey] ?: false }

    suspend fun setThemeMode(mode: Int) {
        dataStore.edit { it[themeModeKey] = mode }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[notificationsEnabledKey] = enabled }
    }

    suspend fun setExactAlarmNudgeDismissed(dismissed: Boolean) {
        dataStore.edit { it[exactAlarmNudgeDismissedKey] = dismissed }
    }
}

/** Modos de apariencia: 0 = automática, 1 = claro, 2 = oscuro. */
object ThemeMode {
    const val SYSTEM = 0
    const val LIGHT = 1
    const val DARK = 2
}
