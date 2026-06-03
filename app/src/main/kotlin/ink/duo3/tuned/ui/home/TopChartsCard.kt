package ink.duo3.tuned.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.model.PodcastSearchResult
import ink.duo3.tuned.ui.components.SectionCard

/**
 * Home's "Top Charts" discovery strip: a horizontal artwork row of the country's top podcasts.
 * Tapping one subscribes by its feed URL (the tapped tile shows a spinner while it resolves),
 * after which the home screen navigates to the new podcast. While the first fetch is in flight
 * the card shows a spinner; when there's nothing to show (loaded but empty) the card is omitted
 * entirely so a failed/blank fetch leaves no empty shell behind.
 */
@Composable
fun TopChartsCard(
    charts: List<PodcastSearchResult>,
    isLoading: Boolean,
    subscribingFeedUrl: String?,
    onSubscribe: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isLoading && charts.isEmpty()) return
    SectionCard(
        title = stringResource(R.string.home_top_charts),
        modifier = modifier,
    ) {
        if (charts.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .size(92.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyRow(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                items(charts, key = { it.feedUrl }) { entry ->
                    ChartArtwork(
                        entry = entry,
                        isSubscribing = entry.feedUrl == subscribingFeedUrl,
                        onClick = { onSubscribe(entry.feedUrl) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartArtwork(
    entry: PodcastSearchResult,
    isSubscribing: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .padding(end = 8.dp)
                .size(92.dp)
                .clickable(enabled = !isSubscribing, onClick = onClick),
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
                model = entry.artworkUrl,
                contentDescription = stringResource(R.string.home_top_charts_subscribe, entry.title),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().align(Alignment.Center),
            )
            if (isSubscribing) {
                Box(
                    modifier = Modifier.fillMaxSize().background(SCRIM),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

private val SCRIM = Color.Black.copy(alpha = 0.4f)
