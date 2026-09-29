package com.ahora.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ahora.app.AhoraApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Se dispara cuando vence un recordatorio y muestra la notificación local.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as AhoraApplication
                val container = app.container

                if (!container.settingsRepository.notificationsEnabled.first()) return@launch

                val task = container.database.taskDao().getById(taskId) ?: return@launch
                // Solo avisa si la tarea sigue pendiente y el recordatorio sigue vigente.
                if (task.isDone || task.reminderAt == null) return@launch

                NotificationHelper.showReminder(context = context, task = task)

                // El recordatorio ya sonó: limpia el campo para que el pill
                // no quede obsoleto para siempre. Solo si el recordatorio
                // guardado es el que acaba de vencer (el usuario pudo poner
                // uno nuevo después de programarse esta alarma).
                if ((task.reminderAt ?: 0L) <= System.currentTimeMillis()) {
                    container.database.taskDao().upsert(task.copy(reminderAt = null))
                }
            } finally {
                pending.finish()
            }
        }
    }
}
