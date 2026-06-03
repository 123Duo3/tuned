package ink.duo3.tuned.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.PathEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay
import ink.duo3.tuned.domain.player.PlaybackController
import ink.duo3.tuned.presentation.episode.EpisodeDetailViewModel
import ink.duo3.tuned.ui.components.LocalMiniPlayerBottomClearance
import ink.duo3.tuned.ui.components.LocalMiniPlayerVisible
import ink.duo3.tuned.ui.components.MINI_PLAYER_HEIGHT
import ink.duo3.tuned.ui.components.MiniPlayer
import ink.duo3.tuned.ui.components.MiniPlayerBottomBackdrop
import ink.duo3.tuned.ui.components.miniPlayerPlatformHeight
import ink.duo3.tuned.ui.episode.EpisodeDetailScreen
import ink.duo3.tuned.ui.home.HomeScreen
import ink.duo3.tuned.ui.library.LibraryScreen
import ink.duo3.tuned.ui.player.PlayerScreen
import ink.duo3.tuned.ui.podcast.PodcastDetailScreen
import ink.duo3.tuned.ui.search.SearchScreen
import ink.duo3.tuned.ui.settings.SettingsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Central NavDisplay plus the persistent floating mini-player. The mini-player reads the shared
 * [PlaybackController] state and overlays content whenever something is loaded and the full player
 * isn't already on top; tapping it opens [Route.Player].
 */
@Composable
fun TunedNavGraph(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Route.Home)
    val controller = koinInject<PlaybackController>()
    val playbackState by controller.state.collectAsStateWithLifecycle()
    val activityTransitionOffset =
        with(LocalDensity.current) { ActivityTransitionDistance.roundToPx() } *
            if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
    val navDisplayState =
        rememberAndroidPredictiveBackNavDisplayState(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = tunedEntryProvider(backStack),
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
        )
    val showMiniPlayer = playbackState.episodeId != null && backStack.lastOrNull() != Route.Player
    val miniPlayerContent: @Composable () -> Unit = {
        if (showMiniPlayer) {
            MiniPlayer(
                state = playbackState,
                onPlayPause = { if (playbackState.isPlaying) controller.pause() else controller.resume() },
                onClick = { backStack.add(Route.Player) },
            )
        }
    }
    TunedNavContent(modifier, navDisplayState, activityTransitionOffset, showMiniPlayer, miniPlayerContent)
}

@Composable
private fun TunedNavContent(
    modifier: Modifier,
    navDisplayState: AndroidPredictiveBackNavDisplayState<NavKey>,
    activityTransitionOffset: Int,
    showMiniPlayer: Boolean,
    miniPlayerContent: @Composable () -> Unit,
) {
    LaunchedEffect(navDisplayState.visualState.suppressNextPopTransition) {
        if (navDisplayState.visualState.suppressNextPopTransition) {
            withFrameNanos {}
            withFrameNanos {}
            navDisplayState.visualState.clearPopTransitionSuppression()
        }
    }
    val miniPlayerPlatformHeight = miniPlayerPlatformHeight()
    CompositionLocalProvider(
        LocalMiniPlayerBottomClearance provides
            if (showMiniPlayer) MINI_PLAYER_HEIGHT + miniPlayerPlatformHeight else miniPlayerPlatformHeight,
        LocalMiniPlayerVisible provides showMiniPlayer,
    ) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
            if (navDisplayState.visualState.isActive()) {
                AndroidPredictiveBackPreview(
                    state = navDisplayState.visualState,
                    sceneState = navDisplayState.sceneState,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                // This NavDisplay overload is predictive-back aware: it reads navigationEventState
                // and runs predictivePopTransitionSpec on any InProgress while it is the renderer
                // (sub-threshold flicks, and the one-frame handoff before the preview swaps in).
                // Pin it to our own close transition so Nav3's default never leaks through.
                val suppressPop = navDisplayState.visualState.suppressNextPopTransition
                NavDisplay(
                    sceneState = navDisplayState.sceneState,
                    navigationEventState = navDisplayState.navigationEventState,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = { activityOpenTransition(activityTransitionOffset) },
                    popTransitionSpec = { tunedPopTransition(suppressPop, activityTransitionOffset) },
                    predictivePopTransitionSpec = { tunedPopTransition(suppressPop, activityTransitionOffset) },
                )
            }
            if (showMiniPlayer) {
                // The backdrop now lives inside each page (see MiniPlayerBackdropScaffold) so it
                // travels with the page during transitions; only the floating mini-player itself
                // stays pinned above the navigation-bar platform here.
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(
                            start = MINI_PLAYER_HORIZONTAL_PADDING,
                            end = MINI_PLAYER_HORIZONTAL_PADDING,
                            bottom = miniPlayerPlatformHeight,
                        ),
                ) {
                    miniPlayerContent()
                }
            }
        }
    }
}

private fun AnimatedContentTransitionScope<Scene<NavKey>>.tunedPopTransition(
    suppress: Boolean,
    horizontalOffset: Int,
): ContentTransform =
    if (suppress) {
        // The predictive preview already animated the swap; let NavDisplay snap in silently.
        EnterTransition.None togetherWith ExitTransition.None
    } else {
        activityCloseTransition(horizontalOffset)
    }

