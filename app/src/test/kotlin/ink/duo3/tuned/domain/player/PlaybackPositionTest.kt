package ink.duo3.tuned.domain.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackPositionTest {
    @Test
    fun `unknown and zero durations do not clamp seek position`() {
        assertEquals(42_000L, 42_000L.coerceToKnownDuration(null))
        assertEquals(42_000L, 42_000L.coerceToKnownDuration(0L))
    }

    @Test
    fun `known positive duration clamps seek position`() {
        assertEquals(30_000L, 42_000L.coerceToKnownDuration(30_000L))
        assertEquals(0L, (-1L).coerceToKnownDuration(30_000L))
    }
}
