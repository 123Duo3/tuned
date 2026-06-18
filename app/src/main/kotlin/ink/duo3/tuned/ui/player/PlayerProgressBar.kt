package ink.duo3.tuned.ui.player

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.domain.model.Chapter
import ink.duo3.tuned.domain.player.PlaybackState
import ink.duo3.tuned.ui.components.interaction.LocalTunedHapticFeedbackEnabled
import ink.duo3.tuned.ui.components.interaction.performTunedEndpointHapticFeedback
import ink.duo3.tuned.ui.components.interaction.performTunedThresholdHapticFeedback
import ink.duo3.tuned.ui.components.text.Text
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@Composable
internal fun PlayerProgressBar(
    state: PlaybackState,
    chapters: List<Chapter>,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    observer: ProgressScrubObserver = ProgressScrubObserver(),
) {
    val scrubState = rememberProgressScrubUiState()
    val model = playerProgressModel(state, scrubState.positionMs)
    val density = LocalDensity.current
    val haptics = rememberProgressHaptics()
    val scrubConfig =
        remember(model.durationMs, model.displayedPositionMs, chapters, density) {
            ProgressScrubConfig(
                enabled = model.hasSeekableDuration,
                durationMs = model.durationMs,
                currentPositionMs = model.displayedPositionMs,
                inertiaThresholdPx = with(density) { SCRUB_INERTIA_MIN_VELOCITY.toPx() },
                snapRangePx = with(density) { CHAPTER_SNAP_RANGE.toPx() },
                snapReleaseRangePx = with(density) { CHAPTER_SNAP_RELEASE_RANGE.toPx() },
                snapTargetsMs =
                    chapters
                        .asSequence()
                        .map { chapter -> chapter.startTimeMs }
                        .filter { startMs -> startMs > 0L && startMs < model.durationMs }
                        .distinct()
                        .toList(),
            )
        }
    val latestScrubConfig by rememberUpdatedState(scrubConfig)
    val callbacks =
        remember(onSeek, scrubState, observer, haptics) {
            scrubState.callbacks(
                onSeek = onSeek,
                observer = observer,
                onChapterCrossed = haptics.onChapter,
                onEndpointReached = haptics.onEndpoint,
            )
        }
    val latestCallbacks by rememberUpdatedState(callbacks)
    scrubState.ObservePlaybackPosition(state.positionMs, observer.onPositionChanged)
    val trackHeight by animateDpAsState(
        targetValue = if (scrubState.isScrubbing) PRESSED_TRACK_HEIGHT else TRACK_HEIGHT,
        label = "player-progress-height",
    )

    Column(modifier.fillMaxWidth()) {
        ProgressTrack(
            model = model,
            chapters = chapters,
            trackHeight = trackHeight,
            scrub =
                ProgressTrackScrub(
                    config = scrubConfig,
                    configProvider = { latestScrubConfig },
                    callbacksProvider = { latestCallbacks },
                ),
        )
        ProgressTimeLabels(
            positionMs = model.displayedPositionMs,
            durationMs = model.durationMs,
            isScrubbing = scrubState.isScrubbing,
            trackHeight = trackHeight,
        )
    }
}

@Composable
private fun rememberProgressHaptics(): ProgressHaptics {
    val view = LocalView.current
    val enabled = LocalTunedHapticFeedbackEnabled.current
    return remember(view, enabled) {
        ProgressHaptics(
            onChapter = { if (enabled) view.performTunedThresholdHapticFeedback() },
            onEndpoint = { if (enabled) view.performTunedEndpointHapticFeedback() },
        )
    }
}

@Composable
private fun ProgressTrack(
    model: PlayerProgressModel,
    chapters: List<Chapter>,
    trackHeight: Dp,
    scrub: ProgressTrackScrub,
) {
    val colors = progressTrackColors()
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(PROGRESS_TOUCH_HEIGHT)
                .progressScrubInput(
                    enabled = scrub.config.enabled,
                    durationMs = scrub.config.durationMs,
                    inertiaThresholdPx = scrub.config.inertiaThresholdPx,
                    configProvider = scrub.configProvider,
                    callbacksProvider = scrub.callbacksProvider,
                ).semantics {
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(
                            current = model.displayedPositionMs.toFloat(),
                            range = 0f..model.durationMs.coerceAtLeast(1L).toFloat(),
                        )
                    setProgress { requestedPosition ->
                        if (!scrub.config.enabled) return@setProgress false
                        val targetMs = requestedPosition.roundToLong().coerceIn(0L, scrub.config.durationMs)
                        scrub.callbacksProvider().run {
                            onStart()
                            onScrub(targetMs)
                            onEnd(targetMs)
                        }
                        true
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(trackHeight),
        ) {
            drawSegmentedProgressTrack(
                input =
                    ProgressTrackInput(
                        positionMs = model.displayedPositionMs,
                        durationMs = model.durationMs,
                        chapters = chapters,
                    ),
                colors = colors,
                metrics = progressTrackMetrics(trackHeight),
            )
        }
    }
}