private fun activityOpenTransition(horizontalOffset: Int): ContentTransform =
    (
        slideInHorizontally(
            animationSpec = tween(ACTIVITY_TRANSITION_DURATION_MILLIS, easing = AndroidActivityEasing),
            initialOffsetX = { horizontalOffset },
        ) +
            fadeIn(
                animationSpec =
                    tween(
                        durationMillis = ACTIVITY_FADE_DURATION_MILLIS,
                        delayMillis = ACTIVITY_OPEN_FADE_DELAY_MILLIS,
                        easing = LinearEasing,
                    ),
            )
    ) togetherWith
        slideOutHorizontally(
            animationSpec = tween(ACTIVITY_TRANSITION_DURATION_MILLIS, easing = AndroidActivityEasing),
            targetOffsetX = { -horizontalOffset },
        )

private fun activityCloseTransition(horizontalOffset: Int): ContentTransform =
    slideInHorizontally(
        animationSpec = tween(ACTIVITY_TRANSITION_DURATION_MILLIS, easing = AndroidActivityEasing),
        initialOffsetX = { -horizontalOffset },
    ) togetherWith
        (
            slideOutHorizontally(
                animationSpec = tween(ACTIVITY_TRANSITION_DURATION_MILLIS, easing = AndroidActivityEasing),
                targetOffsetX = { horizontalOffset },
            ) +
                fadeOut(
                    animationSpec =
                        tween(
                            durationMillis = ACTIVITY_FADE_DURATION_MILLIS,
                            delayMillis = ACTIVITY_CLOSE_FADE_DELAY_MILLIS,
                            easing = LinearEasing,
                        ),
                )
        )

private val ActivityTransitionDistance = 96.dp
private val MINI_PLAYER_HORIZONTAL_PADDING = 16.dp
private const val ACTIVITY_TRANSITION_DURATION_MILLIS = 450
private const val ACTIVITY_FADE_DURATION_MILLIS = 83
private const val ACTIVITY_OPEN_FADE_DELAY_MILLIS = 50
private const val ACTIVITY_CLOSE_FADE_DELAY_MILLIS = 35

private val AndroidActivityEasing =
    PathEasing(
        Path().apply {
            moveTo(0f, 0f)
            cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
            cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
        },
    )

/** The destination graph. Cross-page navigation only ever mutates [backStack] — pages never call each other. */
@Suppress("LongMethod")
private fun tunedEntryProvider(backStack: NavBackStack<NavKey>) =
    entryProvider<NavKey> {
        entry<Route.Home> {
            MiniPlayerBackdropScaffold {
                HomeScreen(
                    viewModel = koinViewModel(),
                    onOpenSearch = { backStack.add(Route.Search) },
                    onOpenLibrary = { backStack.add(Route.Library) },
                    onOpenSettings = { backStack.add(Route.Settings) },
                    onPodcastClick = { podcastId -> backStack.add(Route.PodcastDetail(podcastId)) },
                    onEpisodeClick = { episodeId -> backStack.add(Route.EpisodeDetail(episodeId)) },
                )
            }
        }

        entry<Route.Search> {
            MiniPlayerBackdropScaffold {
                SearchScreen(
                    viewModel = koinViewModel(),
                    onPodcastAdded = { podcastId -> backStack.add(Route.PodcastDetail(podcastId)) },
                )
            }
        }
        entry<Route.Library> {
            MiniPlayerBackdropScaffold {
                LibraryScreen(
                    viewModel = koinViewModel(),
                    onPodcastClick = { podcastId -> backStack.add(Route.PodcastDetail(podcastId)) },
                )
            }
        }
        entry<Route.Settings> {
            MiniPlayerBackdropScaffold {
                SettingsScreen(
                    viewModel = koinViewModel(),
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        }
        entry<Route.PodcastDetail> { key ->
            MiniPlayerBackdropScaffold {
                PodcastDetailScreen(
                    viewModel = koinViewModel { parametersOf(key.podcastId) },
                    onBack = { backStack.removeLastOrNull() },
                    onEpisodeClick = { episodeId -> backStack.add(Route.EpisodeDetail(episodeId)) },
                )
            }
        }
        entry<Route.EpisodeDetail> { key ->
            MiniPlayerBackdropScaffold {
                val viewModel = koinViewModel<EpisodeDetailViewModel> { parametersOf(key.episodeId) }
                EpisodeDetailScreen(
                    viewModel = viewModel,
                    onBack = { backStack.removeLastOrNull() },
                    onPlay = {
                        viewModel.play()
                        backStack.add(Route.Player)
                    },
                )
            }
        }
        entry<Route.Player> {
            PlayerScreen(
                viewModel = koinViewModel(),
                onBack = { backStack.removeLastOrNull() },
            )
        }
    }

/**
 * Wraps a page so the mini-player's bottom backdrop is drawn as part of the page content. Because
 * it lives inside the transitioning entry, the backdrop slides/scales together with the page during
 * navigation instead of staying pinned to the window like a global overlay.
 */
@Composable
private fun MiniPlayerBackdropScaffold(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        content()
        if (LocalMiniPlayerVisible.current) {
            MiniPlayerBottomBackdrop(
                platformHeight = miniPlayerPlatformHeight(),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
