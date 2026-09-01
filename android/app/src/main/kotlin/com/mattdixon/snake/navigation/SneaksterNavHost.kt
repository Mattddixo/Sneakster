package com.mattdixon.snake.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mattdixon.snake.ui.screens.GameScreen
import com.mattdixon.snake.ui.screens.LeaderboardScreen
import com.mattdixon.snake.ui.screens.MenuScreen
import com.mattdixon.snake.ui.screens.SettingsScreen
import com.mattdixon.snake.ui.screens.ShopScreen

private object Routes {
    const val MENU = "menu"
    const val GAME = "game"
    const val LEADERBOARD = "leaderboard"
    const val SETTINGS = "settings"
    const val SHOP = "shop"
}

@Composable
fun SneaksterNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.MENU) {
        composable(Routes.MENU) {
            MenuScreen(
                onPlay = { navController.navigate(Routes.GAME) },
                onLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onShop = { navController.navigate(Routes.SHOP) },
            )
        }
        composable(Routes.GAME) {
            GameScreen(onExitToMenu = { navController.popBackStack(Routes.MENU, inclusive = false) })
        }
        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SHOP) {
            ShopScreen(onBack = { navController.popBackStack() })
        }
    }
}
