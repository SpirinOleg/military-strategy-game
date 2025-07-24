package com.example.military_strategy_game.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.game.presentation.GameScreen
import com.example.menu.presentation.MenuScreenCompact
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
            MenuScreenCompact (
                onNavigateToShop = {
                    println("DEBUG: Navigating to shop from menu")
                    navController.navigate("shop")
                },
                onNavigateToGame = { points ->
                    println("DEBUG: Navigating to game with points: $points")
                    navController.navigate("game/$points")
                }
            )
        }

        composable("shop") {
            ShopScreen(
                onNavigateBack = {
                    println("DEBUG: Navigating back from shop")
                    navController.popBackStack()
                },
                onStartGame = { points ->
                    println("DEBUG: Starting game from shop with points: $points")
                    navController.navigate("game/$points") {
                        popUpTo("menu")
                    }
                }
            )
        }

        composable("game/{points}") { backStackEntry ->
            val points = backStackEntry.arguments?.getString("points")?.toIntOrNull() ?: 1000
            println("DEBUG: Game screen opened with points: $points")
            GameScreen(
                initialPoints = points,
                onNavigateBack = {
                    println("DEBUG: Navigating back from game")
                    navController.navigate("menu") {
                        popUpTo("menu") { inclusive = true }
                    }
                }
            )
        }
    }
}