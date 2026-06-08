package ink.duo3.tuned.ui.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.model.RecentEpisode
import ink.duo3.tuned.ui.components.Text
import java.util.concurrent.TimeUnit

/**
 * Adds Home's "Recently Updated" section to its parent lazy list. Each episode stays a
 * separate lazy item so opening Home only composes rows that are on screen.
 */
fun LazyListScope.recentlyUpdatedSection(
    episodes: List<RecentEpisode>,
    onEpisodeClick: (String) -> Unit,
) {
    if (episodes.isEmpty()) return
    item(key = RECENTLY_UPDATED_HEADER_KEY) {
        RecentlyUpdatedHeader()
    }
    itemsIndexed(
        items = episodes,
        key = { _, episode -> episode.id },
    ) { index, episode ->
        RecentEpisodeRow(
            episode = episode,
            isLast = index == episodes.lastIndex,
            onClick = { onEpisodeClick(episode.id) },
        )
    }
}

@Composable
private fun RecentlyUpdatedHeader() {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Text(
            text = stringResource(R.string.home_recently_updated),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun RecentEpisodeRow(
    episode: RecentEpisode,
    isLast: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = if (isLast) 8.dp else 0.dp),
        shape =
            if (isLast) {
                RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            } else {
                RectangleShape
            },
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        RecentEpisodeRowContent(
            episode = episode,
            onClick = onClick,
        )
    }
}

@Composable
private fun RecentEpisodeRowContent(
    episode: RecentEpisode,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Box(
                Modifier.border(
                    width = 0.1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp),
                ),
            ) {
                AsyncImage(
                    model = episode.artworkUrl ?: episode.podcastArtworkUrl,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = episode.title ?: stringResource(R.string.podcast_episode_untitled),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = episode.podcastTitle.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        episode.durationMs?.let { ms ->
            val minutes = TimeUnit.MILLISECONDS.toMinutes(ms)
            if (minutes > 0) {
                Text(
                    text = stringResource(R.string.podcast_episode_duration, minutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val RECENTLY_UPDATED_HEADER_KEY = "recently-updated-header"
