package com.ahora.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/**
 * Título de diálogo de Ahora (evolución visual, 2026-09-30).
 *
 * `AlertDialog` no aplica ningún estilo tipográfico a su slot de título por
 * sí solo: un `Text()` plano cae a `bodyLarge` y el título parece cuerpo de
 * texto. Este componente aplica el rol «Título de pantalla» del sistema de
 * diseño (`titleLarge`), el mismo que usan «Todas» y «Ajustes», para que
 * todos los diálogos compartan la jerarquía. Marcado como encabezado para
 * TalkBack.
 */
@Composable
fun DialogTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() }
    )
}
