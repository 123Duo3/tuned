package ink.duo3.tuned.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.ui.componets.ControlBar
import ink.duo3.tuned.ui.componets.HomeHeader
import ink.duo3.tuned.ui.componets.LastListenedCard
import ink.duo3.tuned.ui.componets.RecentlyUpdatedCard
import ink.duo3.tuned.ui.componets.RecentlyUpdatedItem
import ink.duo3.tuned.ui.componets.SubscribedCard

@Composable
fun HomeScreen() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val bottomPadding = if (
        WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() == 0.dp
    ) {
        16.dp
    } else {
        WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    }

    val density = LocalDensity.current
    val showLastListened = remember { mutableStateOf(true) }
    val closeLastListened = {showLastListened.value = false}

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
//        if ((screenHeight < 600.dp) && (screenWidth < 600.dp)) {
//            HomeScreenExtremeCompact()
//        } else if (screenWidth < 600.dp) {
            HomeScreenCompact(bottomPadding, density, showLastListened, closeLastListened)
//        } else if (screenWidth < 840.dp) {
//            HomeScreenMedium()
//        } else {
//            HomeScreenExpanded()
//        }
    }
}

@Composable
fun HomeScreenExtremeCompact() {
    TODO("Not yet implemented")
}

@Composable
fun HomeScreenCompact(
    bottomPadding: Dp,
    density: Density,
    showLastListened: MutableState<Boolean>,
    closeLastListened: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize()
        ) {
            val lazyListState = rememberLazyListState()
            HomeHeader(lazyListState.canScrollBackward)

            LazyColumn(
                Modifier
                    .weight(1f),
                state = lazyListState
            ) {
                item {
                    AnimatedVisibility(
                        visible = showLastListened.value,
                        enter = slideInVertically {
                            // Slide in from 40 dp from the top.
                            with(density) { -40.dp.roundToPx() }
                        } + expandVertically(
                            // Expand from the top.
                            expandFrom = Alignment.Top
                        ) + fadeIn(
                            // Fade in with the initial alpha of 0.3f.
                            initialAlpha = 0.3f
                        ),
                        exit = slideOutVertically() + shrinkVertically() + fadeOut()
                    ) {
                        LastListenedCard(closeLastListened)
                    }
                }
                item {
                    SubscribedCard()
                }
                item {
                    RecentlyUpdatedCard()
                }
                items(20) {
                    RecentlyUpdatedItem(
                        title = "#$it: This is the episode title",
                        isLastItem = (it == 19)
                    )
                }
                item {
                    AnimatedVisibility(
                        modifier = Modifier,
                        visible = !showLastListened.value,
                        enter = slideInVertically(),
                        exit = slideOutVertically()
                    ) {
                        Spacer(modifier = Modifier.height(80.dp + 16.dp))
                    }
                    Spacer(modifier = Modifier.height(bottomPadding))
                }
            }
        }
        AnimatedVisibility(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            visible = !showLastListened.value,
            enter = slideInVertically(initialOffsetY = {it}),
            exit = slideOutVertically(targetOffsetY = {-it})
        ) {
            ControlBar(bottomPadding = bottomPadding)
        }
    }
}

@Composable
fun HomeScreenMedium() {
    TODO("Not yet implemented")
}

@Composable
fun HomeScreenExpanded() {
    TODO("Not yet implemented")
}
