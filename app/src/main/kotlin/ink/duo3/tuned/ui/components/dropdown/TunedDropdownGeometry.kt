package ink.duo3.tuned.ui.components.dropdown

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntRect
import kotlin.math.abs

internal enum class TunedDropdownHorizontalOrigin {
    Left,
    Right,
}

internal enum class TunedDropdownVerticalOrigin {
    Top,
    Bottom,
}

@Immutable
internal data class TunedDropdownPlacement(
    val horizontalOrigin: TunedDropdownHorizontalOrigin,
    val verticalOrigin: TunedDropdownVerticalOrigin,
) {
    companion object {
        val Default =
            TunedDropdownPlacement(
                horizontalOrigin = TunedDropdownHorizontalOrigin.Right,
                verticalOrigin = TunedDropdownVerticalOrigin.Top,
            )
    }
}

@Immutable
data class TunedDropdownRevealOrigin(
    val xFraction: Float,
    val yFraction: Float,
) {
    init {
        require(xFraction in 0f..1f) { "xFraction must be between 0 and 1" }
        require(yFraction in 0f..1f) { "yFraction must be between 0 and 1" }
    }

    companion object {
        val Center = TunedDropdownRevealOrigin(xFraction = 0.5f, yFraction = 0.5f)
    }
}

@Immutable
internal data class TunedDropdownVerticalPosition(
    val panelTop: Int,
    val origin: TunedDropdownVerticalOrigin,
)

internal fun LayoutCoordinates.boundsInScreen(): Rect {
    val topLeft = localToScreen(Offset.Zero)
    return Rect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height,
    )
}

internal fun calculateTunedDropdownPlacement(
    anchorBounds: IntRect,
    panelBounds: IntRect,
): TunedDropdownPlacement =
    TunedDropdownPlacement(
        horizontalOrigin =
            if (anchorBounds.center.x <= panelBounds.center.x) {
                TunedDropdownHorizontalOrigin.Left
            } else {
                TunedDropdownHorizontalOrigin.Right
            },
        verticalOrigin =
            if (anchorBounds.center.y <= panelBounds.center.y) {
                TunedDropdownVerticalOrigin.Top
            } else {
                TunedDropdownVerticalOrigin.Bottom
            },
    )

internal fun calculateTunedDropdownVerticalPosition(
    anchorBounds: IntRect,
    windowHeight: Int,
    panelHeight: Int,
    verticalOffset: Int,
    margin: Int,
): TunedDropdownVerticalPosition {
    val belowPanelTop = anchorBounds.bottom + verticalOffset
    val abovePanelTop = anchorBounds.top - panelHeight - verticalOffset
    val maxPanelTop = windowHeight - margin - panelHeight
    val belowFits = belowPanelTop + panelHeight <= windowHeight - margin
    val aboveFits = abovePanelTop >= margin
    return when {
        belowFits -> TunedDropdownVerticalPosition(belowPanelTop, TunedDropdownVerticalOrigin.Top)
        aboveFits -> TunedDropdownVerticalPosition(abovePanelTop, TunedDropdownVerticalOrigin.Bottom)
        else -> {
            val fallbackTop = maxPanelTop.coerceAtLeast(margin)
            val fallbackOrigin =
                if (anchorBounds.center.y <= fallbackTop + panelHeight / 2) {
                    TunedDropdownVerticalOrigin.Top
                } else {
                    TunedDropdownVerticalOrigin.Bottom
                }
            TunedDropdownVerticalPosition(fallbackTop, fallbackOrigin)
        }
    }
}

internal fun Offset.pointsTowardDropdown(
    placement: TunedDropdownPlacement,
    touchSlop: Float,
): Boolean {
    val directedVerticalDistance =
        when (placement.verticalOrigin) {
            TunedDropdownVerticalOrigin.Top -> y
            TunedDropdownVerticalOrigin.Bottom -> -y
        }
    return directedVerticalDistance > touchSlop &&
        abs(x) <= directedVerticalDistance * MENU_GESTURE_HORIZONTAL_RATIO
}

internal fun calculateTunedDropdownRevealOffset(
    containerSize: Size,
    revealSize: Size,
    origin: TunedDropdownRevealOrigin,
): Offset =
    Offset(
        x = (containerSize.width - revealSize.width) * origin.xFraction,
        y = (containerSize.height - revealSize.height) * origin.yFraction,
    )

internal fun calculateTunedDropdownItemRevealIndex(
    visualIndex: Int,
    itemCount: Int,
    originFraction: Float,
): Int {
    val originIndex = originFraction * (itemCount - 1).coerceAtLeast(0)
    return (0 until itemCount)
        .sortedWith(compareBy<Int> { abs(it - originIndex) }.thenBy { it })
        .indexOf(visualIndex)
}

internal fun TunedDropdownPlacement.anchorFacingRevealOrigin(): TunedDropdownRevealOrigin =
    TunedDropdownRevealOrigin(
        xFraction =
            when (horizontalOrigin) {
                TunedDropdownHorizontalOrigin.Left -> 0f
                TunedDropdownHorizontalOrigin.Right -> 1f
            },
        yFraction =
            when (verticalOrigin) {
                TunedDropdownVerticalOrigin.Top -> 0f
                TunedDropdownVerticalOrigin.Bottom -> 1f
            },
    )

private const val MENU_GESTURE_HORIZONTAL_RATIO = 1.5f
