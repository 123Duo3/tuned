package ink.duo3.tuned.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.awaitCancellation

@Composable
internal fun TunedDropdownDragPressInteraction(
    isDragSelected: Boolean,
    isDragReleased: Boolean,
    enabled: Boolean,
    pressPosition: Offset,
    interactionSource: MutableInteractionSource,
) {
    val latestPressPosition by rememberUpdatedState(pressPosition)
    val latestDragReleased by rememberUpdatedState(isDragReleased)

    LaunchedEffect(isDragSelected, enabled) {
        if (!isDragSelected || !enabled) return@LaunchedEffect

        val press = PressInteraction.Press(latestPressPosition)
        interactionSource.emit(press)
        try {
            awaitCancellation()
        } finally {
            if (latestDragReleased) {
                interactionSource.emit(PressInteraction.Release(press))
            } else {
                interactionSource.emit(PressInteraction.Cancel(press))
            }
        }
    }
}
