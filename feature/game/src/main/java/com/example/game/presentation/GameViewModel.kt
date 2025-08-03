package com.example.game.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.constants.GameConstants
import com.example.common.model.GameState
import com.example.common.model.PlayerSide
import com.example.common.model.Position
import com.example.common.model.UnitType
import com.example.common.model.UnitGaming
import com.example.game.domain.GameEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

data class GameUiState(
    val gameState: GameState = GameState(
        playerUnitGamings = emptyList(),
        enemyUnitGamings = emptyList(),
        playerPoints = GameConstants.INITIAL_POINTS,
        enemyPoints = GameConstants.INITIAL_POINTS,
        isGameActive = false
    ),
    val availableUnits: List<UnitType> = emptyList(),
    val selectedUnitGaming: UnitGaming? = null,
    val isPaused: Boolean = false
)

class GameViewModel(
    private val gameEngine: GameEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var gameLoopJob: Job? = null

    // ИСПРАВЛЕННАЯ функция инициализации с поддержкой покупок
    fun initializeGame(initialPoints: Int, purchasedUnits: Map<UnitType, Int> = emptyMap()) {
        println("DEBUG: Initializing game with points: $initialPoints, units: $purchasedUnits")

        val playerCommandPost = UnitGaming(
            id = UUID.randomUUID().toString(),
            type = UnitType.COMMAND_POST,
            position = GameConstants.PLAYER_COMMAND_POST_POSITION,
            health = GameConstants.UNIT_STATS[UnitType.COMMAND_POST]?.health ?: 300,
            maxHealth = GameConstants.UNIT_STATS[UnitType.COMMAND_POST]?.health ?: 300,
            damage = 0,
            range = 0f,
            speed = 0f,
            side = PlayerSide.BLUE
        )

        val playerRadar = UnitGaming(
            id = UUID.randomUUID().toString(),
            type = UnitType.RADAR,
            position = GameConstants.PLAYER_RADAR_POSITION,
            health = GameConstants.UNIT_STATS[UnitType.RADAR]?.health ?: 150,
            maxHealth = GameConstants.UNIT_STATS[UnitType.RADAR]?.health ?: 150,
            damage = 0,
            range = 0f,
            speed = 0f,
            side = PlayerSide.BLUE
        )

        // СОЗДАЕМ ЮНИТОВ ИЗ ПОКУПОК - ИСПРАВЛЕНИЕ ПРОБЛЕМЫ 1
        val purchasedPlayerUnits = mutableListOf<UnitGaming>()
        val baseUnits = listOf(playerCommandPost, playerRadar) // Для проверки коллизий

        purchasedUnits.forEach { (unitType, quantity) ->
            repeat(quantity) {
                val unitStats = GameConstants.UNIT_STATS[unitType] ?: return@repeat

                // НОВАЯ ЛОГИКА: Специальное размещение для зенитки
                val spawnPosition = if (unitType == UnitType.AIR_DEFENSE) {
                    findAirDefensePosition(baseUnits + purchasedPlayerUnits)
                } else {
                    // Обычная логика для других юнитов
                    Position(
                        x = GameConstants.PLAYER_COMMAND_POST_POSITION.x + 100f + (Math.random().toFloat() * 200f),
                        y = GameConstants.PLAYER_COMMAND_POST_POSITION.y - 100f + (Math.random().toFloat() * 200f)
                    )
                }

                val newUnit = UnitGaming(
                    id = UUID.randomUUID().toString(),
                    type = unitType,
                    position = spawnPosition,
                    health = unitStats.health,
                    maxHealth = unitStats.health,
                    damage = unitStats.damage,
                    range = unitStats.range,
                    speed = unitStats.speed,
                    side = PlayerSide.BLUE
                )

                purchasedPlayerUnits.add(newUnit)
                println("DEBUG: Created purchased unit: ${unitType.name} at (${spawnPosition.x}, ${spawnPosition.y})")
            }
        }

        val enemyCommandPost = UnitGaming(
            id = UUID.randomUUID().toString(),
            type = UnitType.COMMAND_POST,
            position = GameConstants.ENEMY_COMMAND_POST_POSITION,
            health = GameConstants.UNIT_STATS[UnitType.COMMAND_POST]?.health ?: 300,
            maxHealth = GameConstants.UNIT_STATS[UnitType.COMMAND_POST]?.health ?: 300,
            damage = 0,
            range = 0f,
            speed = 0f,
            side = PlayerSide.RED
        )

        val enemyRadar = UnitGaming(
            id = UUID.randomUUID().toString(),
            type = UnitType.RADAR,
            position = GameConstants.ENEMY_RADAR_POSITION,
            health = GameConstants.UNIT_STATS[UnitType.RADAR]?.health ?: 150,
            maxHealth = GameConstants.UNIT_STATS[UnitType.RADAR]?.health ?: 150,
            damage = 0,
            range = 0f,
            speed = 0f,
            side = PlayerSide.RED
        )

        // Объединяем базовые строения с купленными юнитами
        val allPlayerUnits = listOf(playerCommandPost, playerRadar) + purchasedPlayerUnits

        println("DEBUG: Total player units created: ${allPlayerUnits.size}")
        allPlayerUnits.forEach { unit ->
            println("DEBUG: Player unit: ${unit.type.name} at position (${unit.position.x}, ${unit.position.y})")
        }

        val initialGameState = GameState(
            playerUnitGamings = allPlayerUnits,
            enemyUnitGamings = listOf(enemyCommandPost, enemyRadar),
            playerPoints = initialPoints,
            enemyPoints = GameConstants.INITIAL_POINTS,
            isGameActive = true,
            playerCanControl = true,
            enemyCanControl = true
        )

        // ИСПРАВЛЕНИЕ: Добавили зенитную установку в список доступных юнитов
        val availableUnits = listOf(
            UnitType.HELICOPTER,
            UnitType.AIRPLANE,
            UnitType.TANK,
            UnitType.FORTIFY_VEHICLE,
            UnitType.BTR,
            UnitType.BMP,
            UnitType.RIFLEMAN,
            UnitType.MACHINE_GUNNER,
            UnitType.ROCKET_SOLDIER,
            UnitType.MISSILE,
            UnitType.AIR_DEFENSE // НОВЫЙ: Зенитная установка
        )

        _uiState.value = _uiState.value.copy(
            gameState = initialGameState,
            availableUnits = availableUnits
        )

        startGameLoop()
    }

    // ИСПРАВЛЕННАЯ ФУНКЦИЯ: Размещение зенитки строго перед РЛС игрока
    private fun findAirDefensePosition(existingUnits: List<UnitGaming>): Position {
        val radarPos = GameConstants.PLAYER_RADAR_POSITION

        // Зенитка размещается перед РЛС игрока (ближе к центру поля)
        // РЛС игрока находится в левом нижнем углу, значит "перед" = правее и выше

        val preferredPositions = listOf(
            // Основная позиция - прямо перед РЛС на расстоянии 80-120 пикселей
            Position(radarPos.x + 100f, radarPos.y - 80f),
            Position(radarPos.x + 120f, radarPos.y - 60f),
            Position(radarPos.x + 80f, radarPos.y - 100f),

            // Альтернативные позиции перед РЛС
            Position(radarPos.x + 100f, radarPos.y - 120f),
            Position(radarPos.x + 140f, radarPos.y - 80f),
            Position(radarPos.x + 80f, radarPos.y - 60f),

            // Дополнительные позиции в секторе перед РЛС
            Position(radarPos.x + 90f, radarPos.y - 110f),
            Position(radarPos.x + 110f, radarPos.y - 70f),
            Position(radarPos.x + 130f, radarPos.y - 100f)
        )

        // Выбираем первую свободную позицию
        for (position in preferredPositions) {
            if (isPositionValid(position, existingUnits)) {
                println("DEBUG: Air defense placed at (${position.x}, ${position.y}) in front of radar")
                return position
            }
        }

        // Если все предпочтительные позиции заняты, ищем в секторе перед РЛС
        var attempts = 0
        while (attempts < 30) {
            // Генерируем позицию в секторе перед РЛС
            val angle = Math.random() * Math.PI / 3 - Math.PI / 6 // Сектор ±30° от направления "вперед"
            val distance = 80f + Math.random().toFloat() * 60f // От 80 до 140 пикселей от РЛС

            val position = Position(
                x = (radarPos.x + Math.cos(angle) * distance).toFloat(),
                y = (radarPos.y - Math.sin(angle + Math.PI/4) * distance).toFloat() // Смещение вперед-вправо
            )

            if (isPositionValid(position, existingUnits)) {
                println("DEBUG: Air defense placed at random position (${position.x}, ${position.y}) in front of radar")
                return position
            }
            attempts++
        }

        // В крайнем случае размещаем на фиксированной позиции перед РЛС
        val fallbackPosition = Position(radarPos.x + 100f, radarPos.y - 80f)
        println("DEBUG: Air defense placed at fallback position (${fallbackPosition.x}, ${fallbackPosition.y})")
        return fallbackPosition
    }

    // УЛУЧШЕННАЯ ФУНКЦИЯ: Проверка валидности позиции
    private fun isPositionValid(position: Position, existingUnits: List<UnitGaming>): Boolean {
        // Проверяем, что позиция в пределах поля
        if (position.x < 50f || position.x > GameConstants.FIELD_WIDTH - 50f ||
            position.y < 50f || position.y > GameConstants.FIELD_HEIGHT - 50f) {
            return false
        }

        // Проверяем минимальное расстояние до других юнитов (60 пикселей)
        val minDistance = 60f
        return existingUnits.none { unit ->
            val dx = unit.position.x - position.x
            val dy = unit.position.y - position.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            distance < minDistance
        }
    }

    // НОВАЯ ФУНКЦИЯ: Проверка свободности позиции
    private fun isPositionFree(position: Position, existingUnits: List<UnitGaming>): Boolean {
        val minDistance = 50f // Минимальное расстояние между юнитами
        return existingUnits.none { unit ->
            val dx = unit.position.x - position.x
            val dy = unit.position.y - position.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            distance < minDistance
        }
    }
    fun spawnUnit(unitType: UnitType) {
        val currentState = _uiState.value.gameState
        if (!currentState.isGameActive || !currentState.playerCanControl) return

        val unitStats = GameConstants.UNIT_STATS[unitType] ?: return
        if (currentState.playerPoints < unitStats.cost) return

        // Проверка ограничения на зенитки
        if (unitType == UnitType.AIR_DEFENSE) {
            val currentAirDefenseCount = currentState.playerUnitGamings.count {
                it.type == UnitType.AIR_DEFENSE && it.isAlive
            }
            if (currentAirDefenseCount >= GameConstants.MAX_AIR_DEFENSE_PER_PLAYER) return
        }

        // Проверка ограничения на ракеты
        if (unitType == UnitType.MISSILE) {
            val currentMissileCount = currentState.playerUnitGamings.count {
                it.type == UnitType.MISSILE && it.isAlive
            }
            if (currentMissileCount >= GameConstants.MAX_MISSILES_PER_PLAYER) return
        }

        // ИСПРАВЛЕННОЕ РАЗМЕЩЕНИЕ: Зенитка строится перед РЛС
        val spawnPosition = if (unitType == UnitType.AIR_DEFENSE) {
            findAirDefensePosition(currentState.playerUnitGamings)
        } else {
            // Обычная логика для других юнитов
            Position(
                x = GameConstants.PLAYER_COMMAND_POST_POSITION.x + 100f + (Math.random().toFloat() * 200f),
                y = GameConstants.PLAYER_COMMAND_POST_POSITION.y - 100f + (Math.random().toFloat() * 200f)
            )
        }

        val newUnitGaming = UnitGaming(
            id = UUID.randomUUID().toString(),
            type = unitType,
            position = spawnPosition,
            health = unitStats.health,
            maxHealth = unitStats.health,
            damage = unitStats.damage,
            range = unitStats.range,
            speed = unitStats.speed,
            side = PlayerSide.BLUE
        )

        val updatedPlayerUnits = currentState.playerUnitGamings + newUnitGaming
        val updatedGameState = currentState.copy(
            playerUnitGamings = updatedPlayerUnits,
            playerPoints = currentState.playerPoints - unitStats.cost
        )

        _uiState.value = _uiState.value.copy(gameState = updatedGameState)

        println("DEBUG: Spawned ${unitType.name} at position (${spawnPosition.x}, ${spawnPosition.y})")
    }

    fun onFieldClick(position: Position) {
        val currentState = _uiState.value.gameState
        if (!currentState.isGameActive) return

        // Логика выбора юнита или перемещения
        val clickedUnit = findUnitAtPosition(position, currentState.playerUnitGamings)

        if (clickedUnit != null) {
            // Выбираем юнит
            _uiState.value = _uiState.value.copy(selectedUnitGaming = clickedUnit)
        } else if (_uiState.value.selectedUnitGaming != null) {
            // Перемещаем выбранный юнит
            moveUnit(_uiState.value.selectedUnitGaming!!, position)
        }
    }

    private fun moveUnit(unitGaming: UnitGaming, targetPosition: Position) {
        val currentState = _uiState.value.gameState
        val updatedUnits = currentState.playerUnitGamings.map { u ->
            if (u.id == unitGaming.id) {
                u.copy(position = targetPosition)
            } else {
                u
            }
        }

        val updatedGameState = currentState.copy(playerUnitGamings = updatedUnits)
        _uiState.value = _uiState.value.copy(
            gameState = updatedGameState,
            selectedUnitGaming = null
        )
    }

    private fun findUnitAtPosition(position: Position, unitGamings: List<UnitGaming>): UnitGaming? {
        val clickRadiusSquared = 30.0 * 30.0 // 900
        return unitGamings.find { unit ->
            val deltaX = unit.position.x - position.x
            val deltaY = unit.position.y - position.y
            val distanceSquared = deltaX * deltaX + deltaY * deltaY
            distanceSquared < clickRadiusSquared
        }
    }

    fun pauseGame() {
        _uiState.value = _uiState.value.copy(isPaused = !_uiState.value.isPaused)
    }

    fun restartGame() {
        gameLoopJob?.cancel()
        initializeGame(GameConstants.INITIAL_POINTS)
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            while (isActive && _uiState.value.gameState.isGameActive) {
                if (!_uiState.value.isPaused) {
                    updateGame()
                }
                delay(50) // 20 FPS
            }
        }
    }

    private fun updateGame() {
        val currentState = _uiState.value.gameState
        val updatedState = gameEngine.updateGameState(currentState)

        _uiState.value = _uiState.value.copy(gameState = updatedState)

        // Проверяем условия победы/поражения
        checkGameEnd(updatedState)
    }

    private fun checkGameEnd(gameState: GameState) {
        val playerCommandPost = gameState.playerUnitGamings.find { it.type == UnitType.COMMAND_POST }
        val enemyCommandPost = gameState.enemyUnitGamings.find { it.type == UnitType.COMMAND_POST }

        when {
            playerCommandPost?.isAlive != true -> {
                // Поражение игрока
                val endedState = gameState.copy(
                    isGameActive = false,
                    winner = PlayerSide.RED
                )
                _uiState.value = _uiState.value.copy(gameState = endedState)
            }
            enemyCommandPost?.isAlive != true -> {
                // Победа игрока
                val endedState = gameState.copy(
                    isGameActive = false,
                    winner = PlayerSide.BLUE
                )
                _uiState.value = _uiState.value.copy(gameState = endedState)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        gameLoopJob?.cancel()
    }
}