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
): WaveProgress {
    var progress by remember { mutableStateOf(WaveProgress.Icon) }
    val currentPropagationSpeed by rememberUpdatedState(propagationSpeed.coerceIn(0.25f, 4f))
    val pullDistance = pullProgress.coerceAtLeast(0f)
    val pullBacktrack =
        MAXIMUM_PULL_BACKTRACK_IN_RINGS *
            pullDistance / (PULL_RESISTANCE + pullDistance)

    LaunchedEffect(isAnimating, if (isAnimating) 0f else pullBacktrack) {
        if (isAnimating) {
            runActiveWave(
                initialProgress = progress,
                propagationSpeed = { currentPropagationSpeed },
                updateProgress = { progress = it },
            )
        } else if (pullBacktrack > 0f) {
            progress = WaveProgress.pull(backtrack = pullBacktrack)
        } else if (progress.isPulled) {
            progress = WaveProgress.Icon
        } else {
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
                wrapProgress(
                    pulse +
                        deltaMillis / ANIMATION_CYCLE_MILLIS *
                        propagationSpeed() *
                        strength,
                )
            updateProgress(WaveProgress(baseline = baseline, pulse = pulse, strength = strength))
        }
    }
}

private suspend fun settleWave(
    initialProgress: WaveProgress,
    propagationSpeed: () -> Float,
    updateProgress: (WaveProgress) -> Unit,
) {
    if (initialProgress == WaveProgress.Icon) return

    val completedProgress =
        finishPropagation(
            initialProgress = initialProgress,
            propagationSpeed = propagationSpeed,
            updateProgress = updateProgress,
        )
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
private const val MAXIMUM_PULL_BACKTRACK_IN_RINGS = 1.25f
private const val PULL_RESISTANCE = 1.5f
