package ink.duo3.tuned.ui.components.scaffold

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The collapsed now-playing surface (the floating "mini player") is rendered by the expandable
 * sheet itself; this file only keeps the shared layout metrics around it — the bottom backdrop
 * gradient, the scrollable-content clearance, and the navigation-bar platform height.
 */
@Composable
internal fun MiniPlayerBottomBackdrop(
    bottomClearanceHeight: Dp,
    modifier: Modifier = Modifier,
) {
    TunedBottomBackdrop(
        totalHeight = bottomClearanceHeight + MINI_PLAYER_HEIGHT,
        modifier = modifier,
    )
}

@Composable
internal fun miniPlayerPlatformHeight(): Dp {
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

internal val MINI_PLAYER_HEIGHT = 64.dp
private val MINIMUM_MINI_PLAYER_PLATFORM_HEIGHT = 24.dp
