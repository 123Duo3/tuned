package ink.duo3.tuned.ui.player

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp

/**
 * All geometry of the now-playing morph for a given [progress], in pixels (root-space) plus a
 * few dp values for shapes. The collapsed end is the floating mini bar; the expanded end is the
 * full-screen player. Everything is a plain lerp on the eased progress.
 */
@Suppress("LongParameterList")
internal class SheetMetrics(
    val eased: Float,
    val statusTop: Float,
    val sheetLeft: Float,
    val sheetTop: Float,
    val sheetWidth: Float,
    val sheetHeight: Float,
    val cornerDp: Dp,
    val containerColor: Color,
    val contentColor: Color,
    val artSize: Float,
    val artExpandedSize: Float,
    val artCornerDp: Dp,
    val artLeftRoot: Float,
    val artTopRoot: Float,
    val artExpandedTop: Float,
    val collapsedArtSize: Float,
    val collapsedHeight: Float,
    val topBarHeight: Float,
    val travelPx: Float,
)

@Composable
@Suppress("LongMethod", "LongParameterList")
internal fun sheetMetrics(
    progress: Float,
    rootWidth: Float,
    rootHeight: Float,
    statusTop: Float,
    platform: Float,
    pageCorner: Dp,
    density: Density,
): SheetMetrics {
    // The spring drives progress and may overshoot past 0/1 — that overshoot IS the bounce, so
    // spatial values use it raw. Colour/shape use a clamped copy so they don't extrapolate.
    val p = progress
    val clamped = p.coerceIn(0f, 1f)

    fun Dp.px() = with(density) { toPx() }

    val sideInset = lerp(MINI_SIDE_INSET.px(), 0f, p)
    val collapsedHeight = MINI_HEIGHT.px()
    val sheetTopCollapsed = rootHeight - platform - collapsedHeight
    val sheetTop = lerp(sheetTopCollapsed, 0f, p)
    val sheetBottom = rootHeight - lerp(platform, 0f, p)

    val collapsedArtSize = MINI_ARTWORK.px()
    val collapsedArtLeft = MINI_SIDE_INSET.px() + MINI_ARTWORK_PAD.px()
    val collapsedArtTop = sheetTopCollapsed + (collapsedHeight - collapsedArtSize) / 2f
    val topBarHeight = EXPANDED_TOP_BAR.px()
    val artExpandedTop = statusTop + topBarHeight + EXPANDED_ARTWORK_TOP_GAP.px()
    // Cap the expanded artwork by the available height too, so it stays square and on-screen on
    // short/wide displays instead of overflowing (where width * fraction would exceed the height).
    val artExpandedSize =
        minOf(
            rootWidth * EXPANDED_ARTWORK_FRACTION,
            (rootHeight - artExpandedTop) * EXPANDED_ARTWORK_MAX_HEIGHT_FRACTION,
        )
    val artExpandedLeft = (rootWidth - artExpandedSize) / 2f

    // Arc motion (MD): the size and the horizontal centre both lead along the arc, while the
    // vertical centre stays synced to the container — so the art grows and sweeps to centre early,
    // then rises into place. (Arcing the centre, which travels the full width, is what's visible.)
    val horizontalFraction = arcFraction(clamped, ARC_HORIZONTAL_EASING)
    // The size grows on a plain symmetric ease-in-out (gentle at both ends, steep in the middle),
    // in step with the container rather than leading with the arc — which also avoids clipping.
    val artSize = lerp(collapsedArtSize, artExpandedSize, SIZE_EASING.transform(clamped))
    val centerX =
        lerp(
            collapsedArtLeft + collapsedArtSize / 2f,
            artExpandedLeft + artExpandedSize / 2f,
            horizontalFraction,
        )
    val centerY =
        lerp(
            collapsedArtTop + collapsedArtSize / 2f,
            artExpandedTop + artExpandedSize / 2f,
            p,
        )
    val artLeftRoot = centerX - artSize / 2f
    val artTopRoot = centerY - artSize / 2f

    // Like the predictive-back gesture: hold the device's screen corner radius through the morph,
    // rounding down to a square edge only in the final sliver — where the display's own corners
    // take over at full screen.
    val cornerDp =
        when {
            clamped <= CORNER_PAGE_START -> lerp(MINI_CORNER, pageCorner, clamped / CORNER_PAGE_START)
            clamped >= CORNER_SQUARE_START ->
                lerp(pageCorner, 0.dp, (clamped - CORNER_SQUARE_START) / (1f - CORNER_SQUARE_START))
            else -> pageCorner
        }

    return SheetMetrics(
        eased = p,
        statusTop = statusTop,
        sheetLeft = sideInset,
        sheetTop = sheetTop,
        sheetWidth = rootWidth - sideInset * 2f,
        sheetHeight = sheetBottom - sheetTop,
        cornerDp = cornerDp,
        containerColor =
            lerp(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.surfaceContainer,
                clamped,
            ),
        // Lerp the text colour explicitly: Surface's contentColorFor only resolves at the exact
        // theme endpoints, so mid-morph it would fall back and the text would flicker.
        contentColor =
            lerp(
                MaterialTheme.colorScheme.onSecondaryContainer,
                MaterialTheme.colorScheme.onSurface,
                clamped,
            ),
        artSize = artSize,
        artExpandedSize = artExpandedSize,
        artCornerDp = lerp(MINI_ARTWORK_CORNER, EXPANDED_ARTWORK_CORNER, clamped),
        artLeftRoot = artLeftRoot,
        artTopRoot = artTopRoot,
        artExpandedTop = artExpandedTop,
        collapsedArtSize = collapsedArtSize,
        collapsedHeight = collapsedHeight,
        topBarHeight = topBarHeight,
        travelPx = sheetTopCollapsed.coerceAtLeast(1f),
    )
}

