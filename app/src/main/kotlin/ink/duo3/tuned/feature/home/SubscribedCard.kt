package ink.duo3.tuned.feature.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ink.duo3.tuned.R
import ink.duo3.tuned.core.designsystem.SectionCard
import ink.duo3.tuned.domain.model.Podcast

/**
 * Home's "Subscribed" section: a horizontal artwork strip of the user's subscriptions,
 * tapping the trailing arrow opens the full library. An empty library shows a hint
 * instead of a blank strip. Artwork loads via Coil; tapping one opens that podcast.
 */
@Composable
fun SubscribedCard(
    subscriptions: List<Podcast>,
    onOpenLibrary: () -> Unit,
    onPodcastClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(R.string.home_subscribed),
        modifier = modifier,
        onMore = onOpenLibrary,
        moreContentDescription = stringResource(R.string.home_open_library),
    ) {
        if (subscriptions.isEmpty()) {
            Text(
                text = stringResource(R.string.library_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        } else {
            LazyRow(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                items(subscriptions, key = { it.id }) { podcast ->
                    PodcastArtwork(
                        podcast = podcast,
                        onClick = { onPodcastClick(podcast.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PodcastArtwork(
    podcast: Podcast,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .padding(end = 8.dp)
                .size(92.dp)
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
    ) {
        Box(
            Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(4.dp),
            ),
        ) {
            AsyncImage(
                model = podcast.artworkUrl,
                contentDescription = podcast.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().align(Alignment.Center),
            )
        }
    }
}
