package com.ahora.app.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Invariantes del sistema de espaciado: la escala debe ser estrictamente
 * creciente para que el ritmo visual sea predecible. Si alguien añade un
 * valor que rompe el orden, este test lo detecta.
 */
class SpacingTest {

    @Test
    fun `la escala de espaciado es estrictamente creciente`() {
        val scale = listOf(
            Spacing.xxs,
            Spacing.xs,
            Spacing.s,
            Spacing.m,
            Spacing.l,
            Spacing.xl,
            Spacing.xxl,
            Spacing.xxxl
        )
        scale.zipWithNext { a, b ->
            assertTrue(
                "se esperaba $a < $b en la escala de espaciado",
                a < b
            )
        }
    }

    @Test
    fun `el margen horizontal de pantalla coincide con el token xl`() {
        assertEquals(Spacing.xl, Spacing.screenHorizontal)
    }

    @Test
    fun `la fila tactil minima respeta el objetivo de accesibilidad`() {
        assertTrue(
            "las filas tactiles deben medir al menos 48dp",
            Spacing.minTouchRow >= 48.dp
        )
    }
}
