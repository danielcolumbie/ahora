package com.ahora.app.ui.adaptive

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decisiones de layout adaptable (bloque J): los umbrales viven en
 * [AdaptiveLayout] como funciones puras para poder probarlos sin un
 * dispositivo.
 */
class AdaptiveLayoutTest {

    @Test
    fun `dos paneles a partir de 600dp`() {
        assertFalse(
            "a 599dp sigue la columna única de teléfono",
            AdaptiveLayout.isTwoPane(599.dp)
        )
        assertTrue(
            "a 600dp exactos ya hay lista + detalle",
            AdaptiveLayout.isTwoPane(600.dp)
        )
        assertTrue(
            "en tablet apaisada hay dos paneles",
            AdaptiveLayout.isTwoPane(1280.dp)
        )
    }

    @Test
    fun `encabezado compacto en pantallas bajas`() {
        assertTrue(
            "landscape de teléfono (~360dp de alto) compacta el encabezado",
            AdaptiveLayout.isCompactHeader(360.dp)
        )
        assertFalse(
            "a 480dp exactos no se compacta",
            AdaptiveLayout.isCompactHeader(480.dp)
        )
        assertFalse(
            "en portrait no se compacta",
            AdaptiveLayout.isCompactHeader(800.dp)
        )
    }

    @Test
    fun `los anchos máximos son coherentes con los umbrales`() {
        assertTrue(
            "el panel de detalle cabe dentro del umbral de dos paneles",
            AdaptiveLayout.detailPaneMaxWidth < AdaptiveLayout.twoPaneMinWidth
        )
        assertTrue(
            "la columna única de ajustes es más ancha que el corte de dos paneles",
            AdaptiveLayout.singleColumnMaxWidth > AdaptiveLayout.twoPaneMinWidth
        )
    }

    @Test
    fun `el umbral de dos paneles es el corte clásico tablet`() {
        assertTrue(
            "600dp es el corte documentado entre teléfono y tablet",
            AdaptiveLayout.twoPaneMinWidth == 600.dp
        )
    }
}
