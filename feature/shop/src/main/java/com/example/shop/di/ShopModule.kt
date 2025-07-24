package com.example.shop.di

import com.example.shop.presentation.ShopViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val shopModule = module {
    viewModel { ShopViewModel() }
}