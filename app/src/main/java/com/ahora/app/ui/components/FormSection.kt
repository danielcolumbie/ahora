package com.ahora.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ahora.app.ui.theme.Spacing
import java.util.Locale

/**
 * Sección de formulario con la etiqueta del sistema: `titleMedium`
 * (13sp, semibold, tracking 1.5), siempre en mayúsculas, en
 * `onSurfaceVariant`. La etiqueta y el contenido se separan con
 * `Spacing.s`.
 *
 * La comparten el diálogo de edición (`EditTaskDialog`), el de creación
 * avanzada (a través de [TaskFormFields]), el de recordatorio
 * (`ReminderDialog`) y la pantalla de ajustes (`SettingsScreen`): una sola
 * etiqueta, un solo espaciado, una sola jerarquía en diálogos y ajustes.
 */
@Composable
fun FormSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}
