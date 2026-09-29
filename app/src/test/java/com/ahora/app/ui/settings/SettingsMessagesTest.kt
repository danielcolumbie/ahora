package com.ahora.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Mensajes del respaldo de Ajustes (BLOQUE F del rediseño premium): los
 * textos que salen en el Snackbar del sistema son lógica pura y se prueban
 * aquí, sin Compose.
 */
class SettingsMessagesTest {

    @Test
    fun `exportar OK muestra mensaje simple`() {
        assertEquals("Respaldo guardado.", formatBackupSaved())
    }

    @Test
    fun `fallo al guardar incluye la causa`() {
        assertEquals(
            "No se pudo guardar: permiso denegado",
            formatBackupError("guardar", "permiso denegado")
        )
    }

    @Test
    fun `fallo al importar incluye la causa`() {
        assertEquals(
            "No se pudo importar: JSON inválido",
            formatBackupError("importar", "JSON inválido")
        )
    }

    @Test
    fun `causa nula usa texto generico`() {
        assertEquals(
            "No se pudo guardar: error desconocido",
            formatBackupError("guardar", null)
        )
    }

    @Test
    fun `importar sin omitidas no menciona omitidas`() {
        assertEquals("Se importaron 5 tareas.", formatImportSuccess(5, 0))
    }

    @Test
    fun `importar con omitidas las cuenta entre parentesis`() {
        assertEquals("Se importaron 5 tareas (2 omitidas).", formatImportSuccess(5, 2))
    }

    @Test
    fun `importar cero tareas tambien se anuncia`() {
        assertEquals("Se importaron 0 tareas.", formatImportSuccess(0, 0))
    }
}
