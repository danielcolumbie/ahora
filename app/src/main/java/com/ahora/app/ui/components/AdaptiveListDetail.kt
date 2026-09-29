package com.ahora.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ahora.app.ui.theme.Spacing

/**
 * Layout adaptable lista + detalle (BLOQUE J del rediseño premium,
 * FASE 15).
 *
 * En pantallas estrechas muestra solo la lista: el detalle se abre
 * como diálogo, igual que antes. En pantallas anchas (≥600dp, ver
 * `AdaptiveLayout.isTwoPane`) divide el espacio en dos paneles
 * iguales — lista a la izquierda, detalle a la derecha — separados por
 * un divisor sutil, el mismo lenguaje que separa las filas de la lista.
 *
 * Los lambdas reciben el [Modifier] con el que deben llenar su panel
 * (`fillMaxSize`): cada lado decide su contenido sin conocer el ancho.
 */
@Composable
fun AdaptiveListDetail(
    wide: Boolean,
    modifier: Modifier = Modifier,
    list: @Composable (Modifier) -> Unit,
    detail: @Composable (Modifier) -> Unit
) {
    if (!wide) {
        Box(modifier = modifier) {
            list(Modifier.fillMaxSize())
        }
    } else {
        Row(modifier = modifier) {
            Box(modifier = Modifier.weight(1f)) {
                list(Modifier.fillMaxSize())
            }
            VerticalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = Spacing.l)
            )
            Box(modifier = Modifier.weight(1f)) {
                detail(Modifier.fillMaxSize())
            }
        }
    }
}
