package com.ahora.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.ahora.app.ui.theme.Sizes
import com.ahora.app.ui.theme.Spacing

/**
 * Fila de ajustes reutilizable (BLOQUE F del rediseño premium).
 *
 * Icono en caja neutra (`surfaceVariant`) + título (`bodyLarge`) +
 * subtítulo opcional (`bodyMedium`, `onSurfaceVariant`) + control a la
 * derecha (switch, chevron, valor). Altura mínima 48dp garantizada
 * (`Spacing.minTouchRow`): el área táctil no depende del contenido.
 *
 * Sin `dp`/`sp` sueltos: todo sale de `Spacing`, `Sizes` y
 * `AhoraTypography`. El icono es decorativo (el título ya describe la
 * fila); la interacción se inyecta con [interaction] para que cada
 * variante anuncie un solo elemento en TalkBack (`Role.Button` para
 * enlaces, `Role.Switch` para interruptores con su estado).
 */
@Composable
fun SettingRow(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    interaction: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.l),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Spacing.minTouchRow)
            .padding(vertical = Spacing.s)
            .then(interaction)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Sizes.settingIconBox)
                .clip(RoundedCornerShape(Spacing.s))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Sizes.settingIcon)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing()
    }
}

/** Fila de ajustes que navega a otra pantalla o abre un diálogo: chevron a la derecha. */
@Composable
fun SettingLinkRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    SettingRow(
        title = title,
        icon = icon,
        subtitle = subtitle,
        interaction = Modifier.clickable(role = Role.Button, onClick = onClick),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Fila de ajustes con interruptor. Tocar cualquier parte de la fila lo
 * alterna; el [Switch] es solo el indicador visual (su semántica se
 * limpia para que TalkBack anuncie la fila una sola vez, con estado).
 */
@Composable
fun SettingSwitchRow(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    SettingRow(
        title = title,
        icon = icon,
        subtitle = subtitle,
        interaction = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange
        ),
        modifier = modifier
    ) {
        Switch(
            checked = checked,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics {}
        )
    }
}
