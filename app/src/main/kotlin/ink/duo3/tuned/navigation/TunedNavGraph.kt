package ink.duo3.tuned.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import ink.duo3.tuned.domain.player.PlaybackController
import ink.duo3.tuned.presentation.episode.EpisodeDetailViewModel
import ink.duo3.tuned.ui.components.MiniPlayer
import ink.duo3.tuned.ui.episode.EpisodeDetailScreen
import ink.duo3.tuned.ui.home.HomeScreen
import ink.duo3.tuned.ui.library.LibraryScreen
import ink.duo3.tuned.ui.player.PlayerScreen
import ink.duo3.tuned.ui.podcast.PodcastDetailScreen
import ink.duo3.tuned.ui.search.SearchScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

/**
 * Central NavDisplay plus the persistent mini-player. The mini-player reads the shared
 * [PlaybackController] state and is shown below content whenever something is loaded and
 * the full player isn't already on top; tapping it opens [Route.Player].
 */
@Composable
fun TunedNavGraph(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Route.Home)
    val controller = koinInject<PlaybackController>()
    val playbackState by controller.state.collectAsStateWithLifecycle()
    Column(modifier) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.fillMaxSize().weight(1f),
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
            entryProvider = tunedEntryProvider(backStack),
        )
        if (playbackState.episodeId != null && backStack.lastOrNull() != Route.Player) {
            MiniPlayer(
                state = playbackState,
                onPlayPause = { if (playbackState.isPlaying) controller.pause() else controller.resume() },
                onClick = { backStack.add(Route.Player) },
            )
        }
    }
}

/** The destination graph. Cross-page navigation only ever mutates [backStack] — pages never call each other. */
private fun tunedEntryProvider(backStack: NavBackStack<NavKey>) =
    entryProvider<NavKey> {
        entry<Route.Home> {
            HomeScreen(
                viewModel = koinViewModel(),
                onOpenSearch = { backStack.add(Route.Search) },
                onOpenLibrary = { backStack.add(Route.Library) },
                onPodcastClick = { podcastId -> backStack.add(Route.PodcastDetail(podcastId)) },
            )
        }
        entry<Route.Search> {
            SearchScreen(
                viewModel = koinViewModel(),
                onPodcastAdded = { podcastId -> backStack.add(Route.PodcastDetail(podcastId)) },
            )
        }
        entry<Route.Library> {
            LibraryScreen(
                viewModel = koinViewModel(),
                onPodcastClick = { podcastId -> backStack.add(Route.PodcastDetail(podcastId)) },
            )
        }
        entry<Route.PodcastDetail> { key ->
            PodcastDetailScreen(
                viewModel = koinViewModel { parametersOf(key.podcastId) },
                onBack = { backStack.removeLastOrNull() },
                onEpisodeClick = { episodeId -> backStack.add(Route.EpisodeDetail(episodeId)) },
            )
        }
        entry<Route.EpisodeDetail> { key ->
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
        entry<Route.Player> {
            PlayerScreen(
                viewModel = koinViewModel(),
                onBack = { backStack.removeLastOrNull() },
            )
        }
    }
