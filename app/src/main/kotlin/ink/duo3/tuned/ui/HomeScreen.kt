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

@Composable
fun HomeHeader(float: Boolean) {
    val elevation = animateDpAsState(
        if (float) 16.dp else 0.dp, label = "Elevation")
    Surface(
        Modifier
            .fillMaxWidth(),
        tonalElevation = elevation.value
    ) {
        Row(
            Modifier
                .statusBarsPadding()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Icon(
                painter = painterResource(id = R.drawable.tuned_wave), 
                contentDescription = "Logo",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.height(28.dp)
            )

            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun LastListenedCard() {
    Surface(
        modifier = Modifier.padding(16.dp, 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = (-2).dp
    ) {
        Column {
            Row(
            ) {
                Text(
                    text = "Last listened",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = cabinFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp, 12.dp)
                )
                IconButton(
                    modifier = Modifier.padding(8.dp, 4.dp),
                    onClick = { /*TODO*/ }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Row(
                Modifier.padding(16.dp, 0.dp)
            ) {
                Column(
                    Modifier.weight(1f)
                ) {
                    Text(
                        text = "ULSUM RADIO" + " · " + "3 天前发布",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "EP171：一场完美求婚什么样？男女看法大不同？",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = cabinFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(44.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {}
            }
            Row(
                Modifier.padding(16.dp, 16.dp, 8.dp, 16.dp,)
            ) {
                Button(
                    onClick = { /*TODO*/ },
                    contentPadding = PaddingValues(12.dp, 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_resume_24dp),
                        contentDescription = "Resume",
                        modifier = Modifier
                    )
                    Text(
                        text = "30 minutes left",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(6.dp, 0.dp, 6.dp, 1.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (true) {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download_done_24dp),
                            contentDescription = "Downloaded",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    IconButton(onClick = { /*TODO*/ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_download_24dp),
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun SubscribedCard() {
    Surface(
        modifier = Modifier.padding(16.dp, 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = (-2).dp
    ) {
        Column {
            Row(
            ) {
                Text(
                    text = "Subscribed",
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = cabinFamily,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp, 12.dp)
                )
                IconButton(
                    modifier = Modifier.padding(8.dp, 4.dp),
                    onClick = { /*TODO*/ }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            LazyRow(Modifier.padding(bottom = 16.dp)) {
                items(10) {
                    Surface(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(92.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {}
                }
                items(1){
                    Spacer(Modifier.width(16.dp))
                }
            }
        }
    }
}

@Composable
fun RecentlyUpdatedCard() {
    Surface(
        modifier = Modifier.padding(16.dp, 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = (-2).dp
    ) {
        Column {
            Text(
                text = "Recently updated",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = cabinFamily,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 12.dp)
            )
            (1..20).forEach {
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                RecentlyUpdatedItem(title = "#$it: This is the episode title")
            }
        }
    }
}

@Composable
fun RecentlyUpdatedItem(title: String) {
    Column(
        Modifier.padding(0.dp, 16.dp)
    ) {
        Row(Modifier.padding(16.dp, 0.dp)) {
            Column(
                Modifier.weight(1f)
            ) {
                Text(
                    text = "Series name" + " · " + "3 天前",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = cabinFamily,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Surface(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(44.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {}
        }
        Text(
            text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
            maxLines = 3,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(16.dp, 8.dp)
        )
        Row(
            Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp)
        ) {
            Button(
                onClick = { /*TODO*/ },
                contentPadding = PaddingValues(12.dp, 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier
                )
                Text(
                    text = "2 hours 31 minutes",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(6.dp, 0.dp, 6.dp, 1.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (false) {
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_playlist_add_check_24dp),
                        contentDescription = "Remove from playlist",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_playlist_add_24dp),
                        contentDescription = "Added to playlist",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (false) {
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download_done_24dp),
                        contentDescription = "Downloaded",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                IconButton(onClick = { /*TODO*/ }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_download_24dp),
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            IconButton(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}