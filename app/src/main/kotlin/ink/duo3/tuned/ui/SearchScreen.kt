package ink.duo3.tuned.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun SearchScreen(navigationBack: () -> Unit) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
//        if ((screenHeight < 600.dp) && (screenWidth < 600.dp)) {
//            SearchScreenExtremeCompact(navigationBack)
//        } else if (screenWidth < 600.dp) {
            SearchScreenCompact(navigationBack)
//        } else if (screenWidth < 840.dp) {
//            SearchScreenMedium(navigationBack)
//        } else {
//            SearchScreenExpanded(navigationBack)
//        }
    }
}

@Composable
fun SearchScreenExtremeCompact(navigationBack: () -> Unit) {
    TODO("Not yet implemented")
}

@Composable
fun SearchScreenCompact(navigationBack: () -> Unit) {
    TODO("Not yet implemented")
}

@Composable
fun SearchScreenMedium(navigationBack: () -> Unit) {
    TODO("Not yet implemented")
}

@Composable
fun SearchScreenExpanded(navigationBack: () -> Unit) {
    TODO("Not yet implemented")
}


