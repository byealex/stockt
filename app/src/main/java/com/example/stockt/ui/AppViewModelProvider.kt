package com.example.stockt.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.stockt.StocktApplication

object AppViewModelProvider {
    val Factory = viewModelFactory {
        initializer {
            val stocktApplication = this[APPLICATION_KEY] as StocktApplication
            InventoryViewModel(stocktApplication.stocktRepository)
        }

        initializer {
            val stocktApplication = this[APPLICATION_KEY] as StocktApplication
            ItemEntryViewModel(stocktApplication.stocktRepository)
        }
    }
}