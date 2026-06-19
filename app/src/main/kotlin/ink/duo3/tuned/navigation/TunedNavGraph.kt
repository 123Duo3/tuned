package ink.duo3.tuned.navigation

import android.net.Uri
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import ink.duo3.tuned.presentation.player.PlayerViewModel
import ink.duo3.tuned.ui.components.scaffold.LocalMiniPlayerBottomClearance
import ink.duo3.tuned.ui.components.scaffold.LocalMiniPlayerVisible
import ink.duo3.tuned.ui.components.scaffold.MINI_PLAYER_HEIGHT
import ink.duo3.tuned.ui.components.scaffold.MiniPlayerBottomBackdrop
import ink.duo3.tuned.ui.components.scaffold.miniPlayerPlatformHeight
import ink.duo3.tuned.ui.episode.EpisodeDetailScreen
import ink.duo3.tuned.ui.home.HomeScreen
import ink.duo3.tuned.ui.library.LibraryScreen
import ink.duo3.tuned.ui.player.NowPlayingSheet
import ink.duo3.tuned.ui.player.NowPlayingSheetState
import ink.duo3.tuned.ui.player.rememberNowPlayingSheetState
import ink.duo3.tuned.ui.podcast.PodcastDetailScreen
import ink.duo3.tuned.ui.search.SearchScreen
import ink.duo3.tuned.ui.settings.SettingsScreen
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Central NavDisplay plus the persistent now-playing sheet. The sheet is a single surface that
 * morphs between a collapsed mini bar and the full-screen player (see [NowPlayingSheet]); it is
 * shown whenever an episode is loaded and floats above whatever page is on the back stack —
 * there is no separate player route.
 */
@Composable
fun TunedNavGraph(
    externalOpmlUri: Uri? = null,
    onExternalOpmlUriConsumed: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(Route.Home)
    val controller = koinInject<PlaybackController>()
    val playbackState by controller.state.collectAsStateWithLifecycle()
    val playerViewModel = koinViewModel<PlayerViewModel>()
    val sheetState = rememberNowPlayingSheetState()
    val showSheet = playbackState.episodeId != null
    val onExpandPlayer = rememberNowPlayingExpansionRequester(showSheet, sheetState)
    val onShowNotes = rememberShowNotesRequester(backStack, sheetState)
    LaunchedEffect(externalOpmlUri) {
        if (externalOpmlUri != null && backStack.lastOrNull() != Route.Settings) {
            backStack.remove(Route.Settings)
            backStack.add(Route.Settings)
        }
    }
    val density = LocalDensity.current
    val activityTransitionOffset =
        with(density) { ActivityTransitionDistance.roundToPx() } *
            if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
    val navDisplayState =
        rememberAndroidPredictiveBackNavDisplayState(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider =
                tunedEntryProvider(
                    backStack = backStack,
                    onExpandPlayer = onExpandPlayer,
                    externalOpmlUri = externalOpmlUri,
                    onExternalOpmlUriConsumed = onExternalOpmlUriConsumed,
                ),
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
        )

    val platformHeight = miniPlayerPlatformHeight()
    CompositionLocalProvider(
        LocalMiniPlayerBottomClearance provides
            if (showSheet) MINI_PLAYER_HEIGHT + platformHeight else platformHeight,
        LocalMiniPlayerVisible provides showSheet,
    ) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
            TunedNavigationScene(
                navDisplayState = navDisplayState,
                activityTransitionOffset = activityTransitionOffset,
            )
            if (showSheet) {
                NowPlayingSheet(
                    viewModel = playerViewModel,
                    sheetState = sheetState,
                    onShowNotes = onShowNotes,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun rememberShowNotesRequester(
    backStack: NavBackStack<NavKey>,
    sheetState: NowPlayingSheetState,
): (String) -> Unit {
    val scope = rememberCoroutineScope()
    return { episodeId ->
        val destination = Route.EpisodeDetail(episodeId)
        if (backStack.lastOrNull() != destination) backStack.add(destination)
        scope.launch { sheetState.collapse() }
    }
}

@Composable
private fun rememberNowPlayingExpansionRequester(
    showSheet: Boolean,
    sheetState: NowPlayingSheetState,
): () -> Unit {
    var expandWhenSheetAppears by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(showSheet, expandWhenSheetAppears) {
        when {
            showSheet && expandWhenSheetAppears -> {
                expandWhenSheetAppears = false
                sheetState.expand()
            }
            !showSheet -> {
                expandWhenSheetAppears = false
                sheetState.snapToCollapsed()
            }
        }
    }
    return { expandWhenSheetAppears = true }
}

@Composable
private fun TunedNavigationScene(
    navDisplayState: AndroidPredictiveBackNavDisplayState<NavKey>,
    activityTransitionOffset: Int,
) {
    LaunchedEffect(navDisplayState.visualState.suppressNextPopTransition) {
        if (navDisplayState.visualState.suppressNextPopTransition) {
            withFrameNanos {}
            withFrameNanos {}
            navDisplayState.visualState.clearPopTransitionSuppression()
        }
    }
    if (navDisplayState.visualState.isActive()) {
        AndroidPredictiveBackPreview(
            state = navDisplayState.visualState,
            sceneState = navDisplayState.sceneState,
            modifier = Modifier.fillMaxSize(),
        )
    } else {
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
            animationSpec = tween(ACTIVITY_TRANSITION_DURATION_MILLIS, easing = TunedActivityEasing),
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
            animationSpec = tween(ACTIVITY_TRANSITION_DURATION_MILLIS, easing = TunedActivityEasing),
            targetOffsetX = { -horizontalOffset },
        )

private fun activityCloseTransition(horizontalOffset: Int): ContentTransform =
    slideInHorizontally(
        animationSpec = tween(ACTIVITY_TRANSITION_DURATION_MILLIS, easing = TunedActivityEasing),
        initialOffsetX = { -horizontalOffset },
    ) togetherWith
        (
            slideOutHorizontally(
                animationSpec = tween(ACTIVITY_TRANSITION_DURATION_MILLIS, easing = TunedActivityEasing),
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
private const val ACTIVITY_TRANSITION_DURATION_MILLIS = 450
private const val ACTIVITY_FADE_DURATION_MILLIS = 83
private const val ACTIVITY_OPEN_FADE_DELAY_MILLIS = 50
private const val ACTIVITY_CLOSE_FADE_DELAY_MILLIS = 35

internal val TunedActivityEasing =
    PathEasing(
        Path().apply {
            moveTo(0f, 0f)
            cubicTo(0.05f, 0f, 0.133333f, 0.06f, 0.166666f, 0.4f)
            cubicTo(0.208333f, 0.82f, 0.25f, 1f, 1f, 1f)
        },
    )

/** The destination graph. Cross-page navigation only ever mutates [backStack] — pages never call each other. */
@Suppress("LongMethod")
private fun tunedEntryProvider(
    backStack: NavBackStack<NavKey>,
    onExpandPlayer: () -> Unit,
    externalOpmlUri: Uri?,
    onExternalOpmlUriConsumed: () -> Unit,
) = entryProvider<NavKey> {
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
                externalOpmlUri = externalOpmlUri,
                onExternalOpmlUriConsumed = onExternalOpmlUriConsumed,
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
                    onExpandPlayer()
                },
            )
        }
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
                bottomClearanceHeight = miniPlayerPlatformHeight(),
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
