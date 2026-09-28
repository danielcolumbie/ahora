package com.ahora.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Programa recordatorios con AlarmManager: funcionan sin internet
 * y sobreviven al cierre de la app (se reprograman al reiniciar).
 */
class ReminderScheduler(private val context: Context) {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
        private const val REQUEST_CODE_OFFSET = 10_000
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            (REQUEST_CODE_OFFSET + taskId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun schedule(taskId: Long, atMillis: Long) {
        val pending = pendingIntent(taskId)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        } else {
            // Sin permiso de alarmas exactas: se usa la mejor aproximación disponible.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending)
        }
    }

    fun cancel(taskId: Long) {
        alarmManager.cancel(pendingIntent(taskId))
    }
}
