package com.ahora.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ahora.app.AhoraApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Tras reiniciar el teléfono, las alarmas se pierden:
 * aquí se vuelven a programar desde la base de datos local.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as AhoraApplication
                app.container.taskRepository.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }
}
