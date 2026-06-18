package ink.duo3.tuned.ui.player

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.abs
import kotlin.math.roundToLong

internal data class ProgressScrubConfig(
    val enabled: Boolean,
    val durationMs: Long,
    val currentPositionMs: Long,
    val inertiaThresholdPx: Float,
    val snapRangePx: Float,
    val snapReleaseRangePx: Float,
    val snapTargetsMs: List<Long>,
)

internal data class ProgressScrubCallbacks(
    val onStart: () -> Unit,
    val onScrub: (Long) -> Unit,
    val onChapterCrossed: () -> Unit,
    val onEndpointReached: () -> Unit,
    val onEnd: (Long) -> Unit,
    val onCancel: () -> Unit,
)

internal fun Modifier.progressScrubInput(
    enabled: Boolean,
    durationMs: Long,
    inertiaThresholdPx: Float,
    configProvider: () -> ProgressScrubConfig,
    callbacksProvider: () -> ProgressScrubCallbacks,
): Modifier =
    if (!enabled) {
        this
    } else {
        pointerInput(enabled, durationMs, inertiaThresholdPx) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val config = configProvider()
                val callbacks = callbacksProvider()
                var completed = false
                callbacks.onStart()
                down.consume()
                try {
                    completed = scrubRelativeToPointer(down, config, callbacks)
                } finally {
                    if (!completed) callbacks.onCancel()
                }
            }
        }
    }

private suspend fun AwaitPointerEventScope.scrubRelativeToPointer(
    down: PointerInputChange,
    config: ProgressScrubConfig,
    callbacks: ProgressScrubCallbacks,
): Boolean {
    val trackWidthPx = size.width.toFloat().coerceAtLeast(1f)
    var positionMs = config.currentPositionMs.toFloat()
    var snappedTargetMs: Long? = null
    val endpointHaptics = EndpointHapticState(config.durationMs.toFloat(), callbacks.onEndpointReached)
    val velocityTracker = VelocityTracker()
    velocityTracker.addPosition(down.uptimeMillis, down.position)
    val drag =
        awaitHorizontalScrubSlopOrCancellation(down.id) { change, overSlop ->
            positionMs = applyScrubDelta(positionMs, overSlop, trackWidthPx, config.durationMs)
            endpointHaptics.update(positionMs)
            val updatedSnapTarget = snappedTargetMs.updatedSnapTarget(positionMs, config, trackWidthPx)
            if (updatedSnapTarget != null && updatedSnapTarget != snappedTargetMs) callbacks.onChapterCrossed()
            snappedTargetMs = updatedSnapTarget
            velocityTracker.addPosition(change.uptimeMillis, change.position)
            callbacks.onScrub((snappedTargetMs?.toFloat() ?: positionMs).roundToLong())
            change.consume()
        } ?: return false

    horizontalDrag(drag.id) { change ->
        positionMs = applyScrubDelta(positionMs, change.positionChange().x, trackWidthPx, config.durationMs)
        endpointHaptics.update(positionMs)
        val updatedSnapTarget = snappedTargetMs.updatedSnapTarget(positionMs, config, trackWidthPx)
        if (updatedSnapTarget != null && updatedSnapTarget != snappedTargetMs) callbacks.onChapterCrossed()
        snappedTargetMs = updatedSnapTarget
        velocityTracker.addPosition(change.uptimeMillis, change.position)
        callbacks.onScrub((snappedTargetMs?.toFloat() ?: positionMs).roundToLong())
        change.consume()
    }

    val targetMs =
        snappedTargetMs?.toFloat()
            ?: inertialScrubTarget(
                positionMs = positionMs,
                velocityPxPerSecond = velocityTracker.calculateVelocity().x,
                trackWidthPx = trackWidthPx,
                config = config,
            )
    callbacks.onEnd(targetMs.roundToLong())
    return true
}

