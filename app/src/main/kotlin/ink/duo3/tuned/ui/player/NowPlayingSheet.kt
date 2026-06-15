@file:Suppress("TooManyFunctions")

package ink.duo3.tuned.ui.player

import android.os.Build
import android.view.RoundedCorner
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Precision
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.player.PlaybackState
import ink.duo3.tuned.presentation.player.PlayerUiState
import ink.duo3.tuned.presentation.player.PlayerViewModel
import ink.duo3.tuned.ui.components.ArtworkImageDefaults
import ink.duo3.tuned.ui.components.Text
import ink.duo3.tuned.ui.components.TunedDropdownMenuBox
import ink.duo3.tuned.ui.components.miniPlayerPlatformHeight
import ink.duo3.tuned.ui.components.rememberTunedDropdownMenuState
import ink.duo3.tuned.ui.components.tunedAnimatedRoundedCornerShape
import ink.duo3.tuned.ui.components.tunedRoundedCornerShape
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * The persistent now-playing surface. A single element that interpolates between a collapsed
 * mini bar ([NowPlayingSheetState.progress] = 0) and the full-screen player (= 1): the
 * background container grows, the artwork is one shared element that physically moves and
 * scales, and the two control layouts cross-fade. There is no separate player route — expanding
 * is a state change, not a navigation.
 */
@Composable
internal fun NowPlayingSheet(
    viewModel: PlayerViewModel,
    sheetState: NowPlayingSheetState,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val actions = remember(viewModel) { viewModel.playerActions() }
    val sleepTimerRemainingMs = state.playback.sleepTimerRemainingMs
    val sleepTimer = remember(viewModel, sleepTimerRemainingMs) { viewModel.sleepTimerControls(sleepTimerRemainingMs) }
    val contentState =
        remember(state, actions, sleepTimer) {
            SheetContentState(
                player = state,
                actions = actions,
                sleepTimer = sleepTimer,
            )
        }

    BackHandler(enabled = sheetState.expandedTarget) { scope.launch { sheetState.collapse() } }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val density = LocalDensity.current
        // Reuse the same platform height the content clearance reserves, so the collapsed sheet
        // floats exactly where the old mini player did (navigation bar + 8dp).
        val platformPx = with(density) { miniPlayerPlatformHeight().toPx() }
        val metrics =
            sheetMetrics(
                progress = sheetState.progress,
                rootWidth = constraints.maxWidth.toFloat(),
                rootHeight = constraints.maxHeight.toFloat(),
                statusTop = WindowInsets.statusBars.getTop(density).toFloat(),
                platform = platformPx,
                pageCorner = deviceScreenCornerRadius(),
                density = density,
            )
        sheetState.travelPx = metrics.travelPx

        Box(
            Modifier
                .fillMaxSize()
                .alpha((metrics.eased * SCRIM_MAX_ALPHA).coerceIn(0f, 1f))
                .background(Color.Black),
        )
        SheetFrame(
            content = contentState,
            sheetState = sheetState,
            metrics = metrics,
            onCollapse = { scope.launch { sheetState.collapse() } },
        )
    }
}

@Composable
private fun SheetSurface(metrics: SheetMetrics) {
    // The mini player's original soft drop shadow (not Material elevation), fading out as the sheet
    // expands to full screen where a lifted shadow no longer makes sense.
    val baseShadowAlpha = if (isSystemInDarkTheme()) MINI_SHADOW_ALPHA_DARK else MINI_SHADOW_ALPHA_LIGHT
    val shadowAlpha = baseShadowAlpha * (1f - metrics.eased).coerceIn(0f, 1f)
    Surface(
        modifier =
            Modifier
                .absolute(0f, 0f, metrics.sheetWidth, metrics.sheetHeight)
                .dropShadow(
                    shape = tunedAnimatedRoundedCornerShape(metrics.cornerDp),
                    shadow =
                        Shadow(
                            radius = MINI_SHADOW_RADIUS,
                            color = Color.Black,
                            offset = DpOffset(0.dp, MINI_SHADOW_Y_OFFSET),
                            alpha = shadowAlpha,
                        ),
                ),
        shape = tunedAnimatedRoundedCornerShape(metrics.cornerDp),
        color = metrics.containerColor,
        contentColor = metrics.contentColor,
    ) {}
}

