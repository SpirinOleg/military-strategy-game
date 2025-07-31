package com.example.game.domain

import com.example.common.constants.GameConstants
import com.example.common.model.GameState
import com.example.common.model.PlayerSide
import com.example.common.model.Position
import com.example.common.model.UnitType
import kotlin.math.*
import kotlin.random.Random
import com.example.common.model.UnitGaming

class GameEngine {

    fun updateGameState(gameState: GameState): GameState {
        if (!gameState.isGameActive) return gameState

        // 1. Обновляем AI противника
        var updatedState = updateEnemyAI(gameState)

        // 2. Обновляем перемещения всех юнитов
        updatedState = updateUnitMovement(updatedState)

        // 3. Обрабатываем атаки
        updatedState = processAttacks(updatedState)

        // 4. Удаляем мертвых юнитов и начисляем награды
        updatedState = cleanupDeadUnits(updatedState)

        // 5. Обновляем статусы управления (РЛС)
        updatedState = updateControlStatus(updatedState)

        // 6. Обрабатываем специальную логику (самолеты, укрепления)
        updatedState = processSpecialUnits(updatedState)

        return updatedState
    }

    private fun updateEnemyAI(gameState: GameState): GameState {
        // Простой AI: каждые 3-5 секунд спавнит случайный юнит
        if (Random.nextFloat() < 0.02f && gameState.enemyPoints > 100) {
            return spawnEnemyUnit(gameState)
        }
        return gameState
    }

    private fun spawnEnemyUnit(gameState: GameState): GameState {
        val availableUnits = listOf(
            UnitType.TANK,
            UnitType.HELICOPTER,
            UnitType.BTR,
            UnitType.BMP,
            UnitType.RIFLEMAN,
            UnitType.MACHINE_GUNNER,
            UnitType.ROCKET_SOLDIER,
            UnitType.AIR_DEFENSE // НОВОЕ: AI тоже может создавать зенитки
        ).filter {
            val cost = GameConstants.UNIT_STATS[it]?.cost ?: Int.MAX_VALUE
            gameState.enemyPoints >= cost
        }

        if (availableUnits.isEmpty()) return gameState

        val selectedUnitType = availableUnits.random()
        val unitStats = GameConstants.UNIT_STATS[selectedUnitType] ?: return gameState

        // ОБНОВЛЕННАЯ ЛОГИКА: Специальное размещение для зенитки противника
        val spawnPosition = if (selectedUnitType == UnitType.AIR_DEFENSE) {
            findEnemyAirDefensePosition(gameState.enemyUnitGamings)
        } else {
            // Обычная логика для других юнитов
            Position(
                x = GameConstants.ENEMY_COMMAND_POST_POSITION.x - 100f - (Random.nextFloat() * 200f),
                y = GameConstants.ENEMY_COMMAND_POST_POSITION.y + 100f - (Random.nextFloat() * 200f)
            )
        }

        val newUnitGaming = UnitGaming(
            id = generateId(),
            type = selectedUnitType,
            position = spawnPosition,
            health = unitStats.health,
            maxHealth = unitStats.health,
            damage = unitStats.damage,
            range = unitStats.range,
            speed = unitStats.speed,
            side = PlayerSide.RED
        )

        return gameState.copy(
            enemyUnitGamings = gameState.enemyUnitGamings + newUnitGaming,
            enemyPoints = gameState.enemyPoints - unitStats.cost
        )
    }

