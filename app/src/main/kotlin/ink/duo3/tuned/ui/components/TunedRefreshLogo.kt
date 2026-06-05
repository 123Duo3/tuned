package ink.duo3.tuned.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun TunedRefreshLogo(
    motion: TunedRefreshLogoMotion,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val progress =
        animateProgress(
            isAnimating = motion.isAnimating,
            pullProgress = motion.pullProgress,
            propagationSpeed = DEFAULT_PROPAGATION_SPEED,
            isPlaying = motion.isPlaying,
            releasePulseKey = motion.releasePulseKey,
        )
    val clipPath =
        remember {
            Path().apply {
                moveTo(205f, 6.44f)
                lineTo(205f, 41.56f)
                lineTo(-15f, 24f)
                close()
            }
        }

    Canvas(modifier = modifier) {
        drawWaves(progress, color, clipPath)
    }
}

@Immutable
data class TunedRefreshLogoMotion(
    val isAnimating: Boolean,
    val pullProgress: Float = 0f,
    val isPlaying: Boolean = false,
    val releasePulseKey: Int = 0,
)

private fun DrawScope.drawWaves(
    progress: WaveProgress,
    color: Color,
    clipPath: Path,
) {
    val scale = min(size.width / DESIGN_WIDTH, size.height / DESIGN_HEIGHT)
    val dx = (size.width - DESIGN_WIDTH * scale) / 2f
    val dy = (size.height - DESIGN_HEIGHT * scale) / 2f

    withTransform({
        translate(left = dx, top = dy)
        scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
    }) {
        clipPath(clipPath) {
            for (ringIndex in ENTERING_RING_INDEX..EXITING_RING_INDEX) {
                val radius =
                    INNER_RADIUS +
                        (ringIndex + progress.baseline) * RING_SPACING +
                        pulseOffset(ringIndex, progress)
                if (radius <= 0f) continue
                val opacity = edgeOpacity(radius)
                if (opacity > 0f) {
                    drawCircle(
                        color = color.copy(alpha = opacity),
                        radius = radius,
                        center = WaveCenter,
                        style = Stroke(width = strokeWidth(radius)),
                    )
                }
            }
        }
    }
}

private fun pulseOffset(
    ringIndex: Int,
    progress: WaveProgress,
): Float {
    val pulseCenter =
        lerp(
            -PULSE_RANGE_IN_RINGS,
            LAST_VISIBLE_RING_INDEX + PULSE_RANGE_IN_RINGS,
            progress.pulse,
        )
    val ringPosition = ringIndex + progress.baseline
    val normalizedDistance =
        ((pulseCenter - ringPosition) / PULSE_RANGE_IN_RINGS + 0.5f)
            .coerceIn(0f, 1f)
    return RING_SPACING * handoffProgress(normalizedDistance)
}

private fun handoffProgress(progress: Float): Float =
    if (progress <= 0.5f) {
        CENTER_APPROACH_TO_NEXT_RING *
            FastOutSlowInEasing.transform(progress * 2f)
    } else {
        CENTER_APPROACH_TO_NEXT_RING +
            (1f - CENTER_APPROACH_TO_NEXT_RING) *
            FastOutSlowInEasing.transform((progress - 0.5f) * 2f)
    }

private fun edgeOpacity(radius: Float): Float {
    val fadeIn =
        ((radius - (INNER_RADIUS - RING_SPACING)) / RING_SPACING)
            .coerceIn(0f, 1f)
    val fadeOut =
        (((OUTER_RADIUS + RING_SPACING) - radius) / RING_SPACING)
            .coerceIn(0f, 1f)
    return FastOutSlowInEasing.transform(min(fadeIn, fadeOut))
}

private fun strokeWidth(radius: Float): Float =
    (
        INNER_STROKE_WIDTH -
            (radius - INNER_RADIUS) / RING_SPACING
    ).coerceAtLeast(MINIMUM_STROKE_WIDTH)

private fun lerp(
    start: Float,
    stop: Float,
    fraction: Float,
): Float = start + (stop - start) * fraction

private const val DESIGN_WIDTH = 200f
private const val DESIGN_HEIGHT = 48f
private const val RING_SPACING = 20.5f
private const val INNER_RADIUS = 35.5f
private const val OUTER_RADIUS = 199.5f
private const val INNER_STROKE_WIDTH = 9f
private const val MINIMUM_STROKE_WIDTH = 0.5f
private const val CENTER_APPROACH_TO_NEXT_RING = 0.98f
private const val PULSE_RANGE_IN_RINGS = 10f
private const val ENTERING_RING_INDEX = -2
private const val LAST_VISIBLE_RING_INDEX = 8f
private const val EXITING_RING_INDEX = 10
private const val DEFAULT_PROPAGATION_SPEED = 0.75f
private val WaveCenter = Offset(-15f, 24f)

@Preview(name = "Tuned Refresh Logo")
@Composable
internal fun TunedRefreshLogoPreview() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                TunedRefreshLogo(
                    motion = TunedRefreshLogoMotion(isAnimating = false),
                    modifier = Modifier.size(width = 200.dp, height = 48.dp),
                )
            }
        }
    }
}
