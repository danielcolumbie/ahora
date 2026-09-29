package com.ahora.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Contrato mínimo para programar/cancelar recordatorios.
 * Existe como interfaz para poder probar [com.ahora.app.domain.TaskRepository]
 * y el aviso de permiso de alarmas exactas en JVM con un programador falso,
 * sin AlarmManager ni Context.
 */
interface AlarmScheduler {
    fun schedule(taskId: Long, atMillis: Long)
    fun cancel(taskId: Long)

    /**
     * true si se puede usar alarma exacta. En Android 11 y anteriores siempre
     * es true (no existe el permiso); en Android 12+ depende del usuario.
     */
    fun hasExactAlarmPermission(): Boolean

    /**
     * Intent a los ajustes del sistema para pedir el permiso de alarmas
     * exactas (Android 12+). null en versiones anteriores (no hace falta).
     */
    fun exactAlarmSettingsIntent(): Intent?
}

/**
 * Programa recordatorios con AlarmManager: funcionan sin internet
 * y sobreviven al cierre de la app (se reprograman al reiniciar).
 *
 * Precisión:
 * - Android 11 y anteriores: alarma exacta siempre (no necesita permiso).
 * - Android 12+: alarma exacta solo con el permiso de "alarmas exactas";
 *   sin él se usa la mejor aproximación disponible, que puede llegar tarde.
 */
class ReminderScheduler(private val context: Context) : AlarmScheduler {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        private const val REQUEST_CODE_OFFSET = 10_000

        /**
         * Código de request del PendingIntent de una tarea. Función pura:
         * cada tarea necesita un PendingIntent distinto para que sus alarmas
         * no se pisen entre sí.
         */
        internal fun requestCodeFor(taskId: Long): Int = (REQUEST_CODE_OFFSET + taskId).toInt()
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCodeFor(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun schedule(taskId: Long, atMillis: Long) {
        val pending = pendingIntent(taskId)
        if (hasExactAlarmPermission()) {
            runCatching {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
            }.onFailure {
                // El permiso pudo revocarse entre la comprobación y la llamada
                // (p. ej. al reprogramar tras el reinicio): degradar a alarma
                // inexacta en vez de tumbar la app.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
            }
        } else {
            // Sin permiso de alarmas exactas (Android 12+): la mejor aproximación disponible.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        }
    }

    override fun cancel(taskId: Long) {
        alarmManager.cancel(pendingIntent(taskId))
    }

    /**
     * true si se puede usar alarma exacta. En Android 11 y anteriores siempre
     * es true (no existe el permiso); en Android 12+ depende del usuario.
     */
    override fun hasExactAlarmPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /**
     * Intent a los ajustes del sistema para pedir el permiso de alarmas
     * exactas (Android 12+). null en versiones anteriores (no hace falta).
     */
    override fun exactAlarmSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(
                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            null
        }
}
