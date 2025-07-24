package com.example.common.constants

import com.example.common.model.Position
import com.example.common.model.UnitStats
import com.example.common.model.UnitType

object GameConstants {
    // Игровые настройки
    const val INITIAL_POINTS = 1000
    const val FIELD_WIDTH = 1200f
    const val FIELD_HEIGHT = 800f

    // Позиции базовых строений
    val PLAYER_COMMAND_POST_POSITION = Position(100f, FIELD_HEIGHT - 100f)
    val PLAYER_RADAR_POSITION = Position(200f, FIELD_HEIGHT - 100f)
    val ENEMY_COMMAND_POST_POSITION = Position(FIELD_WIDTH - 100f, 100f)
    val ENEMY_RADAR_POSITION = Position(FIELD_WIDTH - 200f, 100f)

    // Характеристики юнитов
    val UNIT_STATS = mapOf(
        UnitType.HELICOPTER to UnitStats(
            type = UnitType.HELICOPTER,
            cost = 150,
            health = 100,
            damage = 40,
            range = 200f,
            speed = 3f,
            killReward = 75,
            description = "Быстрый вертолет с хорошей огневой мощью"
        ),
        UnitType.AIRPLANE to UnitStats(
            type = UnitType.AIRPLANE,
            cost = 200,
            health = 80,
            damage = 100,
            range = 300f,
            speed = 5f,
            killReward = 100,
            description = "Одноразовый авиаудар с большим уроном"
        ),
        UnitType.TANK to UnitStats(
            type = UnitType.TANK,
            cost = 180,
            health = 200,
            damage = 60,
            range = 250f,
            speed = 1.5f,
            killReward = 90,
            description = "Тяжелая бронированная техника"
        ),
        UnitType.FORTIFY_VEHICLE to UnitStats(
            type = UnitType.FORTIFY_VEHICLE,
            cost = 100,
            health = 120,
            damage = 0,
            range = 0f,
            speed = 2f,
            killReward = 50,
            description = "Машина для укрепления базовых строений"
        ),
        UnitType.BTR to UnitStats(
            type = UnitType.BTR,
            cost = 120,
            health = 150,
            damage = 35,
            range = 180f,
            speed = 2.5f,
            killReward = 60,
            description = "Бронетransporter с хорошей защитой"
        ),
        UnitType.BMP to UnitStats(
            type = UnitType.BMP,
            cost = 140,
            health = 130,
            damage = 45,
            range = 200f,
            speed = 2.2f,
            killReward = 70,
            description = "Боевая машина пехоты"
        ),
        UnitType.RIFLEMAN to UnitStats(
            type = UnitType.RIFLEMAN,
            cost = 30,
            health = 50,
            damage = 20,
            range = 120f,
            speed = 2f,
            killReward = 15,
            description = "Легкая пехота с автоматом"
        ),
        UnitType.MACHINE_GUNNER to UnitStats(
            type = UnitType.MACHINE_GUNNER,
            cost = 50,
            health = 60,
            damage = 25,
            range = 150f,
            speed = 1.8f,
            killReward = 25,
            description = "Пехотинец с пулеметом"
        ),
        UnitType.ROCKET_SOLDIER to UnitStats(
            type = UnitType.ROCKET_SOLDIER,
            cost = 80,
            health = 70,
            damage = 50,
            range = 220f,
            speed = 1.5f,
            killReward = 40,
            description = "Солдат с противотанковой ракетой"
        ),
        UnitType.MISSILE to UnitStats(
            type = UnitType.MISSILE,
            cost = 60,
            health = 30,
            damage = 80,
            range = 300f,
            speed = 4f,
            killReward = 30,
            description = "Управляемая ракета"
        ),
        UnitType.COMMAND_POST to UnitStats(
            type = UnitType.COMMAND_POST,
            cost = 0,
            health = 300,
            damage = 0,
            range = 0f,
            speed = 0f,
            killReward = 0,
            description = "Командно-штабная машина"
        ),
        UnitType.RADAR to UnitStats(
            type = UnitType.RADAR,
            cost = 0,
            health = 150,
            damage = 0,
            range = 0f,
            speed = 0f,
            killReward = 0,
            description = "РЛС для управления войсками"
        )
    )

    // Настройки игры
    const val ATTACK_COOLDOWN = 1000L // мс между атаками
    const val AIRPLANE_LIFETIME = 5000L // время жизни самолета
    const val FORTIFICATION_BONUS_HEALTH = 100 // бонус здоровья от укреплений

    // Настройки AI
    const val AI_SPAWN_PROBABILITY = 0.02f // вероятность спавна врага за тик
    const val AI_MIN_POINTS_TO_SPAWN = 100 // минимум очков для спавна
}