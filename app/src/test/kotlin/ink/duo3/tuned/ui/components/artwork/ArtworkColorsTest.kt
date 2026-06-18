package ink.duo3.tuned.ui.components.artwork

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.PaletteStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkColorsTest {
    @Test
    fun `grayscale artwork selects a monochrome palette without blue fallback`() {
        val colors =
            selectArtworkSourceColors(
                mapOf(
                    Color.Black.toArgb() to 60,
                    Color.White.toArgb() to 40,
                ),
            )

        assertNotNull(colors)
        assertEquals(PaletteStyle.Monochrome, colors?.style)
        assertNotEquals(GOOGLE_BLUE, colors?.primary?.toArgb())
        assertTrue(colors?.primary == Color.Black || colors?.primary == Color.White)
    }

    @Test
    fun `colorful artwork keeps the multi color tonal palette`() {
        val colors =
            selectArtworkSourceColors(
                mapOf(
                    Color.Red.toArgb() to 40,
                    Color.Green.toArgb() to 35,
                    Color.Blue.toArgb() to 25,
                ),
            )

        assertEquals(PaletteStyle.TonalSpot, colors?.style)
        assertNotNull(colors?.primary)
    }

    @Test
    fun `empty population has no artwork palette`() {
        assertNull(selectArtworkSourceColors(emptyMap()))
    }

    private companion object {
        const val GOOGLE_BLUE = -0xBD7A0C
    }
}
