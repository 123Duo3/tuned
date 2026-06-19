@file:Suppress("TooManyFunctions")

package ink.duo3.tuned.ui.player

import android.os.Build
import android.view.RoundedCorner
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.lerp
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
import com.materialkolor.ktx.animateColorScheme
import com.materialkolor.ktx.harmonizeWithPrimary
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.player.PlaybackState
import ink.duo3.tuned.presentation.player.PlayerUiState
import ink.duo3.tuned.presentation.player.PlayerViewModel
import ink.duo3.tuned.ui.components.artwork.ArtworkImageDefaults
import ink.duo3.tuned.ui.components.artwork.rememberArtworkColorScheme
import ink.duo3.tuned.ui.components.button.TunedButtonGroup
import ink.duo3.tuned.ui.components.button.TunedButtonGroupButton
import ink.duo3.tuned.ui.components.button.TunedButtonGroupButtonStyle
import ink.duo3.tuned.ui.components.button.TunedButtonGroupItem
import ink.duo3.tuned.ui.components.button.TunedButtonGroupScope
import ink.duo3.tuned.ui.components.dropdown.TunedDropdownMenuBox
import ink.duo3.tuned.ui.components.dropdown.TunedDropdownMenuScope
import ink.duo3.tuned.ui.components.dropdown.rememberTunedDropdownMenuState
import ink.duo3.tuned.ui.components.playback.AnimatedSkipIcon
import ink.duo3.tuned.ui.components.playback.PlaybackSkipDirection
import ink.duo3.tuned.ui.components.playback.PlaybackSkipSeconds
import ink.duo3.tuned.ui.components.scaffold.miniPlayerPlatformHeight
import ink.duo3.tuned.ui.components.shape.tunedAnimatedRoundedCornerShape
import ink.duo3.tuned.ui.components.shape.tunedRoundedCornerShape
import ink.duo3.tuned.ui.components.text.Text
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
    onShowNotes: (String) -> Unit,
    onPlaylist: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val actions = remember(viewModel, onShowNotes, onPlaylist) { viewModel.playerActions(onShowNotes, onPlaylist) }
    val sleepTimer = rememberSleepTimerControls(viewModel, state.playback.sleepTimerRemainingMs)
    val progressScrub = rememberProgressScrubPresentation()
    val displayedState = remember(state, progressScrub.positionMs) { state.atPosition(progressScrub.positionMs) }
    val artworkColorScheme = animateColorScheme(rememberArtworkColorScheme(displayedState.artworkUrl))
    val miniColors = MaterialTheme.colorScheme.harmonizedMiniColors(artworkColorScheme)
    val contentState =
        remember(
            displayedState,
            state.playback,
            actions,
            sleepTimer,
            progressScrub.observer,
        ) {
            SheetContentState(
                player = displayedState,
                progressPlayback = state.playback,
                actions = actions,
                sleepTimer = sleepTimer,
                progressScrubObserver = progressScrub.observer,
            )
        }

    BackHandler(enabled = sheetState.expandedTarget) { scope.launch { sheetState.collapse() } }

    MaterialTheme(colorScheme = artworkColorScheme) {
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
                    collapsedContainerColor = miniColors.container,
                    collapsedContentColor = miniColors.content,
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
                sheetDragEnabled = !progressScrub.active,
            )
        }
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

