package ink.duo3.tuned.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.model.RecentEpisode
import ink.duo3.tuned.domain.player.EpisodePlaybackSnapshot
import ink.duo3.tuned.ui.components.artwork.ArtworkImage
import ink.duo3.tuned.ui.components.artwork.rememberArtworkPalette
import ink.duo3.tuned.ui.components.html.htmlToPlainText
import ink.duo3.tuned.ui.components.playback.EpisodePlayButton
import ink.duo3.tuned.ui.components.shape.tunedRoundedCornerShape
import ink.duo3.tuned.ui.components.text.Text
import ink.duo3.tuned.ui.components.text.rememberRelativeTimestamp
import kotlinx.coroutines.flow.Flow
import androidx.compose.material3.Text as ComposeText

/**
 * Adds Home's "Recently Updated" section to its parent lazy list. Each episode is emitted as its
 * own lazy item so Home can keep long recent lists cheap to compose.
 */
fun LazyListScope.recentlyUpdatedSection(
    episodes: List<RecentEpisode>,
    episodePlayback: Map<String, EpisodePlaybackSnapshot>,
    audioLevelBars: Flow<List<Float>>,
    onPlay: (RecentEpisode) -> Unit,
    onEpisodeClick: (String) -> Unit,
) {
    if (episodes.isEmpty()) return
    item(key = RECENTLY_UPDATED_HEADER_KEY) {
        HomeSectionHeader(title = stringResource(R.string.home_recently_updated))
    }
    itemsIndexed(
        items = episodes,
        key = { _, episode -> episode.id },
    ) { index, episode ->
        RecentEpisodeCard(
            episode = episode,
            playback = episodePlayback[episode.id] ?: EpisodePlaybackSnapshot(),
            audioLevelBars = audioLevelBars,
            isFirst = index == 0,
            isLast = index == episodes.lastIndex,
            onPlay = { onPlay(episode) },
            onClick = { onEpisodeClick(episode.id) },
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun RecentEpisodeCard(
    episode: RecentEpisode,
    playback: EpisodePlaybackSnapshot,
    audioLevelBars: Flow<List<Float>>,
    isFirst: Boolean,
    isLast: Boolean,
    onPlay: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = CARD_MARGIN)
                .padding(bottom = if (isLast) 8.dp else CARD_SEAM),
        shape = recentEpisodeCardShape(isFirst = isFirst, isLast = isLast),
        color = MaterialTheme.colorScheme.surfaceBright,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
        ) {
            EpisodeWrappedHeading(episode = episode)
            EpisodeNotes(
                episode = episode,
                modifier =
                    Modifier.padding(
                        start = TEXT_HORIZONTAL_PADDING,
                        top = NOTES_TOP_PADDING,
                        end = TEXT_HORIZONTAL_PADDING,
                    ),
            )
            EpisodeActions(
                episode = episode,
                playback = playback,
                audioLevelBars = audioLevelBars,
                onPlay = onPlay,
                modifier =
                    Modifier.padding(
                        start = EDGE_CONTROL_PADDING,
                        top = ACTIONS_TOP_PADDING,
                        end = EDGE_CONTROL_PADDING,
                        bottom = ACTIONS_BOTTOM_PADDING,
                    ),
            )
        }
    }
}

private fun recentEpisodeCardShape(
    isFirst: Boolean,
    isLast: Boolean,
) = tunedRoundedCornerShape(
    topStart = if (isFirst) CARD_CORNER else CARD_SEAM_CORNER,
    topEnd = if (isFirst) CARD_CORNER else CARD_SEAM_CORNER,
    bottomStart = if (isLast) CARD_CORNER else CARD_SEAM_CORNER,
    bottomEnd = if (isLast) CARD_CORNER else CARD_SEAM_CORNER,
)

