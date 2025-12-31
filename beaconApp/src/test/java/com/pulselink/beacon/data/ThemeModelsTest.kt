package com.pulselink.beacon.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModelsTest {

    @Test
    fun `default theme returns legacy light palette`() {
        val theme = ThemePalette.default()
        assertEquals(0xFFD7F8D6, theme.outgoing)
        assertEquals(0xFFF6F7FB, theme.threadBackground)
    }

    @Test
    fun `futureDeep theme returns correct dark palette`() {
        val theme = ThemePalette.futureDeep()
        assertEquals(0xFF6366F1, theme.outgoing)
        assertEquals(0xFF030407, theme.threadBackground)
    }

    @Test
    fun `theme encode decode preserves values`() {
        val original = ThemePalette.futureDeep()
        val encoded = original.encode()
        val decoded = ThemePalette.decode(encoded)
        assertEquals(original, decoded)
    }
}
