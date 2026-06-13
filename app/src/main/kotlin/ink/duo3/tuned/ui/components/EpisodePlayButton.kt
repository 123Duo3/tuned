package ink.duo3.tuned.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.player.EpisodePlaybackSnapshot
import ink.duo3.tuned.domain.player.EpisodePlaybackStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

@Composable
@Suppress("LongParameterList")
fun EpisodePlayButton(
    durationMs: Long?,
    playback: EpisodePlaybackSnapshot,
    palette: ArtworkPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    parentUsesContainerColor: Boolean = false,
    audioLevelBars: Flow<List<Float>> = emptyFlow(),
    contentPadding: PaddingValues = PaddingValues(start = 12.dp, end = 16.dp),
) {
    val colors = palette.buttonColors(parentUsesContainerColor)
    val contentAlpha = if (enabled) 1f else DISABLED_CONTENT_ALPHA
    val label = playback.label(durationMs)
    Surface(
        onClick = onClick,
        modifier =
            modifier
                .defaultMinSize(minHeight = BUTTON_HEIGHT)
                .clip(BUTTON_SHAPE),
        enabled = enabled,
        shape = BUTTON_SHAPE,
        color = colors.container,
        contentColor = colors.content.copy(alpha = contentAlpha),
    ) {
        Box(
            modifier =
                Modifier
                    .height(BUTTON_HEIGHT),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .padding(PROGRESS_INSET)
                        .clip(PROGRESS_CLIP_SHAPE)
                        .episodeProgressTrack(
                            progress = playback.progress,
                            color = colors.progress,
                        ),
            )
            Row(
                modifier = Modifier.padding(contentPadding),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaybackStatusIcon(
                    status = playback.status,
                    audioLevelBars = audioLevelBars,
                )
                AnimatedButtonLabel(label = label, status = playback.status)
            }
        }
    }
}

