package ink.duo3.tuned.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlayerProgressScrubTest {
    @Test
    fun `chapter near end still snaps but exact endpoint wins`() {
        val config = scrubConfig(snapTargetMs = 990L)

        assertEquals(990L, null.updatedSnapTarget(positionMs = 970f, config, TRACK_WIDTH_PX))
        assertEquals(990L, 990L.updatedSnapTarget(positionMs = 995f, config, TRACK_WIDTH_PX))
        assertNull(990L.updatedSnapTarget(positionMs = 1_000f, config, TRACK_WIDTH_PX))
    }

    @Test
    fun `chapter near start still snaps but exact endpoint wins`() {
        val config = scrubConfig(snapTargetMs = 10L)

        assertEquals(10L, null.updatedSnapTarget(positionMs = 30f, config, TRACK_WIDTH_PX))
        assertEquals(10L, 10L.updatedSnapTarget(positionMs = 5f, config, TRACK_WIDTH_PX))
        assertNull(10L.updatedSnapTarget(positionMs = 0f, config, TRACK_WIDTH_PX))
    }

    private fun scrubConfig(snapTargetMs: Long) =
        ProgressScrubConfig(
            enabled = true,
            durationMs = 1_000L,
            currentPositionMs = 500L,
            inertiaThresholdPx = 640f,
            snapRangePx = 4f,
            snapReleaseRangePx = 8f,
            snapTargetsMs = listOf(snapTargetMs),
        )

    private companion object {
        const val TRACK_WIDTH_PX = 100f
    }
}
