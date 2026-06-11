package ink.duo3.tuned.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates

internal fun LayoutCoordinates.boundsInScreen(): Rect {
    val topLeft = localToScreen(Offset.Zero)
    return Rect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + size.width,
        bottom = topLeft.y + size.height,
    )
}