private fun Modifier.sheetDragInput(
    state: NowPlayingSheetState,
    enabled: Boolean,
): Modifier =
    composed {
        draggable(
            state =
                rememberDraggableState { delta ->
                    state.onDrag(delta)
                },
            orientation = Orientation.Vertical,
            enabled = enabled,
            onDragStopped = { velocity -> state.settle(velocity) },
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
private fun SheetMetrics.expandedContentOffsetY(): Float {
    val currentCenter = artTopRoot + artSize / 2f
    val expandedCenter = artExpandedTop + artExpandedSize / 2f
    return -sheetTop + currentCenter - expandedCenter
}

private fun miniContentAlpha(progress: Float): Float = (1f - progress / CONTENT_CROSSFADE_END).coerceIn(0f, 1f)

private fun expandedContentAlpha(progress: Float): Float =
    ((progress - CONTENT_CROSSFADE_START) / (1f - CONTENT_CROSSFADE_START)).coerceIn(0f, 1f)

@Composable
private fun SheetFrame(
    content: SheetContentState,
    sheetState: NowPlayingSheetState,
    metrics: SheetMetrics,
    onCollapse: () -> Unit,
    sheetDragEnabled: Boolean,
) {
    Box(
        modifier =
            Modifier
                .absolute(metrics.sheetLeft, metrics.sheetTop, metrics.sheetWidth, metrics.sheetHeight)
                .sheetDragInput(sheetState, enabled = sheetDragEnabled),
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
    val progressPlayback: PlaybackState,
    val actions: PlayerActions,
    val sleepTimer: SleepTimerControls,
    val progressScrubObserver: ProgressScrubObserver,
)

@Immutable
private data class ExpandedContentLayout(
    val statusTop: Dp,
    val topBarToArtwork: Dp,
    val artworkReserve: Dp,
)

private data class MiniPlayerColors(
    val container: Color,
    val content: Color,
)

private fun ColorScheme.harmonizedMiniColors(artwork: ColorScheme): MiniPlayerColors =
    MiniPlayerColors(
        container = harmonizeWithPrimary(artwork.primaryContainer),
        content = harmonizeWithPrimary(artwork.onPrimaryContainer),
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

    CompositionLocalProvider(LocalContentColor provides metrics.contentColor) {
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
                contentColor = metrics.contentColor,
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
                    }.alpha(alpha),
        ) {
            ExpandedContent(
                content = content,
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
                ).clip(artworkShape)
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
    contentColor: Color,
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
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!state.podcastTitle.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.podcastTitle.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(0.6f),
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
    content: SheetContentState,
    layout: ExpandedContentLayout,
    onCollapse: () -> Unit,
) {
    val bottomPlatformHeight = miniPlayerPlatformHeight()
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = layout.statusTop),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(EXPANDED_TOP_BAR_STATUS_GAP))
            ExpandedTopBar(
                playback = content.player.playback,
                actions = content.actions,
                sleepTimer = content.sleepTimer,
                onCollapse = onCollapse,
            )
            Spacer(Modifier.height(layout.topBarToArtwork))
            Spacer(Modifier.height(layout.artworkReserve + 16.dp))
            ExpandedControls(
                state = content.player,
            )
        }
        PlayerBottomStack(
            content = content,
            bottomPlatformHeight = bottomPlatformHeight,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ExpandedTopBar(
    playback: PlaybackState,
    actions: PlayerActions,
    sleepTimer: SleepTimerControls,
    onCollapse: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().height(EXPANDED_TOP_BAR_HEIGHT), verticalAlignment = Alignment.CenterVertically) {
        PlayerTopBarIconButton(onClick = onCollapse) {
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.player_back))
        }
        Text(
            text = playback.podcastTitle.orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        PlayerOverflowMenu(playback.speed, actions.onCycleSpeed, sleepTimer)
    }
}

@Composable
private fun PlayerTopBarIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        content = content,
    )
}

@Composable
private fun ExpandedControls(state: PlayerUiState) {
    PlayerTitle(state)
}

@Composable
private fun PlayerBottomStack(
    content: SheetContentState,
    bottomPlatformHeight: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        TrackBottomAnchoredProgress(
            content = content,
            trackBottomPadding =
                bottomPlatformHeight +
                    BOTTOM_ACTION_GROUP_HEIGHT +
                    TRANSPORT_TO_BOTTOM_ACTION_GAP +
                    TRANSPORT_HEIGHT +
                    PROGRESS_TRACK_TO_TRANSPORT_GAP,
            modifier = Modifier.fillMaxSize(),
        )
        TransportRow(
            playback = content.player.playback,
            actions = content.actions,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        bottom =
                            bottomPlatformHeight +
                                BOTTOM_ACTION_GROUP_HEIGHT +
                                TRANSPORT_TO_BOTTOM_ACTION_GAP,
                    ),
        )
        PlayerBottomActionGroup(
            playback = content.player.playback,
            actions = content.actions,
            sleepTimer = content.sleepTimer,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomPlatformHeight),
        )
    }
}