private fun Modifier.sheetDragInput(state: NowPlayingSheetState): Modifier =
    composed {
        val density = LocalDensity.current
        val commitDistancePx = with(density) { DRAG_COMMIT_DISTANCE.toPx() }
        var dragDeltaPx by remember { mutableFloatStateOf(0f) }
        draggable(
            state =
                rememberDraggableState { delta ->
                    dragDeltaPx += delta
                    state.onDrag(delta)
                },
            orientation = Orientation.Vertical,
            onDragStarted = { dragDeltaPx = 0f },
            onDragStopped = { velocity ->
                state.settle(
                    velocityPx = velocity,
                    dragDeltaPx = dragDeltaPx,
                    commitDistancePx = commitDistancePx,
                )
                dragDeltaPx = 0f
            },
        )
    }

// The expanded player is one rigid page laid out at its final full-screen position, with an empty
// slot reserved where the artwork lands. The whole page — top bar, slot, and controls — moves as a
// single block; only the vertical axis follows the artwork.
//
// The page layer is anchored at the sheet's top-left. Y drops the sheet origin, then adds the gap
// between where the page reserves the artwork (its expanded centre) and where the artwork actually
// is right now, so the slot rides the cover up. At rest (p=1) the follow term is zero and the page
// sits exactly at its final layout.
private fun SheetMetrics.expandedContentOffsetY(): Float =
    -sheetTop + (artTopRoot + artSize / 2f) - (artExpandedTop + artExpandedSize / 2f)

private fun miniContentAlpha(progress: Float): Float = (1f - progress / CONTENT_CROSSFADE_END).coerceIn(0f, 1f)

private fun expandedContentAlpha(progress: Float): Float =
    ((progress - CONTENT_CROSSFADE_START) / (1f - CONTENT_CROSSFADE_START)).coerceIn(0f, 1f)

@Composable
private fun SheetFrame(
    content: SheetContentState,
    sheetState: NowPlayingSheetState,
    metrics: SheetMetrics,
    onCollapse: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .absolute(metrics.sheetLeft, metrics.sheetTop, metrics.sheetWidth, metrics.sheetHeight)
                .sheetDragInput(sheetState),
    ) {
        SheetSurface(metrics = metrics)
        SheetContentLayers(content, sheetState, metrics, onCollapse)
        SharedArtwork(state = content.player, metrics = metrics)
    }
}

@Composable
private fun SheetContentLayers(
    content: SheetContentState,
    sheetState: NowPlayingSheetState,
    metrics: SheetMetrics,
    onCollapse: () -> Unit,
) {
    val density = LocalDensity.current
    val miniStartPadding =
        with(density) {
            (metrics.collapsedArtSize + COLLAPSED_GAP.toPx() + COLLAPSED_INNER_PAD.toPx()).toDp()
        }
    val collapsedHeight = with(density) { metrics.collapsedHeight.toDp() }
    val expandedLayout =
        ExpandedContentLayout(
            statusTop = with(density) { metrics.statusTop.toDp() },
            topBarToArtwork =
                with(density) { (metrics.artExpandedTop - metrics.statusTop - metrics.topBarHeight).toDp() },
            artworkReserve = with(density) { metrics.artExpandedSize.toDp() },
        )

    Box(
        modifier =
            Modifier
                .absolute(0f, 0f, metrics.sheetWidth, metrics.sheetHeight)
                .clip(tunedAnimatedRoundedCornerShape(metrics.cornerDp))
                .nestedScroll(rememberSheetNestedScrollConnection(sheetState)),
    ) {
        MiniContentLayer(content, sheetState, metrics, collapsedHeight, miniStartPadding)
        ExpandedContentLayer(content, metrics, expandedLayout, onCollapse)
    }
}

@Immutable
private data class SheetContentState(
    val player: PlayerUiState,
    val actions: PlayerActions,
    val sleepTimer: SleepTimerControls,
)

@Immutable
private data class ExpandedContentLayout(
    val statusTop: Dp,
    val topBarToArtwork: Dp,
    val artworkReserve: Dp,
)

