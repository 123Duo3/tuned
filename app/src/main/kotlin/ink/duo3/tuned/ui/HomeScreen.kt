package ink.duo3.tuned.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ink.duo3.tuned.R
import ink.duo3.tuned.ui.componets.HomeHeader
import ink.duo3.tuned.ui.componets.LastListenedCard
import ink.duo3.tuned.ui.componets.RecentlyUpdatedCard
import ink.duo3.tuned.ui.componets.SubscribedCard
import ink.duo3.tuned.ui.theme.cabinFamily

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

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
//        if ((screenHeight < 600.dp) && (screenWidth < 600.dp)) {
//            HomeScreenExtremeCompact()
//        } else if (screenWidth < 600.dp) {
            HomeScreenCompact(bottomPadding)
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
fun HomeScreenCompact(bottomPadding: Dp) {
    Column(
        Modifier.fillMaxSize()
    ) {
        val scrollState = rememberScrollState()
        HomeHeader(scrollState.value > 10f)

        Column(
            Modifier
                .verticalScroll(scrollState)
                .fillMaxSize()
                .padding(bottom = bottomPadding)
        ) {
            LastListenedCard()
            SubscribedCard()
            RecentlyUpdatedCard()
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
