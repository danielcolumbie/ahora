package com.ahora.app.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.UUID

/**
 * Pruebas del repositorio de ajustes con un DataStore real sobre un
 * archivo temporal (sin Android, sin Context).
 *
 * [SettingsRepository] recibe el DataStore ya construido, así que en
 * producción se le pasa el de la app y aquí uno de prueba.
 *
 * Nota: DataStore no permite dos instancias activas sobre el mismo archivo;
 * por eso las pruebas de persistencia cancelan el scope de la primera
 * instancia antes de abrir la segunda.
 */
class SettingsRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun TestScope.storeScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + testScheduler)

    private fun repository(scope: CoroutineScope, file: File): SettingsRepository =
        SettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        )

    private fun freshFile(): File =
        File(tempFolder.root, "ajustes-${UUID.randomUUID()}.preferences_pb")

    /**
     * Cierra el DataStore para poder abrir otra instancia sobre el mismo
     * archivo. Usa cancelAndJoin (suspend) en vez de cancel() a secas:
     * espera a que el scope termine de liberar el archivo antes de
     * crear la segunda instancia, eliminando la carrera entre ambas.
     * Es el mismo patrón que usa AndroidX en
     * testCreateDataStore_withSameFileAsInactiveDataStore.
     */
    private suspend fun closeStore(scope: CoroutineScope) {
        scope.coroutineContext[Job]!!.cancelAndJoin()
    }

    @Test
    fun `el tema por defecto es automatico`() = runTest {
        val scope = storeScope()
        try {
            assertEquals(ThemeMode.SYSTEM, repository(scope, freshFile()).themeMode.first())
        } finally {
            closeStore(scope)
        }
    }

    @Test
    fun `cambiar el tema persiste`() = runTest {
        val file = freshFile()
        val scope1 = storeScope()
        try {
            val repo1 = repository(scope1, file)
            repo1.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, repo1.themeMode.first())
        } finally {
            closeStore(scope1)
        }

        val scope2 = storeScope()
        try {
            assertEquals(ThemeMode.DARK, repository(scope2, file).themeMode.first())
        } finally {
            closeStore(scope2)
        }
    }

    @Test
    fun `las notificaciones vienen activadas por defecto`() = runTest {
        val scope = storeScope()
        try {
            assertTrue(repository(scope, freshFile()).notificationsEnabled.first())
        } finally {
            closeStore(scope)
        }
    }

    @Test
    fun `apagar notificaciones persiste`() = runTest {
        val file = freshFile()
        val scope1 = storeScope()
        try {
            val repo1 = repository(scope1, file)
            repo1.setNotificationsEnabled(false)
            assertFalse(repo1.notificationsEnabled.first())
        } finally {
            closeStore(scope1)
        }

        // Una segunda instancia sobre el mismo archivo ve lo guardado.
        val scope2 = storeScope()
        try {
            assertFalse(repository(scope2, file).notificationsEnabled.first())
        } finally {
            closeStore(scope2)
        }
    }

    @Test
    fun `el aviso de alarmas exactas no viene descartado por defecto`() = runTest {
        val scope = storeScope()
        try {
            assertFalse(repository(scope, freshFile()).exactAlarmNudgeDismissed.first())
        } finally {
            closeStore(scope)
        }
    }

    @Test
    fun `descartar el aviso de alarmas exactas persiste`() = runTest {
        val file = freshFile()
        val scope1 = storeScope()
        try {
            val repo1 = repository(scope1, file)
            repo1.setExactAlarmNudgeDismissed(true)
            assertTrue(repo1.exactAlarmNudgeDismissed.first())
        } finally {
            closeStore(scope1)
        }

        val scope2 = storeScope()
        try {
            assertTrue(repository(scope2, file).exactAlarmNudgeDismissed.first())
        } finally {
            closeStore(scope2)
        }
    }
}
