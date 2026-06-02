package ink.duo3.tuned.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import ink.duo3.tuned.ui.episode.EpisodeDetailScreen
import ink.duo3.tuned.ui.home.HomeScreen
import ink.duo3.tuned.ui.library.LibraryScreen
import ink.duo3.tuned.ui.podcast.PodcastDetailScreen
import ink.duo3.tuned.ui.search.SearchScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Central NavDisplay. Destinations currently render placeholders; each feature
 * screen replaces its placeholder when the feature lands (build order steps 2+).
 */
@Composable
fun TunedNavGraph(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(Route.Home)
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                entry<Route.Home> {
                    HomeScreen(
                        viewModel = koinViewModel(),
                        onOpenSearch = { backStack.add(Route.Search) },
                        onOpenLibrary = { backStack.add(Route.Library) },
                        onPodcastClick = { podcastId ->
                            backStack.add(Route.PodcastDetail(podcastId))
                        },
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
                    EpisodeDetailScreen(
                        viewModel = koinViewModel { parametersOf(key.episodeId) },
                        onBack = { backStack.removeLastOrNull() },
                        // Playback is wired to PlaybackController in build-order step 3.
                        onPlay = {},
                    )
                }
                entry<Route.Player> { Placeholder("Player") }
            },
    )
}

@Composable
private fun Placeholder(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name)
    }
}
