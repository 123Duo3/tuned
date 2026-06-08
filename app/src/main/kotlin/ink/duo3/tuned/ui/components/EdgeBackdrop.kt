package ink.duo3.tuned.ui.components

import androidx.compose.animation.core.CubicBezierEasing
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
            if (edge == BackdropEdge.Top) {
                val yFraction = EDGE_BACKDROP_PLATFORM_FRACTION + fadeHeightFraction * fadeFraction
                val opacity = edgeBackdropEasing.transform(1f - fadeFraction)
                yFraction to overlayColor.copy(alpha = overlayColor.alpha * opacity)
            } else {
                val yFraction = fadeHeightFraction * fadeFraction
                val opacity = edgeBackdropEasing.transform(fadeFraction)
                yFraction to overlayColor.copy(alpha = overlayColor.alpha * opacity)
            }
        }
    return if (edge == BackdropEdge.Top) {
        arrayOf(0f to overlayColor) + fadeStops
    } else {
        fadeStops + arrayOf(1f to overlayColor)
    }
}

internal val edgeBackdropEasing = CubicBezierEasing(0.3f, 0f, 0.7f, 1f)

private enum class BackdropEdge {
    Top,
    Bottom,
}

private const val EDGE_BACKDROP_ALPHA = 0.87f
internal const val EDGE_BACKDROP_PLATFORM_FRACTION = 0.3f
private const val EDGE_BACKDROP_GRADIENT_STEPS = 24
