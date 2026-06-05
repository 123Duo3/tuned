package ink.duo3.tuned.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.model.Chapter
import ink.duo3.tuned.domain.player.PlaybackState
import ink.duo3.tuned.presentation.player.PlayerUiState
import ink.duo3.tuned.presentation.player.PlayerViewModel
import ink.duo3.tuned.ui.components.TunedLargeTopBarScaffold
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Full-screen player: large artwork, title/podcast, a scrubbable progress bar, and the
 * transport row (skip back 15s · play/pause · skip forward 30s) plus a speed toggle and a
 * chapter list. It renders from [PlayerUiState]; an empty state shows when nothing is loaded.
 */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TunedLargeTopBarScaffold(
        title = state.playback.podcastTitle.orEmpty(),
        onBack = onBack,
        backContentDescription = stringResource(R.string.player_back),
        modifier = modifier,
        actions = {
            if (state.playback.episodeId != null) {
                SleepTimerAction(
                    remainingMs = state.playback.sleepTimerRemainingMs,
                    presetsMinutes = viewModel.sleepTimerPresetsMinutes,
                    onStart = viewModel::startSleepTimer,
                    onCancel = viewModel::cancelSleepTimer,
                )
            }
        },
    ) { hazeModifier, contentPadding ->
        if (state.playback.episodeId == null) {
            Box(hazeModifier.fillMaxSize().padding(contentPadding)) {
                Text(
                    text = stringResource(R.string.player_nothing_playing),
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            PlayerContent(
                state = state,
                actions =
                    PlayerActions(
                        onPlayPause = viewModel::playPause,
                        onSeek = viewModel::seekTo,
                        onSkipBack = viewModel::skipBack,
                        onSkipForward = viewModel::skipForward,
                        onCycleSpeed = viewModel::cycleSpeed,
                    ),
                hazeModifier = hazeModifier,
                contentPadding = contentPadding,
            )
        }
    }
}

/** The player's event callbacks, grouped so [PlayerContent] stays a two-argument composable. */
private class PlayerActions(
    val onPlayPause: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onSkipBack: () -> Unit,
    val onSkipForward: () -> Unit,
    val onCycleSpeed: () -> Unit,
)

@Composable
private fun PlayerContent(
    state: PlayerUiState,
    actions: PlayerActions,
    hazeModifier: Modifier,
    contentPadding: PaddingValues,
) {
    val playback = state.playback
    Column(
        modifier =
            hazeModifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PlayerHeader(
            artworkUrl = state.artworkUrl,
            title = playback.title,
            chapterTitle = state.currentChapter?.title,
        )
        ProgressBar(state = playback, onSeek = actions.onSeek)
        TransportRow(
            isPlaying = playback.isPlaying,
            buffering = playback.buffering,
            onPlayPause = actions.onPlayPause,
            onSkipBack = actions.onSkipBack,
            onSkipForward = actions.onSkipForward,
        )
        TextButton(onClick = actions.onCycleSpeed) {
            Text(stringResource(R.string.player_speed, formatSpeed(playback.speed)))
        }
        playback.sleepTimerRemainingMs?.let { remaining ->
            Text(
                text = stringResource(R.string.player_sleep_timer_remaining, formatTime(remaining)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.chapters.isNotEmpty()) {
            ChapterList(
                chapters = state.chapters,
                currentChapterIndex = state.currentChapterIndex,
                onChapterClick = actions.onSeek,
            )
        }
    }
}

/** Large square artwork (chapter art when active), the episode title, and the current chapter. */
@Composable
private fun PlayerHeader(
    artworkUrl: String?,
    title: String?,
    chapterTitle: String?,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth(fraction = 0.8f)
                .aspectRatio(1f),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
    Text(
        text = title.orEmpty(),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    chapterTitle?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The episode's chapters as a tappable list (seeks to a chapter's start), with the active
 * one highlighted and each chapter's own artwork shown as a leading thumbnail when present.
 * Rendered as a plain Column because it lives inside the screen's vertical scroll.
 */
@Composable
private fun ChapterList(
    chapters: List<Chapter>,
    currentChapterIndex: Int?,
    onChapterClick: (Long) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.player_chapters),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        chapters.forEachIndexed { index, chapter ->
            ChapterRow(
                chapter = chapter,
                isCurrent = index == currentChapterIndex,
                onClick = { onChapterClick(chapter.startTimeMs) },
            )
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: Chapter,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val contentColor =
        if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        chapter.imageUrl?.let { imageUrl ->
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
            )
        }
        Text(
            text = chapter.title.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatTime(chapter.startTimeMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Top-bar sleep-timer control: a bedtime icon (filled while a timer runs) that opens a
 * menu of preset durations, plus a cancel entry once a timer is active.
 */
@Composable
private fun SleepTimerAction(
    remainingMs: Long?,
    presetsMinutes: List<Int>,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = if (remainingMs != null) Icons.Filled.Bedtime else Icons.Outlined.Bedtime,
                contentDescription = stringResource(R.string.player_sleep_timer),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            presetsMinutes.forEach { minutes ->
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.player_sleep_timer_minutes, minutes)) },
                    onClick = {
                        onStart(minutes)
                        expanded = false
                    },
                )
            }
            if (remainingMs != null) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.player_sleep_timer_cancel)) },
                    onClick = {
                        onCancel()
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ProgressBar(
    state: PlaybackState,
    onSeek: (Long) -> Unit,
) {
    var scrub by remember { mutableStateOf<Float?>(null) }
    val durationMs = state.durationMs.coerceAtLeast(0)
    val positionMs = scrub?.toLong() ?: state.positionMs
    Column(Modifier.fillMaxWidth()) {
        Slider(
            value = positionMs.toFloat(),
            onValueChange = { scrub = it },
            onValueChangeFinished = {
                scrub?.let { onSeek(it.toLong()) }
                scrub = null
            },
            valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(positionMs), style = MaterialTheme.typography.labelMedium)
            Text(formatTime(durationMs), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun TransportRow(
    isPlaying: Boolean,
    buffering: Boolean,
    onPlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onSkipBack) {
            Icon(Icons.Filled.FastRewind, contentDescription = stringResource(R.string.player_skip_back))
        }
        FilledIconButton(onClick = onPlayPause, modifier = Modifier.size(64.dp)) {
            if (buffering) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription =
                        stringResource(if (isPlaying) R.string.player_pause else R.string.player_play),
                )
            }
        }
        IconButton(onClick = onSkipForward) {
            Icon(Icons.Filled.FastForward, contentDescription = stringResource(R.string.player_skip_forward))
        }
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) {
        speed.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", speed)
    }

private fun formatTime(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0))
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}
