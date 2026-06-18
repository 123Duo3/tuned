package ink.duo3.tuned.ui.components.dropdown

import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent
import kotlin.math.abs

internal class TunedDropdownForwardingTouchState(
    private val menuState: TunedDropdownMenuState,
    private val touchSlop: Float,
    private val disallowIntercept: RequestDisallowInterceptTouchEvent,
) {
    var pointerDown by mutableStateOf(false)
        private set

    private var forwarding = false
    private var rejectedByDirection = false
    private var downPosition = Offset.Zero
    private var currentPosition = Offset.Zero

    fun onLongPressTimeout() {
        if (pointerDown && !forwarding && !rejectedByDirection) {
            startForwarding()
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        currentPosition = Offset(event.rawX, event.rawY)
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> handleDown()
            MotionEvent.ACTION_MOVE -> handleMove()
            MotionEvent.ACTION_UP -> handleUp()
            MotionEvent.ACTION_CANCEL -> handleCancel()
            else -> forwarding
        }
    }

    private fun handleDown(): Boolean {
        disallowIntercept(true)
        pointerDown = true
        forwarding = false
        rejectedByDirection = false
        downPosition = currentPosition
        menuState.cancelDragSelection()
        return true
    }

    private fun handleMove(): Boolean {
        if (rejectedByDirection) {
            disallowIntercept(false)
            return true
        }
        disallowIntercept(true)
        val delta = currentPosition - downPosition
        if (!forwarding && !rejectedByDirection && delta.getDistance() > touchSlop) {
            if (delta.pointsTowardMenu()) {
                startForwarding()
            } else {
                rejectedByDirection = true
                disallowIntercept(false)
            }
        }
        if (forwarding) {
            menuState.updateDragSelection(currentPosition)
        }
        return true
    }

    private fun handleUp(): Boolean {
        disallowIntercept(false)
        pointerDown = false
        if (forwarding) {
            menuState.updateDragSelection(currentPosition)
            menuState.endDragSelection()
        } else if (!rejectedByDirection) {
            menuState.cancelDragSelection()
            menuState.expanded = true
        } else {
            menuState.cancelDragSelection()
        }
        forwarding = false
        rejectedByDirection = false
        return true
    }

    private fun handleCancel(): Boolean {
        disallowIntercept(false)
        pointerDown = false
        forwarding = false
        rejectedByDirection = false
        menuState.cancelDragSelection()
        return true
    }

    private fun startForwarding() {
        disallowIntercept(true)
        forwarding = true
        menuState.expanded = true
        menuState.updateDragSelection(currentPosition)
    }

    private fun Offset.pointsTowardMenu(): Boolean = y > touchSlop && abs(x) <= y * MENU_GESTURE_HORIZONTAL_RATIO
}

private const val MENU_GESTURE_HORIZONTAL_RATIO = 1.5f