@Composable
private fun progressTrackColors(): ProgressTrackColors {
    val colors = MaterialTheme.colorScheme
    return ProgressTrackColors(
        inactive = colors.onSurfaceVariant.copy(alpha = INACTIVE_TRACK_ALPHA),
        played = colors.primary,
        activeChapterDivider = colors.surfaceContainer.copy(alpha = CHAPTER_DIVIDER_ALPHA),
        remainingChapterDivider = colors.primary.copy(alpha = CHAPTER_DIVIDER_ALPHA),
        activeStopIndicator = colors.surfaceContainer,
        remainingStopIndicator = colors.primary,
    )
}

private fun progressTrackMetrics(trackHeight: Dp): ProgressTrackMetrics =
    ProgressTrackMetrics(
        height = trackHeight,
        cornerRadius = TRACK_CORNER_RADIUS,
        progressGap = PROGRESS_GAP,
        chapterDividerWidth = CHAPTER_DIVIDER_WIDTH,
        chapterDividerInset = CHAPTER_DIVIDER_INSET,
        stopIndicatorSize = STOP_INDICATOR_SIZE,
    )

@Composable
private fun ProgressTimeLabels(
    positionMs: Long,
    durationMs: Long,
    isScrubbing: Boolean,
    trackHeight: Dp,
) {
    val scale by animateFloatAsState(
        targetValue = if (isScrubbing) PRESSED_TIME_SCALE else 1f,
        label = "player-progress-time-scale",
    )
    val baseStyle = MaterialTheme.typography.labelMedium
    val weightProgress = ((scale - 1f) / (PRESSED_TIME_SCALE - 1f)).coerceIn(0f, 1f)
    val startWeight = baseStyle.fontWeight ?: FontWeight.Medium
    val animatedWeightValue =
        startWeight.weight + (FontWeight.Normal.weight - startWeight.weight) * weightProgress
    val animatedWeight = FontWeight(animatedWeightValue.roundToInt())
    val timeStyle = baseStyle.copy(fontWeight = animatedWeight)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .offset(y = trackHeight / 2 - PROGRESS_TOUCH_HEIGHT / 2 + PROGRESS_TIME_GAP),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatTime(positionMs),
            style = timeStyle,
            modifier =
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(0f, 0f)
                },
        )
        Text(
            text = "\u2212${formatTime((durationMs - positionMs).coerceAtLeast(0L))}",
            style = timeStyle,
            modifier =
                Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(1f, 0f)
                },
        )
    }
}

private fun playerProgressModel(
    state: PlaybackState,
    scrubPositionMs: Long,
): PlayerProgressModel {
    val durationMs = state.durationMs?.coerceAtLeast(0L) ?: 0L
    val displayedPositionMs =
        if (scrubPositionMs != NO_SCRUB_POSITION) {
            scrubPositionMs
        } else {
            state.positionMs
        }.coerceForDuration(durationMs)
    return PlayerProgressModel(
        durationMs = durationMs,
        displayedPositionMs = displayedPositionMs,
        hasSeekableDuration = durationMs > 0L,
    )
}

private fun Long.coerceForDuration(durationMs: Long): Long = if (durationMs > 0L) coerceIn(0L, durationMs) else 0L

@Composable
private fun rememberProgressScrubUiState(): ProgressScrubUiState {
    val scope = rememberCoroutineScope()
    return remember(scope) { ProgressScrubUiState(scope) }
}

private suspend fun animateScrubInertia(
    startMs: Long,
    targetMs: Long,
    update: (Long) -> Unit,
) {
    if (startMs == targetMs) {
        update(targetMs)
        return
    }
    animate(
        initialValue = startMs.toFloat(),
        targetValue = targetMs.toFloat(),
        animationSpec = tween(durationMillis = SCRUB_INERTIA_MILLIS),
    ) { value, _ -> update(value.roundToLong()) }
}

