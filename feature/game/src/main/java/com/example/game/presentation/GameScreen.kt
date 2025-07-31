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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.constants.GameConstants
import com.example.common.model.GameState
import com.example.common.model.PlayerSide
import com.example.common.model.UnitType
import com.example.common.model.UnitGaming
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    initialPoints: Int,
    purchasedUnits: Map<UnitType, Int> = emptyMap(),
    onNavigateBack: () -> Unit,
    viewModel: GameViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialPoints, purchasedUnits) {
        viewModel.initializeGame(initialPoints, purchasedUnits)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0f3460))
    ) {
        // ИСПРАВЛЕНИЕ: Верхняя панель с кнопкой назад и отображением очков
        GameTopBar(
            gameState = uiState.gameState,
            onNavigateBack = onNavigateBack,
        )

        // Увеличенное игровое поле
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2d4a22))
                .border(2.dp, Color(0xFF4A90E2), RoundedCornerShape(8.dp))
        ) {
            GameField(
                gameState = uiState.gameState,
            )

            if (uiState.gameState.winner != null) {
                GameEndOverlay(
                    winner = uiState.gameState.winner!!,
                    onRestart = { viewModel.restartGame() },
                    onExit = onNavigateBack
                )
            }
        }

        // ДОРАБОТКА 1 и 2: Обновленная нижняя панель
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
) {
    // Верхняя панель с кнопкой назад и отображением очков
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Маленькая кнопка назад
        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier.size(width = 60.dp, height = 32.dp)
        ) {
            Text(
                "← МЕНЮ",
                color = Color.White,
                fontSize = 10.sp
            )
        }

        // Статус управления (центр)
        if (!gameState.playerCanControl) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.Red
                ),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = "РЛС УНИЧТОЖЕНА!",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
        } else {
            // Пустое место для симметрии
            Spacer(modifier = Modifier.width(60.dp))
        }

        // ИСПРАВЛЕНИЕ: Вернули отображение очков справа
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF4A90E2)
            ),
            modifier = Modifier.height(28.dp)
        ) {
            Text(
                text = "Очки: ${gameState.playerPoints}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun GameField(
    gameState: GameState,
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

        drawGrid(scaleX, scaleY)

        gameState.playerUnitGamings.forEach { unit ->
            drawUnit(unit, scaleX, scaleY, Color.Blue)
        }

        gameState.enemyUnitGamings.forEach { unit ->
            drawUnit(unit, scaleX, scaleY, Color.Red)
        }
    }
}

