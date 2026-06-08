package ink.duo3.tuned.ui.podcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.presentation.podcast.PodcastDetailUiState
import ink.duo3.tuned.presentation.podcast.PodcastDetailViewModel
import ink.duo3.tuned.ui.components.LocalMiniPlayerBottomClearance
import ink.duo3.tuned.ui.components.Text
import ink.duo3.tuned.ui.components.TunedLargeTopBarScaffold
import ink.duo3.tuned.ui.components.appErrorMessage
import ink.duo3.tuned.ui.components.htmlToPlainText
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit

/**
 * Podcast detail: the feed header (artwork, title, author, description) followed by its
 * episode list, newest first. A refresh action re-fetches the feed; failures surface as
 * a snackbar. A null podcast after load means it was unsubscribed elsewhere.
 */
@Composable
fun PodcastDetailScreen(
    viewModel: PodcastDetailViewModel,
    onBack: () -> Unit,
    onEpisodeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage = state.refreshError?.let { appErrorMessage(it) }
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.consumeError()
        }
    }
    TunedLargeTopBarScaffold(
        title = state.podcast?.title.orEmpty(),
        onBack = onBack,
        backContentDescription = stringResource(R.string.podcast_back),
        modifier = modifier,
        actions = {
            if (state.isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(end = 24.dp).size(24.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                FilledTonalIconButton(
                    onClick = viewModel::refresh,
                    modifier = Modifier.padding(end = 12.dp).size(48.dp),
                ) {
                    Icon(Icons.Filled.Refresh, stringResource(R.string.podcast_refresh))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { hazeModifier, contentPadding ->
        when {
            state.isLoading ->
                Box(hazeModifier.fillMaxSize().padding(contentPadding)) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

            state.podcast == null ->
                Box(hazeModifier.fillMaxSize().padding(contentPadding)) {
                    Text(
                        text = stringResource(R.string.podcast_not_found),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            else ->
                PodcastDetailList(
                    state = state,
                    onEpisodeClick = onEpisodeClick,
                    hazeModifier = hazeModifier,
                    contentPadding = contentPadding,
                )
        }
    }
}

@Composable
private fun PodcastDetailList(
    state: PodcastDetailUiState,
    onEpisodeClick: (String) -> Unit,
    hazeModifier: Modifier,
    contentPadding: PaddingValues,
) {
    val podcast = state.podcast ?: return
    LazyColumn(
        modifier = hazeModifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item { PodcastHeader(podcast) }
        if (state.episodes.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.podcast_episodes_empty),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.episodes, key = { it.id }) { episode ->
                EpisodeRow(episode = episode, onClick = { onEpisodeClick(episode.id) })
                HorizontalDivider()
            }
        }
        item { Spacer(Modifier.height(LocalMiniPlayerBottomClearance.current)) }
    }
}

@Composable
private fun PodcastHeader(podcast: Podcast) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                modifier = Modifier.size(96.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                AsyncImage(
                    model = podcast.artworkUrl,
                    contentDescription = podcast.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = podcast.title ?: stringResource(R.string.library_untitled),
                    style = MaterialTheme.typography.titleLarge,
                )
                val author = podcast.author
                if (!author.isNullOrBlank()) {
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        val description = podcast.description
        if (!description.isNullOrBlank()) {
            // A truncated teaser: plain text (tags stripped), not the full block renderer.
            Text(
                text = htmlToPlainText(description),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: Episode,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = episode.title ?: stringResource(R.string.podcast_episode_untitled),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val meta = episodeMeta(episode)
        if (meta != null) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun episodeMeta(episode: Episode): String? {
    val date =
        episode.publishedAtMs
            .takeIf { it > 0 }
            ?.let {
                Instant
                    .ofEpochMilli(it)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(DATE_FORMAT)
            }
    val minutes = episode.durationMs?.let { TimeUnit.MILLISECONDS.toMinutes(it) }?.takeIf { it > 0 }
    val duration = minutes?.let { stringResource(R.string.podcast_episode_duration, it) }
    return listOfNotNull(date, duration).joinToString(" · ").ifEmpty { null }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
