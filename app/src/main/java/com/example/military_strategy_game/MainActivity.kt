package com.example.military_strategy_game

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.military_strategy_game.navigation.AppNavigation
import com.example.ui.theme.MilitaryStrategyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Принудительно горизонтальная ориентация для игрового поля
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        enableEdgeToEdge()
        setContent {
            MilitaryStrategyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}