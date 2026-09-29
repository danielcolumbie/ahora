package com.ahora.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ratios de contraste WCAG 2.1 de los pares de colores del tema
 * (BLOQUE G del rediseño premium, 1.20.0).
 *
 * El dark mode se diseñó como paleta propia (no inversión) y se audita
 * por cálculo sobre los hex de los tokens, ya que sin dispositivo no se
 * puede medir con colorímetro. Si algún par queda bajo AA (4.5:1 para
 * texto normal), el test falla: el ajuste debe hacerse en `Color.kt` y
 * documentarse en `docs/DESIGN_SYSTEM.md`.
 *
 * Los divisores (`outlineVariant`) son decorativos (líneas de 1dp): no se
 * les exige AA, pero se fija que sigan siendo sutiles (ratio < 2.0) para
 * que no se conviertan en bordes.
 */
class ColorContrastTest {

    /** Luminancia relativa WCAG de un color sRGB (componentes 0..1). */
    private fun luminance(color: Color): Double {
        fun linear(c: Float): Double {
            val v = c.toDouble()
            return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * linear(color.red) +
            0.7152 * linear(color.green) +
            0.0722 * linear(color.blue)
    }

    private fun contrast(fg: Color, bg: Color): Double {
        val l1 = luminance(fg)
        val l2 = luminance(bg)
        val (lighter, darker) = if (l1 >= l2) l1 to l2 else l2 to l1
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** Mezcla alfa sobre fondo opaco (aprox. sRGB, como hace Compose). */
    private fun over(fg: Color, alpha: Float, bg: Color): Color = Color(
        red = fg.red * alpha + bg.red * (1 - alpha),
        green = fg.green * alpha + bg.green * (1 - alpha),
        blue = fg.blue * alpha + bg.blue * (1 - alpha)
    )

    private fun assertAA(fg: Color, bg: Color, what: String) {
        val ratio = contrast(fg, bg)
        assertTrue(
            "$what: %.2f:1, por debajo de AA (4.5:1)".format(ratio),
            ratio >= 4.5
        )
    }

    // ---------- Modo oscuro ----------

    @Test
    fun `dark texto secundario sobre fondo`() {
        assertAA(DarkOnSurfaceVariant, DarkBackground, "dark onSurfaceVariant/fondo")
    }

    @Test
    fun `dark texto secundario sobre superficie`() {
        assertAA(DarkOnSurfaceVariant, DarkSurface, "dark onSurfaceVariant/superficie")
    }

    @Test
    fun `dark texto de pills sobre fondo de pill`() {
        assertAA(DarkOnSurfaceVariant, DarkSurfaceVariant, "dark texto pill/fondo pill")
    }

    @Test
    fun `dark contador de seccion al 90 por ciento`() {
        // El contador de «HOY» usa onSurfaceVariant al 90% (bloque G).
        assertAA(
            over(DarkOnSurfaceVariant, 0.90f, DarkBackground),
            DarkBackground,
            "dark contador/fondo"
        )
    }

    @Test
    fun `dark texto protagonista sobre fondo`() {
        assertAA(DarkOnBackground, DarkBackground, "dark onBackground/fondo")
    }

    @Test
    fun `dark error sobre pill y sobre fondo`() {
        assertAA(DarkError, DarkSurfaceVariant, "dark error/fondo pill")
        assertAA(DarkError, DarkBackground, "dark error/fondo")
    }

    @Test
    fun `dark acento sobre fondo`() {
        assertAA(ElectricBlueDark, DarkBackground, "dark primary/fondo")
    }

    @Test
    fun `dark seleccion de navegacion`() {
        assertAA(
            DarkOnPrimaryContainer,
            DarkPrimaryContainer,
            "dark onPrimaryContainer/primaryContainer"
        )
    }

    @Test
    fun `dark jerarquia de superficies`() {
        // Fondo -> superficie -> superficie elevada: pasos claros de elevación.
        val bg = luminance(DarkBackground)
        val surface = luminance(DarkSurface)
        val variant = luminance(DarkSurfaceVariant)
        assertTrue("dark: superficie no más clara que fondo", surface > bg)
        assertTrue("dark: superficie elevada no más clara", variant > surface)
    }

    @Test
    fun `dark divisor sutil pero visible`() {
        // El divisor es translúcido: hay que componerlo sobre el fondo.
        val onBg = over(DarkDivider, DarkDivider.alpha, DarkBackground)
        val ratio = contrast(onBg, DarkBackground)
        assertTrue("dark divisor demasiado fuerte: %.2f:1".format(ratio), ratio < 2.0)
        assertTrue("dark divisor invisible: %.2f:1".format(ratio), ratio > 1.05)
    }

    // ---------- Modo claro ----------

    @Test
    fun `light texto secundario sobre fondo`() {
        assertAA(LightOnSurfaceVariant, LightBackground, "light onSurfaceVariant/fondo")
    }

    @Test
    fun `light texto de pills sobre fondo de pill`() {
        assertAA(LightOnSurfaceVariant, LightSurfaceVariant, "light texto pill/fondo pill")
    }

    @Test
    fun `light contador de seccion al 90 por ciento`() {
        assertAA(
            over(LightOnSurfaceVariant, 0.90f, LightBackground),
            LightBackground,
            "light contador/fondo"
        )
    }

    @Test
    fun `light error sobre pill`() {
        assertAA(LightError, LightSurfaceVariant, "light error/fondo pill")
    }

    @Test
    fun `light acento sobre fondo`() {
        assertAA(ElectricBlue, LightBackground, "light primary/fondo")
    }

    @Test
    fun `light jerarquia de superficies`() {
        val bg = luminance(LightBackground)
        val surface = luminance(LightSurface)
        val variant = luminance(LightSurfaceVariant)
        // En claro la jerarquía se invierte: la superficie es más clara que el fondo.
        assertTrue("light: superficie no más clara que fondo", surface > bg)
        assertTrue("light: pill no más oscura que superficie", variant < surface)
    }

    @Test
    fun `light divisor sutil pero visible`() {
        val onBg = over(LightDivider, LightDivider.alpha, LightBackground)
        val ratio = contrast(onBg, LightBackground)
        assertTrue("light divisor demasiado fuerte: %.2f:1".format(ratio), ratio < 2.0)
        assertTrue("light divisor invisible: %.2f:1".format(ratio), ratio > 1.05)
    }
}
