package ink.duo3.tuned.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.feature.library.LibraryUiState
import ink.duo3.tuned.feature.library.LibraryViewModel
import ink.duo3.tuned.ui.designsystem.appErrorMessage

/**
 * Library tab: the user's subscriptions. Loading covers the first DB read; an empty
 * library shows a hint rather than a blank screen. Refresh failures surface as a
 * snackbar mapped from [AppError]. The screen collects exactly one [LibraryUiState].
 */
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onPodcastClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LibraryScreen(
        state = state,
        onPodcastClick = onPodcastClick,
        onRefresh = viewModel::refresh,
        onErrorShown = viewModel::consumeError,
        modifier = modifier,
    )
}

@Composable
private fun LibraryScreen(
    state: LibraryUiState,
    onPodcastClick: (String) -> Unit,
    onRefresh: (String) -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = if (state.refreshError != null) appErrorMessage(state.refreshError) else null
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onErrorShown()
        }
    }
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.podcasts.isEmpty() ->
                    Text(
                        text = stringResource(R.string.library_empty),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                else ->
                    LibraryList(
                        podcasts = state.podcasts,
                        refreshingIds = state.refreshingIds,
                        onPodcastClick = onPodcastClick,
                        onRefresh = onRefresh,
                    )
            }
        }
    }
}

@Composable
private fun LibraryList(
    podcasts: List<Podcast>,
    refreshingIds: Set<String>,
    onPodcastClick: (String) -> Unit,
    onRefresh: (String) -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(podcasts, key = { it.id }) { podcast ->
            PodcastRow(
                podcast = podcast,
                refreshing = podcast.id in refreshingIds,
                onClick = { onPodcastClick(podcast.id) },
                onRefresh = { onRefresh(podcast.id) },
            )
        }
    }
}

@Composable
private fun PodcastRow(
    podcast: Podcast,
    refreshing: Boolean,
    onClick: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = podcast.title ?: stringResource(R.string.library_untitled),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val author = podcast.author
            if (!author.isNullOrBlank()) {
                Text(
                    text = author,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (refreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
            )
        } else {
            TextButton(onClick = onRefresh) {
                Text(stringResource(R.string.library_refresh))
            }
        }
    }
}
