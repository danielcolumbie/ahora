package com.ahora.app

import android.app.Application
import com.ahora.app.di.AppContainer
import com.ahora.app.notifications.NotificationHelper
import com.ahora.app.widget.WidgetRefresher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class AhoraApplication : Application() {

    lateinit var container: AppContainer
        private set

    /**
     * Alcance para el trabajo de arranque en segundo plano. La app vive
     * mientras el proceso viva, así que no hay fuga que cancelar.
     */
    private val startScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
        // Reconciliación en frío: AlarmManager no permite enumerar las
        // alarmas programadas, así que se reprograman todas las futuras
        // (idempotente: el PendingIntent es el mismo y se sobrescribe) y se
        // limpian los recordatorios vencidos. Sin polling: solo al arrancar.
        startScope.launch {
            runCatching {
                container.taskRepository.pruneExpiredReminders()
                container.taskRepository.rescheduleAll()
            }
        }
        // Widget (ETAPA 12): refresco reactivo, sin polling. Room solo emite
        // cuando la tabla cambia, así que no hay trabajo periódico ni
        // temporizadores: el widget se actualiza exactamente cuando cambian
        // las tareas (crear, completar, editar, borrar, importar, etc.).
        startScope.launch {
            runCatching {
                container.taskRepository.observePending()
                    .distinctUntilChanged()
                    .collect { WidgetRefresher.refreshAll(this@AhoraApplication) }
            }
        }
    }
}
