package com.example.military_strategy_game.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.game.presentation.GameScreen
import com.example.menu.presentation.MenuScreen
import com.example.shop.presentation.ShopScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "menu"
    ) {
        composable("menu") {
            MenuScreen(
                onNavigateToShop = { navController.navigate("shop") },
                onNavigateToGame = { points ->
                    navController.navigate("game/$points")
                }
            )
        }

        composable("shop") {
            ShopScreen(
                onNavigateBack = { navController.popBackStack() },
                onStartGame = { points ->
                    navController.navigate("game/$points") {
                        popUpTo("menu")
                    }
                }
            )
        }

        composable("game/{points}") { backStackEntry ->
            val points = backStackEntry.arguments?.getString("points")?.toIntOrNull() ?: 1000
            GameScreen(
                initialPoints = points,
                onNavigateBack = {
                    navController.navigate("menu") {
                        popUpTo("menu") { inclusive = true }
                    }
                }
            )
        }
    }
}