@Composable
private fun TrackBottomAnchoredProgress(
    content: SheetContentState,
    trackBottomPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Layout(
        modifier = modifier,
        content = {
            PlayerProgressBar(
                state = content.progressPlayback,
                chapters = content.player.chapters,
                onSeek = content.actions.onSeek,
                observer = content.progressScrubObserver,
            )
        },
    ) { measurables, constraints ->
        val progress = measurables.single().measure(constraints.copy(minHeight = 0))
        val trackBottom = PLAYER_PROGRESS_TRACK_BOTTOM_OFFSET.roundToPx()
        val y = constraints.maxHeight - trackBottomPadding.roundToPx() - trackBottom
        layout(constraints.maxWidth, constraints.maxHeight) {
            progress.place(0, y)
        }
    }
}

@Composable
private fun PlayerTitle(state: PlayerUiState) {
    val chapterTitle = state.currentChapter?.title?.takeIf(String::isNotBlank)
    val chapterContent =
        chapterTitle?.let {
            ChapterTitleContent(
                chapterIndex = state.currentChapterIndex,
                title = it,
            )
        }
    val chapterPresence by
        animateFloatAsState(
            targetValue = if (chapterContent == null) 0f else 1f,
            animationSpec = tween(TITLE_ENTER_DURATION_MS),
            label = "Chapter title presence",
        )
    val episodeTitleColor = animateEpisodeTitleColor(hasChapter = chapterContent != null)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
    ) {
        AnimatedContent(
            targetState = chapterContent,
            modifier = Modifier.fillMaxWidth(),
            transitionSpec = {
                fadeIn(tween(TITLE_ENTER_DURATION_MS)).togetherWith(fadeOut(tween(TITLE_EXIT_DURATION_MS)))
            },
            label = "Chapter title",
        ) { content ->
            if (content != null) {
                Text(
                    text = content.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(4.dp * chapterPresence))
        Text(
            text = state.playback.title.orEmpty(),
            style =
                lerp(
                    MaterialTheme.typography.headlineSmall,
                    MaterialTheme.typography.titleMedium,
                    chapterPresence,
                ),
            color = episodeTitleColor,
            textAlign = TextAlign.Start,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun animateEpisodeTitleColor(hasChapter: Boolean): Color =
    animateColorAsState(
        targetValue = if (hasChapter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(TITLE_ENTER_DURATION_MS),
        label = "Episode title color",
    ).value

@Immutable
private data class ChapterTitleContent(
    val chapterIndex: Int?,
    val title: String,
)

private fun PlayerUiState.atPosition(positionMs: Long?): PlayerUiState {
    if (positionMs == null) return this
    val chapterIndex = chapters.indexOfLast { chapter -> chapter.startTimeMs <= positionMs }.takeIf { it >= 0 }
    return copy(
        playback = playback.copy(positionMs = positionMs),
        currentChapterIndex = chapterIndex,
    )
}

@Composable
private fun rememberProgressScrubPresentation(): ProgressScrubPresentation = remember { ProgressScrubPresentation() }

@Stable
private class ProgressScrubPresentation {
    var active by mutableStateOf(false)
        private set
    var positionMs by mutableStateOf<Long?>(null)
        private set

    val observer =
        ProgressScrubObserver(
            onInteractionChanged = { active = it },
            onPositionChanged = { positionMs = it },
        )
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
    val navigation: PlayerNavigationActions,
)

internal class PlayerNavigationActions(
    val onShowNotes: (String) -> Unit,
    val onPlaylist: () -> Unit,
)

internal class SleepTimerControls(
    val remainingMs: Long?,
    val presetsMinutes: List<Int>,
    val onStart: (Int) -> Unit,
    val onCancel: () -> Unit,
)

private fun PlayerViewModel.playerActions(
    onShowNotes: (String) -> Unit,
    onPlaylist: () -> Unit,
) = PlayerActions(
    onPlayPause = ::playPause,
    onSeek = ::seekTo,
    onSkipBack = ::skipBack,
    onSkipForward = ::skipForward,
    onCycleSpeed = ::cycleSpeed,
    navigation = PlayerNavigationActions(onShowNotes, onPlaylist),
)

private fun PlayerViewModel.sleepTimerControls(remainingMs: Long?) =
    SleepTimerControls(
        remainingMs = remainingMs,
        presetsMinutes = sleepTimerPresetsMinutes,
        onStart = ::startSleepTimer,
        onCancel = ::cancelSleepTimer,
    )

@Composable
private fun rememberSleepTimerControls(
    viewModel: PlayerViewModel,
    remainingMs: Long?,
): SleepTimerControls = remember(viewModel, remainingMs) { viewModel.sleepTimerControls(remainingMs) }

@Composable
private fun PlayerOverflowMenu(
    speed: Float,
    onCycleSpeed: () -> Unit,
    sleepTimer: SleepTimerControls,
) {
    val menuState = rememberTunedDropdownMenuState()
    TunedDropdownMenuBox(
        state = menuState,
        anchor = { anchorModifier, openMenu ->
            PlayerTopBarIconButton(onClick = openMenu, modifier = anchorModifier) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.home_more_options),
                )
            }
        },
    ) {
        Item(
            text = { Text(stringResource(R.string.player_speed, formatSpeed(speed))) },
            onClick = onCycleSpeed,
            leadingIcon = { Icon(Icons.Filled.Speed, contentDescription = null) },
        )
        Divider()
        SleepTimerItems(sleepTimer)
    }
}

@Composable
private fun BottomSleepTimerAction(
    controls: SleepTimerControls,
    item: TunedButtonGroupItem,
    style: TunedButtonGroupButtonStyle,
) {
    val menuState = rememberTunedDropdownMenuState()
    TunedDropdownMenuBox(
        state = menuState,
        modifier = item.modifier.fillMaxHeight(),
        anchor = { anchorModifier, openMenu ->
            TransportActionButton(
                onClick = openMenu,
                item = item,
                style = style,
                modifier = anchorModifier.fillMaxSize(),
            ) {
                Icon(
                    imageVector = if (controls.remainingMs != null) Icons.Filled.Bedtime else Icons.Outlined.Bedtime,
                    contentDescription = stringResource(R.string.player_sleep_timer),
                )
            }
        },
    ) {
        SleepTimerItems(controls)
    }
}

@Composable
private fun PlayerBottomActionGroup(
    playback: PlaybackState,
    actions: PlayerActions,
    sleepTimer: SleepTimerControls,
    modifier: Modifier = Modifier,
) {
    TunedButtonGroup(
        modifier = modifier.fillMaxWidth().height(BOTTOM_ACTION_GROUP_HEIGHT),
        expandedRatio = 0f,
        horizontalArrangement = Arrangement.spacedBy(BOTTOM_ACTION_GROUP_GAP),
    ) {
        item(weight = 1f) { item ->
            val style = rememberBottomActionStyle(connectedActionShape(0), active = playback.speed != 1f)
            TransportActionButton(actions.onCycleSpeed, item, style) {
                Text(
                    text = stringResource(R.string.player_speed, formatSpeed(playback.speed)),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                )
            }
        }
        item(weight = 1f) { item ->
            val style = rememberBottomActionStyle(connectedActionShape(1))
            TransportActionButton(
                onClick = { playback.episodeId?.let(actions.navigation.onShowNotes) },
                item = item,
                style = style,
            ) {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.player_show_notes),
                )
            }
        }
        item(weight = 1f) { item ->
            val style = rememberBottomActionStyle(connectedActionShape(2))
            TransportActionButton(actions.navigation.onPlaylist, item, style) {
                Icon(
                    painter = painterResource(R.drawable.ic_list_alt_24dp),
                    contentDescription = stringResource(R.string.player_playlist),
                )
            }
        }
        item(weight = 1f) { item ->
            val style = rememberBottomActionStyle(connectedActionShape(3), active = sleepTimer.remainingMs != null)
            BottomSleepTimerAction(sleepTimer, item, style)
        }
    }
}

