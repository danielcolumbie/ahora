package com.ahora.app.ui.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la validación de fecha pasada del diálogo de recordatorio.
 * Instantes fijos: nada depende de la hora real ni de la zona del equipo.
 */
class FutureInstantTest {

    private companion object {
        const val NOW = 1_700_000_000_000L
    }

    @Test
    fun `un instante futuro es valido`() {
        assertTrue(isFutureInstant(NOW + 1, NOW))
        assertTrue(isFutureInstant(NOW + 3_600_000, NOW))
    }

    @Test
    fun `un instante pasado no es valido`() {
        assertFalse(isFutureInstant(NOW - 1, NOW))
        assertFalse(isFutureInstant(NOW - 3_600_000, NOW))
    }

    @Test
    fun `el instante exacto de ahora no cuenta como futuro`() {
        // Programar "ahora mismo" es programar en el pasado en la práctica:
        // la alarma vencería antes de registrarse.
        assertFalse(isFutureInstant(NOW, NOW))
    }
}
