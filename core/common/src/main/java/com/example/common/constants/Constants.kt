package com.example.common.constants

import com.example.common.model.Position
import com.example.common.model.UnitStats
import com.example.common.model.UnitType

object GameConstants {
    // Игровые настройки - УВЕЛИЧЕННОЕ ПОЛЕ
    const val INITIAL_POINTS = 1000
    const val FIELD_WIDTH = 2000f  // Было 1200f, увеличил до 2000f
    const val FIELD_HEIGHT = 1200f // Было 800f, увеличил до 1200f

    // Позиции базовых строений - обновленные под новые размеры
    val PLAYER_COMMAND_POST_POSITION = Position(150f, FIELD_HEIGHT - 150f)
    val PLAYER_RADAR_POSITION = Position(300f, FIELD_HEIGHT - 150f)
    val ENEMY_COMMAND_POST_POSITION = Position(FIELD_WIDTH - 150f, 150f)
    val ENEMY_RADAR_POSITION = Position(FIELD_WIDTH - 300f, 150f)

    // НОВОЕ: Ограничения на юниты
    const val MAX_MISSILES_PER_PLAYER = 3 // Максимум 3 ракеты на игрока

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
        // ИЗМЕНЕНО: Ракета теперь стоит 300 очков вместо 60
        UnitType.MISSILE to UnitStats(
            type = UnitType.MISSILE,
            cost = 300, // Увеличили стоимость с 60 до 300
            health = 30,
            damage = 80,
            range = 300f,
            speed = 4f,
            killReward = 30,
            description = "Управляемая ракета (макс. 3 шт.)"
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
        ),
        // НОВЫЙ ЮНИТ: Зенитная установка для перехвата ракет
        UnitType.AIR_DEFENSE to UnitStats(
            type = UnitType.AIR_DEFENSE,
            cost = 120,
            health = 80,
            damage = 60,
            range = 250f,
            speed = 0f, // Неподвижная установка
            killReward = 60,
            description = "Зенитная установка для перехвата ракет и самолетов"
        )
    )

    // Настройки игры (без изменений)
    const val ATTACK_COOLDOWN = 1000L // мс между атаками
    const val AIRPLANE_LIFETIME = 5000L // время жизни самолета
    const val FORTIFICATION_BONUS_HEALTH = 100 // бонус здоровья от укреплений

    // Настройки AI (без изменений)
    const val AI_SPAWN_PROBABILITY = 0.02f // вероятность спавна врага за тик
    const val AI_MIN_POINTS_TO_SPAWN = 100 // минимум очков для спавна
}