private class ProgressScrubUiState(
    private val scope: CoroutineScope,
) {
    var isScrubbing by mutableStateOf(false)
        private set

    var positionMs by mutableLongStateOf(NO_SCRUB_POSITION)
        private set

    private var inertiaJob: Job? = null

    fun callbacks(
        onSeek: (Long) -> Unit,
        observer: ProgressScrubObserver,
        onChapterCrossed: () -> Unit,
        onEndpointReached: () -> Unit,
    ): ProgressScrubCallbacks =
        ProgressScrubCallbacks(
            onStart = {
                inertiaJob?.cancel()
                pendingSeekMs = null
                isScrubbing = true
                observer.onInteractionChanged(true)
            },
            onScrub = {
                positionMs = it
                observer.onPositionChanged(it)
            },
            onChapterCrossed = onChapterCrossed,
            onEndpointReached = onEndpointReached,
            onEnd = { targetMs ->
                inertiaJob =
                    scope.launch {
                        animateScrubInertia(
                            startMs = positionMs.takeIf { it != NO_SCRUB_POSITION } ?: targetMs,
                            targetMs = targetMs,
                            update = {
                                positionMs = it
                                observer.onPositionChanged(it)
                            },
                        )
                        pendingSeekMs = targetMs
                        onSeek(targetMs)
                        isScrubbing = false
                        observer.onInteractionChanged(false)
                    }
            },
            onCancel = {
                inertiaJob?.cancel()
                pendingSeekMs = null
                positionMs = NO_SCRUB_POSITION
                observer.onPositionChanged(null)
                isScrubbing = false
                observer.onInteractionChanged(false)
            },
        )

    private var pendingSeekMs: Long? = null

    @Composable
    fun ObservePlaybackPosition(
        playbackPositionMs: Long,
        onScrubPositionChanged: (Long?) -> Unit,
    ) {
        val latestCallback by rememberUpdatedState(onScrubPositionChanged)
        LaunchedEffect(playbackPositionMs) {
            onPlaybackPositionChanged(playbackPositionMs, latestCallback)
        }
    }

    fun onPlaybackPositionChanged(
        playbackPositionMs: Long,
        onScrubPositionChanged: (Long?) -> Unit,
    ) {
        val targetMs = pendingSeekMs ?: return
        if (abs(playbackPositionMs - targetMs) > SEEK_CONFIRM_TOLERANCE_MS) return
        pendingSeekMs = null
        positionMs = NO_SCRUB_POSITION
        onScrubPositionChanged(null)
    }
}

private data class PlayerProgressModel(
    val durationMs: Long,
    val displayedPositionMs: Long,
    val hasSeekableDuration: Boolean,
)

private data class ProgressHaptics(
    val onChapter: () -> Unit,
    val onEndpoint: () -> Unit,
)

internal data class ProgressScrubObserver(
    val onInteractionChanged: (Boolean) -> Unit = {},
    val onPositionChanged: (Long?) -> Unit = {},
)

private data class ProgressTrackScrub(
    val config: ProgressScrubConfig,
    val configProvider: () -> ProgressScrubConfig,
    val callbacksProvider: () -> ProgressScrubCallbacks,
)

private const val NO_SCRUB_POSITION = Long.MIN_VALUE
private const val INACTIVE_TRACK_ALPHA = 0.16f
private const val CHAPTER_DIVIDER_ALPHA = 0.56f
private const val SCRUB_INERTIA_MILLIS = 180
private const val SEEK_CONFIRM_TOLERANCE_MS = 2_000L
private val TRACK_HEIGHT = 12.dp
private val TRACK_CORNER_RADIUS = TRACK_HEIGHT / 2
private val PRESSED_TRACK_HEIGHT = 24.dp
private const val PRESSED_TIME_SCALE = 2f
private val PROGRESS_TOUCH_HEIGHT = 40.dp
private val PROGRESS_TIME_GAP = 8.dp
private val PROGRESS_GAP = 2.dp
private val CHAPTER_SNAP_RANGE = 4.dp
private val CHAPTER_SNAP_RELEASE_RANGE = 8.dp
private val CHAPTER_DIVIDER_WIDTH = 1.dp
private val CHAPTER_DIVIDER_INSET = 4.dp
private val STOP_INDICATOR_SIZE = 4.dp
private val SCRUB_INERTIA_MIN_VELOCITY = 640.dp