private fun lerp(
    start: Float,
    stop: Float,
    fraction: Float,
): Float = start + (stop - start) * fraction

// Blends one axis between a straight line (ARC_STRENGTH = 0) and the full arc easing (= 1), so the
// bow depth is a single tunable number.
private fun arcFraction(
    progress: Float,
    easing: Easing,
): Float = lerp(progress, easing.transform(progress), ARC_STRENGTH)

private val MINI_HEIGHT = 64.dp
private val MINI_SIDE_INSET = 16.dp
private val MINI_ARTWORK = 56.dp
private val MINI_ARTWORK_PAD = 4.dp
private val MINI_CORNER = 16.dp
private val MINI_ARTWORK_CORNER = 12.dp
private val EXPANDED_TOP_BAR = 56.dp
private val EXPANDED_ARTWORK_TOP_GAP = 16.dp
private val EXPANDED_ARTWORK_CORNER = 16.dp
private const val EXPANDED_ARTWORK_FRACTION = 0.7f
private const val EXPANDED_ARTWORK_MAX_HEIGHT_FRACTION = 0.5f

// Corner radius timing: ramp mini → device corner across almost the whole morph (first 99%), then
// square off in just the final 1% before full screen.
private const val CORNER_PAGE_START = 0.99f
private const val CORNER_SQUARE_START = 0.99f

// Arc motion (MD): only the horizontal travel is eased to lead (decelerates, sliding toward centre
// early) while the vertical stays synced to the container — the offset bows the path downward.
// ARC_STRENGTH dials the bow depth: 0 = straight line, 1 = the full lead this curve describes.
private val ARC_HORIZONTAL_EASING = CubicBezierEasing(0.2f, 0.8f, 0.3f, 1f)
private const val ARC_STRENGTH = 1f

// The artwork size grows on a symmetric ease-in-out — gentle at both ends, steep in the middle.
private val SIZE_EASING = CubicBezierEasing(0.1f, 0f, 0.4f, 1f)
