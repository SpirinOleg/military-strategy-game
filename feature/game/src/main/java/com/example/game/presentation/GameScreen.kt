package com.example.game.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.constants.GameConstants
import com.example.common.model.GameState
import com.example.common.model.PlayerSide
import com.example.common.model.Position
import com.example.common.model.UnitType
import com.example.common.model.UnitGaming
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    initialPoints: Int,
    onNavigateBack: () -> Unit,
    viewModel: GameViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialPoints) {
        viewModel.initializeGame(initialPoints)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0f3460))
    ) {
        // Верхняя панель игры
        GameTopBar(
            gameState = uiState.gameState,
            onNavigateBack = onNavigateBack,
            onPause = { viewModel.pauseGame() }
        )

        // Основное игровое поле
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2d4a22))
                .border(2.dp, Color(0xFF4A90E2), RoundedCornerShape(8.dp))
        ) {
            GameField(
                gameState = uiState.gameState,
                onFieldClick = { position -> viewModel.onFieldClick(position) }
            )

            // Оверлей победы/поражения
            if (uiState.gameState.winner != null) {
                GameEndOverlay(
                    winner = uiState.gameState.winner!!,
                    onRestart = { viewModel.restartGame() },
                    onExit = onNavigateBack
                )
            }
        }

        // Нижняя панель с юнитами
        UnitSpawnPanel(
            availableUnits = uiState.availableUnits,
            gameState = uiState.gameState,
            onSpawnUnitGaming = { unitType -> viewModel.spawnUnit(unitType) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameTopBar(
    gameState: GameState,
    onNavigateBack: () -> Unit,
    onPause: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ПОЛЕ БОЯ",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                // Статус управления
                if (!gameState.playerCanControl) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Red
                        )
                    ) {
                        Text(
                            text = "РЛС УНИЧТОЖЕНА!",
                            modifier = Modifier.padding(8.dp),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        navigationIcon = {
            TextButton(onClick = onNavigateBack) {
                Text("← МЕНЮ", color = Color.White)
            }
        },
        actions = {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4A90E2)
                )
            ) {
                Text(
                    text = "Очки: ${gameState.playerPoints}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun GameField(
    gameState: GameState,
    onFieldClick: (Position) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .clickable { /* Handled in Canvas */ }
    ) {
        drawGameField(
            drawScope = this,
            gameState = gameState
        )
    }
}

private fun drawGameField(
    drawScope: DrawScope,
    gameState: GameState
) {
    with(drawScope) {
        val scaleX = size.width / GameConstants.FIELD_WIDTH
        val scaleY = size.height / GameConstants.FIELD_HEIGHT

        // Рисуем сетку поля
        drawGrid(scaleX, scaleY)

        // Рисуем юнитов игрока (синие)
        gameState.playerUnitGamings.forEach { unit ->
            drawUnit(unit, scaleX, scaleY, Color.Blue)
        }

        // Рисуем юнитов противника (красные)
        gameState.enemyUnitGamings.forEach { unit ->
            drawUnit(unit, scaleX, scaleY, Color.Red)
        }

        // Рисуем дальность атаки выбранного юнита
        // TODO: Implement selection logic
    }
}

private fun DrawScope.drawGrid(scaleX: Float, scaleY: Float) {
    val gridSpacing = 100f
    val gridColor = Color.Gray.copy(alpha = 0.3f)

    // Вертикальные линии
    var x = gridSpacing
    while (x < GameConstants.FIELD_WIDTH) {
        drawLine(
            color = gridColor,
            start = Offset(x * scaleX, 0f),
            end = Offset(x * scaleX, size.height),
            strokeWidth = 1.dp.toPx()
        )
        x += gridSpacing
    }

    // Горизонтальные линии
    var y = gridSpacing
    while (y < GameConstants.FIELD_HEIGHT) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y * scaleY),
            end = Offset(size.width, y * scaleY),
            strokeWidth = 1.dp.toPx()
        )
        y += gridSpacing
    }
}