@Composable
private fun rememberBottomActionStyle(
    shape: Shape,
    active: Boolean = false,
): TunedButtonGroupButtonStyle {
    val containerColor by
        animateColorAsState(
            if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceBright,
            label = "Bottom action container color",
        )
    val contentColor by
        animateColorAsState(
            if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            label = "Bottom action content color",
        )
    return TunedButtonGroupButtonStyle(
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        shape = if (active) CircleShape else shape,
        pressedShape = if (active) CircleShape else shape,
        contentPadding = PaddingValues(0.dp),
    )
}

private fun connectedActionShape(index: Int): Shape =
    RoundedCornerShape(
        topStart = if (index == 0) BOTTOM_ACTION_GROUP_OUTER_CORNER else BOTTOM_ACTION_GROUP_INNER_CORNER,
        bottomStart = if (index == 0) BOTTOM_ACTION_GROUP_OUTER_CORNER else BOTTOM_ACTION_GROUP_INNER_CORNER,
        topEnd =
            if (index == BOTTOM_ACTION_COUNT - 1) {
                BOTTOM_ACTION_GROUP_OUTER_CORNER
            } else {
                BOTTOM_ACTION_GROUP_INNER_CORNER
            },
        bottomEnd =
            if (index == BOTTOM_ACTION_COUNT - 1) {
                BOTTOM_ACTION_GROUP_OUTER_CORNER
            } else {
                BOTTOM_ACTION_GROUP_INNER_CORNER
            },
    )