@Composable
private fun MiniContentLayer(
    content: SheetContentState,
    sheetState: NowPlayingSheetState,
    metrics: SheetMetrics,
    height: Dp,
    startPadding: Dp,
) {
    val alpha = miniContentAlpha(metrics.eased)
    if (alpha <= 0f) return

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSecondaryContainer) {
        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier =
                Modifier
                    .absolute(0f, 0f, metrics.collapsedSheetWidth, metrics.collapsedHeight)
                    .alpha(alpha)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = metrics.eased < TAP_EXPAND_PROGRESS_LIMIT,
                        onClick = sheetState::expand,
                    ),
        ) {
            MiniContent(
                state = content.player.playback,
                height = height,
                startPadding = startPadding,
                onPlayPause = content.actions.onPlayPause,
            )
        }
    }
}

@Composable
private fun ExpandedContentLayer(
    content: SheetContentState,
    metrics: SheetMetrics,
    layout: ExpandedContentLayout,
    onCollapse: () -> Unit,
) {
    val alpha = expandedContentAlpha(metrics.eased)
    if (alpha <= 0f) return

    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Box(
            modifier =
                Modifier
                    .absoluteRequired(0f, 0f, metrics.rootWidth, metrics.rootHeight)
                    .graphicsLayer {
                        translationY = metrics.expandedContentOffsetY() * -1
                    }
                    .alpha(alpha),
        ) {
            ExpandedContent(
                state = content.player,
                actions = content.actions,
                sleepTimer = content.sleepTimer,
                layout = layout,
                onCollapse = onCollapse,
            )
        }
    }
}

@Composable
private fun SharedArtwork(
    state: PlayerUiState,
    metrics: SheetMetrics,
) {
    // A plain Box, not a Surface: it sits on top of the sheet, and a Surface would consume pointer
    // events (Material's click-through guard) and swallow drags meant for the sheet underneath.
    val artworkShape = tunedRoundedCornerShape(metrics.artCornerDp)
    val outline = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier =
            Modifier
                .absolute(
                    x = metrics.artLeftRoot - metrics.sheetLeft,
                    y = metrics.artTopRoot - metrics.sheetTop,
                    widthPx = metrics.artSize,
                    heightPx = metrics.artSize,
                )
                .clip(artworkShape)
                .background(outline)
                .border(ArtworkImageDefaults.BorderWidth, outline, artworkShape),
    ) {
        // Two resolutions so the artwork is crisp at both ends: a full-size bitmap underneath for
        // the expanded player, and a thumbnail on top — decoded small by Coil rather than a large
        // bitmap GPU-downscaled to 56dp, which aliases — fading out as the sheet opens.
        ArtworkImage(
            url = state.artworkUrl,
            title = state.playback.title,
            requestSizePx = metrics.artExpandedSize,
            modifier = Modifier.fillMaxSize(),
        )
        ArtworkImage(
            url = state.artworkUrl,
            title = state.playback.title,
            requestSizePx = metrics.collapsedArtSize,
            modifier =
                Modifier
                    .fillMaxSize()
                    .alpha(miniContentAlpha(metrics.eased)),
        )
    }
}

@Composable
private fun ArtworkImage(
    url: String?,
    title: String?,
    requestSizePx: Float,
    modifier: Modifier,
) {
    val context = LocalPlatformContext.current
    val size = requestSizePx.roundToInt().coerceAtLeast(1)
    val request =
        remember(url, size) {
            ImageRequest
                .Builder(context)
                .data(url)
                .size(size)
                .precision(Precision.EXACT)
                .build()
        }
    AsyncImage(
        model = request,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

@Composable
private fun rememberSheetNestedScrollConnection(state: NowPlayingSheetState): NestedScrollConnection =
    remember(state) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                // Only a live finger drag drives the sheet; fling deltas (SideEffect) would call
                // onDrag, which cancels a settling spring and strands the morph mid-way.
                if (source != NestedScrollSource.UserInput || available.y >= 0f || state.progress >= 1f) {
                    return Offset.Zero
                }
                return Offset(x = 0f, y = state.onDrag(available.y))
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f || state.progress <= 0f) {
                    return Offset.Zero
                }
                return Offset(x = 0f, y = state.onDrag(available.y))
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Only finish a sheet the user actually dragged part-way (progress strictly between
                // the two ends). A fling of the list itself must NOT carry into the sheet, so there
                // is no onPostFling handling — the list just keeps its own momentum.
                if (state.progress > 0f && state.progress < 1f) {
                    state.settle(available.y)
                    return Velocity(x = 0f, y = available.y)
                }
                return Velocity.Zero
            }
        }
    }

