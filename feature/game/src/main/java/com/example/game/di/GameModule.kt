package com.example.game.di

import com.example.game.domain.GameEngine
import com.example.game.presentation.GameViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val gameModule = module {

    // Domain layer
    single<GameEngine> { GameEngine() }

    // Presentation layer
    viewModel { GameViewModel(gameEngine = get()) }
}