private suspend fun AwaitPointerEventScope.awaitHorizontalScrubSlopOrCancellation(
    pointerId: PointerId,
    onSlopReached: (PointerInputChange, Float) -> Unit,
): PointerInputChange? {
    var totalX = 0f
    var result: PointerInputChange? = null
    var awaitingSlop = true
    while (awaitingSlop) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == pointerId }
        if (change == null || !change.pressed) {
            awaitingSlop = false
        } else {
            val delta = change.positionChange()
            if (delta != Offset.Zero) {
                totalX += delta.x
                change.consume()
            }

            val overSlop = totalX.exceedsHorizontalSlop(viewConfiguration.touchSlop)
            if (overSlop != null) {
                onSlopReached(change, overSlop)
                result = change
                awaitingSlop = false
            }
        }
    }
    return result
}

private fun Float.exceedsHorizontalSlop(touchSlop: Float): Float? =
    when {
        this > touchSlop -> this - touchSlop
        this < -touchSlop -> this + touchSlop
        else -> null
    }

private fun applyScrubDelta(
    positionMs: Float,
    deltaPx: Float,
    trackWidthPx: Float,
    durationMs: Long,
): Float {
    val deltaMs = deltaPx / trackWidthPx * durationMs
    return (positionMs + deltaMs).coerceIn(0f, durationMs.toFloat())
}

private fun inertialScrubTarget(
    positionMs: Float,
    velocityPxPerSecond: Float,
    trackWidthPx: Float,
    config: ProgressScrubConfig,
): Float {
    if (abs(velocityPxPerSecond) < config.inertiaThresholdPx) return positionMs
    val projectedMs = velocityPxPerSecond / trackWidthPx * config.durationMs * SCRUB_INERTIA_SECONDS
    val maxProjection = config.durationMs * MAX_INERTIA_FRACTION
    val cappedProjection = projectedMs.coerceIn(-maxProjection, maxProjection)
    return (positionMs + cappedProjection).coerceIn(0f, config.durationMs.toFloat())
}

internal fun Long?.updatedSnapTarget(
    positionMs: Float,
    config: ProgressScrubConfig,
    trackWidthPx: Float,
): Long? {
    if (positionMs <= 0f || positionMs >= config.durationMs) return null
    val enterRangeMs = config.snapRangePx.toMs(trackWidthPx, config.durationMs)
    val releaseRangeMs = config.snapReleaseRangePx.toMs(trackWidthPx, config.durationMs)
    val currentTarget = this
    val enterCandidate = config.snapTargetsMs.nearestWithin(positionMs, enterRangeMs)
    val candidateIsCloser =
        currentTarget != null &&
            enterCandidate != null &&
            enterCandidate != currentTarget &&
            abs(enterCandidate - positionMs) < abs(currentTarget - positionMs)
    return when {
        candidateIsCloser -> enterCandidate
        currentTarget != null && abs(currentTarget - positionMs) <= releaseRangeMs -> currentTarget
        else -> enterCandidate
    }
}

private fun Float.toMs(
    trackWidthPx: Float,
    durationMs: Long,
): Float = this / trackWidthPx * durationMs

private fun List<Long>.nearestWithin(
    positionMs: Float,
    rangeMs: Float,
): Long? {
    var nearest: Long? = null
    var nearestDistance = Float.POSITIVE_INFINITY
    for (target in this) {
        val distance = abs(target - positionMs)
        if (distance <= rangeMs && distance < nearestDistance) {
            nearest = target
            nearestDistance = distance
        }
    }
    return nearest
}

private class EndpointHapticState(
    private val durationMs: Float,
    private val onEndpointReached: () -> Unit,
) {
    private var reachedEndpoint: ScrubEndpoint? = null

    fun update(positionMs: Float) {
        val endpoint =
            when {
                positionMs <= 0f -> ScrubEndpoint.Start
                positionMs >= durationMs -> ScrubEndpoint.End
                else -> null
            }
        if (endpoint != null && endpoint != reachedEndpoint) onEndpointReached()
        reachedEndpoint = endpoint
    }
}

private enum class ScrubEndpoint {
    Start,
    End,
}

private const val SCRUB_INERTIA_SECONDS = 0.18f
private const val MAX_INERTIA_FRACTION = 0.12f