@Composable
private fun MiniContent(
    state: PlaybackState,
    height: Dp,
    startPadding: Dp,
    onPlayPause: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(start = startPadding, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = state.title.orEmpty(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!state.podcastTitle.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.podcastTitle.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        PlayPauseButton(isPlaying = state.isPlaying || state.buffering, onClick = onPlayPause)
    }
}

@Composable
private fun ExpandedContent(
    state: PlayerUiState,
    actions: PlayerActions,
    sleepTimer: SleepTimerControls,
    layout: ExpandedContentLayout,
    onCollapse: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(top = layout.statusTop)
                .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ExpandedTopBar(podcastTitle = state.playback.podcastTitle, sleepTimer = sleepTimer, onCollapse = onCollapse)
        Spacer(Modifier.height(layout.topBarToArtwork))
        Spacer(Modifier.height(layout.artworkReserve + 24.dp))
        ExpandedControls(state = state, actions = actions)
    }
}

@Composable
private fun ExpandedTopBar(
    podcastTitle: String?,
    sleepTimer: SleepTimerControls,
    onCollapse: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(EXPANDED_TOP_BAR_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onCollapse) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.player_back))
        }
        Text(
            text = podcastTitle.orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        SleepTimerAction(sleepTimer)
    }
}

@Composable
private fun ExpandedControls(
    state: PlayerUiState,
    actions: PlayerActions,
) {
    Text(
        text = state.playback.title.orEmpty(),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    state.currentChapter?.title?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    Spacer(Modifier.height(16.dp))
    ProgressBar(state = state.playback, onSeek = actions.onSeek)
    Spacer(Modifier.height(16.dp))
    TransportRow(
        isPlaying = state.playback.isPlaying,
        buffering = state.playback.buffering,
        onPlayPause = actions.onPlayPause,
        onSkipBack = actions.onSkipBack,
        onSkipForward = actions.onSkipForward,
    )
    TextButton(onClick = actions.onCycleSpeed) {
        Text(stringResource(R.string.player_speed, formatSpeed(state.playback.speed)))
    }
    if (state.chapters.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            ChapterList(
                chapters = state.chapters,
                currentChapterIndex = state.currentChapterIndex,
                onChapterClick = actions.onSeek,
            )
        }
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = stringResource(if (isPlaying) R.string.player_pause else R.string.player_play),
        )
    }
}

/** The player's event callbacks, grouped so content composables stay small. */
internal class PlayerActions(
    val onPlayPause: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onSkipBack: () -> Unit,
    val onSkipForward: () -> Unit,
    val onCycleSpeed: () -> Unit,
)

internal class SleepTimerControls(
    val remainingMs: Long?,
    val presetsMinutes: List<Int>,
    val onStart: (Int) -> Unit,
    val onCancel: () -> Unit,
)

private fun PlayerViewModel.playerActions() =
    PlayerActions(
        onPlayPause = ::playPause,
        onSeek = ::seekTo,
        onSkipBack = ::skipBack,
        onSkipForward = ::skipForward,
        onCycleSpeed = ::cycleSpeed,
    )

private fun PlayerViewModel.sleepTimerControls(remainingMs: Long?) =
    SleepTimerControls(
        remainingMs = remainingMs,
        presetsMinutes = sleepTimerPresetsMinutes,
        onStart = ::startSleepTimer,
        onCancel = ::cancelSleepTimer,
    )

@Composable
private fun SleepTimerAction(controls: SleepTimerControls) {
    val menuState = rememberTunedDropdownMenuState()
    TunedDropdownMenuBox(
        state = menuState,
        anchor = { anchorModifier, openMenu ->
            IconButton(modifier = anchorModifier, onClick = openMenu) {
                Icon(
                    imageVector = if (controls.remainingMs != null) Icons.Filled.Bedtime else Icons.Outlined.Bedtime,
                    contentDescription = stringResource(R.string.player_sleep_timer),
                )
            }
        },
    ) {
        controls.presetsMinutes.forEach { minutes ->
            Item(
                text = { Text(stringResource(R.string.player_sleep_timer_minutes, minutes)) },
                onClick = { controls.onStart(minutes) },
            )
        }
        if (controls.remainingMs != null) {
            Divider()
            Item(
                text = { Text(stringResource(R.string.player_sleep_timer_cancel)) },
                onClick = controls.onCancel,
            )
        }
    }
}

