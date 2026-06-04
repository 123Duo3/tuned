@file:Suppress("TooManyFunctions")

package ink.duo3.tuned.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.math.min

@Composable
internal fun animateProgress(
    isAnimating: Boolean,
    pullProgress: Float,
    propagationSpeed: Float,
    isPlaying: Boolean,
): WaveProgress {
    var progress by remember { mutableStateOf(WaveProgress.Icon) }
    val currentPropagationSpeed by rememberUpdatedState(propagationSpeed.coerceIn(0.25f, 4f))
    val pullDistance = pullProgress.coerceAtLeast(0f)
    val pullBacktrack =
        MAXIMUM_PULL_BACKTRACK_IN_RINGS *
            pullDistance / (PULL_RESISTANCE + pullDistance)

    LaunchedEffect(isAnimating, isPlaying, if (isAnimating) 0f else pullBacktrack) {
        when {
            isAnimating ->
                runActiveWave(
                    initialProgress = progress,
                    propagationSpeed = { currentPropagationSpeed },
                    withPulse = true,
                    updateProgress = { progress = it },
                )
            pullBacktrack > 0f -> progress = WaveProgress.pull(backtrack = pullBacktrack)
            progress.isPulled -> progress = WaveProgress.Icon
            // Playing has no traveling pulse: the rings just drift outward at the baseline rate.
            isPlaying ->
                driftBaseline(
                    initialProgress = progress,
                    propagationSpeed = { currentPropagationSpeed },
                    updateProgress = { progress = it },
                )
            else ->
                settleWave(
                    initialProgress = progress,
                    propagationSpeed = { currentPropagationSpeed },
                    updateProgress = { progress = it },
                )
        }
    }
    return progress
}

private suspend fun runActiveWave(
    initialProgress: WaveProgress,
    propagationSpeed: () -> Float,
    withPulse: Boolean,
    updateProgress: (WaveProgress) -> Unit,
) {
    var baseline = initialProgress.baseline
    var pulse = initialProgress.pulse
    var previousFrameNanos: Long? = null
    var startupElapsedMillis = 0f

    while (true) {
        withFrameNanos { frameNanos ->
            val previousFrame = previousFrameNanos
            previousFrameNanos = frameNanos
            if (previousFrame == null) return@withFrameNanos

            val deltaMillis = (frameNanos - previousFrame) / NANOS_PER_MILLI
            startupElapsedMillis += deltaMillis
            val startupProgress = (startupElapsedMillis / START_EASING_MILLIS).coerceIn(0f, 1f)
            val strength =
                lerp(
                    initialProgress.strength,
                    1f,
                    FastOutSlowInEasing.transform(startupProgress),
                )
            baseline = wrapProgress(baseline + deltaMillis / ANIMATION_CYCLE_MILLIS * strength)
            pulse =
                if (withPulse) {
                    wrapProgress(
                        pulse +
                            deltaMillis / ANIMATION_CYCLE_MILLIS *
                            propagationSpeed() *
                            strength,
                    )
                } else {
                    0f
                }
            updateProgress(WaveProgress(baseline = baseline, pulse = pulse, strength = strength))
        }
    }
}

/**
 * Drives the pulseless "playing" drift, but first carries any pulse still mid-flight out to
 * the edge. Switching straight into [runActiveWave] with `withPulse = false` would zero the
 * pulse on the very next frame, snapping a traveling ring out of existence — e.g. the moment a
 * pull-to-refresh wave hands off to playback. Finishing the propagation first lets the ring
 * reach the baseline before the steady drift takes over.
 */
private suspend fun driftBaseline(
    initialProgress: WaveProgress,
    propagationSpeed: () -> Float,
    updateProgress: (WaveProgress) -> Unit,
) {
    val settled =
        if (initialProgress.pulse > MINIMUM_PHASE_DISTANCE) {
            finishPropagation(
                initialProgress = initialProgress,
                propagationSpeed = propagationSpeed,
                updateProgress = updateProgress,
            )
        } else {
            initialProgress
        }
    runActiveWave(
        initialProgress = settled,
        propagationSpeed = propagationSpeed,
        withPulse = false,
        updateProgress = updateProgress,
    )
}

private suspend fun settleWave(
    initialProgress: WaveProgress,
    propagationSpeed: () -> Float,
    updateProgress: (WaveProgress) -> Unit,
) {
    if (initialProgress == WaveProgress.Icon) return

    // Only unwind a pulse that is actually mid-flight; the playing baseline carries none.
    val completedProgress =
        if (initialProgress.pulse > MINIMUM_PHASE_DISTANCE) {
            finishPropagation(
                initialProgress = initialProgress,
                propagationSpeed = propagationSpeed,
                updateProgress = updateProgress,
            )
        } else {
            initialProgress
        }
    settleBaseline(
        initialProgress = completedProgress,
        updateProgress = updateProgress,
    )
}

