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

    fun initializeGame(initialPoints: Int) {
        val playerCommandPost = com.example.common.model.UnitGaming(
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

        val initialGameState = GameState(
            playerUnitGamings = listOf(playerCommandPost, playerRadar),
            enemyUnitGamings = listOf(enemyCommandPost, enemyRadar),
            playerPoints = initialPoints,
            enemyPoints = GameConstants.INITIAL_POINTS,
            isGameActive = true,
            playerCanControl = true,
            enemyCanControl = true
        )

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
            UnitType.MISSILE
        )

        _uiState.value = _uiState.value.copy(
            gameState = initialGameState,
            availableUnits = availableUnits
        )

        startGameLoop()
    }

    fun spawnUnit(unitType: UnitType) {
        val currentState = _uiState.value.gameState
        if (!currentState.isGameActive || !currentState.playerCanControl) return

        val unitStats = GameConstants.UNIT_STATS[unitType] ?: return
        if (currentState.playerPoints < unitStats.cost) return

        // Создаем новый юнит рядом с КШМ игрока с большей зоной разброса для увеличенного поля
        val spawnPosition = Position(
            x = GameConstants.PLAYER_COMMAND_POST_POSITION.x + 100f + (Math.random().toFloat() * 200f),
            y = GameConstants.PLAYER_COMMAND_POST_POSITION.y - 100f + (Math.random().toFloat() * 200f)
        )

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

    private fun moveUnit(unitGaming: com.example.common.model.UnitGaming, targetPosition: Position) {
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

    private suspend fun updateGame() {
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