@Composable
private fun TunedDropdownMenuScope.SleepTimerItems(controls: SleepTimerControls) {
    controls.presetsMinutes.forEach { minutes ->
        Item(
            text = { Text(stringResource(R.string.player_sleep_timer_minutes, minutes)) },
            onClick = { controls.onStart(minutes) },
            leadingIcon = { Icon(Icons.Outlined.Bedtime, contentDescription = null) },
        )
    }
    if (controls.remainingMs != null) {
        Divider()
        Item(
            text = { Text(stringResource(R.string.player_sleep_timer_cancel)) },
            onClick = controls.onCancel,
            leadingIcon = { Icon(Icons.Filled.Bedtime, contentDescription = null) },
        )
    }
}

@Composable
private fun TransportRow(
    playback: PlaybackState,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
) {
    val styles = rememberTransportButtonStyles()

    TunedButtonGroup(
        modifier = modifier.fillMaxWidth().height(TRANSPORT_HEIGHT),
        horizontalArrangement = Arrangement.spacedBy(TRANSPORT_GAP),
    ) {
        transportItems(playback, actions, styles)
    }
}

private fun TunedButtonGroupScope.transportItems(
    playback: PlaybackState,
    actions: PlayerActions,
    styles: TransportButtonStyles,
) {
    item(weight = 1f) { item ->
        TransportSkipButton(
            action =
                TransportSkipAction(
                    actions.onSkipBack,
                    PlaybackSkipDirection.Back,
                    PlaybackSkipSeconds.Fifteen,
                    R.string.player_skip_back,
                ),
            item = item,
            styles = styles,
        )
    }
    item(weight = 1.5f) { item ->
        TransportPlayPauseButton(playback, actions.onPlayPause, item, styles)
    }
    item(weight = 1f) { item ->
        TransportSkipButton(
            action =
                TransportSkipAction(
                    actions.onSkipForward,
                    PlaybackSkipDirection.Forward,
                    PlaybackSkipSeconds.Thirty,
                    R.string.player_skip_forward,
                ),
            item = item,
            styles = styles,
        )
    }
}

@Composable
private fun TransportSkipButton(
    action: TransportSkipAction,
    item: TunedButtonGroupItem,
    styles: TransportButtonStyles,
) {
    var animationKey by remember { mutableIntStateOf(0) }
    TransportActionButton(
        onClick = {
            animationKey++
            action.onClick()
        },
        item = item,
        style = styles.secondary,
    ) {
        AnimatedSkipIcon(
            direction = action.direction,
            seconds = action.seconds,
            animationKey = animationKey,
            contentDescription = stringResource(action.contentDescriptionRes),
        )
    }
}

