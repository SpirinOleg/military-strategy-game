package com.example.menu.di

import com.example.menu.presentation.MenuViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val menuModule = module {
    viewModel { MenuViewModel() }
}