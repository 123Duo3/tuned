package ink.duo3.tuned.ui.player

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Drives the mini ⇄ full now-playing morph as one continuous [progress] (0 = collapsed mini
 * bar, 1 = expanded full player). A drag updates [progress] directly for a 1:1 feel; releasing
 * settles to the nearer end (or follows a fling). [expandedTarget] is where it's heading, so
 * back-handling and hit-testing don't have to wait for the spring to finish.
 */
@Stable
internal class NowPlayingSheetState(
    private val scope: CoroutineScope,
    initialExpanded: Boolean = false,
) {
    var progress by mutableFloatStateOf(if (initialExpanded) 1f else 0f)
        private set
    var expandedTarget by mutableStateOf(initialExpanded)
        private set

    // Vertical distance (px) of a full expand; set from the laid-out sheet so drag maps 1:1.
    var travelPx: Float = 1f

    // A slow, smoothly-decelerating spring for this full-screen morph. Material's Expressive
    // "slow spatial" (stiffness 200) is tuned for small UI motion and finishes in ~270ms, which
    // reads as flat/linear at this scale; a much lower stiffness gives the deliberate ~1s glide.
    var animationSpec: AnimationSpec<Float> =
        spring(
            dampingRatio = SPATIAL_DAMPING,
            stiffness = SPATIAL_STIFFNESS,
            // progress is 0..1, so the default 0.01 threshold stops the spring ~1% (tens of px)
            // short of rest. Settle far finer; animateTo() also snaps exactly at the end.
            visibilityThreshold = PROGRESS_THRESHOLD,
        )

    private var animation: Job? = null

    fun onDrag(deltaPx: Float): Float {
        animation?.cancel()
        val previous = progress
        progress = (progress - deltaPx / travelPx).coerceIn(0f, 1f)
        return (previous - progress) * travelPx
    }

    fun settle(velocityPx: Float) {
        val target =
            when {
                velocityPx < -FLING_VELOCITY -> 1f
                velocityPx > FLING_VELOCITY -> 0f
                progress > COMMIT_THRESHOLD -> 1f
                else -> 0f
            }
        if (abs(progress - target) <= ENDPOINT_THRESHOLD) {
            snapTo(target)
            return
        }
        // Hand the release velocity to the spring (px/s → progress/s; an up-drag is -px, +progress).
        animateTo(target, initialVelocity = -velocityPx / travelPx)
    }

    fun expand() = animateTo(1f)

    fun collapse() = animateTo(0f)

    fun snapToCollapsed() = snapTo(0f)

    private fun snapTo(target: Float) {
        animation?.cancel()
        expandedTarget = target == 1f
        progress = target
    }

    private fun animateTo(
        target: Float,
        initialVelocity: Float = 0f,
    ) {
        expandedTarget = target == 1f
        animation?.cancel()
        val spec = animationSpec
        animation =
            scope.launch {
                animate(
                    initialValue = progress,
                    targetValue = target,
                    initialVelocity = initialVelocity,
                    animationSpec = spec,
                ) { value, _ -> progress = value }
                // Land exactly on the target; the spring stops within the threshold, not on it.
                progress = target
            }
    }

    private companion object {
        const val COMMIT_THRESHOLD = 0.4f
        const val FLING_VELOCITY = 1000f
        const val SPATIAL_DAMPING = 1f
        const val SPATIAL_STIFFNESS = 500f
        const val PROGRESS_THRESHOLD = 0.0001f
        const val ENDPOINT_THRESHOLD = 0.001f
    }
}

@Composable
internal fun rememberNowPlayingSheetState(): NowPlayingSheetState {
    val scope = rememberCoroutineScope()
    val saver =
        remember(scope) {
            Saver<NowPlayingSheetState, Boolean>(
                save = { state -> state.expandedTarget || state.progress > SAVE_EXPANDED_THRESHOLD },
                restore = { expanded -> NowPlayingSheetState(scope, initialExpanded = expanded) },
            )
        }
    return rememberSaveable(saver = saver) { NowPlayingSheetState(scope) }
}

private const val SAVE_EXPANDED_THRESHOLD = 0.5f