private fun DrawScope.drawUnit(
    unitGaming: UnitGaming,
    scaleX: Float,
    scaleY: Float,
    color: Color
) {
    val x = unitGaming.position.x * scaleX
    val y = unitGaming.position.y * scaleY
    val radius = getUnitRadius(unitGaming.type)

    // Основная фигура юнита
    drawCircle(
        color = color,
        radius = radius * scaleX,
        center = Offset(x, y)
    )

    // Обводка
    drawCircle(
        color = Color.White,
        radius = radius * scaleX,
        center = Offset(x, y),
        style = Stroke(width = 2.dp.toPx())
    )

    // Полоска здоровья
    drawHealthBar(unitGaming, x, y, radius * scaleX)

    // Дальность атаки (для выбранных юнитов)
    if (unitGaming.range > 0) {
        drawCircle(
            color = color.copy(alpha = 0.1f),
            radius = unitGaming.range * scaleX,
            center = Offset(x, y),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

private fun DrawScope.drawHealthBar(unitGaming: UnitGaming, x: Float, y: Float, radius: Float) {
    val barWidth = radius * 2
    val barHeight = 4.dp.toPx()
    val healthPercent = unitGaming.health.toFloat() / unitGaming.maxHealth.toFloat()

    // Фон полоски здоровья
    drawRect(
        color = Color.Red,
        topLeft = Offset(x - barWidth / 2, y - radius - barHeight - 4.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
    )

    // Текущее здоровье
    drawRect(
        color = Color.Green,
        topLeft = Offset(x - barWidth / 2, y - radius - barHeight - 4.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(barWidth * healthPercent, barHeight)
    )
}

private fun getUnitRadius(unitType: UnitType): Float {
    return when (unitType) {
        UnitType.HELICOPTER, UnitType.AIRPLANE -> 20f
        UnitType.TANK, UnitType.BTR, UnitType.BMP -> 25f
        UnitType.FORTIFY_VEHICLE -> 22f
        UnitType.RIFLEMAN, UnitType.MACHINE_GUNNER, UnitType.ROCKET_SOLDIER -> 15f
        UnitType.MISSILE -> 12f
        UnitType.COMMAND_POST -> 35f
        UnitType.RADAR -> 30f
    }
}

// Обновленная панель спавна юнитов
@Composable
private fun UnitSpawnPanel(
    availableUnits: List<UnitType>,
    gameState: GameState,
    onSpawnUnitGaming: (UnitType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp), // Увеличил высоту для подписей
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Заголовок панели
            Text(
                text = "ВЫБЕРИТЕ ЮНИТ ДЛЯ СПАВНА",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(availableUnits) { unitType ->
                    UnitSpawnButton(
                        unitType = unitType,
                        canSpawn = canSpawnUnit(unitType, gameState),
                        onClick = { onSpawnUnitGaming(unitType) }
                    )
                }
            }
        }
    }
}

@Composable
private fun UnitSpawnButton(
    unitType: UnitType,
    canSpawn: Boolean,
    onClick: () -> Unit
) {
    val unitStats = GameConstants.UNIT_STATS[unitType] ?: return

    Card(
        modifier = Modifier
            .width(80.dp) // Увеличил ширину для подписи
            .height(64.dp)
            .clickable(enabled = canSpawn) { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (canSpawn) Color(0xFF4A90E2) else Color.Gray
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = getUnitIcon(unitType),
                fontSize = 18.sp,
                color = Color.White
            )

            // Название юнита
            Text(
                text = getUnitShortName(unitType),
                fontSize = 8.sp,
                color = Color.White,
                maxLines = 1,
                fontWeight = FontWeight.Medium
            )

            // Стоимость
            Text(
                text = "${unitStats.cost}",
                fontSize = 10.sp,
                color = Color.Yellow,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// Добавляем функцию для коротких названий юнитов
private fun getUnitShortName(unitType: UnitType): String {
    return when (unitType) {
        UnitType.HELICOPTER -> "Вертолет"
        UnitType.AIRPLANE -> "Самолет"
        UnitType.TANK -> "Танк"
        UnitType.FORTIFY_VEHICLE -> "Укрепления"
        UnitType.BTR -> "БТР"
        UnitType.BMP -> "БМП"
        UnitType.RIFLEMAN -> "Стрелок"
        UnitType.MACHINE_GUNNER -> "Пулеметчик"
        UnitType.ROCKET_SOLDIER -> "Ракетчик"
        UnitType.MISSILE -> "Ракета"
        UnitType.COMMAND_POST -> "КШМ"
        UnitType.RADAR -> "РЛС"
    }
}

private fun canSpawnUnit(unitType: UnitType, gameState: GameState): Boolean {
    val unitStats = GameConstants.UNIT_STATS[unitType] ?: return false
    return gameState.playerPoints >= unitStats.cost &&
            gameState.isGameActive &&
            gameState.playerCanControl
}

private fun getUnitIcon(unitType: UnitType): String {
    return when (unitType) {
        UnitType.HELICOPTER -> "🚁"
        UnitType.AIRPLANE -> "✈️"
        UnitType.TANK -> "🔥"
        UnitType.FORTIFY_VEHICLE -> "🔧"
        UnitType.BTR -> "🚛"
        UnitType.BMP -> "🚚"
        UnitType.RIFLEMAN -> "🔫"
        UnitType.MACHINE_GUNNER -> "⚡"
        UnitType.ROCKET_SOLDIER -> "🚀"
        UnitType.MISSILE -> "💥"
        UnitType.COMMAND_POST -> "🏢"
        UnitType.RADAR -> "📡"
    }
}

@Composable
private fun GameEndOverlay(
    winner: PlayerSide,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black.copy(alpha = 0.9f)
            )
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (winner == PlayerSide.BLUE) "ПОБЕДА!" else "ПОРАЖЕНИЕ!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (winner == PlayerSide.BLUE) Color.Green else Color.Red
                )

                Text(
                    text = if (winner == PlayerSide.BLUE)
                        "Вы уничтожили силы противника!" else
                        "Враг уничтожил ваши силы!",
                    fontSize = 16.sp,
                    color = Color.White
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onRestart,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("ЗАНОВО")
                    }

                    Button(
                        onClick = onExit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        )
                    ) {
                        Text("ВЫХОД")
                    }
                }
            }
        }
    }
}