package ink.duo3.tuned.ui.home

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.model.SubscriptionEpisode
import ink.duo3.tuned.ui.components.ArtworkPalette
import ink.duo3.tuned.ui.components.Text
import ink.duo3.tuned.ui.components.TunedDropdownMenuBox
import ink.duo3.tuned.ui.components.htmlToPlainText
import ink.duo3.tuned.ui.components.rememberArtworkPalette
import ink.duo3.tuned.ui.components.rememberRelativeTimestamp
import ink.duo3.tuned.ui.components.rememberTunedDropdownMenuState
import java.util.concurrent.TimeUnit
import androidx.compose.material3.Text as ComposeText

/**
 * Home's "Subscribed" section: a header (title + library arrow) on the page background, then a
 * horizontal row of cards — one per subscription, ordered by its latest episode's date. Each card
 * is tinted by a colour extracted from that episode's cover and shows the cover, "podcast · updated",
 * a fixed three-line title/notes block, and play / download / more actions.
 */
@Composable
@Suppress("LongParameterList")
fun SubscribedCard(
    subscriptions: List<SubscriptionEpisode>,
    onOpenLibrary: () -> Unit,
    onPlay: (SubscriptionEpisode) -> Unit,
    onMarkPlayed: (String) -> Unit,
    onEpisodeClick: (String) -> Unit,
    onPodcastClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        HomeSectionHeader(title = stringResource(R.string.home_subscribed)) {
            IconButton(onClick = onOpenLibrary) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.home_open_library),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (subscriptions.isEmpty()) {
            Text(
                text = stringResource(R.string.library_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(subscriptions, key = { it.episodeId }) { episode ->
                    SubscriptionCard(
                        episode = episode,
                        onPlay = onPlay,
                        onMarkPlayed = onMarkPlayed,
                        onEpisodeClick = onEpisodeClick,
                        onPodcastClick = onPodcastClick,
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun SubscriptionCard(
    episode: SubscriptionEpisode,
    onPlay: (SubscriptionEpisode) -> Unit,
    onMarkPlayed: (String) -> Unit,
    onEpisodeClick: (String) -> Unit,
    onPodcastClick: (String) -> Unit,
) {
    val artwork = episode.artworkUrl ?: episode.podcastArtworkUrl
    val palette = rememberArtworkPalette(artwork)
    val container by animateColorAsState(palette.container, label = "subscriptionContainer")
    val onContainer by animateColorAsState(palette.onContainer, label = "subscriptionOnContainer")
    Surface(
        onClick = { onPodcastClick(episode.podcastId) },
        modifier = Modifier.width(CARD_WIDTH),
        shape = RoundedCornerShape(CARD_CORNER),
        color = container,
    ) {
        Column(
            modifier = Modifier.padding(CARD_PADDING, CARD_PADDING, CARD_PADDING, 4.dp),
        ) {
            AsyncImage(
                model = artwork,
                contentDescription = episode.title,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(COVER_CORNER)),
            )
            val updated = rememberRelativeTimestamp(episode.publishedAtMs)
            Column(
                modifier =
                    Modifier
                        .padding(horizontal = 4.dp)
                        .padding(top = 10.dp, bottom = 4.dp),
            ) {
                Text(
                    text = "${episode.podcastTitle.orEmpty()} · $updated",
                    style = MaterialTheme.typography.labelMedium,
                    color = onContainer.copy(alpha = SECONDARY_ALPHA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TitleAndNotes(episode = episode, onContainer = onContainer)
            }
            CardActions(
                episode = episode,
                palette = palette,
                onContainer = onContainer,
                onPlay = onPlay,
                onMarkPlayed = onMarkPlayed,
                onEpisodeClick = onEpisodeClick,
                onPodcastClick = onPodcastClick,
            )
        }
    }
}

/** Title over plain-text show notes, always exactly three title-lines tall so cards line up. */
@Composable
private fun TitleAndNotes(
    episode: SubscriptionEpisode,
    onContainer: Color,
) {
    val title = episode.title.orEmpty()
    val notes =
        remember(episode.description) {
            episode.description?.let { htmlToPlainText(it).replace('\n', ' ') }.orEmpty()
        }
    val titleStyle = MaterialTheme.typography.titleSmall
    // Notes use the real bodyMedium style; toSpanStyle() drops its lineHeight, so the whole block still
    // lays out at the title's line height and stays exactly three title-lines tall.
    val notesSpan =
        MaterialTheme.typography.bodyMedium
            .toSpanStyle()
            .copy(color = onContainer.copy(alpha = SECONDARY_ALPHA), fontSize = 14.sp)
    val measurer = rememberTextMeasurer()
    val contentWidthPx = with(LocalDensity.current) { (CARD_WIDTH - CARD_PADDING * 2).roundToPx() }
    // If the title alone already fills the block, drop the notes — and with them the ellipsis a fully
    // shown title would otherwise pick up from the (now-hidden) notes being clipped.
    val titleFillsBlock =
        remember(title, titleStyle, contentWidthPx) {
            val lines =
                measurer
                    .measure(title, titleStyle, constraints = Constraints(maxWidth = contentWidthPx))
                    .lineCount
            lines >= TITLE_LINES
        }
    val text =
        buildAnnotatedString {
            append(title)
            if (!titleFillsBlock && notes.isNotBlank()) {
                append("\n")
                withStyle(notesSpan) { append(notes) }
            }
        }
    ComposeText(
        text = text,
        style = titleStyle,
        color = onContainer,
        minLines = TITLE_LINES,
        maxLines = TITLE_LINES,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
@Suppress("LongParameterList")
private fun CardActions(
    episode: SubscriptionEpisode,
    palette: ArtworkPalette,
    onContainer: Color,
    onPlay: (SubscriptionEpisode) -> Unit,
    onMarkPlayed: (String) -> Unit,
    onEpisodeClick: (String) -> Unit,
    onPodcastClick: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalButton(
            onClick = { onPlay(episode) },
            enabled = episode.enclosureUrl != null,
            colors =
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = palette.accent,
                    contentColor = palette.onAccent,
                ),
            contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = durationLabel(episode.durationMs),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = {}, enabled = false) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = stringResource(R.string.home_subscription_download),
                tint = onContainer,
            )
        }
        MoreMenu(
            episode = episode,
            onContainer = onContainer,
            onMarkPlayed = onMarkPlayed,
            onEpisodeClick = onEpisodeClick,
            onPodcastClick = onPodcastClick,
        )
    }
}

@Composable
@Suppress("LongParameterList")
private fun MoreMenu(
    episode: SubscriptionEpisode,
    onContainer: Color,
    onMarkPlayed: (String) -> Unit,
    onEpisodeClick: (String) -> Unit,
    onPodcastClick: (String) -> Unit,
) {
    val menuState = rememberTunedDropdownMenuState()
    val context = LocalContext.current
    TunedDropdownMenuBox(
        state = menuState,
        anchor = { anchorModifier, openMenu ->
            IconButton(modifier = anchorModifier, onClick = openMenu) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.home_more_options),
                    tint = onContainer,
                )
            }
        },
    ) {
        Item(
            text = { ComposeText(stringResource(R.string.home_subscription_open_episode)) },
            onClick = { onEpisodeClick(episode.episodeId) },
        )
        Item(
            text = { ComposeText(stringResource(R.string.home_subscription_open_podcast)) },
            onClick = { onPodcastClick(episode.podcastId) },
        )
        Item(
            text = { ComposeText(stringResource(R.string.home_subscription_mark_played)) },
            onClick = { onMarkPlayed(episode.episodeId) },
        )
        Item(
            text = { ComposeText(stringResource(R.string.home_subscription_share)) },
            onClick = { context.startActivity(Intent.createChooser(shareIntent(episode), null)) },
        )
    }
}

private fun shareIntent(episode: SubscriptionEpisode): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, listOfNotNull(episode.title, episode.enclosureUrl).joinToString("\n"))
    }

@Composable
private fun durationLabel(durationMs: Long?): String {
    val minutes = durationMs?.let { TimeUnit.MILLISECONDS.toMinutes(it) }?.takeIf { it > 0 }
    return minutes?.let { stringResource(R.string.podcast_episode_duration, it) }
        ?: stringResource(R.string.episode_play)
}

private val CARD_WIDTH = 256.dp
private val CARD_CORNER = 24.dp
private val CARD_PADDING = 8.dp
private val COVER_CORNER = 16.dp
private const val TITLE_LINES = 3
private const val SECONDARY_ALPHA = 0.6f
