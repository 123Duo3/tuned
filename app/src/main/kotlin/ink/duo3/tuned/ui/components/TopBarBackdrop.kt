package ink.duo3.tuned.ui.components

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
 * [platformHeight] is the opaque status-bar strip; [gradientHeight] is the fading bar body.
 */
@Composable
internal fun TunedTopBarBackdrop(
    hazeState: HazeState,
    platformHeight: Dp,
    gradientHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth()) {
        TopBarProgressiveBlur(
            hazeState = hazeState,
            platformHeight = platformHeight,
            gradientHeight = gradientHeight,
        )
        TunedTopBackdrop(
            platformHeight = platformHeight,
            gradientHeight = gradientHeight,
        )
    }
}

@Composable
private fun TopBarProgressiveBlur(
    hazeState: HazeState,
    platformHeight: Dp,
    gradientHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer
    val gradientStartY = with(density) { platformHeight.toPx() }
    val gradientEndY = with(density) { (platformHeight + gradientHeight).toPx() }

    Box(
        modifier
            .fillMaxWidth()
            .height(platformHeight + gradientHeight)
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
private val TOP_BAR_BLUR_EASING = Easing { fraction -> fraction * fraction * (3f - 2f * fraction) }
