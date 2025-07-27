package com.example.menu.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.system.exitProcess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreenCompact(
    onNavigateToShop: () -> Unit,
    onNavigateToGame: (Int) -> Unit
) {
    val context = LocalContext.current

    // ДОРАБОТКА 4: Главное меню на весь экран с центрированным контентом
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460)
                    )
                )
            ),
        contentAlignment = Alignment.Center // Центрируем весь контент
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.6f) // Ограничиваем ширину до 60% экрана
                .padding(24.dp), // Уменьшили отступы
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // Уменьшили отступы
        ) {
            // Заголовок - уменьшен для экономии места
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.8f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp), // Уменьшили внутренние отступы
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚔️ ВОЕННАЯ СТРАТЕГИЯ",
                        fontSize = 22.sp, // Уменьшили размер шрифта
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Командование полем боя",
                        fontSize = 12.sp, // Уменьшили размер подзаголовка
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ДОРАБОТКА 4: Кнопки меню с фиксированным размером (не растягиваются)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp), // Уменьшили внутренние отступы
                    horizontalAlignment = Alignment.CenterHorizontally, // Центрируем кнопки
                    verticalArrangement = Arrangement.spacedBy(12.dp) // Уменьшили отступы между кнопками
                ) {
                    CompactMenuButton(
                        text = "🎮 НАЧАТЬ ИГРУ",
                        color = Color(0xFF4A90E2),
                        onClick = { onNavigateToGame(1000) }
                    )

                    CompactMenuButton(
                        text = "🛒 МАГАЗИН ВОЙСК",
                        color = Color(0xFFFF5722),
                        onClick = {
                            println("DEBUG: ===== COMPACT SHOP BUTTON CLICKED =====")
                            onNavigateToShop()
                        }
                    )

                    CompactMenuButton(
                        text = "❌ ВЫХОД",
                        color = Color(0xFF757575),
                        onClick = {
                            println("DEBUG: Exit clicked - closing app")
                            // Закрываем приложение
                            (context as? androidx.activity.ComponentActivity)?.finish()
                            exitProcess(0)
                        }
                    )
                }
            }

            // Дополнительная информация внизу
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Версия 1.0 • Стратегическая игра в реальном времени",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CompactMenuButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    // ДОРАБОТКА 4: Кнопки с фиксированным размером, не растягиваются
    Button(
        onClick = onClick,
        modifier = Modifier
            .width(250.dp) // Уменьшили фиксированную ширину
            .height(48.dp), // Уменьшили высоту кнопок
        shape = RoundedCornerShape(10.dp), // Немного уменьшили округлость углов
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp, // Уменьшили размер шрифта кнопок
            fontWeight = FontWeight.Bold
        )
    }
}