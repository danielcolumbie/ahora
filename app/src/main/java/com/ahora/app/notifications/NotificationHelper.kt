package com.ahora.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.ahora.app.MainActivity
import com.ahora.app.R
import com.ahora.app.data.Task

/**
 * Notificaciones 100% locales: sin Firebase, sin servidor, sin internet.
 */
object NotificationHelper {

    const val CHANNEL_ID = "ahora_recordatorios"
    private const val CHANNEL_NAME = "Recordatorios"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos de tus recordatorios"
        }
        manager.createNotificationChannel(channel)
    }

    fun showReminder(
        context: Context,
        task: Task,
        withSound: Boolean,
        withVibration: Boolean
    ) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val tapPending = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(task.title)
            .setContentText("Recordatorio de Ahora")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapPending)

        if (withSound) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            builder.setSilent(true)
        }
        if (withVibration) {
            builder.setVibrate(longArrayOf(0, 250, 250, 250))
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(task.id.toInt(), builder.build())
    }
}
