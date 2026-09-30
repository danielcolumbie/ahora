package com.ahora.app.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.ahora.app.R
import com.ahora.app.data.AhoraDatabase
import kotlinx.coroutines.runBlocking

/**
 * Sirve las filas del widget (ETAPA 12). Lee la base de datos directamente
 * en [onDataSetChanged] (hilo binder, nunca el principal): así el widget
 * funciona aunque el proceso acabara de nacer y no hay caché intermedia
 * que mantener sincronizada.
 */
class WidgetTaskService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        WidgetTaskFactory(applicationContext)
}

private class WidgetTaskFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<WidgetItem> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        // Consulta acotada (LIMIT en SQL): no se materializa toda la tabla.
        val pending = runBlocking {
            AhoraDatabase.getInstance(context).taskDao()
                .getPendingForWidget(WIDGET_MAX_ITEMS)
        }
        items = selectWidgetItems(pending)
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        return RemoteViews(context.packageName, R.layout.widget_row).apply {
            setTextViewText(R.id.widget_row_title, item.title)
            if (item.dueLabel != null) {
                setTextViewText(R.id.widget_row_due, item.dueLabel)
                setTextColor(
                    R.id.widget_row_due,
                    context.getColor(
                        if (item.overdue) R.color.widget_danger else R.color.widget_text_secondary
                    )
                )
            } else {
                setTextViewText(R.id.widget_row_due, "")
            }
            if (item.priorityLevel > 0) {
                setViewVisibility(R.id.widget_row_dot, android.view.View.VISIBLE)
                setColorFilter(item.priorityLevel)
            } else {
                setViewVisibility(R.id.widget_row_dot, android.view.View.GONE)
            }
            // Tocar la fila abre la app; el círculo marca hecha.
            setOnClickFillInIntent(
                R.id.widget_row_root,
                Intent().setAction(AhoraWidgetProvider.ACTION_OPEN_APP)
            )
            setOnClickFillInIntent(
                R.id.widget_toggle,
                Intent()
                    .setAction(AhoraWidgetProvider.ACTION_TOGGLE_DONE)
                    .putExtra(AhoraWidgetProvider.EXTRA_TASK_ID, item.id)
            )
            setContentDescription(R.id.widget_toggle, context.getString(R.string.widget_toggle_desc))
        }
    }

    private fun RemoteViews.setColorFilter(priorityLevel: Int) {
        val color = context.getColor(
            when (priorityLevel) {
                3 -> R.color.widget_danger
                2 -> R.color.widget_accent
                else -> R.color.widget_text_secondary
            }
        )
        // ImageView.setColorFilter(int) por reflexión (API de RemoteViews).
        // Protegido: si un OEM no resuelve el método, el widget sigue
        // funcionando sin el tinte en vez de crashear (auditoría 1.26.0).
        runCatching { setInt(R.id.widget_row_dot, "setColorFilter", color) }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items[position].id

    override fun hasStableIds(): Boolean = true
}
