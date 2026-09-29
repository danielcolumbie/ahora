package com.ahora.app.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Invariantes de accesibilidad del design system (BLOQUE I del rediseño
 * premium): los tokens que garantizan el área táctil mínima no pueden
 * bajar de 48dp sin romper la auditoría.
 */
class AccessibilityTokensTest {

    @Test
    fun `el area tactil minima de la fila es 48dp`() {
        assertEquals(48.dp, Sizes.minTouchRow)
    }

    @Test
    fun `el area tactil minima de ajustes es 48dp`() {
        assertEquals(48.dp, Spacing.minTouchRow)
    }

    @Test
    fun `ambos tokens de area tactil coinciden`() {
        assertEquals(Sizes.minTouchRow, Spacing.minTouchRow)
    }
}
