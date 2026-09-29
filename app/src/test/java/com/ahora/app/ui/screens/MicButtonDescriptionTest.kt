package com.ahora.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Etiquetas de accesibilidad puras (BLOQUE I del rediseño premium).
 */
class MicButtonDescriptionTest {

    @Test
    fun `sin escuchar el boton ofrece dictar`() {
        assertEquals("Dictar tarea", micButtonDescription(listening = false))
    }

    @Test
    fun `escuchando el boton ofrece detener`() {
        assertEquals("Detener dictado", micButtonDescription(listening = true))
    }
}
