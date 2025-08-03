package com.example.game.domain

import com.example.common.constants.GameConstants
import com.example.common.model.GameState
import com.example.common.model.PlayerSide
import com.example.common.model.Position
import com.example.common.model.UnitType
import com.example.common.model.AirDefenseMissile
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

        // 3. НОВОЕ: Обновляем ракеты зенитки
        updatedState = updateAirDefenseMissiles(updatedState)

        // 4. Обрабатываем атаки (теперь включая создание ракет зенитки)
        updatedState = processAttacks(updatedState)

        // 5. Удаляем мертвых юнитов и начисляем награды
        updatedState = cleanupDeadUnits(updatedState)

        // 6. Обновляем статусы управления (РЛС)
        updatedState = updateControlStatus(updatedState)

        // 7. Обрабатываем специальную логику (самолеты, укрепления)
        updatedState = processSpecialUnits(updatedState)

        return updatedState
    }

    // НОВАЯ ФУНКЦИЯ: Обновление ракет зенитки
    private fun updateAirDefenseMissiles(gameState: GameState): GameState {
        val updatedMissiles = mutableListOf<AirDefenseMissile>()
        var updatedPlayerUnits = gameState.playerUnitGamings.toMutableList()
        var updatedEnemyUnits = gameState.enemyUnitGamings.toMutableList()

        gameState.airDefenseMissiles.forEach { missile ->
            // Находим цель
            val allTargets = if (missile.side == PlayerSide.BLUE) updatedEnemyUnits else updatedPlayerUnits
            val target = allTargets.find { it.id == missile.targetId && it.isAlive }

            if (target != null) {
                // Вычисляем новую позицию ракеты
                val distanceToTarget = calculateDistance(missile.position, target.position)

                if (distanceToTarget <= 15f) {
                    // ПОПАДАНИЕ! Наносим урон цели
                    val updatedTarget = target.copy(
                        health = maxOf(0, target.health - missile.damage),
                        isAlive = target.health - missile.damage > 0
                    )

                    if (missile.side == PlayerSide.BLUE) {
                        val targetIndex = updatedEnemyUnits.indexOfFirst { it.id == target.id }
                        if (targetIndex != -1) {
                            updatedEnemyUnits[targetIndex] = updatedTarget
                        }
                    } else {
                        val targetIndex = updatedPlayerUnits.indexOfFirst { it.id == target.id }
                        if (targetIndex != -1) {
                            updatedPlayerUnits[targetIndex] = updatedTarget
                        }
                    }
                    // Ракета уничтожается при попадании
                } else {
                    // Продолжаем движение к цели
                    val direction = calculateDirection(missile.position, target.position)
                    val newPosition = Position(
                        x = missile.position.x + direction.x * missile.speed,
                        y = missile.position.y + direction.y * missile.speed
                    )

                    updatedMissiles.add(
                        missile.copy(
                            position = newPosition,
                            targetPosition = target.position // Обновляем позицию цели
                        )
                    )
                }
            }
            // Если цель уничтожена или не найдена, ракета исчезает
        }

        return gameState.copy(
            playerUnitGamings = updatedPlayerUnits,
            enemyUnitGamings = updatedEnemyUnits,
            airDefenseMissiles = updatedMissiles
        )
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
            UnitType.AIR_DEFENSE // AI тоже может создавать зенитки
        ).filter {
            val cost = GameConstants.UNIT_STATS[it]?.cost ?: Int.MAX_VALUE
            val canAfford = gameState.enemyPoints >= cost

            // Ограничение на зенитку для AI тоже
            if (it == UnitType.AIR_DEFENSE) {
                val hasAirDefense = gameState.enemyUnitGamings.any { unit ->
                    unit.type == UnitType.AIR_DEFENSE && unit.isAlive
                }
                canAfford && !hasAirDefense
            } else {
                canAfford
            }
        }

        if (availableUnits.isEmpty()) return gameState

        val selectedUnitType = availableUnits.random()
        val unitStats = GameConstants.UNIT_STATS[selectedUnitType] ?: return gameState

        // Специальное размещение для зенитки противника
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

        // УЛУЧШЕННАЯ ЛОГИКА ЗЕНИТКИ: Приоритетное поражение всех воздушных целей
        if (unitGaming.type == UnitType.AIR_DEFENSE) {
            val enemies = if (unitGaming.side == PlayerSide.BLUE) gameState.enemyUnitGamings else gameState.playerUnitGamings

            // СТРОГИЙ ПРИОРИТЕТ: Сначала ракеты (самые опасные), потом самолеты, потом вертолеты
            val missiles = enemies.filter { it.isAlive && it.type == UnitType.MISSILE }
            val airplanes = enemies.filter { it.isAlive && it.type == UnitType.AIRPLANE }
            val helicopters = enemies.filter { it.isAlive && it.type == UnitType.HELICOPTER }

            // Ищем ближайшую ракету в радиусе поражения
            val nearestMissile = missiles
                .filter { calculateDistance(unitGaming.position, it.position) <= unitGaming.range }
                .minByOrNull { calculateDistance(unitGaming.position, it.position) }

            if (nearestMissile != null) {
                println("DEBUG: Air defense ${unitGaming.id} targeting missile ${nearestMissile.id}")
                return unitGaming.copy(target = nearestMissile.id)
            }

            // Если нет ракет, ищем ближайший самолет в радиусе поражения
            val nearestAirplane = airplanes
                .filter { calculateDistance(unitGaming.position, it.position) <= unitGaming.range }
                .minByOrNull { calculateDistance(unitGaming.position, it.position) }

            if (nearestAirplane != null) {
                println("DEBUG: Air defense ${unitGaming.id} targeting airplane ${nearestAirplane.id}")
                return unitGaming.copy(target = nearestAirplane.id)
            }

            // Если нет самолетов, ищем ближайший вертолет в радиусе поражения
            val nearestHelicopter = helicopters
                .filter { calculateDistance(unitGaming.position, it.position) <= unitGaming.range }
                .minByOrNull { calculateDistance(unitGaming.position, it.position) }

            if (nearestHelicopter != null) {
                println("DEBUG: Air defense ${unitGaming.id} targeting helicopter ${nearestHelicopter.id}")
                return unitGaming.copy(target = nearestHelicopter.id)
            }

            // Если нет воздушных целей в радиусе, атакуем ближайшего наземного врага
            val nearestGroundEnemy = enemies
                .filter { it.isAlive && it.type != UnitType.MISSILE && it.type != UnitType.AIRPLANE && it.type != UnitType.HELICOPTER }
                .filter { calculateDistance(unitGaming.position, it.position) <= unitGaming.range }
                .minByOrNull { calculateDistance(unitGaming.position, it.position) }

            if (nearestGroundEnemy != null) {
                println("DEBUG: Air defense ${unitGaming.id} targeting ground unit ${nearestGroundEnemy.id}")
                return unitGaming.copy(target = nearestGroundEnemy.id)
            }

            // Если нет целей в радиусе, сбрасываем цель
            return unitGaming.copy(target = null)
        }

        // Остальная логика для других юнитов остается без изменений...
        val enemies = if (unitGaming.side == PlayerSide.BLUE) gameState.enemyUnitGamings else gameState.playerUnitGamings

        // Сначала ищем цели, которых можем атаковать
        val attackableEnemies = enemies.filter { enemy ->
            enemy.isAlive && canAttackTarget(unitGaming, enemy)
        }

        val nearestAttackableEnemy = attackableEnemies.minByOrNull {
            calculateDistance(unitGaming.position, it.position)
        }

        if (nearestAttackableEnemy != null) {
            val distanceToEnemy = calculateDistance(unitGaming.position, nearestAttackableEnemy.position)

            if (distanceToEnemy <= unitGaming.range) {
                // Можем атаковать - останавливаемся и атакуем
                return unitGaming.copy(target = nearestAttackableEnemy.id)
            } else {
                // Двигаемся к цели, которую можем атаковать
                val direction = calculateDirection(unitGaming.position, nearestAttackableEnemy.position)
                val newPosition = Position(
                    x = (unitGaming.position.x + direction.x * unitGaming.speed).coerceIn(0f, GameConstants.FIELD_WIDTH),
                    y = (unitGaming.position.y + direction.y * unitGaming.speed).coerceIn(0f, GameConstants.FIELD_HEIGHT)
                )

                return unitGaming.copy(
                    position = newPosition,
                    target = nearestAttackableEnemy.id
                )
            }
        }

        // Если нет атакуемых целей, продолжаем движение к любому врагу
        val anyNearestEnemy = findNearestEnemy(unitGaming, enemies)
        if (anyNearestEnemy != null) {
            // Двигаемся к любому врагу, даже если не можем его атаковать
            val direction = calculateDirection(unitGaming.position, anyNearestEnemy.position)
            val newPosition = Position(
                x = (unitGaming.position.x + direction.x * unitGaming.speed).coerceIn(0f, GameConstants.FIELD_WIDTH),
                y = (unitGaming.position.y + direction.y * unitGaming.speed).coerceIn(0f, GameConstants.FIELD_HEIGHT)
            )

            return unitGaming.copy(
                position = newPosition,
                target = null // Не устанавливаем цель, если не можем атаковать
            )
        }

        return unitGaming
    }

    // НОВАЯ ВСПОМОГАТЕЛЬНАЯ ФУНКЦИЯ: Проверка возможности атаки цели (без учета дистанции и кулдауна)
    private fun canAttackTarget(attacker: UnitGaming, target: UnitGaming): Boolean {
        if (!target.isAlive) return false

        // Зенитки могут атаковать любые цели
        if (attacker.type == UnitType.AIR_DEFENSE) {
            return true
        }

        // Воздушные цели могут быть сбиты зенитками, самолетами и вертолетами
        if (target.type == UnitType.MISSILE || target.type == UnitType.AIRPLANE || target.type == UnitType.HELICOPTER) {
            return attacker.type == UnitType.AIR_DEFENSE ||
                    attacker.type == UnitType.AIRPLANE ||
                    attacker.type == UnitType.HELICOPTER
        }

        // Все юниты могут атаковать наземные цели
        return true
    }
    private fun processAttacks(gameState: GameState): GameState {
        val currentTime = System.currentTimeMillis()

        var updatedPlayerUnits = gameState.playerUnitGamings.toMutableList()
        var updatedEnemyUnits = gameState.enemyUnitGamings.toMutableList()
        var updatedMissiles = gameState.airDefenseMissiles.toMutableList()

        // Обрабатываем атаки игроков
        updatedPlayerUnits.forEachIndexed { index, unit ->
            if (unit.isAlive && unit.damage > 0 && unit.target != null) {
                val target = updatedEnemyUnits.find { it.id == unit.target }
                if (target != null && canAttack(unit, target, currentTime)) {

                    // ЗЕНИТКА: создает ракету вместо мгновенного урона
                    if (unit.type == UnitType.AIR_DEFENSE) {
                        val distance = calculateDistance(unit.position, target.position)
                        println("DEBUG: Player air defense attacking ${target.type.name} at distance $distance (max range: ${unit.range})")

                        val newMissile = AirDefenseMissile(
                            id = generateId(),
                            position = unit.position.copy(),
                            targetId = target.id,
                            targetPosition = target.position.copy(),
                            shooterId = unit.id,
                            side = unit.side
                        )
                        updatedMissiles.add(newMissile)
                        updatedPlayerUnits[index] = unit.copy(lastAttackTime = currentTime)

                        println("DEBUG: Created air defense missile ${newMissile.id} targeting ${target.type.name}")
                    } else {
                        // Обычная атака для других юнитов
                        val updatedTarget = target.copy(
                            health = maxOf(0, target.health - unit.damage),
                            isAlive = target.health - unit.damage > 0
                        )

                        val targetIndex = updatedEnemyUnits.indexOfFirst { it.id == target.id }
                        if (targetIndex != -1) {
                            updatedEnemyUnits[targetIndex] = updatedTarget
                        }

                        // Ракета и самолет уничтожаются сразу после атаки
                        if (unit.type == UnitType.MISSILE || unit.type == UnitType.AIRPLANE) {
                            updatedPlayerUnits[index] = unit.copy(
                                lastAttackTime = currentTime,
                                isAlive = false
                            )
                        } else {
                            updatedPlayerUnits[index] = unit.copy(lastAttackTime = currentTime)
                        }
                    }
                }
            }
        }

        // Обрабатываем атаки противника (аналогично)
        updatedEnemyUnits.forEachIndexed { index, unit ->
            if (unit.isAlive && unit.damage > 0 && unit.target != null) {
                val target = updatedPlayerUnits.find { it.id == unit.target }
                if (target != null && canAttack(unit, target, currentTime)) {

                    // ЗЕНИТКА ПРОТИВНИКА: тоже создает ракету
                    if (unit.type == UnitType.AIR_DEFENSE) {
                        val distance = calculateDistance(unit.position, target.position)
                        println("DEBUG: Enemy air defense attacking ${target.type.name} at distance $distance (max range: ${unit.range})")

                        val newMissile = AirDefenseMissile(
                            id = generateId(),
                            position = unit.position.copy(),
                            targetId = target.id,
                            targetPosition = target.position.copy(),
                            shooterId = unit.id,
                            side = unit.side
                        )
                        updatedMissiles.add(newMissile)
                        updatedEnemyUnits[index] = unit.copy(lastAttackTime = currentTime)

                        println("DEBUG: Created enemy air defense missile ${newMissile.id} targeting ${target.type.name}")
                    } else {
                        // Обычная атака для других юнитов
                        val updatedTarget = target.copy(
                            health = maxOf(0, target.health - unit.damage),
                            isAlive = target.health - unit.damage > 0
                        )

                        val targetIndex = updatedPlayerUnits.indexOfFirst { it.id == target.id }
                        if (targetIndex != -1) {
                            updatedPlayerUnits[targetIndex] = updatedTarget
                        }

                        // Ракета и самолет противника тоже уничтожаются после атаки
                        if (unit.type == UnitType.MISSILE || unit.type == UnitType.AIRPLANE) {
                            updatedEnemyUnits[index] = unit.copy(
                                lastAttackTime = currentTime,
                                isAlive = false
                            )
                        } else {
                            updatedEnemyUnits[index] = unit.copy(lastAttackTime = currentTime)
                        }
                    }
                }
            }
        }

        return gameState.copy(
            playerUnitGamings = updatedPlayerUnits,
            enemyUnitGamings = updatedEnemyUnits,
            airDefenseMissiles = updatedMissiles
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
        // Убрал логику времени жизни самолетов, так как они теперь уничтожаются сразу после атаки
        return gameState
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

    // ПРОВЕРЕННАЯ ФУНКЦИЯ: Может ли юнит атаковать цель
    private fun canAttack(attacker: UnitGaming, target: UnitGaming, currentTime: Long): Boolean {
        val distance = calculateDistance(attacker.position, target.position)
        val canReachTarget = distance <= attacker.range
        val cooldownPassed = currentTime - attacker.lastAttackTime >= GameConstants.ATTACK_COOLDOWN

        // Базовые условия для атаки
        if (!canReachTarget || !cooldownPassed || !target.isAlive) {
            return false
        }

        // ЗЕНИТКА: может атаковать ВСЕ цели в радиусе поражения
        if (attacker.type == UnitType.AIR_DEFENSE) {
            println("DEBUG: Air defense can attack ${target.type.name} at distance $distance")
            return true
        }

        // ВОЗДУШНЫЕ ЦЕЛИ: могут быть сбиты зенитками, самолетами и вертолетами
        if (target.type == UnitType.MISSILE || target.type == UnitType.AIRPLANE || target.type == UnitType.HELICOPTER) {
            val canInterceptAircraft = attacker.type == UnitType.AIR_DEFENSE ||
                    attacker.type == UnitType.AIRPLANE ||
                    attacker.type == UnitType.HELICOPTER
            return canInterceptAircraft
        }

        // Все остальные юниты могут атаковать наземные цели
        return true
    }

    private fun generateId(): String {
        return "unit_${System.currentTimeMillis()}_${Random.nextInt(1000)}"
    }
}