package ink.duo3.tuned.ui.components.dropdown

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.IntRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TunedDropdownGeometryTest {
    @Test
    fun `placement uses anchor-facing corner below and to the left`() {
        val placement =
            calculateTunedDropdownPlacement(
                anchorBounds = IntRect(left = 280, top = 80, right = 320, bottom = 120),
                panelBounds = IntRect(left = 120, top = 128, right = 320, bottom = 328),
            )

        assertEquals(TunedDropdownHorizontalOrigin.Right, placement.horizontalOrigin)
        assertEquals(TunedDropdownVerticalOrigin.Top, placement.verticalOrigin)
    }

    @Test
    fun `placement uses anchor-facing corner above and to the right`() {
        val placement =
            calculateTunedDropdownPlacement(
                anchorBounds = IntRect(left = 20, top = 400, right = 60, bottom = 440),
                panelBounds = IntRect(left = 20, top = 192, right = 220, bottom = 392),
            )

        assertEquals(TunedDropdownHorizontalOrigin.Left, placement.horizontalOrigin)
        assertEquals(TunedDropdownVerticalOrigin.Bottom, placement.verticalOrigin)
    }

    @Test
    fun `positioning prefers below when both sides fit`() {
        val position =
            calculateTunedDropdownVerticalPosition(
                anchorBounds = IntRect(left = 280, top = 500, right = 320, bottom = 540),
                windowHeight = 800,
                panelHeight = 180,
                verticalOffset = 8,
                margin = 8,
            )

        assertEquals(TunedDropdownVerticalOrigin.Top, position.origin)
        assertEquals(548, position.panelTop)
    }

    @Test
    fun `positioning uses above only when below does not fit`() {
        val position =
            calculateTunedDropdownVerticalPosition(
                anchorBounds = IntRect(left = 280, top = 700, right = 320, bottom = 740),
                windowHeight = 800,
                panelHeight = 180,
                verticalOffset = 8,
                margin = 8,
            )

        assertEquals(TunedDropdownVerticalOrigin.Bottom, position.origin)
        assertEquals(512, position.panelTop)
    }

    @Test
    fun `drag direction follows actual vertical placement`() {
        val below = TunedDropdownPlacement.Default
        val above = below.copy(verticalOrigin = TunedDropdownVerticalOrigin.Bottom)

        assertTrue(Offset(6f, 12f).pointsTowardDropdown(below, touchSlop = 8f))
        assertFalse(Offset(6f, -12f).pointsTowardDropdown(below, touchSlop = 8f))
        assertTrue(Offset(6f, -12f).pointsTowardDropdown(above, touchSlop = 8f))
        assertFalse(Offset(6f, 12f).pointsTowardDropdown(above, touchSlop = 8f))
    }

    @Test
    fun `gesture opening waits for measured placement`() {
        val state = TunedDropdownMenuState()
        state.requestGesturePlacement()

        assertTrue(state.expanded)
        assertTrue(state.awaitingGesturePlacement)
        assertFalse(state.placementReady)

        state.updatePlacement(
            TunedDropdownPlacement.Default.copy(verticalOrigin = TunedDropdownVerticalOrigin.Bottom),
        )

        assertTrue(state.placementReady)
        assertEquals(1, state.placementGeneration)
        assertTrue(state.awaitingGesturePlacement)

        state.acceptGesturePlacement()
        assertFalse(state.awaitingGesturePlacement)
    }

    @Test
    fun `gesture reopening does not reuse stale placement`() {
        val state = TunedDropdownMenuState()
        state.requestGesturePlacement()
        state.updatePlacement(TunedDropdownPlacement.Default)
        state.rejectGesturePlacement()

        state.requestGesturePlacement()

        assertTrue(state.expanded)
        assertTrue(state.awaitingGesturePlacement)
        assertFalse(state.placementReady)
        assertEquals(1, state.placementGeneration)

        state.updatePlacement(
            TunedDropdownPlacement.Default.copy(verticalOrigin = TunedDropdownVerticalOrigin.Bottom),
        )

        assertTrue(state.placementReady)
        assertEquals(2, state.placementGeneration)
        assertEquals(TunedDropdownVerticalOrigin.Bottom, state.placement.verticalOrigin)
    }

    @Test
    fun `reveal starts at actual anchor-facing corner`() {
        val container = Size(width = 200f, height = 300f)
        val reveal = Size(width = 80f, height = 120f)

        assertEquals(
            Offset(120f, 0f),
            calculateTunedDropdownRevealOffset(
                container,
                reveal,
                TunedDropdownPlacement.Default.anchorFacingRevealOrigin(),
            ),
        )
        assertEquals(
            Offset(0f, 180f),
            calculateTunedDropdownRevealOffset(
                container,
                reveal,
                TunedDropdownPlacement(
                    horizontalOrigin = TunedDropdownHorizontalOrigin.Left,
                    verticalOrigin = TunedDropdownVerticalOrigin.Bottom,
                ).anchorFacingRevealOrigin(),
            ),
        )
        assertEquals(
            Offset(60f, 90f),
            calculateTunedDropdownRevealOffset(container, reveal, TunedDropdownRevealOrigin.Center),
        )
    }

    @Test
    fun `item reveal order starts at anchor-facing edge`() {
        assertEquals(
            1,
            calculateTunedDropdownItemRevealIndex(
                visualIndex = 1,
                itemCount = 4,
                originFraction = 0f,
            ),
        )
        assertEquals(
            2,
            calculateTunedDropdownItemRevealIndex(
                visualIndex = 1,
                itemCount = 4,
                originFraction = 1f,
            ),
        )
        assertEquals(
            0,
            calculateTunedDropdownItemRevealIndex(
                visualIndex = 1,
                itemCount = 4,
                originFraction = 0.5f,
            ),
        )
    }

    @Test
    fun `item reveal order uses actual vertical bounds`() {
        val state = TunedDropdownMenuState()
        val topKey = Any()
        val bottomKey = Any()
        state.updatePlacement(
            TunedDropdownPlacement.Default.copy(verticalOrigin = TunedDropdownVerticalOrigin.Bottom),
        )
        state.registerItem(bottomKey, Rect(0f, 100f, 100f, 148f), enabled = true) {}
        state.registerItem(topKey, Rect(0f, 20f, 100f, 68f), enabled = true) {}

        assertEquals(0, state.itemRevealIndex(bottomKey, originFraction = 1f))
        assertEquals(1, state.itemRevealIndex(topKey, originFraction = 1f))
    }
}
