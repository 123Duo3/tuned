package ink.duo3.tuned.ui.episode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.model.Episode
import ink.duo3.tuned.domain.model.Podcast
import ink.duo3.tuned.presentation.episode.EpisodeDetailUiState
import ink.duo3.tuned.presentation.episode.EpisodeDetailViewModel
import ink.duo3.tuned.ui.components.EpisodePlayButton
import ink.duo3.tuned.ui.components.HtmlText
import ink.duo3.tuned.ui.components.LocalMiniPlayerBottomClearance
import ink.duo3.tuned.ui.components.Text
import ink.duo3.tuned.ui.components.TunedLargeTopBarScaffold
import ink.duo3.tuned.ui.components.rememberArtworkPalette
import ink.duo3.tuned.ui.components.rememberLargeTopBarScrollEnabled
import ink.duo3.tuned.ui.components.rememberRelativeTimestamp
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

/**
 * Episode detail: artwork (the episode's own, falling back to the podcast's), title,
 * publish date and duration, a play entry point for episodes with audio, and the HTML
 * show notes. A null episode after load means it is no longer stored.
 */
@Composable
fun EpisodeDetailScreen(
    viewModel: EpisodeDetailViewModel,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val bottomClearance = LocalMiniPlayerBottomClearance.current
    val topBarScrollEnabled =
        state.episode != null &&
            rememberLargeTopBarScrollEnabled(
                scrollState = scrollState,
                contentKey =
                    EpisodeDetailScrollContentKey(
                        episodeId = state.episode?.id,
                        description = state.episode?.description,
                        hasEpisodeArtwork = state.episode?.artworkUrl != null,
                        hasPodcastArtwork = state.podcast?.artworkUrl != null,
                        bottomClearance = bottomClearance,
                    ),
            )
    TunedLargeTopBarScaffold(
        title = state.podcast?.title.orEmpty(),
        onBack = onBack,
        backContentDescription = stringResource(R.string.episode_back),
        modifier = modifier,
        enableTopBarScroll = topBarScrollEnabled,
    ) { hazeModifier, contentPadding ->
        val episode = state.episode
        when {
            state.isLoading ->
                Box(hazeModifier.fillMaxSize().padding(contentPadding)) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }

            episode == null ->
                Box(hazeModifier.fillMaxSize().padding(contentPadding)) {
                    Text(
                        text = stringResource(R.string.episode_not_found),
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

            else ->
                EpisodeDetailContent(
                    episode = episode,
                    podcast = state.podcast,
                    state = state,
                    audioLevelBars = viewModel.audioLevelBars,
                    onPlay = onPlay,
                    onTimestampClick = viewModel::playAt,
                    scrollState = scrollState,
                    hazeModifier = hazeModifier,
                    contentPadding = contentPadding,
                )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun EpisodeDetailContent(
    episode: Episode,
    podcast: Podcast?,
    state: EpisodeDetailUiState,
    audioLevelBars: Flow<List<Float>>,
    onPlay: () -> Unit,
    onTimestampClick: (Long) -> Unit,
    scrollState: androidx.compose.foundation.ScrollState,
    hazeModifier: Modifier,
    contentPadding: PaddingValues,
) {
    Column(
        modifier =
            hazeModifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(contentPadding)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val artworkUrl = episode.artworkUrl ?: podcast?.artworkUrl
        if (artworkUrl != null) {
            EpisodeArtwork(artworkUrl = artworkUrl, contentDescription = episode.title)
        }
        EpisodeHeader(episode = episode, podcastTitle = podcast?.title)
        if (episode.enclosureUrl != null) {
            val palette = rememberArtworkPalette(artworkUrl)
            EpisodePlayButton(
                durationMs = episode.durationMs,
                playback = state.playback,
                palette = palette,
                onClick = onPlay,
                audioLevelBars = audioLevelBars,
            )
        }
        EpisodeNotes(html = episode.description, onTimestampClick = onTimestampClick)
        Spacer(Modifier.height(LocalMiniPlayerBottomClearance.current))
    }
}

private data class EpisodeDetailScrollContentKey(
    val episodeId: String?,
    val description: String?,
    val hasEpisodeArtwork: Boolean,
    val hasPodcastArtwork: Boolean,
    val bottomClearance: Dp,
)

@Composable
private fun EpisodeArtwork(
    artworkUrl: String,
    contentDescription: String?,
) {
    Surface(
        modifier = Modifier.size(120.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun EpisodeHeader(
    episode: Episode,
    podcastTitle: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = episode.title ?: stringResource(R.string.podcast_episode_untitled),
            style = MaterialTheme.typography.titleLarge,
        )
        if (!podcastTitle.isNullOrBlank()) {
            Text(
                text = podcastTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
private fun EpisodeNotes(
    html: String?,
    onTimestampClick: (Long) -> Unit,
) {
    if (!html.isNullOrBlank()) {
        HtmlText(
            html = html,
            modifier = Modifier.fillMaxWidth(),
            onTimestampClick = onTimestampClick,
        )
    } else {
        Text(
            text = stringResource(R.string.episode_notes_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun episodeMeta(episode: Episode): String? {
    val date =
        if (episode.publishedAtMs > 0) {
            rememberRelativeTimestamp(episode.publishedAtMs, showTime = true)
        } else {
            null
        }
    val minutes = episode.durationMs?.let { TimeUnit.MILLISECONDS.toMinutes(it) }?.takeIf { it > 0 }
    val duration = minutes?.let { stringResource(R.string.podcast_episode_duration, it) }
    return listOfNotNull(date, duration).joinToString(" · ").ifEmpty { null }
}
