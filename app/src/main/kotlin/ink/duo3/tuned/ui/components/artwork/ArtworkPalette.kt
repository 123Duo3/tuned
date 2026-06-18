package ink.duo3.tuned.ui.components.artwork

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** The handful of artwork-derived colours a "Subscribed" card paints, each already theme-harmonized. */
@Immutable
data class ArtworkPalette(
    val container: Color,
    val onContainer: Color,
    val accent: Color,
    val onAccent: Color,
)