@Composable
@Suppress("LongMethod")
private fun EpisodeWrappedHeading(episode: RecentEpisode) {
    val title = episode.title ?: stringResource(R.string.podcast_episode_untitled)
    val meta = episodeMeta(episode)
    val metaStyle = MaterialTheme.typography.labelMedium
    val titleStyle = MaterialTheme.typography.titleLarge
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val titleLineHeightPx =
        remember(titleStyle, textMeasurer) {
            val layout =
                textMeasurer.measure(
                    text = AnnotatedString("Hg\nHg\nHg"),
                    style = titleStyle,
                    maxLines = SIDE_TITLE_LINES,
                )
            layout.size.height
        }
    val artworkSizePx = titleLineHeightPx
    val artworkSize = with(density) { artworkSizePx.toDp() }

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = TEXT_HORIZONTAL_PADDING,
                    top = EDGE_CONTROL_PADDING,
                    end = EDGE_CONTROL_PADDING,
                ),
    ) {
        val maxWidthPx = with(density) { maxWidth.roundToPx() }
        val artworkGapPx = with(density) { ARTWORK_GAP.roundToPx() }
        val sideTitleWidthPx = (maxWidthPx - artworkSizePx - artworkGapPx).coerceAtLeast(1)
        val sideTitleMeasurement =
            remember(title, titleStyle, sideTitleWidthPx, textMeasurer) {
                val layout =
                    textMeasurer.measure(
                        text = AnnotatedString(title),
                        style = titleStyle,
                        maxLines = SIDE_TITLE_LINES,
                        overflow = TextOverflow.Clip,
                        constraints = Constraints(maxWidth = sideTitleWidthPx),
                    )
                SideTitleMeasurement(
                    end =
                        layout
                            .getLineEnd(lineIndex = layout.lineCount - 1, visibleEnd = true)
                            .coerceIn(0, title.length),
                    lineCount = layout.lineCount,
                )
            }
        val sideTitle = title.substring(0, sideTitleMeasurement.end).trimEnd()
        val remainingTitle = title.substring(sideTitleMeasurement.end).trimStart()
        val inlineMeta = remainingTitle.isBlank() && sideTitleMeasurement.lineCount < SIDE_TITLE_LINES

        Column {
            Row(horizontalArrangement = Arrangement.spacedBy(ARTWORK_GAP)) {
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(top = TEXT_TOP_OFFSET),
                ) {
                    Text(
                        text = sideTitle,
                        style = titleStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = SIDE_TITLE_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (inlineMeta) {
                        ComposeText(
                            text = meta,
                            style = metaStyle,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = TITLE_META_SPACING),
                        )
                    }
                }
                EpisodeArtwork(
                    artworkUrl = episode.artworkUrl ?: episode.podcastArtworkUrl,
                    contentDescription = episode.title,
                    modifier = Modifier.size(artworkSize),
                )
            }
            if (remainingTitle.isNotBlank()) {
                Text(
                    text = remainingTitle,
                    style = titleStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = REMAINING_TITLE_LINES,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.padding(
                            top = 2.dp,
                            end = TEXT_HORIZONTAL_PADDING - EDGE_CONTROL_PADDING,
                        ),
                )
            }
            if (!inlineMeta) {
                ComposeText(
                    text = meta,
                    style = metaStyle,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.padding(
                            top = TITLE_META_SPACING,
                            end = TEXT_HORIZONTAL_PADDING - EDGE_CONTROL_PADDING,
                        ),
                )
            }
        }
    }
}

private data class SideTitleMeasurement(
    val end: Int,
    val lineCount: Int,
)

@Composable
private fun episodeMeta(episode: RecentEpisode): AnnotatedString {
    val updated = rememberRelativeTimestamp(episode.publishedAtMs)
    val podcastTitle = episode.podcastTitle.orEmpty()
    return buildAnnotatedString {
        append(podcastTitle.ifBlank { stringResource(R.string.library_untitled) })
        append(" · ")
        append(updated)
    }
}

@Composable
private fun EpisodeNotes(
    episode: RecentEpisode,
    modifier: Modifier = Modifier,
) {
    val notes =
        remember(episode.description) {
            episode.description
                ?.let { htmlToPlainText(it).replace('\n', ' ') }
                .orEmpty()
        }
    if (notes.isNotBlank()) {
        Text(
            text = notes,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}

@Composable
private fun EpisodeArtwork(
    artworkUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    ArtworkImage(
        model = artworkUrl,
        contentDescription = contentDescription,
        modifier = modifier,
        shape = tunedRoundedCornerShape(ARTWORK_CORNER),
    )
}

@Composable
private fun EpisodeActions(
    episode: RecentEpisode,
    playback: EpisodePlaybackSnapshot,
    audioLevelBars: Flow<List<Float>>,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = rememberArtworkPalette(episode.artworkUrl ?: episode.podcastArtworkUrl)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EpisodePlayButton(
            durationMs = episode.durationMs,
            playback = playback,
            palette = palette,
            onClick = onPlay,
            enabled = episode.enclosureUrl != null,
            audioLevelBars = audioLevelBars,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = {}) {
            Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null)
        }
        IconButton(onClick = {}) {
            Icon(
                painter = painterResource(R.drawable.ic_download_24dp),
                contentDescription = stringResource(R.string.home_subscription_download),
            )
        }
        IconButton(onClick = {}) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.home_more_options),
            )
        }
    }
}

private const val RECENTLY_UPDATED_HEADER_KEY = "recently-updated-header"
private val CARD_MARGIN = 16.dp
private val CARD_SEAM = 2.dp
private val CARD_CORNER = 24.dp
private val CARD_SEAM_CORNER = 4.dp
private val EDGE_CONTROL_PADDING = 8.dp
private val TEXT_TOP_PADDING = 12.dp
private val TEXT_TOP_OFFSET = TEXT_TOP_PADDING - EDGE_CONTROL_PADDING
private val TEXT_HORIZONTAL_PADDING = 16.dp
private val TITLE_META_SPACING = 2.dp
private val ARTWORK_GAP = 8.dp
private val ARTWORK_CORNER = 16.dp
private val NOTES_TOP_PADDING = 8.dp
private val ACTIONS_TOP_PADDING = 8.dp
private val ACTIONS_BOTTOM_PADDING = 4.dp
private const val SIDE_TITLE_LINES = 3
private const val REMAINING_TITLE_LINES = 2
