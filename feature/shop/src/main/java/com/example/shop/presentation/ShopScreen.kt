package com.example.shop.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.common.model.UnitStats
import com.example.common.model.UnitType
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onNavigateBack: () -> Unit,
    onStartGame: (Int, Map<UnitType, Int>) -> Unit, // Изменили сигнатуру
    viewModel: ShopViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
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
            )
    ) {
        // Верхняя панель
        TopAppBar(
            title = {
                Text(
                    text = "МАГАЗИН ВОЙСК",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                TextButton(onClick = onNavigateBack) {
                    Text("← НАЗАД", color = Color.White)
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
                        text = "Очки: ${uiState.points}",
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

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Магазин юнитов
            Card(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ДОСТУПНАЯ ТЕХНИКА",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.availableUnits) { unitStats ->
                            UnitCard(
                                unitStats = unitStats,
                                quantity = uiState.purchasedUnits[unitStats.type] ?: 0,
                                canAfford = uiState.points >= unitStats.cost,
                                onPurchase = { viewModel.purchaseUnit(unitStats.type) },
                                onSell = { viewModel.sellUnit(unitStats.type) }
                            )
                        }
                    }
                }
            }

            // Боковая панель с информацией - ИСПРАВЛЕНА ПРОБЛЕМА СКРОЛЛИНГА
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "АРМИЯ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Список купленных юнитов с прокруткой
                    LazyColumn(
                        modifier = Modifier.weight(1f), // Занимает оставшееся место
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            uiState.purchasedUnits.filter { it.value > 0 }.entries.toList()
                        ) { (unitType, quantity) ->
                            ArmyUnitItem(
                                unitType = unitType,
                                quantity = quantity
                            )
                        }
                    }

                    // Статистика и кнопка всегда внизу
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Всего единиц: ${uiState.purchasedUnits.values.sum()}",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )

                        // Кнопка начать игру
                        Button(
                            onClick = {
                                onStartGame(uiState.points, uiState.purchasedUnits) // Передаем данные о покупках
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = uiState.purchasedUnits.values.sum() > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text(
                                text = "НАЧАТЬ БОЙ",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitCard(
    unitStats: UnitStats,
    quantity: Int,
    canAfford: Boolean,
    onPurchase: () -> Unit,
    onSell: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .border(
                width = 1.dp,
                color = if (canAfford) Color(0xFF4CAF50) else Color.Gray,
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2d2d2d)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = getUnitName(unitStats.type),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = "${unitStats.cost} оч.",
                    fontSize = 10.sp,
                    color = Color(0xFF4A90E2)
                )
                Text(
                    text = "Здоровье: ${unitStats.health}",
                    fontSize = 8.sp,
                    color = Color.Gray
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (quantity > 0) {
                    TextButton(
                        onClick = onSell,
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text("-", color = Color.Red, fontSize = 14.sp)
                    }
                }

                Text(
                    text = quantity.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = onPurchase,
                    enabled = canAfford,
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text(
                        "+",
                        color = if (canAfford) Color.Green else Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ArmyUnitItem(
    unitType: UnitType,
    quantity: Int
) {
    Card(
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3d3d3d)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = getUnitName(unitType),
                color = Color.White,
                fontSize = 12.sp
            )
            Text(
                text = "x$quantity",
                color = Color(0xFF4A90E2),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun getUnitName(unitType: UnitType): String {
    return when (unitType) {
        UnitType.HELICOPTER -> "Вертолет"
        UnitType.AIRPLANE -> "Самолет"
        UnitType.TANK -> "Танк"
        UnitType.FORTIFY_VEHICLE -> "Укрепления"
        UnitType.BTR -> "БТР"
        UnitType.BMP -> "БМП"
        UnitType.RIFLEMAN -> "Автоматчик"
        UnitType.MACHINE_GUNNER -> "Пулеметчик"
        UnitType.ROCKET_SOLDIER -> "Ракетчик"
        UnitType.MISSILE -> "Ракета"
        UnitType.COMMAND_POST -> "КШМ"
        UnitType.RADAR -> "РЛС"
        UnitType.AIR_DEFENSE -> "Зенитная установка"
    }
}