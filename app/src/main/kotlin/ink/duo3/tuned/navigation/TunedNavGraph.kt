package ink.duo3.tuned.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Central NavHost. Destinations currently render placeholders; each feature
 * screen replaces its placeholder when the feature lands (build order steps 2+).
 */
@Composable
fun TunedNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Route.Home,
        modifier = modifier,
    ) {
        composable<Route.Home> { Placeholder("Home") }
        composable<Route.Search> { Placeholder("Search") }
        composable<Route.Library> { Placeholder("Library") }
        composable<Route.PodcastDetail> { Placeholder("Podcast detail") }
        composable<Route.EpisodeDetail> { Placeholder("Episode detail") }
        composable<Route.Player> { Placeholder("Player") }
    }
}

@Composable
private fun Placeholder(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(name)
    }
}
