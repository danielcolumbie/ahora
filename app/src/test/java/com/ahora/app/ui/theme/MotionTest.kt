package com.ahora.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/** Las constantes de animación centralizadas no cambian el comportamiento visible. */
class MotionTest {

    @Test
    fun `sin primera muestra no hay retardo escalonado`() {
        assertEquals(0, Motion.staggerDelayMillis(5, isFirstShow = false))
        assertEquals(0, Motion.staggerDelayMillis(0, isFirstShow = false))
    }

    @Test
    fun `el retardo escalonado crece con el indice`() {
        assertEquals(0, Motion.staggerDelayMillis(0, isFirstShow = true))
        assertEquals(45, Motion.staggerDelayMillis(1, isFirstShow = true))
        assertEquals(135, Motion.staggerDelayMillis(3, isFirstShow = true))
    }

    @Test
    fun `el retardo escalonado tiene tope`() {
        assertEquals(360, Motion.staggerDelayMillis(8, isFirstShow = true))
        assertEquals(360, Motion.staggerDelayMillis(20, isFirstShow = true))
    }

    @Test
    fun `el pulso del microfono dura 1600ms con el retardo pedido`() {
        val spec = Motion.pulseSpec<Float>(800)
        assertEquals(1600, spec.durationMillis)
        // En animation-core 1.6 la propiedad se llama `delay` (pasó a
        // `delayMillis` en 1.7); el valor verificado es el mismo.
        assertEquals(800, spec.delay)
    }

    @Test
    fun `las constantes conservan los valores originales`() {
        assertEquals(280, Motion.ENTER_DURATION_MILLIS)
        assertEquals(45, Motion.STAGGER_STEP_MILLIS)
        assertEquals(360, Motion.STAGGER_MAX_MILLIS)
        assertEquals(1600, Motion.PULSE_DURATION_MILLIS)
    }

    @Test
    fun `las constantes del bloque H tienen los valores acordados`() {        // Salidas cortas y discretas (eliminar fila, recoger chips): 200ms.
        assertEquals(200, Motion.QUICK_EXIT_MILLIS)
        // Expansión suave de los chips de feedback: 240ms.
        assertEquals(240, Motion.SOFT_EXPAND_MILLIS)
        // Entrada de diálogos: 200ms.
        assertEquals(200, Motion.DIALOG_ENTER_MILLIS)
        // Entrada suave del estado vacío: 350ms (antes un literal dentro
        // de softEnter; el bloque H lo centralizó como constante).
        assertEquals(350, Motion.SOFT_ENTER_MILLIS)
    }

    @Test
    fun `el fundido de seleccion es rapido y discreto`() {
        // Evolución visual 2026-09-30: la banda de la fila seleccionada en
        // dos paneles aparece en 150ms, sin rebote.
        assertEquals(150, Motion.SELECTION_FADE_MILLIS)
        val spec = Motion.selectionFade<Float>()
        assertEquals(150, spec.durationMillis)
    }
}
