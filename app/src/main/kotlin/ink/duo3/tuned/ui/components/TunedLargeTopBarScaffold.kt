package ink.duo3.tuned.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource

/**
 * The shared page scaffold for back-navigable screens: a Material 3 large top app bar that
 * collapses on scroll, dressed in the home aesthetic (tinted icon buttons over a progressive
 * blur + surface-container gradient). Page content scrolls *under* the floating bar, so the
 * [content] must attach the supplied `hazeModifier` to its scrollable container and consume
 * `contentPadding` (its top clears the fully-expanded bar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
fun TunedLargeTopBarScaffold(
    title: String,
    onBack: () -> Unit,
    backContentDescription: String,
    modifier: Modifier = Modifier,
    enableTopBarScroll: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (hazeModifier: Modifier, contentPadding: PaddingValues) -> Unit,
) {
    val hazeState = remember { HazeState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    LaunchedEffect(enableTopBarScroll) {
        if (!enableTopBarScroll) {
            scrollBehavior.state.heightOffset = 0f
            scrollBehavior.state.contentOffset = 0f
        }
    }

    Scaffold(
        modifier =
            if (enableTopBarScroll) {
                modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
            } else {
                modifier
            },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = TunedPageContentInsets,
        snackbarHost = snackbarHost,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val statusBarHeight = padding.calculateTopPadding()
        val barBodyHeight =
            lerp(
                LARGE_TOP_BAR_EXPANDED_HEIGHT,
                LARGE_TOP_BAR_COLLAPSED_HEIGHT,
                scrollBehavior.state.collapsedFraction,
            )

        Box(Modifier.fillMaxSize()) {
            content(
                Modifier.hazeSource(hazeState),
                PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection),
                    end = padding.calculateEndPadding(layoutDirection),
                    // Track the bar's live (collapsing) height, not the expanded constant, so page
                    // content rises *with* the bar instead of leaving a growing gap beneath it.
                    top = statusBarHeight + barBodyHeight,
                    bottom = padding.calculateBottomPadding(),
                ),
            )
            TunedTopBarBackdrop(
                hazeState = hazeState,
                platformHeight = statusBarHeight,
                gradientHeight = barBodyHeight,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            FrostedLargeTopBar(
                title = title,
                onBack = onBack,
                backContentDescription = backContentDescription,
                actions = actions,
                scrollBehavior = scrollBehavior,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongParameterList")
private fun FrostedLargeTopBar(
    title: String,
    onBack: () -> Unit,
    backContentDescription: String,
    actions: @Composable RowScope.() -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
) {
    LargeTopAppBar(
        title = {
            Text(
                text = title,
                // Only when collapsed does the title sit next to the back button; inset it so the
                // small title keeps a 16.dp margin (this 12.dp + the framework's 4.dp slot padding).
                modifier =
                    Modifier.padding(
                        horizontal = COLLAPSED_TITLE_INSET * scrollBehavior.state.collapsedFraction,
                    ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
        navigationIcon = {
            FilledTonalIconButton(
                onClick = onBack,
                modifier =
                    Modifier
                        .padding(start = TOP_BAR_BUTTON_EDGE_INSET)
                        .size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backContentDescription,
                )
            }
        },
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent,
            ),
    )
}

private val LARGE_TOP_BAR_EXPANDED_HEIGHT = 152.dp
private val LARGE_TOP_BAR_COLLAPSED_HEIGHT = 64.dp

// The framework already insets the navigation/action slots by TopAppBarHorizontalPadding (4.dp);
// adding 12.dp lands the tinted icon buttons 16.dp from the screen edge, matching the home bar.
private val TOP_BAR_BUTTON_EDGE_INSET = 12.dp
private val COLLAPSED_TITLE_INSET = 12.dp