@Composable
private fun AnimatedButtonLabel(
    label: String,
    status: EpisodePlaybackStatus,
) {
    val sizeAnimation =
        spring<IntSize>(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        )
    AnimatedContent(
        targetState =
            EpisodePlayButtonLabel(
                text = label,
                completed = status == EpisodePlaybackStatus.Completed,
            ),
        modifier = Modifier.padding(start = 6.dp),
        transitionSpec = {
            val fadeMillis =
                if (initialState.completed != targetState.completed) {
                    LABEL_CROSSFADE_MILLIS
                } else {
                    LABEL_DIRECT_CHANGE_MILLIS
                }
            fadeIn(tween(durationMillis = fadeMillis)) togetherWith
                fadeOut(tween(durationMillis = fadeMillis)) using
                SizeTransform(clip = false) { _, _ -> sizeAnimation }
        },
        label = "episodePlayButtonLabel",
    ) { targetLabel ->
        Text(
            text = targetLabel.text,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private data class EpisodePlayButtonLabel(
    val text: String,
    val completed: Boolean,
)

private data class EpisodePlayButtonColors(
    val container: androidx.compose.ui.graphics.Color,
    val content: androidx.compose.ui.graphics.Color,
    val progress: androidx.compose.ui.graphics.Color,
)

private fun ArtworkPalette.buttonColors(parentUsesContainerColor: Boolean): EpisodePlayButtonColors =
    if (parentUsesContainerColor) {
        EpisodePlayButtonColors(
            container = accent,
            content = onAccent,
            progress = container.copy(alpha = PROGRESS_ALPHA),
        )
    } else {
        EpisodePlayButtonColors(
            container = container,
            content = onContainer,
            progress = accent.copy(alpha = PROGRESS_ALPHA),
        )
    }

@Composable
private fun EpisodePlaybackSnapshot.label(durationMs: Long?): String =
    if (status == EpisodePlaybackStatus.Completed) {
        stringResource(R.string.episode_completed)
    } else {
        val timeLabel =
            when (status) {
                EpisodePlaybackStatus.Unplayed -> durationMs?.let { formatEpisodeButtonTime(it) }
                EpisodePlaybackStatus.Loading -> remainingMs?.let { formatEpisodeButtonTime(it) }
                EpisodePlaybackStatus.Playing,
                EpisodePlaybackStatus.Resume,
                -> remainingMs?.let { formatEpisodeButtonTime(it) }
                EpisodePlaybackStatus.Completed -> null
            }
        timeLabel
            ?: when (status) {
                EpisodePlaybackStatus.Playing -> stringResource(R.string.episode_playing)
                EpisodePlaybackStatus.Resume -> stringResource(R.string.episode_resume)
                else -> stringResource(R.string.episode_play)
            }
    }

@Composable
private fun formatEpisodeButtonTime(durationMs: Long): String {
    val totalSeconds =
        if (durationMs <= 0L) {
            0L
        } else {
            (durationMs + MILLIS_PER_SECOND - 1L) / MILLIS_PER_SECOND
        }
    val hours = TimeUnit.SECONDS.toHours(totalSeconds)
    val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) % MINUTES_PER_HOUR
    val seconds = totalSeconds % SECONDS_PER_MINUTE
    return when {
        hours > 0L -> stringResource(R.string.episode_time_hours_minutes, hours, minutes)
        minutes > 0L -> stringResource(R.string.episode_time_minutes, minutes)
        else -> stringResource(R.string.episode_time_seconds, seconds)
    }
}

@Composable
private fun PlaybackStatusIcon(
    status: EpisodePlaybackStatus,
    audioLevelBars: Flow<List<Float>>,
) {
    val contentDescription =
        when (status) {
            EpisodePlaybackStatus.Unplayed -> stringResource(R.string.episode_play)
            EpisodePlaybackStatus.Loading -> stringResource(R.string.episode_loading)
            EpisodePlaybackStatus.Playing -> stringResource(R.string.player_pause)
            EpisodePlaybackStatus.Resume -> stringResource(R.string.episode_resume)
            EpisodePlaybackStatus.Completed -> stringResource(R.string.episode_replay)
        }
    val modifier = Modifier.size(18.dp)
    AnimatedContent(
        targetState = status.iconKind(),
        transitionSpec = {
            fadeIn(tween(durationMillis = ICON_TRANSITION_MILLIS)) +
                scaleIn(
                    initialScale = ICON_TRANSITION_SCALE,
                    animationSpec = tween(durationMillis = ICON_TRANSITION_MILLIS),
                ) togetherWith
                fadeOut(tween(durationMillis = ICON_TRANSITION_MILLIS)) +
                scaleOut(
                    targetScale = ICON_TRANSITION_SCALE,
                    animationSpec = tween(durationMillis = ICON_TRANSITION_MILLIS),
                )
        },
        label = "episodePlayButtonIcon",
    ) { iconKind ->
        when (iconKind) {
            EpisodePlayButtonIconKind.Play ->
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = contentDescription,
                    modifier = modifier,
                )
            EpisodePlayButtonIconKind.Equalizer ->
                PlaybackEqualizerIcon(
                    loading = status == EpisodePlaybackStatus.Loading,
                    audioLevelBars = audioLevelBars,
                    contentDescription = contentDescription,
                    modifier = modifier,
                )
            EpisodePlayButtonIconKind.Resume ->
                Icon(
                    painter = painterResource(R.drawable.ic_resume_20px),
                    contentDescription = contentDescription,
                    modifier = modifier,
                )
            EpisodePlayButtonIconKind.Completed ->
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = contentDescription,
                    modifier = modifier,
                )
        }
    }
}

private enum class EpisodePlayButtonIconKind {
    Play,
    Equalizer,
    Resume,
    Completed,
}

private fun EpisodePlaybackStatus.iconKind(): EpisodePlayButtonIconKind =
    when (this) {
        EpisodePlaybackStatus.Unplayed -> EpisodePlayButtonIconKind.Play
        EpisodePlaybackStatus.Loading,
        EpisodePlaybackStatus.Playing,
        -> EpisodePlayButtonIconKind.Equalizer
        EpisodePlaybackStatus.Resume -> EpisodePlayButtonIconKind.Resume
        EpisodePlaybackStatus.Completed -> EpisodePlayButtonIconKind.Completed
    }

