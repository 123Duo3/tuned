package ink.duo3.tuned.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ink.duo3.tuned.R
import ink.duo3.tuned.domain.player.PlaybackState

/**
 * The persistent floating mini-player shown above app content while something is loaded. Tapping
 * the row opens the full player; the trailing button toggles play/pause. Render only when
 * [PlaybackState.episodeId] is non-null (the host decides) — this composable assumes it is.
 */
@Composable
fun MiniPlayer(
    state: PlaybackState,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .height(MINI_PLAYER_HEIGHT)
                .miniPlayerShadow(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier =
                Modifier
                    .clickable(onClick = onClick)
                    .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                AsyncImage(
                    model = state.artworkUrl,
                    contentDescription = state.title,
                    contentScale = ContentScale.Crop,
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = state.title.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!state.podcastTitle.isNullOrBlank()) {
                    Text(
                        text = state.podcastTitle.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription =
                        stringResource(if (state.isPlaying) R.string.player_pause else R.string.player_play),
                )
            }
        }
    }
}

@Composable
internal fun MiniPlayerBottomBackdrop(
    bottomClearanceHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    TunedBottomBackdrop(
        totalHeight = bottomClearanceHeight + MINI_PLAYER_HEIGHT,
        modifier = modifier,
    )
}

@Composable
internal fun miniPlayerPlatformHeight(): androidx.compose.ui.unit.Dp {
    val density = LocalDensity.current
    val navigationBarHeight = with(density) { WindowInsets.navigationBars.getBottom(this).toDp() }
    return when {
        navigationBarHeight < MINIMUM_MINI_PLAYER_PLATFORM_HEIGHT -> MINIMUM_MINI_PLAYER_PLATFORM_HEIGHT
        else -> navigationBarHeight + 8.dp
    }
}

/** Scrollable-content clearance for the floating mini-player and its navigation-bar platform. */
internal val LocalMiniPlayerBottomClearance = compositionLocalOf { 0.dp }

/** Whether the floating mini-player — and therefore its bottom backdrop — is currently shown. */
internal val LocalMiniPlayerVisible = compositionLocalOf { false }

/** A soft drop shadow lifting the mini-player off the content, strengthened in dark mode. */
@Composable
private fun Modifier.miniPlayerShadow(): Modifier =
    dropShadow(
        shape = RoundedCornerShape(16.dp),
        shadow =
            Shadow(
                radius = 24.dp,
                color = Color.Black,
                offset = DpOffset(0.dp, 4.dp),
                alpha =
                    if (isSystemInDarkTheme()) {
                        MINI_PLAYER_SHADOW_ALPHA_DARK
                    } else {
                        MINI_PLAYER_SHADOW_ALPHA_LIGHT
                    },
            ),
    )

internal val MINI_PLAYER_HEIGHT = 64.dp
private const val MINI_PLAYER_SHADOW_ALPHA_LIGHT = 0.03f
private const val MINI_PLAYER_SHADOW_ALPHA_DARK = 0.1f
private val MINIMUM_MINI_PLAYER_PLATFORM_HEIGHT = 24.dp
