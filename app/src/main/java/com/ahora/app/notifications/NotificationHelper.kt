package com.ahora.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ahora.app.MainActivity
import com.ahora.app.R
import com.ahora.app.data.Task

/**
 * Notificaciones 100% locales: sin Firebase, sin servidor, sin internet.
 *
 * Detalles que hacen que el aviso se vea y se oiga de verdad:
 * - Icono propio de campana para la barra de estado (el icono del launcher
 *   ahí se ve tenue o invisible).
 * - En Android 8+ el sonido y la vibración los gobierna el CANAL, no el
 *   builder: el canal se crea con ambos explícitos y los ajustes de la app
 *   se aplican al canal con [applyPreferences].
 * - Se usa un canal nuevo ("_v2") porque Android no deja cambiar la
 *   configuración de un canal ya creado.
 */
object NotificationHelper {

    const val CHANNEL_ID = "ahora_recordatorios_v2"
    private const val CHANNEL_NAME = "Recordatorios"
    private const val LEGACY_CHANNEL_ID = "ahora_recordatorios"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Limpia el canal de la versión anterior para no dejar ajustes viejos duplicados.
        if (manager.getNotificationChannel(LEGACY_CHANNEL_ID) != null) {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos de tus recordatorios"
            // Explícito: en Android 8+ esto es lo que realmente suena y vibra.
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                null
            )
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    /**
     * Aplica los ajustes de sonido/vibración de la app al canal del sistema.
     * Sin esto, en Android 8+ los interruptores de Ajustes no tendrían
     * efecto real porque el canal manda sobre el builder.
     */
    fun applyPreferences(context: Context, withSound: Boolean, withVibration: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(CHANNEL_ID) ?: return
        if (withSound) {
            channel.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                null
            )
        } else {
            channel.setSound(null, null)
        }
        channel.enableVibration(withVibration)
        manager.createNotificationChannel(channel)
    }

    /**
     * true si el sistema permite publicar notificaciones ahora mismo.
     * En Android 13+ mira el permiso POST_NOTIFICATIONS; antes, el
     * interruptor global de notificaciones de la app.
     */
    fun canPostNotifications(context: Context): Boolean {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            manager.areNotificationsEnabled()
        }
    }

    fun showReminder(
        context: Context,
        task: Task,
        withSound: Boolean,
        withVibration: Boolean
    ) {
        // Sin permiso del sistema, notify() puede lanzar SecurityException
        // (Android 13+): no hay nada que mostrar, salir sin tumbar el proceso.
        if (!canPostNotifications(context)) return
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
            .setSmallIcon(R.drawable.ic_stat_recordatorio)
            .setColor(context.getColor(R.color.notif_accent))
            .setContentTitle(task.title)
            .setContentText("Es hora de hacerlo")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Es hora de hacerlo: ${task.title}")
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setWhen(task.reminderAt ?: System.currentTimeMillis())
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(tapPending)

        // En Android 8+ el canal gobierna; esto cubre Android 7 y anteriores.
        if (withSound) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        } else {
            builder.setSilent(true)
        }
        if (withVibration) {
            builder.setVibrate(longArrayOf(0, 400, 200, 400))
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // El permiso pudo revocarse entre la comprobación y el aviso:
        // nunca tumbar el proceso justo cuando debe sonar la alarma.
        runCatching { manager.notify(task.id.toInt(), builder.build()) }
    }
}