private fun DrawScope.drawGrid(scaleX: Float, scaleY: Float) {
    val gridSpacing = 100f
    val gridColor = Color.Gray.copy(alpha = 0.3f)

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

    // НОВОЕ: Специальное отображение для зенитки в виде пирамиды из квадратиков
    if (unitGaming.type == UnitType.AIR_DEFENSE) {
        drawAirDefenseUnit(x, y, color, scaleX)
    } else {
        drawCircle(
            color = color,
            radius = radius * scaleX,
            center = Offset(x, y)
        )

        drawCircle(
            color = Color.White,
            radius = radius * scaleX,
            center = Offset(x, y),
            style = Stroke(width = 2.dp.toPx())
        )
    }

    drawHealthBar(unitGaming, x, y, radius * scaleX)

    if (unitGaming.range > 0) {
        drawCircle(
            color = color.copy(alpha = 0.1f),
            radius = unitGaming.range * scaleX,
            center = Offset(x, y),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

// НОВАЯ ФУНКЦИЯ: Отрисовка зенитки в виде пирамиды из квадратиков
private fun DrawScope.drawAirDefenseUnit(x: Float, y: Float, color: Color, scale: Float) {
    val squareSize = 8f * scale
    val spacing = 2f * scale

    // Нижний ряд - 2 квадратика
    drawRect(
        color = color,
        topLeft = Offset(x - squareSize - spacing/2, y + spacing),
        size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
    )
    drawRect(
        color = color,
        topLeft = Offset(x + spacing/2, y + spacing),
        size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
    )

    // Верхний квадратик - вершина пирамиды
    drawRect(
        color = color,
        topLeft = Offset(x - squareSize/2, y - squareSize - spacing),
        size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
    )

    // Белые обводки для квадратиков
    drawRect(
        color = Color.White,
        topLeft = Offset(x - squareSize - spacing/2, y + spacing),
        size = androidx.compose.ui.geometry.Size(squareSize, squareSize),
        style = Stroke(width = 1.dp.toPx())
    )
    drawRect(
        color = Color.White,
        topLeft = Offset(x + spacing/2, y + spacing),
        size = androidx.compose.ui.geometry.Size(squareSize, squareSize),
        style = Stroke(width = 1.dp.toPx())
    )
    drawRect(
        color = Color.White,
        topLeft = Offset(x - squareSize/2, y - squareSize - spacing),
        size = androidx.compose.ui.geometry.Size(squareSize, squareSize),
        style = Stroke(width = 1.dp.toPx())
    )
}

private fun DrawScope.drawHealthBar(unitGaming: UnitGaming, x: Float, y: Float, radius: Float) {
    val barWidth = radius * 2
    val barHeight = 4.dp.toPx()
    val healthPercent = unitGaming.health.toFloat() / unitGaming.maxHealth.toFloat()

    drawRect(
        color = Color.Red,
        topLeft = Offset(x - barWidth / 2, y - radius - barHeight - 4.dp.toPx()),
        size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
    )

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
        UnitType.AIR_DEFENSE -> 28f // НОВЫЙ: Зенитка чуть больше танка
        UnitType.COMMAND_POST -> 35f
        UnitType.RADAR -> 30f
    }
}

// ДОРАБОТКА 1 и 2: Обновленная панель спавна
@Composable
private fun UnitSpawnPanel(
    availableUnits: List<UnitType>,
    gameState: GameState,
    onSpawnUnitGaming: (UnitType) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp), // Уменьшили высоту панели с 70dp до 64dp
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 4.dp), // Уменьшили вертикальные отступы
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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

@Composable
private fun UnitSpawnButton(
    unitType: UnitType,
    canSpawn: Boolean,
    onClick: () -> Unit
) {
    val unitStats = GameConstants.UNIT_STATS[unitType] ?: return

    Card(
        modifier = Modifier
            .width(65.dp)
            .height(50.dp)
            .clickable(enabled = canSpawn) { onClick() },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (canSpawn) Color(0xFF4A90E2) else Color.Gray
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-2).dp)
        ) {
            Text(
                text = getUnitIcon(unitType),
                fontSize = 14.sp,
                color = Color.White
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(
                    text = getUnitShortName(unitType),
                    fontSize = 7.sp,
                    color = Color.White,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    lineHeight = 8.sp
                )

                // НОВОЕ: Специальное отображение для ракет
                if (unitType == UnitType.MISSILE) {
                    Text(
                        text = "${unitStats.cost} оч",
                        fontSize = 7.sp, // Уменьшили размер для ракет
                        color = Color.Red, // Красный цвет для дорогих ракет
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 8.sp
                    )
                } else {
                    Text(
                        text = "${unitStats.cost} оч",
                        fontSize = 8.sp,
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 9.sp
                    )
                }
            }
        }
    }
}

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
        UnitType.AIR_DEFENSE -> "Зенитка" // НОВЫЙ: Короткое название
        UnitType.COMMAND_POST -> "КШМ"
        UnitType.RADAR -> "РЛС"
    }
}

private fun canSpawnUnit(unitType: UnitType, gameState: GameState): Boolean {
    val unitStats = GameConstants.UNIT_STATS[unitType] ?: return false

    // Базовые проверки
    val hasEnoughPoints = gameState.playerPoints >= unitStats.cost
    val gameIsActive = gameState.isGameActive
    val canControl = gameState.playerCanControl

    // НОВОЕ: Специальная проверка для ракет - ограничение количества
    if (unitType == UnitType.MISSILE) {
        val currentMissileCount = gameState.playerUnitGamings.count {
            it.type == UnitType.MISSILE && it.isAlive
        }
        val missileLimit = currentMissileCount < GameConstants.MAX_MISSILES_PER_PLAYER

        return hasEnoughPoints && gameIsActive && canControl && missileLimit
    }

    return hasEnoughPoints && gameIsActive && canControl
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
        UnitType.AIR_DEFENSE -> "🎯" // НОВЫЙ: Значок мишени для зенитки
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