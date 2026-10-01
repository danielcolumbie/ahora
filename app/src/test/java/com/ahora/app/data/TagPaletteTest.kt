package com.ahora.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas de la paleta de colores de etiqueta y la limpieza de nombres
 * (1.28.0). Todo Kotlin puro, sin Android: la paleta vive en el modelo
 * para poder probarla aquí.
 */
class TagPaletteTest {

    @Test fun `la paleta tiene 8 colores`() {
        assertEquals(8, TagPalette.COUNT)
        assertEquals(8, TagPalette.COLORS.size)
        assertEquals(8, TagPalette.COLOR_NAMES.size)
    }

    @Test fun `todos los colores son opacos`() {
        TagPalette.COLORS.forEach { color ->
            assertEquals(0xFF shl 24, color and (0xFF shl 24))
        }
    }

    @Test fun `los nombres de color son unicos`() {
        assertEquals(
            TagPalette.COLOR_NAMES.size,
            TagPalette.COLOR_NAMES.toSet().size
        )
    }

    @Test fun `colorFor con indice valido devuelve el color`() {
        assertEquals(TagPalette.COLORS[3], TagPalette.colorFor(3))
    }

    @Test fun `colorFor con indice fuera de rango cae al primero`() {
        assertEquals(TagPalette.COLORS[0], TagPalette.colorFor(-1))
        assertEquals(TagPalette.COLORS[0], TagPalette.colorFor(99))
    }

    @Test fun `sanitizeColorIndex deja pasar los validos`() {
        assertTrue(TagPalette.isValid(0))
        assertTrue(TagPalette.isValid(7))
        assertFalse(TagPalette.isValid(8))
        assertEquals(5, TagPalette.sanitizeColorIndex(5))
        assertEquals(0, TagPalette.sanitizeColorIndex(8))
    }

    @Test fun `sanitizeTagName recorta y limita el largo`() {
        assertEquals("Casa", sanitizeTagName("  Casa  "))
        assertEquals(24, sanitizeTagName("a".repeat(100))!!.length)
        // Se recorta y se vuelve a recortar: sin espacios al final.
        assertEquals("abc", sanitizeTagName("  abc   "))
    }

    @Test fun `sanitizeTagName devuelve null si queda vacio`() {
        assertNull(sanitizeTagName(""))
        assertNull(sanitizeTagName("   "))
    }

    @Test fun `TAG_NAME_MAX_LENGTH es 24`() {
        assertEquals(24, TAG_NAME_MAX_LENGTH)
    }
}
