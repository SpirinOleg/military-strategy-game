package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4A90E2),
    secondary = Color(0xFF7B68EE),
    tertiary = Color(0xFF00CED1),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = Color(0xFFCF6679),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4A90E2),
    secondary = Color(0xFF7B68EE),
    tertiary = Color(0xFF00CED1),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    error = Color(0xFFB3261E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onError = Color.White
)

@Composable
fun MilitaryStrategyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}