    // НОВАЯ ФУНКЦИЯ: Поиск подходящей позиции для зенитки противника
    private fun findEnemyAirDefensePosition(existingUnits: List<UnitGaming>): Position {
        val commandPostPos = GameConstants.ENEMY_COMMAND_POST_POSITION
        val radarPos = GameConstants.ENEMY_RADAR_POSITION

        // Пробуем разместить зенитку в нескольких позициях вокруг КШМ и РЛС противника
        val candidatePositions = listOf(
            // Вокруг КШМ противника
            Position(commandPostPos.x + 80f, commandPostPos.y - 80f),
            Position(commandPostPos.x - 80f, commandPostPos.y - 80f),
            Position(commandPostPos.x + 80f, commandPostPos.y + 80f),
            Position(commandPostPos.x - 80f, commandPostPos.y + 80f),
            // Вокруг РЛС противника
            Position(radarPos.x + 80f, radarPos.y - 80f),
            Position(radarPos.x - 80f, radarPos.y - 80f),
            Position(radarPos.x + 80f, radarPos.y + 80f),
            Position(radarPos.x - 80f, radarPos.y + 80f),
            // Между КШМ и РЛС противника
            Position((commandPostPos.x + radarPos.x) / 2f, (commandPostPos.y + radarPos.y) / 2f - 60f),
            Position((commandPostPos.x + radarPos.x) / 2f, (commandPostPos.y + radarPos.y) / 2f + 60f)
        )

        // Выбираем первую свободную позицию
        for (position in candidatePositions) {
            if (isPositionFree(position, existingUnits)) {
                return position
            }
        }

        // Если все позиции заняты, ищем случайную рядом с КШМ противника
        var attempts = 0
        while (attempts < 20) {
            val angle = Random.nextDouble() * 2 * PI
            val distance = 60f + Random.nextFloat() * 40f // От 60 до 100 пикселей от КШМ
            val position = Position(
                x = (commandPostPos.x + cos(angle) * distance).toFloat(),
                y = (commandPostPos.y + sin(angle) * distance).toFloat()
            )

            if (isPositionFree(position, existingUnits) &&
                position.x >= 0f && position.x <= GameConstants.FIELD_WIDTH &&
                position.y >= 0f && position.y <= GameConstants.FIELD_HEIGHT) {
                return position
            }
            attempts++
        }

        // В крайнем случае возвращаем позицию рядом с КШМ противника
        return Position(commandPostPos.x - 60f, commandPostPos.y + 60f)
    }

    // НОВАЯ ФУНКЦИЯ: Проверка свободности позиции
    private fun isPositionFree(position: Position, existingUnits: List<UnitGaming>): Boolean {
        val minDistance = 50f // Минимальное расстояние между юнитами
        return existingUnits.none { unit ->
            val dx = unit.position.x - position.x
            val dy = unit.position.y - position.y
            val distance = sqrt(dx * dx + dy * dy)
            distance < minDistance
        }
    }

    private fun updateUnitMovement(gameState: GameState): GameState {
        val updatedPlayerUnits = gameState.playerUnitGamings.map { unit ->
            moveUnitTowardsTarget(unit, gameState)
        }

        val updatedEnemyUnits = gameState.enemyUnitGamings.map { unit ->
            moveUnitTowardsTarget(unit, gameState)
        }

        return gameState.copy(
            playerUnitGamings = updatedPlayerUnits,
            enemyUnitGamings = updatedEnemyUnits
        )
    }

    private fun moveUnitTowardsTarget(unitGaming: UnitGaming, gameState: GameState): UnitGaming {
        if (unitGaming.speed == 0f || !unitGaming.isAlive) return unitGaming

        // НОВОЕ: Зенитная установка неподвижна, но может атаковать
        if (unitGaming.type == UnitType.AIR_DEFENSE) {
            // Ищем приоритетные воздушные цели (ракеты и самолеты)
            val enemies = if (unitGaming.side == PlayerSide.BLUE) gameState.enemyUnitGamings else gameState.playerUnitGamings
            val airTargets = enemies.filter {
                it.isAlive && (it.type == UnitType.MISSILE || it.type == UnitType.AIRPLANE)
            }

            val nearestAirTarget = airTargets.minByOrNull {
                calculateDistance(unitGaming.position, it.position)
            }

            if (nearestAirTarget != null) {
                val distanceToTarget = calculateDistance(unitGaming.position, nearestAirTarget.position)
                if (distanceToTarget <= unitGaming.range) {
                    return unitGaming.copy(target = nearestAirTarget.id)
                }
            }

            // Если нет воздушных целей, атакуем ближайшего врага
            val nearestEnemy = findNearestEnemy(unitGaming, enemies)
            if (nearestEnemy != null) {
                val distanceToEnemy = calculateDistance(unitGaming.position, nearestEnemy.position)
                if (distanceToEnemy <= unitGaming.range) {
                    return unitGaming.copy(target = nearestEnemy.id)
                }
            }

            return unitGaming
        }

        // Находим ближайшего врага для обычных юнитов
        val enemies = if (unitGaming.side == PlayerSide.BLUE) gameState.enemyUnitGamings else gameState.playerUnitGamings
        val nearestEnemy = findNearestEnemy(unitGaming, enemies) ?: return unitGaming

        // Если враг в радиусе атаки, не двигаемся
        val distanceToEnemy = calculateDistance(unitGaming.position, nearestEnemy.position)
        if (distanceToEnemy <= unitGaming.range) {
            return unitGaming.copy(target = nearestEnemy.id)
        }

        // Двигаемся к врагу
        val direction = calculateDirection(unitGaming.position, nearestEnemy.position)
        val newPosition = Position(
            x = (unitGaming.position.x + direction.x * unitGaming.speed).coerceIn(0f, GameConstants.FIELD_WIDTH),
            y = (unitGaming.position.y + direction.y * unitGaming.speed).coerceIn(0f, GameConstants.FIELD_HEIGHT)
        )

        return unitGaming.copy(
            position = newPosition,
            target = nearestEnemy.id
        )
    }

