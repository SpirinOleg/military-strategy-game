package com.example.common.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Position(
    val x: Float,
    val y: Float
) : Parcelable

@Serializable
enum class UnitType {
    HELICOPTER,      // Вертолет
    AIRPLANE,        // Самолет
    TANK,           // Танк
    FORTIFY_VEHICLE, // Машина укреплений
    BTR,            // БТР
    BMP,            // БМП
    RIFLEMAN,       // Автоматчик
    MACHINE_GUNNER, // Пулеметчик
    ROCKET_SOLDIER, // Ракетчик
    MISSILE,        // Ракета
    AIR_DEFENSE,    // НОВЫЙ: Зенитная установка
    COMMAND_POST,   // КШМ
    RADAR          // РЛС
}

@Serializable
enum class PlayerSide {
    BLUE,   // Игрок
    RED     // Противник
}

@Serializable
@Parcelize
data class UnitGaming(
    val id: String,
    val type: UnitType,
    val position: Position,
    val health: Int,
    val maxHealth: Int,
    val damage: Int,
    val range: Float,
    val speed: Float,
    val side: PlayerSide,
    val isAlive: Boolean = true,
    val target: String? = null,
    val lastAttackTime: Long = 0
) : Parcelable

@Parcelize
data class UnitStats(
    val type: UnitType,
    val cost: Int,
    val health: Int,
    val damage: Int,
    val range: Float,
    val speed: Float,
    val killReward: Int,
    val description: String
) : Parcelable

@Parcelize
data class GameState(
    val playerUnitGamings: List<UnitGaming>,
    val enemyUnitGamings: List<UnitGaming>,
    val playerPoints: Int,
    val enemyPoints: Int,
    val isGameActive: Boolean,
    val winner: PlayerSide? = null,
    val playerCanControl: Boolean = true, // РЛС работает
    val enemyCanControl: Boolean = true,
    // НОВОЕ: Добавляем ракеты зенитки
    val airDefenseMissiles: List<AirDefenseMissile> = emptyList()
) : Parcelable
// Добавить в GameModels.kt

@Serializable
@Parcelize
data class AirDefenseMissile(
    val id: String,
    val position: Position,
    val targetId: String,
    val targetPosition: Position,
    val shooterId: String, // ID зенитки, которая выпустила ракету
    val speed: Float = 6f, // Быстрые ракеты зенитки
    val damage: Int = 80,
    val side: PlayerSide,
    val creationTime: Long = System.currentTimeMillis()
) : Parcelable