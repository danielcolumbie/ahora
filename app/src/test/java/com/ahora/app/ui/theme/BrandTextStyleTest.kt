package com.ahora.app.ui.theme

import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * La marca "AHORA" es identidad, no contenido: con encabezado compacto
 * (pantallas bajas) y fuente del sistema muy grande, conserva su tamaño
 * diseñado en vez de escalar. Rondas de evolución visual, 2026-09-30.
 */
class BrandTextStyleTest {

    private val displayLarge = AhoraTypography.displayLarge

    @Test
    fun `sin encabezado compacto la marca no cambia aunque la fuente sea enorme`() {
        val style = brandTextStyle(AhoraTypography, compactHeader = false, fontScale = 2.0f)
        assertEquals(displayLarge, style)
    }

    @Test
    fun `con encabezado compacto y fuente normal la marca no cambia`() {
        val style = brandTextStyle(AhoraTypography, compactHeader = true, fontScale = 1.0f)
        assertEquals(displayLarge, style)
    }

    @Test
    fun `con encabezado compacto y fuente al doble la marca conserva 40dp visuales`() {
        val style = brandTextStyle(AhoraTypography, compactHeader = true, fontScale = 2.0f)
        // 40.sp / 2 = 20.sp -> renderiza a 40dp con escala 2x.
        assertEquals(20.sp, style.fontSize)
        assertEquals(displayLarge.fontWeight, style.fontWeight)
        assertEquals(displayLarge.fontFamily, style.fontFamily)
    }

    @Test
    fun `el interletraje tambien se compensa para no deformar la marca`() {
        val style = brandTextStyle(AhoraTypography, compactHeader = true, fontScale = 2.0f)
        assertEquals(displayLarge.letterSpacing / 2.0f, style.letterSpacing)
    }

    @Test
    fun `por debajo del umbral la marca sigue escalando normal`() {
        val style = brandTextStyle(
            AhoraTypography,
            compactHeader = true,
            fontScale = BRAND_FIXED_ABOVE_FONT_SCALE
        )
        assertEquals(displayLarge, style)
    }
}
