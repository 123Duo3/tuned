package ink.duo3.tuned

import androidx.compose.runtime.Composable
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
    
    TunedNavHost(navController = navController)
}

@Composable
fun TunedNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "welcome") {

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