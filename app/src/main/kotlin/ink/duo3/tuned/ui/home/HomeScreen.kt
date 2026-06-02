package ink.duo3.tuned.ui.home

import androidx.compose.animation.core.Easing
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import ink.duo3.tuned.R
import ink.duo3.tuned.presentation.home.HomeUiState
import ink.duo3.tuned.presentation.home.HomeViewModel
import ink.duo3.tuned.ui.components.LocalMiniPlayerBottomClearance
import ink.duo3.tuned.ui.components.TunedPageContentInsets
import ink.duo3.tuned.ui.components.TunedPullToRefreshBox
import ink.duo3.tuned.ui.components.TunedRefreshLogo
import ink.duo3.tuned.ui.components.TunedTopBackdrop
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

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
    HomeScreen(
        state = state,
        onOpenSearch = onOpenSearch,
        onOpenLibrary = onOpenLibrary,
        onOpenSettings = onOpenSettings,
        onPodcastClick = onPodcastClick,
        onEpisodeClick = onEpisodeClick,
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
    modifier: Modifier = Modifier,
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val topBarHazeState = remember { HazeState() }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(DEMO_REFRESH_DURATION_MILLIS.milliseconds)
            isRefreshing = false
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = TunedPageContentInsets,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        TunedPullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = true },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection),
                        bottom = padding.calculateBottomPadding(),
                    ),
        ) { pullProgress ->
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
                                .hazeSource(topBarHazeState),
                        contentPadding =
                            PaddingValues(
                                top = statusBarHeight + HOME_TOP_BAR_HEIGHT,
                                bottom = LocalMiniPlayerBottomClearance.current + 8.dp,
                            ),
                    ) {
                        item {
                            SubscribedCard(
                                subscriptions = state.subscriptions,
                                onOpenLibrary = onOpenLibrary,
                                onPodcastClick = onPodcastClick,
                            )
                        }
                        recentlyUpdatedSection(
                            episodes = state.recentEpisodes,
                            onEpisodeClick = onEpisodeClick,
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth(),
                    ) {
                        TopBarProgressiveBlur(
                            hazeState = topBarHazeState,
                            platformHeight = statusBarHeight,
                            gradientHeight = HOME_TOP_BAR_HEIGHT,
                        )
                        TunedTopBackdrop(
                            platformHeight = statusBarHeight,
                            gradientHeight = HOME_TOP_BAR_HEIGHT,
                        )
                        Column {
                            Spacer(Modifier.height(statusBarHeight))
                            HomeTopBar(
                                isRefreshing = isRefreshing,
                                isPlaying = state.isPlaying,
                                pullProgress = pullProgress,
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

@Composable
private fun TopBarProgressiveBlur(
    hazeState: HazeState,
    platformHeight: Dp,
    gradientHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainer
    val gradientStartY = with(density) { platformHeight.toPx() }
    val gradientEndY = with(density) { (platformHeight + gradientHeight).toPx() }

    Box(
        modifier
            .fillMaxWidth()
            .height(platformHeight + gradientHeight)
            .hazeEffect(hazeState) {
                this.backgroundColor = backgroundColor
                blurRadius = TOP_BAR_MAX_BLUR_RADIUS
                progressive =
                    HazeProgressive.verticalGradient(
                        easing = TOP_BAR_BLUR_EASING,
                        startY = gradientStartY,
                        startIntensity = 1f,
                        endY = gradientEndY,
                        endIntensity = 0f,
                    )
            },
    )
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
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

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

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            TunedRefreshLogo(
                isAnimating = isRefreshing,
                isPlaying = isPlaying,
                pullProgress = pullProgress,
                modifier = Modifier.size(width = (400 / 3).dp, height = 32.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Box {
            FilledTonalIconButton(modifier = Modifier.size(48.dp), onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.home_more_options),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.home_settings)) },
                    onClick = {
                        menuExpanded = false
                        onOpenSettings()
                    },
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
}

private const val DEMO_REFRESH_DURATION_MILLIS = 4_500L
private val HOME_TOP_BAR_HEIGHT = 72.dp
private val TOP_BAR_MAX_BLUR_RADIUS = 20.dp
private val TOP_BAR_BLUR_EASING = Easing { fraction -> fraction * fraction * (3f - 2f * fraction) }
