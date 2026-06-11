package ink.duo3.tuned.ui.components

import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.RequestDisallowInterceptTouchEvent

internal class TunedDropdownForwardingTouchState(
    private val menuState: TunedDropdownMenuState,
    private val touchSlop: Float,
    private val disallowIntercept: RequestDisallowInterceptTouchEvent,
) {
    var pointerDown by mutableStateOf(false)
        private set

    private var forwarding = false
    private var downPosition = Offset.Zero
    private var currentPosition = Offset.Zero

    fun onLongPressTimeout() {
        if (pointerDown && !forwarding) {
            forwarding = true
            menuState.expanded = true
            menuState.updateDragSelection(currentPosition)
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
        downPosition = currentPosition
        menuState.cancelDragSelection()
        return true
    }

    private fun handleMove(): Boolean {
        disallowIntercept(true)
        if (!forwarding && (currentPosition - downPosition).getDistance() > touchSlop) {
            forwarding = true
            menuState.expanded = true
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
        } else {
            menuState.cancelDragSelection()
            menuState.expanded = true
        }
        forwarding = false
        return true
    }

    private fun handleCancel(): Boolean {
        disallowIntercept(false)
        pointerDown = false
        forwarding = false
        menuState.cancelDragSelection()
        return true
    }
}
