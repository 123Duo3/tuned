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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
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
            (1..100).forEach {
                Text(text = it.toString())
            }
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
                    modifier = Modifier.weight(1f).padding(16.dp, 12.dp)
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
                        text = "字谈字畅" + " · " + "3 天前发布",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "#212：狮子山下萨米柯",
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = cabinFamily,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .size(56.dp),
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
                        modifier = Modifier.padding(6.dp, 0.dp, 4.dp, 1.dp)
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
