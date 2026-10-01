package com.ahora.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.ahora.app.data.Tag
import com.ahora.app.data.TagPalette
import com.ahora.app.ui.theme.Sizes
import com.ahora.app.ui.theme.Spacing

/**
 * Punto de color de una etiqueta (1.28.0).
 *
 * El punto es decorativo: el nombre de la etiqueta lleva el significado.
 * Como es un simple `Box` sin texto ni acción, TalkBack lo ignora solo;
 * el texto que lo acompaña ya dice de qué etiqueta se trata.
 */
@Composable
fun TagDot(colorIndex: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(Sizes.tagDot)
            .clip(CircleShape)
            .background(Color(TagPalette.colorFor(colorIndex)))
    )
}

/**
 * Pill de etiqueta en la fila de tarea (1.28.0): neutra como las demás
 * pills de metadatos (`surfaceVariant`), con el punto de color de la
 * etiqueta. El color aquí es dato (identifica la categoría), no acento
 * de acción, igual que la prioridad alta usa el error donde comunica.
 */
@Composable
fun TagPill(tag: Tag, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs)
        ) {
            TagDot(colorIndex = tag.colorIndex)
            Spacer(modifier = Modifier.width(Spacing.xs))
            Text(
                text = tag.name,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