@Composable
private fun ProgressBar(
    state: PlaybackState,
    onSeek: (Long) -> Unit,
) {
    var scrub by remember { mutableStateOf<Float?>(null) }
    val durationMs = state.durationMs?.coerceAtLeast(0)
    val hasSeekableDuration = durationMs != null && durationMs > 0L
    val rawPositionMs = scrub?.toLong() ?: state.positionMs.coerceAtLeast(0)
    val sliderMaxMs = if (hasSeekableDuration) durationMs else 1L
    val sliderPositionMs = if (hasSeekableDuration) rawPositionMs.coerceIn(0L, sliderMaxMs) else 0L
    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = sliderPositionMs.toFloat(),
            onValueChange = { scrub = it },
            onValueChangeFinished = {
                scrub?.let { onSeek(it.toLong()) }
                scrub = null
            },
            valueRange = 0f..sliderMaxMs.toFloat(),
            enabled = hasSeekableDuration,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(rawPositionMs), style = MaterialTheme.typography.labelMedium)
            Text(formatTime(durationMs ?: 0L), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun TransportRow(
    isPlaying: Boolean,
    buffering: Boolean,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSkipBack) {
            Icon(
                painter = painterResource(R.drawable.ic_skip_back_15),
                contentDescription = stringResource(R.string.player_skip_back),
            )
        }
        FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp)) {
            if (buffering) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription =
                        stringResource(if (isPlaying) R.string.player_pause else R.string.player_play),
                )
            }
        }
        IconButton(onClick = onSkipForward) {
            Icon(Icons.Filled.FastForward, contentDescription = stringResource(R.string.player_skip_forward))
        }
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) {
        speed.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", speed)
    }

internal fun formatTime(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0))
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/**
 * The device's physical screen corner radius (API 31+), so the morphing sheet rounds to match it
 * like the predictive-back gesture. Falls back to a sane default when the platform doesn't report
 * one (older APIs, or square-cornered displays).
 */
@Composable
private fun deviceScreenCornerRadius(): Dp {
    val view = LocalView.current
    val density = LocalDensity.current
    return remember(view) {
        val radiusPx =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val insets = view.rootWindowInsets
                intArrayOf(
                    RoundedCorner.POSITION_TOP_LEFT,
                    RoundedCorner.POSITION_TOP_RIGHT,
                    RoundedCorner.POSITION_BOTTOM_LEFT,
                    RoundedCorner.POSITION_BOTTOM_RIGHT,
                ).maxOf { insets?.getRoundedCorner(it)?.radius ?: 0 }
            } else {
                0
            }
        with(density) { radiusPx.toDp() }.takeIf { it > 0.dp } ?: FALLBACK_PAGE_CORNER
    }
}

/** Absolutely positions and sizes an element within its parent, in pixels. */
private fun Modifier.absolute(
    x: Float,
    y: Float,
    widthPx: Float,
    heightPx: Float,
): Modifier =
    composed {
        val density = LocalDensity.current
        offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .width(with(density) { widthPx.toDp() })
            .height(with(density) { heightPx.toDp() })
    }

/** Positions an element in pixels while forcing its measured size past parent constraints. */
private fun Modifier.absoluteRequired(
    x: Float,
    y: Float,
    widthPx: Float,
    heightPx: Float,
): Modifier =
    composed {
        val density = LocalDensity.current
        offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            .requiredWidth(with(density) { widthPx.toDp() })
            .requiredHeight(with(density) { heightPx.toDp() })
    }

private val COLLAPSED_GAP = 12.dp
private val COLLAPSED_INNER_PAD = 4.dp
private val EXPANDED_TOP_BAR_HEIGHT = 56.dp
private val FALLBACK_PAGE_CORNER = 28.dp
private val DRAG_COMMIT_DISTANCE = 16.dp
private val MINI_SHADOW_RADIUS = 24.dp
private val MINI_SHADOW_Y_OFFSET = 4.dp
private const val MINI_SHADOW_ALPHA_LIGHT = 0.03f
private const val MINI_SHADOW_ALPHA_DARK = 0.1f
private const val SCRIM_MAX_ALPHA = 0.32f
private const val TAP_EXPAND_PROGRESS_LIMIT = 0.35f
private const val CONTENT_CROSSFADE_START = 0.35f
private const val CONTENT_CROSSFADE_END = 0.65f