@Composable
private fun TransportPlayPauseButton(
    playback: PlaybackState,
    onClick: () -> Unit,
    item: TunedButtonGroupItem,
    styles: TransportButtonStyles,
) {
    val playbackActive = playback.isPlaying || playback.buffering
    val containerColor by
        animateColorAsState(
            targetValue =
                if (playbackActive) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
            label = "Play button container color",
        )
    val contentColor by
        animateColorAsState(
            targetValue =
                if (playbackActive) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            label = "Play button content color",
        )

    TunedButtonGroupButton(
        onClick = onClick,
        item = item,
        style =
            styles.primary(
                active = playbackActive,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = containerColor,
                        contentColor = contentColor,
                    ),
            ),
        modifier = item.modifier.fillMaxHeight(),
    ) {
        if (playback.buffering) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = contentColor,
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription =
                    stringResource(if (playback.isPlaying) R.string.player_pause else R.string.player_play),
            )
        }
    }
}

@Composable
private fun rememberTransportButtonStyles(): TransportButtonStyles {
    val restingShape = RoundedCornerShape(percent = 50)
    val activeShape = RoundedCornerShape(TRANSPORT_ACTIVE_CORNER_RADIUS)
    return TransportButtonStyles(
        restingShape = restingShape,
        activeShape = activeShape,
        secondaryColors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
    )
}

@Composable
private fun TransportActionButton(
    onClick: () -> Unit,
    item: TunedButtonGroupItem,
    style: TunedButtonGroupButtonStyle,
    modifier: Modifier = item.modifier.fillMaxHeight(),
    content: @Composable RowScope.() -> Unit,
) {
    TunedButtonGroupButton(
        onClick = onClick,
        item = item,
        style = style,
        modifier = modifier,
        content = content,
    )
}

@Immutable
private data class TransportButtonStyles(
    val restingShape: Shape,
    val activeShape: Shape,
    val secondaryColors: ButtonColors,
) {
    val secondary: TunedButtonGroupButtonStyle
        get() = style(colors = secondaryColors, active = false)

    fun primary(
        active: Boolean,
        colors: ButtonColors,
    ): TunedButtonGroupButtonStyle = style(colors, active)

    private fun style(
        colors: ButtonColors,
        active: Boolean,
    ) = TunedButtonGroupButtonStyle(
        colors = colors,
        shape = if (active) activeShape else restingShape,
        pressedShape = activeShape,
        contentPadding = TRANSPORT_CONTENT_PADDING,
    )
}

@Immutable
private data class TransportSkipAction(
    val onClick: () -> Unit,
    val direction: PlaybackSkipDirection,
    val seconds: PlaybackSkipSeconds,
    @StringRes val contentDescriptionRes: Int,
)

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
private val BOTTOM_ACTION_GROUP_GAP = 2.dp
private val BOTTOM_ACTION_GROUP_HEIGHT = 48.dp
private val BOTTOM_ACTION_GROUP_INNER_CORNER = 8.dp
private val BOTTOM_ACTION_GROUP_OUTER_CORNER = 16.dp
private val EXPANDED_TOP_BAR_HEIGHT = 48.dp
private val EXPANDED_TOP_BAR_STATUS_GAP = 16.dp
private val FALLBACK_PAGE_CORNER = 28.dp
private val MINI_SHADOW_RADIUS = 24.dp
private val MINI_SHADOW_Y_OFFSET = 4.dp
private val TRANSPORT_ACTIVE_CORNER_RADIUS = 16.dp
private val TRANSPORT_CONTENT_PADDING = PaddingValues(8.dp)
private val TRANSPORT_GAP = 8.dp
private val TRANSPORT_HEIGHT = 80.dp
private val PROGRESS_TRACK_TO_TRANSPORT_GAP = 48.dp
private val TRANSPORT_TO_BOTTOM_ACTION_GAP = 32.dp
private const val TITLE_ENTER_DURATION_MS = 220
private const val TITLE_EXIT_DURATION_MS = 120
private const val MINI_SHADOW_ALPHA_LIGHT = 0.03f
private const val MINI_SHADOW_ALPHA_DARK = 0.1f
private const val BOTTOM_ACTION_COUNT = 4
private const val SCRIM_MAX_ALPHA = 0.32f
private const val TAP_EXPAND_PROGRESS_LIMIT = 0.35f
private const val CONTENT_CROSSFADE_START = 0.35f
private const val CONTENT_CROSSFADE_END = 0.65f
