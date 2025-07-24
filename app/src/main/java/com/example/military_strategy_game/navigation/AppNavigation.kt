package com.example.military_strategy_game.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.game.presentation.GameScreen
import com.example.menu.presentation.MenuScreenCompact
import com.example.shop.presentation.ShopScreen
import com.example.common.model.UnitType

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
                onStartGame = { points, purchasedUnits ->
                    println("DEBUG: Starting game from shop with points: $points and units: $purchasedUnits")
                    // Кодируем данные о покупках в строку для передачи через навигацию
                    val unitsData = purchasedUnits.entries
                        .filter { it.value > 0 }
                        .joinToString(";") { "${it.key.name}:${it.value}" }

                    navController.navigate("game/$points/$unitsData") {
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
                purchasedUnits = emptyMap(), // Пустая карта для обычного запуска
                onNavigateBack = {
                    println("DEBUG: Navigating back from game")
                    navController.navigate("menu") {
                        popUpTo("menu") { inclusive = true }
                    }
                }
            )
        }

        composable("game/{points}/{units}") { backStackEntry ->
            val points = backStackEntry.arguments?.getString("points")?.toIntOrNull() ?: 1000
            val unitsString = backStackEntry.arguments?.getString("units") ?: ""

            // Декодируем данные о покупках
            val purchasedUnits = if (unitsString.isNotEmpty()) {
                unitsString.split(";")
                    .mapNotNull { unitData ->
                        val parts = unitData.split(":")
                        if (parts.size == 2) {
                            try {
                                UnitType.valueOf(parts[0]) to parts[1].toInt()
                            } catch (e: Exception) {
                                null
                            }
                        } else null
                    }
                    .toMap()
            } else {
                emptyMap()
            }

            println("DEBUG: Game screen opened with points: $points and units: $purchasedUnits")
            GameScreen(
                initialPoints = points,
                purchasedUnits = purchasedUnits,
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