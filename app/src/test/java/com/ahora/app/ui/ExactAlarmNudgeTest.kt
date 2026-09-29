package com.ahora.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la condición del aviso de "permiso de alarmas exactas revocado".
 * Es una función pura: se prueban todas las combinaciones sin Android.
 */
class ExactAlarmNudgeTest {

    @Test
    fun `se muestra cuando falta el permiso y hay recordatorios pendientes`() {
        assertTrue(
            shouldShowExactAlarmNudge(
                sdkAtLeastS = true,
                hasExactAlarmPermission = false,
                hasFutureReminders = true,
                nudgeDismissed = false
            )
        )
    }

    @Test
    fun `no se muestra en Android 11 o anterior (no existe el permiso)`() {
        assertFalse(
            shouldShowExactAlarmNudge(
                sdkAtLeastS = false,
                hasExactAlarmPermission = false,
                hasFutureReminders = true,
                nudgeDismissed = false
            )
        )
    }

    @Test
    fun `no se muestra si el permiso esta concedido`() {
        assertFalse(
            shouldShowExactAlarmNudge(
                sdkAtLeastS = true,
                hasExactAlarmPermission = true,
                hasFutureReminders = true,
                nudgeDismissed = false
            )
        )
    }

    @Test
    fun `no se muestra si no hay recordatorios pendientes (seria ruido)`() {
        assertFalse(
            shouldShowExactAlarmNudge(
                sdkAtLeastS = true,
                hasExactAlarmPermission = false,
                hasFutureReminders = false,
                nudgeDismissed = false
            )
        )
    }

    @Test
    fun `no se muestra si el usuario ya lo descarto (no insistente)`() {
        assertFalse(
            shouldShowExactAlarmNudge(
                sdkAtLeastS = true,
                hasExactAlarmPermission = false,
                hasFutureReminders = true,
                nudgeDismissed = true
            )
        )
    }
}
