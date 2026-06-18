package ink.duo3.tuned.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.model.PodcastSearchResult
import ink.duo3.tuned.ui.components.artwork.ArtworkImage
import ink.duo3.tuned.ui.components.shape.tunedRoundedCornerShape

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
    Column(modifier.fillMaxWidth()) {
        HomeSectionHeader(title = stringResource(R.string.home_top_charts))
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = CARD_MARGIN, end = CARD_MARGIN),
            shape = tunedRoundedCornerShape(CARD_CORNER),
            color = MaterialTheme.colorScheme.surfaceBright,
        ) {
            if (charts.isEmpty()) {
                Box(
                    modifier =
                        Modifier
                            .padding(CARD_PADDING)
                            .size(ARTWORK_SIZE),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyRow(
                    contentPadding = PaddingValues(CARD_PADDING),
                    horizontalArrangement = Arrangement.spacedBy(ARTWORK_SPACING),
                ) {
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
}

@Composable
private fun ChartArtwork(
    entry: PodcastSearchResult,
    isSubscribing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val artworkShape = tunedRoundedCornerShape(ARTWORK_CORNER)
    Box(
        modifier =
            modifier
                .size(ARTWORK_SIZE)
                .clip(artworkShape)
                .clickable(enabled = !isSubscribing, onClick = onClick),
    ) {
        ArtworkImage(
            model = entry.artworkUrl,
            contentDescription = stringResource(R.string.home_top_charts_subscribe, entry.title),
            shape = artworkShape,
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

private val CARD_MARGIN = 16.dp
private val CARD_PADDING = 8.dp
private val ARTWORK_SIZE = 96.dp
private val CARD_CORNER = 24.dp
private val ARTWORK_SPACING = 8.dp
private val ARTWORK_CORNER = 16.dp
private val SCRIM = Color.Black.copy(alpha = 0.4f)
