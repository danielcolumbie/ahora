package com.ahora.app.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Invariantes del design system (BLOQUE A del rediseño premium): los tokens
 * que la auditoría visual pidió centralizar deben existir y ser coherentes.
 */
class DesignSystemTest {

    @Test
    fun `labelSmall esta definido en la tipografia propia`() {
        assertEquals(12.sp, AhoraTypography.labelSmall.fontSize)
    }

    @Test
    fun `el circulo marcado es mayor que el sin marcar`() {
        assertTrue(Sizes.checkCircleChecked > Sizes.checkCircle)
    }

    @Test
    fun `el area tactil minima respeta accesibilidad`() {
        assertTrue(Sizes.minTouchRow >= 48.dp)
    }
}