private suspend fun finishPropagation(
    initialProgress: WaveProgress,
    propagationSpeed: () -> Float,
    updateProgress: (WaveProgress) -> Unit,
): WaveProgress {
    var progress = initialProgress
    var remainingPulseDistance = distanceToFollowingCycle(initialProgress.pulse)
    var previousFrameNanos: Long? = null
    var elapsedMillis = 0f

    while (remainingPulseDistance > 0f) {
        withFrameNanos { frameNanos ->
            val previousFrame = previousFrameNanos
            previousFrameNanos = frameNanos
            if (previousFrame == null) return@withFrameNanos

            val deltaMillis = (frameNanos - previousFrame) / NANOS_PER_MILLI
            elapsedMillis += deltaMillis
            val strength = startingStrength(initialProgress.strength, elapsedMillis)
            val baselineDelta = deltaMillis / ANIMATION_CYCLE_MILLIS * strength
            val pulseDelta = baselineDelta * propagationSpeed()
            val appliedFraction = min(1f, remainingPulseDistance / pulseDelta)
            remainingPulseDistance -= pulseDelta * appliedFraction
            progress =
                WaveProgress(
                    baseline = wrapProgress(progress.baseline + baselineDelta * appliedFraction),
                    pulse = wrapProgress(progress.pulse + pulseDelta * appliedFraction),
                    strength = strength,
                )
            updateProgress(progress)
        }
    }
    return progress.copy(pulse = 0f)
}

private suspend fun settleBaseline(
    initialProgress: WaveProgress,
    updateProgress: (WaveProgress) -> Unit,
) {
    val baselineDistance = distanceToFollowingCycle(initialProgress.baseline)
    val baselineInitialTangent =
        min(
            initialProgress.strength * STOP_EASING_MILLIS / ANIMATION_CYCLE_MILLIS,
            MAXIMUM_HERMITE_TANGENT_FACTOR * baselineDistance,
        )
    var previousFrameNanos: Long? = null
    var elapsedMillis = 0f

    while (elapsedMillis < STOP_EASING_MILLIS) {
        withFrameNanos { frameNanos ->
            val previousFrame = previousFrameNanos
            previousFrameNanos = frameNanos
            if (previousFrame == null) return@withFrameNanos

            elapsedMillis += (frameNanos - previousFrame) / NANOS_PER_MILLI
            val progress = (elapsedMillis / STOP_EASING_MILLIS).coerceIn(0f, 1f)
            updateProgress(
                WaveProgress(
                    baseline =
                        wrapProgress(
                            cubicHermite(
                                start = initialProgress.baseline,
                                end = initialProgress.baseline + baselineDistance,
                                startTangent = baselineInitialTangent,
                                progress = progress,
                            ),
                        ),
                    pulse = 0f,
                    strength = initialProgress.strength * (1f - progress),
                ),
            )
        }
    }
    updateProgress(WaveProgress.Icon)
}

private fun startingStrength(
    initialStrength: Float,
    elapsedMillis: Float,
): Float =
    lerp(
        initialStrength,
        1f,
        FastOutSlowInEasing.transform((elapsedMillis / START_EASING_MILLIS).coerceIn(0f, 1f)),
    )

private fun distanceToFollowingCycle(progress: Float): Float =
    when {
        progress <= MINIMUM_PHASE_DISTANCE -> 1f
        else -> 1f - progress
    }

private fun cubicHermite(
    start: Float,
    end: Float,
    startTangent: Float,
    progress: Float,
): Float {
    val progressSquared = progress * progress
    val progressCubed = progressSquared * progress
    return (2f * progressCubed - 3f * progressSquared + 1f) * start +
        (progressCubed - 2f * progressSquared + progress) * startTangent +
        (-2f * progressCubed + 3f * progressSquared) * end
}

private fun lerp(
    start: Float,
    stop: Float,
    fraction: Float,
): Float = start + (stop - start) * fraction

private fun wrapProgress(progress: Float): Float = progress % 1f

internal data class WaveProgress(
    val baseline: Float,
    val pulse: Float,
    val strength: Float,
    val isPulled: Boolean = false,
) {
    companion object {
        val Icon = WaveProgress(baseline = 0f, pulse = 0f, strength = 0f)

        fun pull(backtrack: Float): WaveProgress =
            WaveProgress(
                baseline = -backtrack,
                pulse = 0f,
                strength = 0f,
                isPulled = true,
            )
    }
}

private const val ANIMATION_CYCLE_MILLIS = 1400
private const val START_EASING_MILLIS = 650f
private const val STOP_EASING_MILLIS = 1600
private const val NANOS_PER_MILLI = 1_000_000f
private const val MINIMUM_PHASE_DISTANCE = 0.0001f
private const val MAXIMUM_HERMITE_TANGENT_FACTOR = 3f

// Pull feedback: a deeper saturation with less early resistance so the wordmark visibly recoils
// as the user drags. backtrack = MAX * d / (RESISTANCE + d) → at a full pull (d≈1) ~1.05 rings,
// vs. the old ~0.5. Tunable by feel; raise MAX for a deeper recoil, lower RESISTANCE to react sooner.
private const val MAXIMUM_PULL_BACKTRACK_IN_RINGS = 2f
private const val PULL_RESISTANCE = 0.9f
