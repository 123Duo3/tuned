package ink.duo3.tuned.ui.components.scaffold

import androidx.compose.animation.core.Easing
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect

/**
 * The shared frosted backdrop behind a floating top bar: a progressive haze blur that fades
 * downward over a surface-container gradient, so page content stays legible as it scrolls under
 * the controls. [hazeState] must be attached to the scrolling content via `hazeSource`.
 * The opaque platform occupies 30% of [totalHeight]; the remaining 70% fades into content.
 */
@Composable
internal fun TunedTopBarBackdrop(
    hazeState: HazeState,
    totalHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        TopBarProgressiveBlur(
            hazeState = hazeState,
            totalHeight = totalHeight,
        )
        TunedTopBackdrop(
            totalHeight = totalHeight,
        )
    }
}

@Composable
private fun TopBarProgressiveBlur(
    hazeState: HazeState,
    totalHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer
    val platformHeight = totalHeight * EDGE_BACKDROP_PLATFORM_FRACTION
    val gradientStartY = with(density) { platformHeight.toPx() }
    val gradientEndY = with(density) { totalHeight.toPx() }

    Box(
        modifier
            .fillMaxWidth()
            .height(totalHeight)
            .hazeEffect(hazeState) {
                this.backgroundColor = backgroundColor
                blurRadius = TOP_BAR_MAX_BLUR_RADIUS
                progressive =
                    HazeProgressive.verticalGradient(
                        easing = TOP_BAR_BLUR_EASING,
                        startY = gradientStartY,
                        startIntensity = 1f,
                        endY = gradientEndY,
                        endIntensity = 0f,
                    )
            },
    )
}

private val TOP_BAR_MAX_BLUR_RADIUS = 20.dp
private val TOP_BAR_BLUR_EASING = Easing { fraction -> 1f - edgeBackdropEasing.transform(1f - fraction) }
