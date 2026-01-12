package com.example.stockt.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockt.data.Item
import com.example.stockt.data.ShelfWithItems
import com.example.stockt.data.StocktRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(private val repository: StocktRepository) : ViewModel() {

    private val storageUnitId = 1

    val uiState: StateFlow<InventoryUiState> =
        repository.getShelvesForStorageUnit(storageUnitId)
            .map { InventoryUiState(shelves = it) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = InventoryUiState()
            )

    fun createShelf(shelfName: String) {
        if (shelfName.isBlank()) return

        viewModelScope.launch {
            // 1. Ensure Fridge exists (Safe guard)
            repository.createDefaultFridge()

            // 2. Create the Shelf
            repository.createShelf(
                com.example.stockt.data.Shelf(
                    name = shelfName,
                    storageId = 1 // Hardcoded Fridge #1
                )
            )
        }
    }

    fun deleteItem(item: Item) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }
}

data class InventoryUiState(
    val shelves: List<ShelfWithItems> = listOf()
)

