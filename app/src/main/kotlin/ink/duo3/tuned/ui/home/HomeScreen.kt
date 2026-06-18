package ink.duo3.tuned.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.model.RecentEpisode
import ink.duo3.tuned.domain.model.SubscriptionEpisode
import ink.duo3.tuned.presentation.home.HomeUiState
import ink.duo3.tuned.presentation.home.HomeViewModel
import ink.duo3.tuned.ui.components.dropdown.TunedDropdownMenuBox
import ink.duo3.tuned.ui.components.dropdown.rememberTunedDropdownMenuState
import ink.duo3.tuned.ui.components.scaffold.LocalMiniPlayerBottomClearance
import ink.duo3.tuned.ui.components.scaffold.TunedPageContentInsets
import ink.duo3.tuned.ui.components.scaffold.TunedTopBarBackdrop
import ink.duo3.tuned.ui.components.text.Text
import kotlinx.coroutines.flow.Flow

/**
 * The home tab: a vertical stack of section cards rather than a bottom-bar of tabs.
 * For now the only card is "Subscribed"; recently-updated and resume cards slot into
 * the same [LazyColumn] as they land. The screen collects exactly one [HomeUiState].
 */
@Composable
@Suppress("LongParameterList")
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenSearch: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onPodcastClick: (String) -> Unit,
    onEpisodeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // A tapped chart subscribes, then opens the new podcast — same destination as a tapped tile.
    LaunchedEffect(state.addedPodcastId) {
        state.addedPodcastId?.let { id ->
            onPodcastClick(id)
            viewModel.consumeAdded()
        }
    }
    HomeScreen(
        state = state,
        onOpenSearch = onOpenSearch,
        onOpenLibrary = onOpenLibrary,
        onOpenSettings = onOpenSettings,
        onPodcastClick = onPodcastClick,
        onEpisodeClick = onEpisodeClick,
        onPlaySubscriptionEpisode = viewModel::play,
        onPlayRecentEpisode = viewModel::play,
        audioLevelBars = viewModel.audioLevelBars,
        onMarkPlayed = viewModel::markPlayed,
        onSubscribeChart = viewModel::subscribe,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
@Suppress("LongParameterList", "LongMethod")
private fun HomeScreen(
    state: HomeUiState,
    onOpenSearch: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenSettings: () -> Unit,
    onPodcastClick: (String) -> Unit,
    onEpisodeClick: (String) -> Unit,
    onPlaySubscriptionEpisode: (SubscriptionEpisode) -> Unit,
    onPlayRecentEpisode: (RecentEpisode) -> Unit,
    audioLevelBars: Flow<List<Float>>,
    onMarkPlayed: (String) -> Unit,
    onSubscribeChart: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val topBarHazeState = remember { HazeState() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = TunedPageContentInsets,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        TunedPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection),
                        bottom = padding.calculateBottomPadding(),
                    ),
        ) { pullProgress, contentPullOffsetPx, releasePulseKey ->
            Box(
                Modifier
                    .fillMaxSize(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    val statusBarHeight = padding.calculateTopPadding()
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = contentPullOffsetPx
                                }.hazeSource(topBarHazeState),
                        contentPadding =
                            PaddingValues(
                                top = statusBarHeight + HOME_TOP_BAR_HEIGHT,
                                bottom = LocalMiniPlayerBottomClearance.current + 8.dp,
                            ),
                    ) {
                        item {
                            SubscribedCard(
                                subscriptions = state.subscriptionEpisodes,
                                episodePlayback = state.episodePlayback,
                                audioLevelBars = audioLevelBars,
                                onOpenLibrary = onOpenLibrary,
                                onPlay = onPlaySubscriptionEpisode,
                                onMarkPlayed = onMarkPlayed,
                                onEpisodeClick = onEpisodeClick,
                                onPodcastClick = onPodcastClick,
                            )
                        }
                        item {
                            TopChartsCard(
                                charts = state.topCharts,
                                isLoading = state.chartsLoading,
                                subscribingFeedUrl = state.subscribingFeedUrl,
                                onSubscribe = onSubscribeChart,
                            )
                        }
                        recentlyUpdatedSection(
                            episodes = state.recentEpisodes,
                            episodePlayback = state.episodePlayback,
                            audioLevelBars = audioLevelBars,
                            onPlay = onPlayRecentEpisode,
                            onEpisodeClick = onEpisodeClick,
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth(),
                    ) {
                        TunedTopBarBackdrop(
                            hazeState = topBarHazeState,
                            totalHeight = statusBarHeight + HOME_TOP_BAR_HEIGHT,
                        )
                        Column {
                            Spacer(Modifier.height(statusBarHeight))
                            HomeTopBar(
                                isRefreshing = state.isRefreshing,
                                isPlaying = state.isPlaying,
                                pullProgress = pullProgress,
                                releasePulseKey = releasePulseKey,
                                onOpenSearch = onOpenSearch,
                                onOpenSettings = onOpenSettings,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top bar for the home screen: search button on the left, the animated wordmark logo
 * centered, and a three-dot overflow menu on the right with the Settings entry.
 */
@Composable
@Suppress("LongParameterList")
private fun HomeTopBar(
    isRefreshing: Boolean,
    isPlaying: Boolean,
    pullProgress: Float,
    releasePulseKey: Int,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(modifier = Modifier.size(48.dp), onClick = onOpenSearch) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.home_search),
            )
        }

        HomeRefreshLogo(
            isRefreshing = isRefreshing,
            isPlaying = isPlaying,
            pullProgress = pullProgress,
            releasePulseKey = releasePulseKey,
            modifier = Modifier.weight(1f),
        )

        val menuState = rememberTunedDropdownMenuState()
        TunedDropdownMenuBox(
            state = menuState,
            anchor = { anchorModifier, openMenu ->
                FilledTonalIconButton(
                    modifier = anchorModifier.size(48.dp),
                    onClick = openMenu,
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.home_more_options),
                    )
                }
            },
        ) {
            Item(
                text = { Text(stringResource(R.string.home_settings)) },
                onClick = onOpenSettings,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@Composable
private fun HomeRefreshLogo(
    isRefreshing: Boolean,
    isPlaying: Boolean,
    pullProgress: Float,
    releasePulseKey: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        TunedRefreshLogo(
            motion =
                TunedRefreshLogoMotion(
                    isAnimating = isRefreshing,
                    isPlaying = isPlaying,
                    pullProgress = pullProgress,
                    releasePulseKey = releasePulseKey,
                ),
            modifier = Modifier.size(width = (400 / 3).dp, height = 32.dp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private val HOME_TOP_BAR_HEIGHT = 72.dp
