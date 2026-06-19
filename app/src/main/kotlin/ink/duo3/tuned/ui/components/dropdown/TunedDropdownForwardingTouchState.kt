package ink.duo3.tuned.ui.components.dropdown

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

internal class TunedDropdownForwardingTouchState(
    private val menuState: TunedDropdownMenuState,
    private val touchSlop: Float,
) {
    var pointerDown by mutableStateOf(false)
        private set

    private var forwarding = false
    private var pendingPlacement = false
    private var rejectedByDirection = false
    private var downPosition = Offset.Zero
    private var currentPosition = Offset.Zero

    private val canStartLongPress: Boolean
        get() = pointerDown && !forwarding && !pendingPlacement && !rejectedByDirection

    val consumesPointerInput: Boolean
        get() = forwarding || pendingPlacement

    fun onLongPressTimeout() {
        if (canStartLongPress) {
            startForwarding()
        }
    }

    fun onPlacementResolved() {
        if (!pendingPlacement) return
        pendingPlacement = false
        if (!pointerDown) {
            menuState.rejectGesturePlacement()
            return
        }
        val delta = currentPosition - downPosition
        if (delta.pointsTowardDropdown(menuState.placement, touchSlop)) {
            menuState.acceptGesturePlacement()
            startForwarding()
        } else {
            rejectedByDirection = true
            menuState.rejectGesturePlacement()
        }
    }

    fun onDown(positionInScreen: Offset) {
        currentPosition = positionInScreen
        pointerDown = true
        forwarding = false
        pendingPlacement = false
        rejectedByDirection = false
        downPosition = currentPosition
        menuState.cancelDragSelection()
    }

    fun onMove(positionInScreen: Offset) {
        currentPosition = positionInScreen
        when {
            rejectedByDirection || pendingPlacement -> Unit
            else -> {
                val delta = currentPosition - downPosition
                if (!forwarding && delta.getDistance() > touchSlop) {
                    pendingPlacement = true
                    menuState.requestGesturePlacement()
                }
                if (forwarding) {
                    menuState.updateDragSelection(currentPosition)
                }
            }
        }
    }

    fun onUp(positionInScreen: Offset) {
        currentPosition = positionInScreen
        pointerDown = false
        if (pendingPlacement) {
            pendingPlacement = false
            menuState.rejectGesturePlacement()
            menuState.cancelDragSelection()
        } else if (forwarding) {
            menuState.updateDragSelection(currentPosition)
            menuState.endDragSelection()
        } else {
            menuState.cancelDragSelection()
        }
        forwarding = false
        rejectedByDirection = false
    }

    fun onCancel() {
        pointerDown = false
        forwarding = false
        if (pendingPlacement) {
            menuState.rejectGesturePlacement()
        }
        pendingPlacement = false
        rejectedByDirection = false
        menuState.cancelDragSelection()
    }

    private fun startForwarding() {
        forwarding = true
        menuState.expanded = true
        menuState.updateDragSelection(currentPosition)
    }
}
