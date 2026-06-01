package ink.duo3.tuned.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The home tab: a vertical stack of section cards rather than a bottom-bar of tabs.
 * For now the only card is "Subscribed"; recently-updated and resume cards slot into
 * the same [LazyColumn] as they land. The screen collects exactly one [HomeUiState].
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenLibrary: () -> Unit,
    onPodcastClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onOpenLibrary = onOpenLibrary,
        onPodcastClick = onPodcastClick,
        modifier = modifier,
    )
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onOpenLibrary: () -> Unit,
    onPodcastClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    item {
                        SubscribedCard(
                            subscriptions = state.subscriptions,
                            onOpenLibrary = onOpenLibrary,
                            onPodcastClick = onPodcastClick,
                        )
                    }
                }
            }
        }
    }
}