    private fun processAttacks(gameState: GameState): GameState {
        val currentTime = System.currentTimeMillis()

        var updatedPlayerUnits = gameState.playerUnitGamings.toMutableList()
        var updatedEnemyUnits = gameState.enemyUnitGamings.toMutableList()

        // Обрабатываем атаки игроков
        updatedPlayerUnits.forEachIndexed { index, unit ->
            if (unit.isAlive && unit.damage > 0 && unit.target != null) {
                val target = updatedEnemyUnits.find { it.id == unit.target }
                if (target != null && canAttack(unit, target, currentTime)) {
                    val updatedTarget = target.copy(
                        health = maxOf(0, target.health - unit.damage),
                        isAlive = target.health - unit.damage > 0
                    )

                    val targetIndex = updatedEnemyUnits.indexOfFirst { it.id == target.id }
                    if (targetIndex != -1) {
                        updatedEnemyUnits[targetIndex] = updatedTarget
                    }

                    // ДОРАБОТКА 3: Ракета уничтожается сразу после атаки
                    if (unit.type == UnitType.MISSILE) {
                        updatedPlayerUnits[index] = unit.copy(
                            lastAttackTime = currentTime,
                            isAlive = false // Ракета уничтожается после попадания
                        )
                    } else {
                        updatedPlayerUnits[index] = unit.copy(lastAttackTime = currentTime)
                    }
                }
            }
        }

        // Обрабатываем атаки противника
        updatedEnemyUnits.forEachIndexed { index, unit ->
            if (unit.isAlive && unit.damage > 0 && unit.target != null) {
                val target = updatedPlayerUnits.find { it.id == unit.target }
                if (target != null && canAttack(unit, target, currentTime)) {
                    val updatedTarget = target.copy(
                        health = maxOf(0, target.health - unit.damage),
                        isAlive = target.health - unit.damage > 0
                    )

                    val targetIndex = updatedPlayerUnits.indexOfFirst { it.id == target.id }
                    if (targetIndex != -1) {
                        updatedPlayerUnits[targetIndex] = updatedTarget
                    }

                    // ДОРАБОТКА 3: Ракета противника тоже уничтожается после атаки
                    if (unit.type == UnitType.MISSILE) {
                        updatedEnemyUnits[index] = unit.copy(
                            lastAttackTime = currentTime,
                            isAlive = false // Ракета уничтожается после попадания
                        )
                    } else {
                        updatedEnemyUnits[index] = unit.copy(lastAttackTime = currentTime)
                    }
                }
            }
        }

        return gameState.copy(
            playerUnitGamings = updatedPlayerUnits,
            enemyUnitGamings = updatedEnemyUnits
        )
    }