@Composable
@Suppress("LongMethod")
private fun PlaybackEqualizerIcon(
    loading: Boolean,
    audioLevelBars: Flow<List<Float>>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val currentAudioLevelBars by audioLevelBars.collectAsState(initial = emptyList())
    val transition = rememberInfiniteTransition(label = "episodePlayLoadingEqualizer")
    val wavePosition by
        transition.animateFloat(
            initialValue = -LOADING_WAVE_WIDTH,
            targetValue = EQUALIZER_BAR_COUNT - 1 + LOADING_WAVE_WIDTH,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = LOADING_WAVE_MILLIS, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
            label = "episodePlayLoadingWave",
        )
    val loadingBars =
        List(EQUALIZER_BAR_COUNT) { index ->
            val distance = abs(index - wavePosition)
            if (distance >= LOADING_WAVE_WIDTH) {
                0f
            } else {
                val wave = ((cos(distance / LOADING_WAVE_WIDTH * PI) + 1.0) / 2.0).toFloat()
                wave * LOADING_WAVE_PEAK
            }
        }
    val useLoadingBars = loading || currentAudioLevelBars.isEmpty()
    val targetBars =
        List(EQUALIZER_BAR_COUNT) { index ->
            if (useLoadingBars) {
                loadingBars[index]
            } else {
                currentAudioLevelBars.getOrNull(index)?.coerceIn(0f, 1f) ?: 0f
            }
        }
    val targetAlphas =
        List(EQUALIZER_BAR_COUNT) { index ->
            if (useLoadingBars) {
                val wave = (loadingBars[index] / LOADING_WAVE_PEAK).coerceIn(0f, 1f)
                LOADING_MAX_ALPHA - (LOADING_MAX_ALPHA - LOADING_MIN_ALPHA) * wave
            } else {
                1f
            }
        }
    val bars =
        List(EQUALIZER_BAR_COUNT) { index ->
            val bar by
                animateFloatAsState(
                    targetValue = targetBars[index],
                    animationSpec = tween(durationMillis = EQUALIZER_RESPONSE_MILLIS, easing = LinearEasing),
                    label = "episodePlayEqualizerBar$index",
                )
            bar
        }
    val alphas =
        List(EQUALIZER_BAR_COUNT) { index ->
            val alpha by
                animateFloatAsState(
                    targetValue = targetAlphas[index],
                    animationSpec = tween(durationMillis = EQUALIZER_RESPONSE_MILLIS, easing = LinearEasing),
                    label = "episodePlayEqualizerAlpha$index",
                )
            alpha
        }
    EqualizerBarsIcon(
        bars = bars,
        alphas = alphas,
        contentDescription = contentDescription,
        modifier = modifier,
    )
}

@Composable
private fun EqualizerBarsIcon(
    bars: List<Float>,
    alphas: List<Float>,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val color = LocalContentColor.current
    Canvas(
        modifier =
            modifier.semantics {
                this.contentDescription = contentDescription
            },
    ) {
        val barWidth = size.width * EQUALIZER_BAR_WIDTH_FRACTION
        val contentWidth = size.width * EQUALIZER_CONTENT_WIDTH_FRACTION
        val gap = (contentWidth - barWidth * EQUALIZER_BAR_COUNT) / (EQUALIZER_BAR_COUNT - 1)
        val start = (size.width - contentWidth) / 2f
        val minHeight = barWidth
        val maxHeight = size.height * EQUALIZER_MAX_HEIGHT_FRACTION
        repeat(EQUALIZER_BAR_COUNT) { index ->
            val height = minHeight + (maxHeight - minHeight) * bars[index]
            val left = start + index * (barWidth + gap)
            drawRoundRect(
                color = color.copy(alpha = alphas[index]),
                topLeft = Offset(left, (size.height - height) / 2f),
                size = Size(barWidth, height),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}

private fun Modifier.episodeProgressTrack(
    progress: Float,
    color: androidx.compose.ui.graphics.Color,
): Modifier =
    drawWithCache {
        val progressWidth = (size.width * progress.coerceIn(0f, 1f)).coerceAtLeast(0f)
        val progressRadius = PROGRESS_RADIUS.toPx()
        onDrawWithContent {
            if (progressWidth > 0f) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset.Zero,
                    size = Size(progressWidth, size.height),
                    cornerRadius = CornerRadius(progressRadius, progressRadius),
                )
            }
            drawContent()
        }
    }

private val BUTTON_HEIGHT = 40.dp
private val BUTTON_SHAPE = RoundedCornerShape(50)
private val PROGRESS_CLIP_SHAPE = RoundedCornerShape(50)
private val PROGRESS_INSET = 4.dp
private val PROGRESS_RADIUS = 4.dp
private const val PROGRESS_ALPHA = 0.15f
private const val DISABLED_CONTENT_ALPHA = 0.38f
private const val LABEL_DIRECT_CHANGE_MILLIS = 0
private const val LABEL_CROSSFADE_MILLIS = 120
private const val ICON_TRANSITION_MILLIS = 180
private const val ICON_TRANSITION_SCALE = 0.68f
private const val EQUALIZER_BAR_COUNT = 5
private const val EQUALIZER_RESPONSE_MILLIS = 90
private const val EQUALIZER_CONTENT_WIDTH_FRACTION = 0.7f
private const val EQUALIZER_BAR_WIDTH_FRACTION = 0.075f
private const val EQUALIZER_MAX_HEIGHT_FRACTION = 0.8f
private const val LOADING_WAVE_MILLIS = 1000
private const val LOADING_WAVE_WIDTH = 4.5f
private const val LOADING_WAVE_PEAK = 0.48f
private const val LOADING_MIN_ALPHA = 0.4f
private const val LOADING_MAX_ALPHA = 1f
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
