package ink.duo3.tuned.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
    platformHeight: Dp,
    gradientHeight: Dp,
    modifier: Modifier = Modifier,
) {
    TunedEdgeBackdrop(
        platformHeight = platformHeight,
        gradientHeight = gradientHeight,
        edge = BackdropEdge.Top,
        modifier = modifier,
    )
}

@Composable
internal fun TunedBottomBackdrop(
    platformHeight: Dp,
    gradientHeight: Dp,
    modifier: Modifier = Modifier,
) {
    TunedEdgeBackdrop(
        platformHeight = platformHeight,
        gradientHeight = gradientHeight,
        edge = BackdropEdge.Bottom,
        modifier = modifier,
    )
}

@Composable
private fun TunedEdgeBackdrop(
    platformHeight: Dp,
    gradientHeight: Dp,
    edge: BackdropEdge,
    modifier: Modifier = Modifier,
) {
    val overlayColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = EDGE_BACKDROP_ALPHA)
    val solid: @Composable () -> Unit = {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(platformHeight)
                .background(overlayColor),
        )
    }
    val gradient: @Composable () -> Unit = {
        Box(
            Modifier
                .fillMaxWidth()
                .height(gradientHeight)
                .background(
                    Brush.verticalGradient(
                        colorStops = edgeBackdropColorStops(edge, overlayColor),
                    ),
                ),
        )
    }
    Column(modifier.fillMaxWidth()) {
        if (edge == BackdropEdge.Top) solid()
        gradient()
        if (edge == BackdropEdge.Bottom) solid()
    }
}

private fun edgeBackdropColorStops(
    edge: BackdropEdge,
    overlayColor: Color,
): Array<Pair<Float, Color>> =
    Array(EDGE_BACKDROP_GRADIENT_STEPS + 1) { index ->
        val fraction = index.toFloat() / EDGE_BACKDROP_GRADIENT_STEPS
        val smoothFraction = fraction * fraction * (3f - 2f * fraction)
        val intensity =
            if (edge == BackdropEdge.Top) {
                1f - smoothFraction
            } else {
                smoothFraction
            }
        fraction to overlayColor.copy(alpha = overlayColor.alpha * intensity)
    }

private enum class BackdropEdge {
    Top,
    Bottom,
}

private const val EDGE_BACKDROP_ALPHA = 0.87f
private const val EDGE_BACKDROP_GRADIENT_STEPS = 8