    private fun cleanupDeadUnits(gameState: GameState): GameState {
        val alivePlayers = gameState.playerUnitGamings.filter { it.isAlive }
        val aliveEnemies = gameState.enemyUnitGamings.filter { it.isAlive }

        // Начисляем награды за убитых врагов
        val killedEnemies = gameState.enemyUnitGamings.filter { !it.isAlive }
        val rewardPoints = killedEnemies.sumOf {
            GameConstants.UNIT_STATS[it.type]?.killReward ?: 0
        }

        val killedPlayers = gameState.playerUnitGamings.filter { !it.isAlive }
        val enemyRewardPoints = killedPlayers.sumOf {
            GameConstants.UNIT_STATS[it.type]?.killReward ?: 0
        }

        return gameState.copy(
            playerUnitGamings = alivePlayers,
            enemyUnitGamings = aliveEnemies,
            playerPoints = gameState.playerPoints + rewardPoints,
            enemyPoints = gameState.enemyPoints + enemyRewardPoints
        )
    }

    private fun updateControlStatus(gameState: GameState): GameState {
        val playerRadarAlive = gameState.playerUnitGamings.any { it.type == UnitType.RADAR && it.isAlive }
        val enemyRadarAlive = gameState.enemyUnitGamings.any { it.type == UnitType.RADAR && it.isAlive }

        return gameState.copy(
            playerCanControl = playerRadarAlive,
            enemyCanControl = enemyRadarAlive
        )
    }

    private fun processSpecialUnits(gameState: GameState): GameState {
        // Обрабатываем самолеты (исчезают через время)
        val currentTime = System.currentTimeMillis()

        val updatedPlayerUnits = gameState.playerUnitGamings.map { unit ->
            if (unit.type == UnitType.AIRPLANE) {
                // Самолеты живут ограниченное время
                val timeSinceSpawn = currentTime - (unit.lastAttackTime - GameConstants.AIRPLANE_LIFETIME)
                if (timeSinceSpawn > GameConstants.AIRPLANE_LIFETIME) {
                    unit.copy(isAlive = false)
                } else {
                    unit
                }
            } else {
                unit
            }
        }

        val updatedEnemyUnits = gameState.enemyUnitGamings.map { unit ->
            if (unit.type == UnitType.AIRPLANE) {
                val timeSinceSpawn = currentTime - (unit.lastAttackTime - GameConstants.AIRPLANE_LIFETIME)
                if (timeSinceSpawn > GameConstants.AIRPLANE_LIFETIME) {
                    unit.copy(isAlive = false)
                } else {
                    unit
                }
            } else {
                unit
            }
        }

        return gameState.copy(
            playerUnitGamings = updatedPlayerUnits,
            enemyUnitGamings = updatedEnemyUnits
        )
    }

    private fun findNearestEnemy(unitGaming: UnitGaming, enemies: List<UnitGaming>): UnitGaming? {
        return enemies
            .filter { it.isAlive }
            .minByOrNull { calculateDistance(unitGaming.position, it.position) }
    }

    private fun calculateDistance(pos1: Position, pos2: Position): Float {
        return sqrt((pos1.x - pos2.x).pow(2) + (pos1.y - pos2.y).pow(2))
    }

    private fun calculateDirection(from: Position, to: Position): Position {
        val distance = calculateDistance(from, to)
        if (distance == 0f) return Position(0f, 0f)

        return Position(
            x = (to.x - from.x) / distance,
            y = (to.y - from.y) / distance
        )
    }

    private fun canAttack(attacker: UnitGaming, target: UnitGaming, currentTime: Long): Boolean {
        val distance = calculateDistance(attacker.position, target.position)
        val canReachTarget = distance <= attacker.range
        val cooldownPassed = currentTime - attacker.lastAttackTime >= GameConstants.ATTACK_COOLDOWN

        // НОВАЯ ЛОГИКА: Ракеты могут быть сбиты только зенитками или самолетами
        if (target.type == UnitType.MISSILE) {
            val canInterceptMissile = attacker.type == UnitType.AIR_DEFENSE || attacker.type == UnitType.AIRPLANE
            return canReachTarget && cooldownPassed && target.isAlive && canInterceptMissile
        }

        return canReachTarget && cooldownPassed && target.isAlive
    }

    private fun generateId(): String {
        return "unit_${System.currentTimeMillis()}_${Random.nextInt(1000)}"
    }
}