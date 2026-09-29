package com.ahora.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.ahora.app.AhoraApplication
import com.ahora.app.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Widget de pantalla de inicio (ETAPA 12): muestra las tareas pendientes
 * de hoy y las próximas, con prioridad y fecha límite.
 *
 * - Tocar una fila abre la app.
 * - El círculo de cada fila marca la tarea como hecha sin abrir la app
 *   (pasa por [com.ahora.app.domain.TaskRepository.toggleDone], así que
 *   respeta recurrencia y alarmas como en la app).
 * - El botón "+" abre la app (crear rápido vive en la app; el widget no
 *   duplica la UI de captura).
 */
class AhoraWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { id ->
                    runCatching {
                        WidgetRefresher.updateWidget(context.applicationContext, appWidgetManager, id)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_DONE -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId == -1L) return
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val app = context.applicationContext as AhoraApplication
                        val repository = app.container.taskRepository
                        val task = app.container.database.taskDao().getById(taskId)
                        // La tarea pudo borrarse desde la app entre el
                        // refresco del widget y el toque: no hacer nada.
                        if (task != null && !task.isDone) {
                            repository.toggleDone(task)
                        }
                    } finally {
                        // toggleDone ya dispara el refresco reactivo vía el
                        // observador de la Application, pero se fuerza aquí
                        // también: si el proceso se creó solo para este
                        // broadcast, el colector puede no haber arrancado.
                        WidgetRefresher.refreshAll(context.applicationContext)
                        pending.finish()
                    }
                }
            }
            ACTION_OPEN_APP -> {
                context.startActivity(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            else -> super.onReceive(context, intent)
        }
    }

    companion object {
        const val ACTION_TOGGLE_DONE = "com.ahora.app.widget.ACTION_TOGGLE_DONE"
        const val ACTION_OPEN_APP = "com.ahora.app.widget.ACTION_OPEN_APP"
        const val EXTRA_TASK_ID = "com.ahora.app.widget.EXTRA_TASK_ID"
    }
}
