package ink.duo3.tuned.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ink.duo3.tuned.R
import ink.duo3.tuned.presentation.home.HomeUiState
import ink.duo3.tuned.presentation.home.HomeViewModel
import ink.duo3.tuned.ui.components.LocalMiniPlayerBottomClearance
import ink.duo3.tuned.ui.components.TunedPageContentInsets
import ink.duo3.tuned.ui.components.TunedPullToRefreshBox
import ink.duo3.tuned.ui.components.TunedRefreshLogo
import kotlinx.coroutines.delay

/**
 * The home tab: a vertical stack of section cards rather than a bottom-bar of tabs.
 * For now the only card is "Subscribed"; recently-updated and resume cards slot into
 * the same [LazyColumn] as they land. The screen collects exactly one [HomeUiState].
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSearch: () -> Unit,
    onOpenLibrary: () -> Unit,
    onPodcastClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onOpenSearch = onOpenSearch,
        onOpenLibrary = onOpenLibrary,
        onPodcastClick = onPodcastClick,
        modifier = modifier,
    )
}

@Composable
private fun HomeScreen(
    state: HomeUiState,
    onOpenSearch: () -> Unit,
    onOpenLibrary: () -> Unit,
    onPodcastClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(DEMO_REFRESH_DURATION_MILLIS)
            isRefreshing = false
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = TunedPageContentInsets,
    ) { padding ->
        TunedPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) { pullProgress ->
            Box(
                Modifier
                    .fillMaxSize(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            SearchEntry(
                                isRefreshing = isRefreshing,
                                isPlaying = state.isPlaying,
                                pullProgress = pullProgress,
                                onClick = onOpenSearch,
                            )
                        }
                        item {
                            SubscribedCard(
                                subscriptions = state.subscriptions,
                                onOpenLibrary = onOpenLibrary,
                                onPodcastClick = onPodcastClick,
                            )
                        }
                        item { Spacer(Modifier.height(LocalMiniPlayerBottomClearance.current)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchEntry(
    isRefreshing: Boolean,
    isPlaying: Boolean,
    pullProgress: Float,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.home_search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                TunedRefreshLogo(
                    isAnimating = isRefreshing,
                    isPlaying = isPlaying,
                    pullProgress = pullProgress,
                    modifier = Modifier.size(width = (400 / 3).dp, height = 32.dp),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.size(24.dp))
        }
    }
}

private const val DEMO_REFRESH_DURATION_MILLIS = 4_500L
