package com.ahora.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ahora.app.AhoraApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Las alarmas de AlarmManager no sobreviven a todo: se pierden al reiniciar
 * el teléfono, quedan obsoletas si el usuario cambia la hora o la zona
 * horaria, y la actualización de la app puede dejarlas huérfanas.
 * Aquí se reconcilian con la base de datos local en cada uno de esos casos:
 * se limpian los recordatorios vencidos y se reprograman los futuros.
 *
 * Los cuatro broadcasts se pueden recibir con un receiver declarado en el
 * manifiesto (ninguno está en la lista de implícitos restringidos).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> Unit
            else -> return
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as AhoraApplication
                val repository = app.container.taskRepository
                repository.pruneExpiredReminders()
                repository.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
