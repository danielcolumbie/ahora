package com.ahora.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.ahora.app.MainActivity
import com.ahora.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Construye y refresca las vistas del widget (ETAPA 12).
 *
 * Sin polling: el widget solo se actualiza cuando algo cambia —
 * [AhoraApplication][com.ahora.app.AhoraApplication] observa las tareas
 * pendientes y llama a [refreshAll] en cada cambio, y el proveedor
 * refresca tras marcar una tarea desde el widget. `updatePeriodMillis`
 * es 0: nada de despertares periódicos que gasten batería.
 */
object WidgetRefresher {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Refresca todos los widgets instalados (llamada reactiva, no periódica). */
    fun refreshAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(
            ComponentName(context, AhoraWidgetProvider::class.java)
        )
        if (ids.isEmpty()) return
        scope.launch {
            ids.forEach { id ->
                runCatching { updateWidget(context.applicationContext, manager, id) }
            }
        }
        // La lista (colección) necesita aviso explícito para releer la BD.
        ids.forEach { id ->
            manager.notifyAppWidgetViewDataChanged(id, R.id.widget_list)
        }
    }

    suspend fun updateWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        views.setTextViewText(R.id.widget_date, formatWidgetDate())
        views.setOnClickPendingIntent(R.id.widget_add, openAppIntent(context))
        views.setOnClickPendingIntent(R.id.widget_header, openAppIntent(context))

        // La lista la sirve WidgetTaskService (lee Room en onDataSetChanged).
        // El data URI único por widget evita que el sistema reutilice el
        // intent de otro widget.
        val serviceIntent = Intent(context, WidgetTaskService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.widget_list, serviceIntent)
        views.setEmptyView(R.id.widget_list, R.id.widget_empty)

        // Plantilla de clics para las filas: cada fila rellena la acción
        // (abrir la app o marcar hecha) con setOnClickFillInIntent.
        // Debe ser MUTABLE: el sistema la completa con el fill-in intent.
        val template = PendingIntent.getBroadcast(
            context,
            widgetId,
            Intent(context, AhoraWidgetProvider::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag()
        )
        views.setPendingIntentTemplate(R.id.widget_list, template)

        manager.updateAppWidget(widgetId, views)
    }

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun mutableFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
}
