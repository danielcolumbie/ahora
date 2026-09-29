package com.ahora.app.ui.components

import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable

/**
 * Colores de chips seleccionados con la paleta de Ahora (auditoría
 * post-rediseño, 1.25.0).
 *
 * Sin esto, `FilterChip` e `InputChip` usan el `secondaryContainer` por
 * defecto de Material 3 (un lila ajeno al sistema de un solo acento),
 * porque `AhoraTheme` no lo define. El seleccionado comunica con el
 * `primaryContainer` propio, igual que el indicador de la barra de
 * navegación y el selector de tema de Ajustes.
 *
 * Sirve para `FilterChip` e `InputChip`: ambos aceptan
 * [SelectableChipColors].
 */
@Composable
fun ahoraSelectedChipColors(): SelectableChipColors {
    val scheme = MaterialTheme.colorScheme
    return FilterChipDefaults.filterChipColors(
        selectedContainerColor = scheme.primaryContainer,
        selectedLabelColor = scheme.onPrimaryContainer,
        selectedLeadingIconColor = scheme.onPrimaryContainer,
        selectedTrailingIconColor = scheme.onPrimaryContainer
    )
}
