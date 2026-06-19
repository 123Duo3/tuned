package ink.duo3.tuned.ui.components.dropdown

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TunedDropdownForwardingTouchStateTest {
    @Test
    fun `ordinary press remains available to anchor indication`() {
        val touchState = TunedDropdownForwardingTouchState(TunedDropdownMenuState(), TOUCH_SLOP)

        touchState.onDown(Offset.Zero)
        assertFalse(touchState.consumesPointerInput)

        touchState.onMove(Offset(2f, 2f))
        assertFalse(touchState.consumesPointerInput)
    }

    @Test
    fun `long press consumes anchor input after ripple starts`() {
        val touchState = TunedDropdownForwardingTouchState(TunedDropdownMenuState(), TOUCH_SLOP)

        touchState.onDown(Offset.Zero)
        touchState.onLongPressTimeout()

        assertTrue(touchState.consumesPointerInput)
    }

    @Test
    fun `direction probe consumes drag before anchor click`() {
        val touchState = TunedDropdownForwardingTouchState(TunedDropdownMenuState(), TOUCH_SLOP)

        touchState.onDown(Offset.Zero)
        touchState.onMove(Offset(0f, 20f))

        assertTrue(touchState.consumesPointerInput)
    }
}

private const val TOUCH_SLOP = 8f
