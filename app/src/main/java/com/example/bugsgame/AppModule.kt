package com.example.bugsgame.di

import com.example.bugsgame.GameViewModel
import com.example.bugsgame.data.DatabaseProvider
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { GameViewModel(get()) }
    single { DatabaseProvider.getDatabase(androidContext()).appDao() }
}