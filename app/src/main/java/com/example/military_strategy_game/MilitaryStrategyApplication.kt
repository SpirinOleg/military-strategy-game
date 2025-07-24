package com.example.military_strategy_game

import android.app.Application
import com.example.common.di.commonModule
import com.example.game.di.gameModule
import com.example.menu.di.menuModule
import com.example.shop.di.shopModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MilitaryStrategyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MilitaryStrategyApplication)
            modules(
                commonModule,
                menuModule,
                shopModule,
                gameModule
            )
        }
    }
}