package ink.duo3.tuned.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.ui.components.interaction.LocalTunedHapticFeedbackEnabled
import ink.duo3.tuned.ui.components.interaction.performTunedThresholdHapticFeedback
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunedPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(
        pullProgress: Float,
        contentPullOffsetPx: Float,
        releasePulseKey: Int,
    ) -> Unit,
) {
    val state = rememberPullToRefreshState()
    val density = LocalDensity.current
    val view = LocalView.current
    val hapticFeedbackEnabled = LocalTunedHapticFeedbackEnabled.current
    val maxContentPullPx = with(density) { MAX_CONTENT_PULL_OFFSET.toPx() }
    var suppressPullProgress by remember { mutableStateOf(false) }
    var releasePulseKey by remember { mutableStateOf(0) }

    LaunchedEffect(isRefreshing, suppressPullProgress) {
        if (suppressPullProgress && !isRefreshing) {
            snapshotFlow { state.distanceFraction }
                .first { it == 0f }
            suppressPullProgress = false
        }
    }

    PullRefreshThresholdHapticEffect(
        state = state,
        hapticFeedbackEnabled = hapticFeedbackEnabled,
        performHapticFeedback = { view.performTunedThresholdHapticFeedback() },
    )

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            suppressPullProgress = true
            releasePulseKey++
            onRefresh()
        },
        modifier = modifier,
        state = state,
        indicator = {},
    ) {
        val pullProgress =
            if (isRefreshing || suppressPullProgress || state.isAnimating) {
                0f
            } else {
                state.distanceFraction
            }
        val contentPullOffsetPx =
            rememberContentPullOffset(
                targetOffsetPx = contentPullOffset(pullProgress, maxContentPullPx),
                isPulling = pullProgress > 0f,
            )
        val logoPullProgress =
            if (isRefreshing || suppressPullProgress) {
                0f
            } else {
                pullProgressFromContentOffset(contentPullOffsetPx, maxContentPullPx)
            }
        content(logoPullProgress, contentPullOffsetPx, releasePulseKey)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PullRefreshThresholdHapticEffect(
    state: PullToRefreshState,
    hapticFeedbackEnabled: Boolean,
    performHapticFeedback: () -> Unit,
) {
    val currentPerformHapticFeedback by rememberUpdatedState(performHapticFeedback)
    LaunchedEffect(state, hapticFeedbackEnabled) {
        var crossedThreshold = false
        snapshotFlow { state.distanceFraction }
            .collect { fraction ->
                when {
                    fraction <= 0f -> crossedThreshold = false
                    fraction >= REFRESH_THRESHOLD_FRACTION && !crossedThreshold -> {
                        crossedThreshold = true
                        if (hapticFeedbackEnabled) {
                            currentPerformHapticFeedback()
                        }
                    }
                }
            }
    }
}

@Composable
private fun rememberContentPullOffset(
    targetOffsetPx: Float,
    isPulling: Boolean,
): Float {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(targetOffsetPx, isPulling) {
        if (isPulling) {
            offset.snapTo(targetOffsetPx)
        } else {
            offset.animateTo(
                targetValue = 0f,
                animationSpec =
                    tween(
                        durationMillis = CONTENT_PULL_RETURN_MILLIS,
                        easing = FastOutSlowInEasing,
                    ),
            )
        }
    }
    return offset.value
}

private fun contentPullOffset(
    pullProgress: Float,
    maxOffsetPx: Float,
): Float {
    val pull = pullProgress.coerceAtLeast(0f)
    return maxOffsetPx * pull / (CONTENT_PULL_RESISTANCE + pull)
}

private fun pullProgressFromContentOffset(
    offsetPx: Float,
    maxOffsetPx: Float,
): Float {
    if (offsetPx <= 0f || maxOffsetPx <= 0f) return 0f
    val offset = offsetPx.coerceIn(0f, maxOffsetPx * MAX_OFFSET_PROGRESS_FRACTION)
    return offset * CONTENT_PULL_RESISTANCE / (maxOffsetPx - offset)
}

private val MAX_CONTENT_PULL_OFFSET = 72.dp
private const val REFRESH_THRESHOLD_FRACTION = 1f
private const val CONTENT_PULL_RESISTANCE = 0.8f
private const val CONTENT_PULL_RETURN_MILLIS = 260
private const val MAX_OFFSET_PROGRESS_FRACTION = 0.999f
