package ink.duo3.tuned

import androidx.compose.animation.core.EaseInOutElastic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ink.duo3.tuned.ui.HomeScreen
import ink.duo3.tuned.ui.InitialSubscriptScreen
import ink.duo3.tuned.ui.WelcomeScreen

@Composable
fun TunedApp() {
    val navController = rememberNavController()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ){
        TunedNavHost(navController = navController)
    }
}

@Composable
fun TunedNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = "welcome",
        enterTransition = {
            slideInHorizontally(initialOffsetX = {it/2}) + fadeIn(tween(300))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = {-it/2} ) + fadeOut(tween(150))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = {-it/2}) + fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = {it/2}) + fadeOut(tween(150))
        }
    ) {

        composable("welcome") {
            WelcomeScreen(navigationNext = {navController.navigate("initial_subscription")})
        }

        composable("initial_subscription") {
            InitialSubscriptScreen(navigationDone = {
                navController.navigate("home") {
                    popUpTo("welcome") { inclusive = true }
                }
            })
        }

        composable(route = "home") {
            HomeScreen()
        }
    }
}