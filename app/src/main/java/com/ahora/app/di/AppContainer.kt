package com.ahora.app.di

import android.content.Context
import com.ahora.app.data.AhoraDatabase
import com.ahora.app.data.SettingsRepository
import com.ahora.app.data.dataStore
import com.ahora.app.domain.TaskRepository
import com.ahora.app.notifications.ReminderScheduler
import com.ahora.app.speech.SpeechInputManager

/**
 * Contenedor manual de dependencias (sin frameworks de DI:
 * la app es pequeña y esto la mantiene simple y ampliable).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AhoraDatabase = AhoraDatabase.getInstance(appContext)

    val reminderScheduler = ReminderScheduler(appContext)

    val taskRepository = TaskRepository(
        dao = database.taskDao(),
        tagDao = database.tagDao(),
        scheduler = reminderScheduler
    )

    val settingsRepository = SettingsRepository(appContext.dataStore)

    /** Se crea bajo demanda porque necesita la Activity para los permisos. */
    fun createSpeechInputManager(context: Context): SpeechInputManager =
        SpeechInputManager(context)
}
