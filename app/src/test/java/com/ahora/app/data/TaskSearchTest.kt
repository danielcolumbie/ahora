package com.ahora.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas de [buildSearchPattern]: el texto del usuario llega al LIKE de
 * SQLite sin que sus caracteres especiales actúen como comodines.
 */
class TaskSearchTest {

    @Test
    fun envuelveLaConsultaEnComodines() {
        assertEquals("%pan%", buildSearchPattern("pan"))
    }

    @Test
    fun recortaEspaciosExtremos() {
        assertEquals("%pan%", buildSearchPattern("  pan  "))
    }

    @Test
    fun escapaElPorcentaje() {
        assertEquals("%100\\%%", buildSearchPattern("100%"))
    }

    @Test
    fun escapaElGuionBajo() {
        assertEquals("%a\\_b%", buildSearchPattern("a_b"))
    }

    @Test
    fun escapaLaBarraInvertida() {
        assertEquals("%a\\\\b%", buildSearchPattern("a\\b"))
    }

    @Test
    fun consultaVaciaDaPatronVacio() {
        assertEquals("%%", buildSearchPattern("   "))
    }
}
