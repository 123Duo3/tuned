package ink.duo3.tuned.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** Surface-container backdrops that let edge controls fade into scrolling page content. */
@Composable
internal fun TunedTopBackdrop(
    totalHeight: Dp,
    modifier: Modifier = Modifier,
) {
    TunedEdgeBackdrop(
        totalHeight = totalHeight,
        edge = BackdropEdge.Top,
        modifier = modifier,
    )
}

@Composable
internal fun TunedBottomBackdrop(
    totalHeight: Dp,
    modifier: Modifier = Modifier,
) {
    TunedEdgeBackdrop(
        totalHeight = totalHeight,
        edge = BackdropEdge.Bottom,
        modifier = modifier,
    )
}

@Composable
private fun TunedEdgeBackdrop(
    totalHeight: Dp,
    edge: BackdropEdge,
    modifier: Modifier = Modifier,
) {
    val overlayColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = EDGE_BACKDROP_ALPHA)
    Box(
        modifier
            .fillMaxWidth()
            .height(totalHeight)
            .background(
                Brush.verticalGradient(
                    colorStops = edgeBackdropColorStops(edge, overlayColor),
                ),
            ),
    )
}

private fun edgeBackdropColorStops(
    edge: BackdropEdge,
    overlayColor: Color,
): Array<Pair<Float, Color>> {
    val fadeHeightFraction = 1f - EDGE_BACKDROP_PLATFORM_FRACTION
    val fadeStops =
        Array(EDGE_BACKDROP_GRADIENT_STEPS + 1) { index ->
            val fadeFraction = index.toFloat() / EDGE_BACKDROP_GRADIENT_STEPS
            val smoothFraction = edgeBackdropFadeProgress(fadeFraction)
            if (edge == BackdropEdge.Top) {
                val yFraction = EDGE_BACKDROP_PLATFORM_FRACTION + fadeHeightFraction * fadeFraction
                yFraction to overlayColor.copy(alpha = overlayColor.alpha * (1f - smoothFraction))
            } else {
                val yFraction = fadeHeightFraction * fadeFraction
                yFraction to overlayColor.copy(alpha = overlayColor.alpha * smoothFraction)
            }
        }
    return if (edge == BackdropEdge.Top) {
        arrayOf(0f to overlayColor) + fadeStops
    } else {
        fadeStops + arrayOf(1f to overlayColor)
    }
}

internal fun edgeBackdropFadeProgress(fraction: Float): Float {
    val cubic = fraction * fraction * fraction
    val quinticTerm = fraction * (fraction * 6f - 15f) + 10f
    return cubic * quinticTerm
}

private enum class BackdropEdge {
    Top,
    Bottom,
}

private const val EDGE_BACKDROP_ALPHA = 0.87f
internal const val EDGE_BACKDROP_PLATFORM_FRACTION = 0.3f
private const val EDGE_BACKDROP_GRADIENT_STEPS = 16
